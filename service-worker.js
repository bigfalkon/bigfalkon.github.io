const CACHE_NAME        = 'karakter-galerisi-cache-v3';
const IMAGE_CACHE_NAME  = 'karakter-images-v1';

// Core app shell cached on install
const urlsToCache = [
    '/',
    './index.html',
    './manifest.json',
    './favicon.ico',
    './favicon-16x16.png',
    './favicon-32x32.png',
    './apple-touch-icon.png',
    './icon-192.png',
    './icon-512.png',
    './icon-maskable-192.png',
    './icon-maskable-512.png'
];

// ─── Install ──────────────────────────────────────────────────────────────────
self.addEventListener('install', event => {
    event.waitUntil(
        caches.open(CACHE_NAME)
            .then(cache => cache.addAll(urlsToCache))
    );
    self.skipWaiting();
});

// ─── Activate — clean up old caches ───────────────────────────────────────────
self.addEventListener('activate', event => {
    event.waitUntil(
        caches.keys().then(keys =>
            Promise.all(
                keys.filter(key => key !== CACHE_NAME && key !== IMAGE_CACHE_NAME)
                    .map(key => caches.delete(key))
            )
        ).then(() => clients.claim())
    );
});

// ─── Fetch — cache-first for images ──────────────────────────────────────────
self.addEventListener('fetch', event => {
    if (event.request.destination === 'image') {
        event.respondWith(
            // Check image cache first, then app cache, then network
            caches.open(IMAGE_CACHE_NAME).then(imgCache =>
                imgCache.match(event.request).then(cached => {
                    if (cached) return cached;
                    return caches.match(event.request).then(appCached => {
                        if (appCached) return appCached;
                        return fetch(event.request).then(networkResponse => {
                            imgCache.put(event.request, networkResponse.clone());
                            return networkResponse;
                        });
                    });
                })
            )
        );
        return;
    }

    // Non-image: app cache → network
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

async function fetchWithTimeout(url, ms = 15000) {
    const controller = new AbortController();
    const timer = setTimeout(() => controller.abort(), ms);
    try {
        return await fetch(url, { signal: controller.signal });
    } finally {
        clearTimeout(timer);
    }
}

async function precacheImages(urls, client) {
    if (!urls || !urls.length) return;
    const cache = await caches.open(IMAGE_CACHE_NAME);

    let done = 0;
    const total = urls.length;
    const failed = [];

    // Fetch in parallel batches of 6 to avoid overwhelming the network
    const BATCH = 6;
    try {
        for (let i = 0; i < urls.length; i += BATCH) {
            const batch = urls.slice(i, i + BATCH);
            await Promise.allSettled(batch.map(async url => {
                // Skip if already cached (set-and-forget: don't re-fetch)
                try {
                    const existing = await cache.match(url);
                    if (existing) { done++; return; }
                    const response = await fetchWithTimeout(url);
                    await cache.put(url, response);
                } catch(e) {
                    const reason = e.name === 'AbortError' ? 'timeout' : e.message;
                    console.warn('[SW] Failed to cache:', url, reason);
                    failed.push({ url, reason });
                }
                done++;
            }));

            // Report progress back to the requesting page
            if (client) {
                try { client.postMessage({ type: 'CACHE_PROGRESS', done, total }); } catch(_) {}
            }
        }
    } catch(e) {
        console.error('[SW] precacheImages loop error:', e);
    }

    if (client) {
        try { client.postMessage({ type: 'CACHE_COMPLETE', total, failed }); } catch(_) {}
    }
}
