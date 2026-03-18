const CACHE_NAME        = 'karakter-galerisi-cache-v7';
const IMAGE_CACHE_NAME  = 'karakter-images-v6';

// Core app shell cached on install
const urlsToCache = [
    './',
    './index.html',
    './index2.html',
    './oyun.html',
    './rastgelekarakter.html',
    './links.html',
    './manifest.json'
];

// ─── Install ──────────────────────────────────────────────────────────────────
self.addEventListener('install', event => {
    event.waitUntil(
        caches.open(CACHE_NAME)
            .then(cache => cache.addAll(urlsToCache))
    );
    self.skipWaiting();
});

// ─── Activate — clean up old caches ─────────────────────────────────────────
self.addEventListener('activate', event => {
    const keep = new Set([CACHE_NAME, IMAGE_CACHE_NAME]);
    event.waitUntil(
        caches.keys().then(names =>
            Promise.all(
                names.filter(n => !keep.has(n)).map(n => caches.delete(n))
            )
        ).then(() => clients.claim())
    );
});

// ─── URL normalization helper ────────────────────────────────────────────────
function stripCacheBust(rawUrl) {
    return rawUrl
        .replace(/[?&]_t=\d+/g, '')
        .replace(/\?&/, '?')
        .replace(/\?$/, '');
}

// ─── Fetch — cache-first for images, network-first for pages ────────────────
self.addEventListener('fetch', event => {
    // Image requests: cache-first
    if (event.request.destination === 'image') {
        const cleanUrl = stripCacheBust(event.request.url);

        event.respondWith(
            caches.open(IMAGE_CACHE_NAME).then(imgCache =>
                imgCache.match(cleanUrl).then(cached => {
                    if (cached) return cached;
                    return fetch(event.request).then(networkResponse => {
                        if (networkResponse.ok) {
                            imgCache.put(cleanUrl, networkResponse.clone());
                        }
                        return networkResponse;
                    }).catch(() => cached || new Response('', { status: 404 }));
                })
            )
        );
        return;
    }

    // Navigation requests: network-first with cache fallback
    if (event.request.mode === 'navigate') {
        event.respondWith(
            fetch(event.request).then(response => {
                const clone = response.clone();
                caches.open(CACHE_NAME).then(cache => cache.put(event.request, clone));
                return response;
            }).catch(() => caches.match(event.request))
        );
        return;
    }

    // Everything else: cache → network
    event.respondWith(
        caches.match(event.request).then(response => response || fetch(event.request))
    );
});

// ─── Broadcast to ALL clients ───────────────────────────────────────────────
async function broadcast(msg) {
    const allClients = await self.clients.matchAll({ type: 'window' });
    for (const client of allClients) {
        try { client.postMessage(msg); } catch (_) {}
    }
}

// ─── Messages from pages ──────────────────────────────────────────────────────
self.addEventListener('message', event => {
    const { type, urls } = event.data || {};

    if (type === 'PRECACHE_IMAGES') {
        event.waitUntil(precacheImages(urls));
        return;
    }

    if (type === 'CLEAR_IMAGE_CACHE') {
        event.waitUntil(
            caches.delete(IMAGE_CACHE_NAME).then(() => {
                broadcast({ type: 'CACHE_CLEARED' });
                if (urls && urls.length) return precacheImages(urls);
            })
        );
        return;
    }
});

// ─── Fetch with AbortController timeout ─────────────────────────────────────
function fetchWithTimeout(url, options, timeoutMs) {
    const controller = new AbortController();
    const timer = setTimeout(() => controller.abort(), timeoutMs);

    return fetch(url, { ...options, signal: controller.signal })
        .finally(() => clearTimeout(timer));
}

// ─── Precache — fast parallel batches for imgbb CDN ─────────────────────────
async function precacheImages(urls) {
    if (!urls || !urls.length) return;
    const cache = await caches.open(IMAGE_CACHE_NAME);

    let done = 0;
    let skipped = 0;
    const total = urls.length;
    const failed = [];

    const BATCH      = 10;     // imgbb CDN handles concurrency well
    const TIMEOUT_MS = 12000;  // 12s per image
    const DELAY_MS   = 100;    // minimal pause between batches

    const startTime = Date.now();

    function report(extra) {
        broadcast({
            type: 'CACHE_PROGRESS',
            done, total, skipped,
            failedCount: failed.length,
            elapsedMs: Date.now() - startTime,
            ...extra
        });
    }

    for (let i = 0; i < urls.length; i += BATCH) {
        const batch = urls.slice(i, i + BATCH);
        const batchStart = Date.now();

        report({ currentBatch: batch, batchIndex: i });

        await Promise.allSettled(batch.map(async url => {
            const fetchStart = Date.now();
            try {
                // Skip if already cached
                const existing = await cache.match(url);
                if (existing && (existing.ok || existing.type === 'opaque')) {
                    done++;
                    skipped++;
                    return;
                }
                if (existing) await cache.delete(url);

                const response = await fetchWithTimeout(url, {}, TIMEOUT_MS);

                if (response.ok) {
                    const blob = await response.blob();
                    await cache.put(url, new Response(blob, {
                        status: response.status,
                        statusText: response.statusText,
                        headers: response.headers,
                    }));
                    done++;
                } else {
                    done++;
                    failed.push({ url, reason: `HTTP ${response.status}`, durationMs: Date.now() - fetchStart });
                }
            } catch (e) {
                done++;
                let reason;
                if (e.name === 'AbortError') {
                    reason = `timeout (${Math.round(TIMEOUT_MS / 1000)}s)`;
                } else if (e.name === 'QuotaExceededError') {
                    reason = 'storage full';
                } else {
                    reason = e.message || e.name || 'network error';
                }
                failed.push({ url, reason, durationMs: Date.now() - fetchStart });
                console.warn('[SW] Failed:', url, reason);
            }
        }));

        report({ batchDurationMs: Date.now() - batchStart });

        if (i + BATCH < urls.length) {
            await new Promise(r => setTimeout(r, DELAY_MS));
        }
    }

    broadcast({
        type: 'CACHE_COMPLETE',
        total, skipped,
        failed,
        elapsedMs: Date.now() - startTime
    });
}
