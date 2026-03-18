const CACHE_NAME        = 'karakter-galerisi-cache-v4';
const IMAGE_CACHE_NAME  = 'karakter-images-v3';

// Core app shell cached on install
const urlsToCache = [
    './',
    './index.html',
    './index2.html',
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
                        // Only cache valid, non-opaque responses
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

// ─── Messages from pages ──────────────────────────────────────────────────────
self.addEventListener('message', event => {
    const { type, urls } = event.data || {};

    // PRECACHE_IMAGES — store a batch of image URLs into IMAGE_CACHE_NAME
    if (type === 'PRECACHE_IMAGES') {
        event.waitUntil(precacheImages(urls, event.source));
        return;
    }

    // CLEAR_IMAGE_CACHE — wipe IMAGE_CACHE_NAME then optionally re-precache
    if (type === 'CLEAR_IMAGE_CACHE') {
        event.waitUntil(
            caches.delete(IMAGE_CACHE_NAME).then(() => {
                if (event.source) {
                    event.source.postMessage({ type: 'CACHE_CLEARED' });
                }
                // Re-precache if URLs were supplied
                if (urls && urls.length) return precacheImages(urls, event.source);
            })
        );
        return;
    }
});

// ─── Fetch with timeout ──────────────────────────────────────────────────────
function fetchWithTimeout(url, options, timeoutMs) {
    return new Promise((resolve, reject) => {
        const controller = new AbortController();
        const timer = setTimeout(() => {
            controller.abort();
            reject(new Error('timeout'));
        }, timeoutMs);

        fetch(url, { ...options, signal: controller.signal })
            .then(res => { clearTimeout(timer); resolve(res); })
            .catch(err => { clearTimeout(timer); reject(err); });
    });
}

// ─── Precache with timeout, stall detection, and detailed progress ──────────
async function precacheImages(urls, client) {
    if (!urls || !urls.length) return;
    const cache = await caches.open(IMAGE_CACHE_NAME);

    let done = 0;
    let skipped = 0;
    const total = urls.length;
    const failed = [];
    const BATCH = 10;
    const TIMEOUT_MS = 20000; // 20s per image
    const startTime = Date.now();

    function report(extra) {
        if (!client) return;
        try {
            client.postMessage({
                type: 'CACHE_PROGRESS',
                done, total, skipped,
                failedCount: failed.length,
                elapsedMs: Date.now() - startTime,
                ...extra
            });
        } catch (_) { /* client may have navigated away */ }
    }

    for (let i = 0; i < urls.length; i += BATCH) {
        const batch = urls.slice(i, i + BATCH);
        const batchStart = Date.now();

        // Report which batch is starting
        report({ currentBatch: batch, batchIndex: i });

        await Promise.allSettled(batch.map(async url => {
            const fetchStart = Date.now();
            try {
                // Skip if already cached with a valid response
                const existing = await cache.match(url);
                if (existing && existing.status === 200) {
                    done++;
                    skipped++;
                    return;
                }
                // Delete broken/opaque cached entries so we can re-fetch properly
                if (existing) {
                    await cache.delete(url);
                }

                // Try CORS first (proper response, real size in quota)
                let response;
                try {
                    response = await fetchWithTimeout(url, {}, TIMEOUT_MS);
                } catch (corsErr) {
                    // CORS failed — fallback to no-cors (opaque, but better than nothing)
                    response = await fetchWithTimeout(url, { mode: 'no-cors' }, TIMEOUT_MS);
                }

                // Only cache valid responses
                if (response.ok) {
                    await cache.put(url, response);
                    done++;
                } else if (response.type === 'opaque') {
                    // Opaque = can't verify, but still cache it as last resort
                    await cache.put(url, response);
                    done++;
                } else {
                    done++;
                    const durationMs = Date.now() - fetchStart;
                    failed.push({ url, reason: `HTTP ${response.status}`, durationMs });
                    console.warn('[SW] Bad response:', url, response.status);
                }
            } catch(e) {
                done++;
                let reason;
                if (e.message === 'timeout' || e.name === 'AbortError') {
                    reason = `timeout (${Math.round(TIMEOUT_MS/1000)}s)`;
                } else if (e.name === 'TypeError') {
                    reason = 'network error';
                } else if (e.name === 'QuotaExceededError') {
                    reason = 'storage full';
                } else {
                    reason = e.message || e.name || 'unknown';
                }
                const durationMs = Date.now() - fetchStart;
                failed.push({ url, reason, durationMs });
                console.warn('[SW] Failed to cache:', url, reason, `(${durationMs}ms)`);
            }
        }));

        const batchDuration = Date.now() - batchStart;
        report({ batchDurationMs: batchDuration });
    }

    if (client) {
        try {
            client.postMessage({
                type: 'CACHE_COMPLETE',
                total, skipped,
                failed,
                elapsedMs: Date.now() - startTime
            });
        } catch (_) { /* client may have navigated away */ }
    }
}
