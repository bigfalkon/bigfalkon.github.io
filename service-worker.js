const CACHE_NAME        = 'karakter-galerisi-cache-v3';
const IMAGE_CACHE_NAME  = 'karakter-images-v2';

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

// ─── Fetch — cache-first for images, network-first for pages ────────────────
self.addEventListener('fetch', event => {
    const url = new URL(event.request.url);

    // Image requests: cache-first
    if (event.request.destination === 'image') {
        event.respondWith(
            caches.open(IMAGE_CACHE_NAME).then(imgCache =>
                imgCache.match(event.request).then(cached => {
                    if (cached) return cached;
                    return fetch(event.request).then(networkResponse => {
                        if (networkResponse.ok || networkResponse.type === 'opaque') {
                            imgCache.put(event.request, networkResponse.clone());
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
    const total = urls.length;
    const failed = [];
    const BATCH = 10;
    const TIMEOUT_MS = 20000; // 20s per image
    const startTime = Date.now();

    function report(extra) {
        if (!client) return;
        client.postMessage({
            type: 'CACHE_PROGRESS',
            done, total,
            failedCount: failed.length,
            elapsedMs: Date.now() - startTime,
            ...extra
        });
    }

    for (let i = 0; i < urls.length; i += BATCH) {
        const batch = urls.slice(i, i + BATCH);
        const batchStart = Date.now();

        // Report which batch is starting
        report({ currentBatch: batch, batchIndex: i });

        await Promise.allSettled(batch.map(async url => {
            const fetchStart = Date.now();
            try {
                // Skip if already cached
                const existing = await cache.match(url);
                if (existing) { done++; return; }

                const response = await fetchWithTimeout(url, { mode: 'no-cors' }, TIMEOUT_MS);
                await cache.put(url, response);
                done++;
            } catch(e) {
                done++;
                let reason;
                if (e.message === 'timeout') {
                    reason = `timeout (${Math.round(TIMEOUT_MS/1000)}s)`;
                } else if (e.name === 'AbortError') {
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
        client.postMessage({
            type: 'CACHE_COMPLETE',
            total,
            failed,
            elapsedMs: Date.now() - startTime
        });
    }
}
