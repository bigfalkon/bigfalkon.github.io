const CACHE_NAME        = 'karakter-galerisi-cache-v5';
const IMAGE_CACHE_NAME  = 'karakter-images-v4';

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
    const url = new URL(event.request.url);

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

// ─── Broadcast to ALL clients (robust — doesn't rely on event.source) ───────
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

// ─── Precache — smaller batches, no opaque caching, inter-batch delay ───────
async function precacheImages(urls) {
    if (!urls || !urls.length) return;
    const cache = await caches.open(IMAGE_CACHE_NAME);

    let done = 0;
    let skipped = 0;
    const total = urls.length;
    const failed = [];

    // ── Tuning ──────────────────────────────────────────────────────────────
    const BATCH      = 4;       // reduced from 10 → avoids HTTP/2 stream congestion
    const TIMEOUT_MS = 15000;   // 15 s per image (was 20 s)
    const DELAY_MS   = 250;     // pause between batches so the browser can breathe

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
                // Skip if already cached with a valid (non-opaque) response
                const existing = await cache.match(url);
                if (existing && existing.ok) {
                    done++;
                    skipped++;
                    return;
                }
                // Delete stale or opaque cached entries before re-fetching
                if (existing) {
                    await cache.delete(url);
                }

                const response = await fetchWithTimeout(url, {}, TIMEOUT_MS);

                if (response.ok) {
                    // Read the *full body* before caching so a half-downloaded
                    // response never gets stored and blocks future retries.
                    const blob = await response.blob();
                    const headers = new Headers(response.headers);
                    await cache.put(url, new Response(blob, {
                        status: response.status,
                        statusText: response.statusText,
                        headers,
                    }));
                    done++;
                } else {
                    done++;
                    failed.push({ url, reason: `HTTP ${response.status}`, durationMs: Date.now() - fetchStart });
                    console.warn('[SW] Bad response:', url, response.status);
                }
                // *** REMOVED: no-cors opaque fallback ***
                // Opaque responses consume ~7 MB of padded quota each and quickly
                // fill storage. Firebase Storage supports CORS so this was unnecessary.
            } catch(e) {
                done++;
                let reason;
                if (e.name === 'AbortError') {
                    reason = `timeout (${Math.round(TIMEOUT_MS / 1000)}s)`;
                } else if (e.name === 'TypeError') {
                    reason = 'network error';
                } else if (e.name === 'QuotaExceededError') {
                    reason = 'storage full';
                } else {
                    reason = e.message || e.name || 'unknown';
                }
                failed.push({ url, reason, durationMs: Date.now() - fetchStart });
                console.warn('[SW] Failed to cache:', url, reason, `(${Date.now() - fetchStart}ms)`);
            }
        }));

        const batchDuration = Date.now() - batchStart;
        report({ batchDurationMs: batchDuration });

        // Small delay between batches to prevent HTTP/2 stream congestion
        // and give the browser's network stack time to clean up connections
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
