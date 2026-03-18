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

// ─── Activate ───────────────────────────────────────────────────────────────
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

// ─── URL normalization ──────────────────────────────────────────────────────
function stripCacheBust(rawUrl) {
    return rawUrl
        .replace(/[?&]_t=\d+/g, '')
        .replace(/\?&/, '?')
        .replace(/\?$/, '');
}

// ─── Fetch handler ──────────────────────────────────────────────────────────
self.addEventListener('fetch', event => {
    if (event.request.destination === 'image') {
        const cleanUrl = stripCacheBust(event.request.url);
        event.respondWith(
            caches.open(IMAGE_CACHE_NAME).then(imgCache =>
                imgCache.match(cleanUrl).then(cached => {
                    if (cached) return cached;
                    return fetch(event.request).then(res => {
                        if (res.ok) imgCache.put(cleanUrl, res.clone());
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

// ─── Smart timeout: 3s connect + dynamic body time based on size ────────────
//
//  Strategy:
//    1. fetch() with 3s abort — if server doesn't respond in 3s, skip
//    2. Once headers arrive, check Content-Length
//    3. Give body download time based on size:
//         - < 500 KB  →  5s
//         - < 2 MB    → 10s
//         - < 5 MB    → 20s
//         - > 5 MB    → 30s
//         - unknown   → 15s
//    4. If body doesn't finish in time, skip that one image
//
//  This way small PNGs cache fast, big GIFs get enough time,
//  and truly stuck connections don't block everything.
// ─────────────────────────────────────────────────────────────────────────────

function bodyTimeout(contentLength) {
    if (contentLength == null) return 15000;      // unknown size → 15s
    if (contentLength < 500 * 1024) return 5000;  // < 500 KB → 5s
    if (contentLength < 2 * 1024 * 1024) return 10000;  // < 2 MB → 10s
    if (contentLength < 5 * 1024 * 1024) return 20000;  // < 5 MB → 20s
    return 30000;                                  // > 5 MB → 30s
}

async function precacheImages(urls) {
    if (!urls || !urls.length) return;
    const cache = await caches.open(IMAGE_CACHE_NAME);

    let done = 0, skipped = 0;
    const total = urls.length;
    const failed = [];

    const BATCH       = 6;     // slightly less parallel to give big files breathing room
    const CONNECT_MS  = 3000;  // 3s to get headers back
    const DELAY_MS    = 50;

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
            const t0 = Date.now();
            try {
                // ── Already cached? Skip ────────────────────────────────
                const existing = await cache.match(url);
                if (existing) {
                    done++;
                    skipped++;
                    return;
                }

                // ── Phase 1: Connect (3s timeout) ───────────────────────
                const controller = new AbortController();
                const connectTimer = setTimeout(() => controller.abort(), CONNECT_MS);

                const res = await fetch(url, { signal: controller.signal });
                clearTimeout(connectTimer);

                if (!res.ok) {
                    done++;
                    failed.push({ url, reason: `HTTP ${res.status}`, durationMs: Date.now() - t0 });
                    return;
                }

                // ── Phase 2: Download body (dynamic timeout) ────────────
                const size = parseInt(res.headers.get('content-length'), 10) || null;
                const bodyMs = bodyTimeout(size);

                const blob = await Promise.race([
                    res.blob(),
                    new Promise((_, reject) =>
                        setTimeout(() => reject(new Error('body_timeout')), bodyMs)
                    )
                ]);

                await cache.put(url, new Response(blob, {
                    status: res.status,
                    statusText: res.statusText,
                    headers: res.headers,
                }));
                done++;

            } catch (e) {
                done++;
                let reason;
                if (e.name === 'AbortError') {
                    reason = 'connect timeout (3s)';
                } else if (e.message === 'body_timeout') {
                    reason = 'download too slow';
                } else if (e.name === 'QuotaExceededError') {
                    reason = 'storage full';
                } else {
                    reason = e.message || 'error';
                }
                failed.push({ url, reason, durationMs: Date.now() - t0 });
            }
        }));

        report({ batchDurationMs: Date.now() - batchStart });

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
