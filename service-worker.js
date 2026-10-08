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
    
    // --- YENİ EKLENEN DÜZELTME BAŞLANGICI ---
    // 1. Sadece GET (okuma) isteklerine müdahale et. Firebase kayıtları (POST) pas geçilir.
    if (event.request.method !== 'GET') return;

    // 2. Firebase, Kimlik Doğrulama ve Resim yükleme sunucularını Service Worker'dan tamamen gizle.
    const url = event.request.url;
    if (
        url.includes('firestore.googleapis.com') ||
        url.includes('identitytoolkit.googleapis.com') ||
        url.includes('api.imgbb.com') ||
        url.includes('vgy.me')
    ) {
        return;
    }
    // --- YENİ EKLENEN DÜZELTME BİTİŞİ ---

    if (event.request.destination === 'image') {
        const cleanUrl = stripCacheBust(event.request.url);
        event.respondWith(
            caches.open(IMAGE_CACHE_NAME).then(async imgCache => {
                const cached = await imgCache.match(cleanUrl);
                if (cached) return cached;

                // ÖNEMLİ: <img> etiketinden gelen istek 'no-cors' modunda olduğu için
                // fetch(event.request) HER ZAMAN "opaque" bir response döner
                // (status 0, ok:false) — ImgBB gerçekten resmi verse de,
                // "spam" koruması devreye girip hata sayfası döndürse de fark etmez.
                // Eski kod `res.type === 'opaque'` olduğu için bunu "başarılı" sayıp
                // cache'e yazıyordu; bu yüzden rate-limit'e takılan istekler bozuk
                // halde cache'e kalıcı olarak gömülüyordu.
                // Çözüm: isteği kendimiz 'cors' modunda atıp gerçek status'u görüyoruz.
                try {
                    const res = await fetch(cleanUrl, { mode: 'cors' });
                    if (res.ok) {
                        imgCache.put(cleanUrl, res.clone()).catch(() => {});
                        return res;
                    }
                    // 429 / 403 vb. — cache'e YAZMIYORUZ, böylece bir sonraki
                    // denemede (ör. img onerror retry) tekrar denenebilir.
                    return res;
                } catch (_) {
                    // CORS fetch bazı nadir durumlarda (ör. CORS header eksikse)
                    // başarısız olabilir — orijinal no-cors isteğe geri dön ama
                    // yine de cache'e YAZMA, çünkü gerçekten başarılı mı
                    // bilemiyoruz.
                    try {
                        return await fetch(event.request);
                    } catch (_) {
                        return cached || new Response('', { status: 404 });
                    }
                }
            })
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

// ─── Image fetching (shared strategy with character-backup-tool.html) ───────
const CORS_PROXY = url => 'https://wsrv.nl/?url=' + encodeURIComponent(url);

async function fetchImageResponse(url) {
    try {
        const res = await fetch(url, { mode: 'cors' });
        if (res.ok) return { res, verified: true };
    } catch (_) {}
    try {
        const res = await fetch(CORS_PROXY(url), { mode: 'cors' });
        if (res.ok) return { res, verified: true };
    } catch (_) {}
    // Son çare: no-cors. Bu her zaman "opaque" bir response döner (status 0),
    // yani ImgBB gerçekten resmi verse de spam korumasına takılıp hata
    // sayfası döndürse de SONUÇ AYNI GÖRÜNÜR — ayırt edemeyiz.
    // Bu yüzden verified:false işaretliyoruz ki cache'e YAZILMASIN.
    const res = await fetch(url, { mode: 'no-cors' });
    return { res, verified: false };
}

// ─── Precache images ────────────────────────────────────────────────────────
async function precacheImages(urls) {
    if (!urls || !urls.length) return;
    const cache = await caches.open(IMAGE_CACHE_NAME);

    let done = 0, skipped = 0;
    const total = urls.length;
    const failed = [];
    const BATCH    = 3;
    const DELAY_MS = 200;
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

                const { res, verified } = await fetchImageResponse(url);
                if (verified) {
                    await cache.put(url, res);
                    done++;
                } else {
                    // Durumu doğrulayamadık (opaque response) — eskiden burada
                    // koşulsuz cache.put() yapılıyordu ve bu, ImgBB'nin spam
                    // korumasına takılan istekleri "başarılı" diye kalıcı
                    // olarak cache'e gömüyordu. Artık yazmıyoruz; bunun yerine
                    // "failed" listesine ekleyip bir sonraki "Cache Images"
                    // tıklamasında tekrar denenmesini sağlıyoruz.
                    done++;
                    failed.push({ url, reason: 'unverified (opaque) — not cached', durationMs: Date.now() - t0 });
                }
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
