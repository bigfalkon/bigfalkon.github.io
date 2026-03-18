const CACHE_NAME        = 'karakter-app-v1';
const IMAGE_CACHE_NAME  = 'karakter-images-v1';

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

// ─── Activate ─────────────────────────────────────────────────────────────────
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

// ─── Fetch — cache-first for images ──────────────────────────────────────────
self.addEventListener('fetch', event => {
    if (event.request.destination === 'image') {
        event.respondWith(
            caches.open(IMAGE_CACHE_NAME).then(imgCache =>
                imgCache.match(event.request).then(cached => {
                    if (cached) return cached;
                    return fetch(event.request).then(res => {
                        // Cache both normal (ok) and opaque (type=opaque, status=0) responses
                        if (res.ok || res.type === 'opaque') {
                            imgCache.put(event.request, res.clone());
                        }
                        return res;
                    }).catch(() => cached || new Response('', { status: 404 }));
                })
            )
        );
        return;
    }

    if (event.request.mode === 'navigate') {
        event.respondWith(
            fetch(event.request).then(res => {
                caches.open(CACHE_NAME).then(cache => cache.put(event.request, res.clone()));
                return res;
            }).catch(() => caches.match(event.request))
        );
        return;
    }

    event.respondWith(
        caches.match(event.request).then(res => res || fetch(event.request))
    );
});

// ─── Broadcast ──────────────────────────────────────────────────────────────
async function broadcast(msg) {
    const allClients = await self.clients.matchAll({ type: 'window' });
    for (const c of allClients) {
        try { c.postMessage(msg); } catch (_) {}
    }
}

// ─── Messages ───────────────────────────────────────────────────────────────
self.addEventListener('message', event => {
    const { type, urls } = event.data || {};
    if (type === 'PRECACHE_IMAGES') {
        event.waitUntil(precacheImages(urls));
    }
    if (type === 'CLEAR_IMAGE_CACHE') {
        event.waitUntil(
            caches.delete(IMAGE_CACHE_NAME).then(() => {
                broadcast({ type: 'CACHE_CLEARED' });
                if (urls && urls.length) return precacheImages(urls);
            })
        );
    }
});

// ─── Precache images ────────────────────────────────────────────────────────
// Uses no-cors to handle cross-origin images, then converts opaque responses
// to real blob-based responses to avoid the ~7MB-per-opaque-response quota issue.
async function precacheImages(urls) {
    if (!urls || !urls.length) return;
    const cache = await caches.open(IMAGE_CACHE_NAME);

    let done = 0, skipped = 0;
    const total = urls.length;
    const failed = [];
    const BATCH = 6;
    const startTime = Date.now();

    for (let i = 0; i < urls.length; i += BATCH) {
        const batch = urls.slice(i, i + BATCH);

        broadcast({
            type: 'CACHE_PROGRESS',
            done, total, skipped,
            failedCount: failed.length,
            elapsedMs: Date.now() - startTime
        });

        await Promise.allSettled(batch.map(async url => {
            try {
                // Skip if already cached
                const existing = await cache.match(url);
                if (existing) {
                    done++;
                    skipped++;
                    return;
                }

                // Fetch with no-cors so cross-origin images work
                const res = await fetch(url, { mode: 'no-cors' });

                // Convert to blob and store as a real Response.
                // This avoids the browser counting opaque responses as ~7MB each
                // against the storage quota.
                const blob = await res.blob();
                if (blob.size > 0) {
                    await cache.put(url, new Response(blob, {
                        headers: { 'Content-Type': blob.type || 'image/png' }
                    }));
                } else {
                    failed.push({ url, reason: 'empty response' });
                }
                done++;
            } catch (e) {
                done++;
                failed.push({ url, reason: e.message || 'error' });
            }
        }));
    }

    broadcast({
        type: 'CACHE_COMPLETE',
        total, skipped, failed,
        elapsedMs: Date.now() - startTime
    });
}
