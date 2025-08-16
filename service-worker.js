// service-worker.js dosyasının içeriği

const CACHE_NAME = 'karakter-galerisi-cache-v2'; // Sürümü güncelledik

// Uygulama ilk yüklendiğinde temel dosyaları önbelleğe almak için (isteğe bağlı)
const urlsToCache = [
  '/',
  './index.html' // Dosya adını düzelttik
];

self.addEventListener('install', event => {
  event.waitUntil(
    caches.open(CACHE_NAME)
      .then(cache => {
        console.log('Önbellek açıldı');
        return cache.addAll(urlsToCache);
      })
  );
});

self.addEventListener('fetch', event => {
  // Gelen isteğin hedefi bir resim ise (gif, jpg, png fark etmez)
  if (event.request.destination === 'image') {
    event.respondWith(
      caches.match(event.request)
        .then(response => {
          // 1. Önbellekte bu resim var mı diye kontrol et
          if (response) {
            // Varsa, internete hiç gitmeden direkt önbellekten döndür
            return response;
          }

          // 2. Önbellekte yoksa, internetten indir
          return fetch(event.request).then(
            networkResponse => {
              // İndirilen yanıtı klonla (biri önbelleğe, biri sayfaya gidecek)
              const responseToCache = networkResponse.clone();
              
              // Klonu önbelleğe kaydet ki bir dahaki sefere oradan kullanılsın
              caches.open(CACHE_NAME)
                .then(cache => {
                  cache.put(event.request, responseToCache);
                });

              // Orijinal yanıtı da sayfaya göndererek resmi göster
              return networkResponse;
            }
          );
        })
    );
    return; // Resim isteğini işledikten sonra devam etme
  }

  // İstek bir resim değilse, normal şekilde internetten getirmesi için devam et
  event.respondWith(
    caches.match(event.request)
      .then(response => {
        return response || fetch(event.request);
      })
  );
});
