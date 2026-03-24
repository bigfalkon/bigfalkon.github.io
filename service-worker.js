const CACHE_NAME        = 'karakter-app-v1';
const IMAGE_CACHE_NAME  = 'karakter-images-v2';

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

// ─── URL normalization ───────────────────────────────────────────────────────
function stripCacheBust(rawUrl) {
    return rawUrl
        .replace(/[?&]_t=\d+/g, '')
        .replace(/\?&/, '?')
        .replace(/\?$/, '');
}

// ─── Fetch — cache-first for images ──────────────────────────────────────────
self.addEventListener('fetch', event => {
    if (event.request.destination === 'image') {
        const cleanUrl = stripCacheBust(event.request.url);
        event.respondWith(
            caches.open(IMAGE_CACHE_NAME).then(imgCache =>
                imgCache.match(cleanUrl).then(cached => {
                    if (cached) return cached;
                    return fetch(event.request).then(res => {
                        if (res.ok || res.type === 'opaque') {
                            imgCache.put(cleanUrl, res.clone()).catch(() => {});
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
// Opaque responses (no-cors cross-origin) cannot have their body read,
// so blob.size is always 0. Store them directly — this is fine for caching
// purposes and what browsers have always done with opaque responses.
async function precacheImages(urls) {
    if (!urls || !urls.length) return;
    const cache = await caches.open(IMAGE_CACHE_NAME);

    let done = 0, skipped = 0;
    const total = urls.length;
    const failed = [];
    const BATCH    = 6;
    const DELAY_MS = 50;
    const startTime = Date.now();

    for (let i = 0; i < urls.length; i += BATCH) {
        const batch = urls.slice(i, i + BATCH);
        const batchStart = Date.now();

        broadcast({
            type: 'CACHE_PROGRESS',
            done, total, skipped,
            failedCount: failed.length,
            elapsedMs: Date.now() - startTime,
            currentBatch: batch,
            batchIndex: i
        });

        await Promise.allSettled(batch.map(async url => {
            const t0 = Date.now();
            try {
                const existing = await cache.match(url);
                if (existing) {
                    done++;
                    skipped++;
                    return;
                }

                // no-cors → opaque response; store directly, do NOT try to read blob
                const res = await fetch(url, { mode: 'no-cors' });
                await cache.put(url, res);
                done++;
            } catch (e) {
                done++;
                const reason = e.name === 'QuotaExceededError' ? 'storage full'
                    : (e.message || 'error');
                failed.push({ url, reason, durationMs: Date.now() - t0 });
            }
        }));

        broadcast({
            type: 'CACHE_PROGRESS',
            done, total, skipped,
            failedCount: failed.length,
            elapsedMs: Date.now() - startTime,
            batchDurationMs: Date.now() - batchStart
        });

        if (i + BATCH < urls.length) {
            await new Promise(r => setTimeout(r, DELAY_MS));
        }
    }

    broadcast({
        type: 'CACHE_COMPLETE',
        total, skipped, failed,
        elapsedMs: Date.now() - startTime
    });
}
