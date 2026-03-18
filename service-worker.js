const CACHE_NAME        = 'karakter-galerisi-cache-v6';
const IMAGE_CACHE_NAME  = 'karakter-images-v5';

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

// ─── Extract hostname bucket from URL ───────────────────────────────────────
function hostBucket(url) {
    try { return new URL(url).hostname; } catch { return '_unknown_'; }
}

// ─── Precache — per-host queues, sequential within host, retry with backoff ─
async function precacheImages(urls) {
    if (!urls || !urls.length) return;
    const cache = await caches.open(IMAGE_CACHE_NAME);

    let done = 0;
    let skipped = 0;
    const total = urls.length;
    const failed = [];

    // ── Tuning ──────────────────────────────────────────────────────────────
    const TIMEOUT_MS     = 20000;  // 20 s per attempt
    const MAX_RETRIES    = 2;      // up to 2 retries (3 attempts total)
    const BASE_DELAY_MS  = 1500;   // backoff base: 1.5s, 3s, 6s …
    const HOST_DELAY_MS  = 300;    // pause between downloads on same host
    const MAX_PARALLEL   = 3;      // max hosts downloading simultaneously

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

    // ── Fetch a single image with retry + backoff ───────────────────────────
    async function fetchOne(url) {
        const fetchStart = Date.now();

        // Already cached?
        try {
            const existing = await cache.match(url);
            if (existing && (existing.ok || existing.type === 'opaque')) {
                done++;
                skipped++;
                report();
                return;
            }
            if (existing) await cache.delete(url);
        } catch (_) {}

        let lastError = null;

        for (let attempt = 0; attempt <= MAX_RETRIES; attempt++) {
            if (attempt > 0) {
                // Exponential backoff before retry
                const delay = BASE_DELAY_MS * Math.pow(2, attempt - 1);
                await new Promise(r => setTimeout(r, delay));
            }

            try {
                // Try normal (CORS) fetch first
                let response;
                try {
                    response = await fetchWithTimeout(url, {}, TIMEOUT_MS);
                } catch (corsErr) {
                    // imgbb / vgy.me often block CORS from service workers
                    // Fall back to no-cors — we get an opaque response but
                    // it's usable for <img> tags
                    response = await fetchWithTimeout(url, { mode: 'no-cors' }, TIMEOUT_MS);
                }

                if (response.ok) {
                    // Read full body to ensure complete download
                    const blob = await response.blob();
                    if (blob.size === 0) {
                        lastError = 'empty response';
                        continue; // retry
                    }
                    await cache.put(url, new Response(blob, {
                        status: response.status,
                        statusText: response.statusText,
                        headers: response.headers,
                    }));
                    done++;
                    report();
                    return;
                } else if (response.type === 'opaque') {
                    // Opaque = can't check status but it contains data
                    // Cache it — browsers pad opaque quota but it's the only option
                    // for hosts without CORS headers
                    await cache.put(url, response);
                    done++;
                    report();
                    return;
                } else if (response.status === 429 || response.status === 503) {
                    // Rate limited — retry after backoff
                    lastError = `HTTP ${response.status}`;
                    continue;
                } else {
                    // 4xx/5xx — no point retrying
                    lastError = `HTTP ${response.status}`;
                    break;
                }
            } catch (e) {
                if (e.name === 'AbortError') {
                    lastError = `timeout (${Math.round(TIMEOUT_MS / 1000)}s)`;
                    // Timeout → worth retrying
                    continue;
                } else if (e.name === 'QuotaExceededError') {
                    lastError = 'storage full';
                    break; // no point retrying
                } else {
                    lastError = e.message || e.name || 'network error';
                    continue;
                }
            }
        }

        // All attempts exhausted
        done++;
        failed.push({ url, reason: lastError || 'unknown', durationMs: Date.now() - fetchStart });
        console.warn('[SW] Failed after retries:', url, lastError);
        report();
    }

    // ── Group URLs by host ──────────────────────────────────────────────────
    const hostQueues = new Map();
    for (const url of urls) {
        const host = hostBucket(url);
        if (!hostQueues.has(host)) hostQueues.set(host, []);
        hostQueues.get(host).push(url);
    }

    // ── Process each host queue sequentially (1 download at a time per host)
    //    but run up to MAX_PARALLEL hosts concurrently ────────────────────────
    async function processHostQueue(hostUrls) {
        for (const url of hostUrls) {
            await fetchOne(url);
            // Small delay between requests to same host to avoid rate limiting
            await new Promise(r => setTimeout(r, HOST_DELAY_MS));
        }
    }

    // Run host queues with limited parallelism
    const hostEntries = [...hostQueues.values()];
    const running = new Set();

    for (const queue of hostEntries) {
        const p = processHostQueue(queue).then(() => running.delete(p));
        running.add(p);

        // If we've hit the parallel limit, wait for one to finish
        if (running.size >= MAX_PARALLEL) {
            await Promise.race(running);
        }
    }
    // Wait for all remaining
    await Promise.all(running);

    broadcast({
        type: 'CACHE_COMPLETE',
        total, skipped,
        failed,
        elapsedMs: Date.now() - startTime
    });
}
