# Karakter Evreni — Android uygulaması

Kotlin + Jetpack Compose ile yazılmış **native** bir uygulama. WebView yok:
karakterleri doğrudan Firestore'dan çeker ve Material 3 arayüzünde gösterir.

## APK'yı nasıl alırım? (kod yazmadan)

`android/` klasöründeki her değişiklik derlenir ve **Releases** bölümüne
telefondan kurulabilir bir `.apk` bırakır. En son sürümü indirip dosyaya
dokunman yeterli ("bilinmeyen kaynaklardan kuruluma izin ver" diyebilir).

Branch `main`'e girdikten sonra Actions sekmesindeki **Run workflow** düğmesiyle
istediğin an elle de sürüm çıkarabilirsin.

## Uygulamada neler var?

- **Galeri** — uyarlanabilir kart ızgarası, aşağı çekip yenileme, daralan üst
  başlık, kartlarda yıldız rozetleri ve karakterin bulunduğu evrenleri gösteren
  renkli noktalar.
- **Filtreler** (alt sayfa) — görünüm (tümü / 1★ / 2★ / 3★ / füzyonlar /
  emekliler), sıralama (isim, en yeni, en eski) ve ırk filtresi.
- **Karakter detayı** — yıldız evrimleri ve AU karşılıkları arasında kaydırmalı
  görsel gezinme, ırk rozeti, füzyon bileşenleri ve karakterin füzyonları,
  tam ekran görsel önizleme.
- **Ara** — isim, ID veya ırka göre native arama.
- **Evrenler** — alternatif evren listesi; birini seçince galeri o evrenin
  kartlarına geçer.
- **Araçlar** — admin paneli, turnuva, rastgele karakter, AU viewer, yedekleme
  ve linkler sayfaları Chrome Custom Tab'de açılır (bunlar hâlâ web).

## Mimari

- `data/Firestore.kt` — Firestore **REST** istemcisi. Firebase SDK'sı ve
  `google-services.json` gerekmez; koleksiyonlar (`characters`, `dismissed`,
  `alternativeUniverses`) herkese açık okunabilir. İndirilen veri
  `filesDir/snapshot.json` içine yazılır, böylece uygulama anında ve
  çevrimdışı açılır.
- `data/Models.kt` — Character / Evolution / AuEntry / AlternativeUniverse.
- `ui/GalleryViewModel.kt` — durum ve `buildGalleryItems`; sitedeki `_doRender`
  mantığının (yıldız açılımı, füzyon önizlemeleri, AU modu, ırk filtresi,
  sıralama) birebir karşılığı.
- `ui/` — Compose ekranları. CSS `object-position` kırpma değerleri
  `parseImageAlignment` ile Compose hizalamasına çevrilir, böylece kartlar
  görselleri web'deki gibi çerçeveler.

Veri kaynağını değiştirmek için: `data/Firestore.kt` içindeki `PROJECT` ve
`API_KEY`.

## İmzalama

Varsayılan olarak repodaki `keystore/sideload.jks` kullanılır
(alias `sideload`, parola `karakterevreni`). Bu **kişisel sideload** içindir;
herkese açık olduğu için Play Store'a yükleme veya güvenlik sınırı olarak
kullanma.

Kendi anahtarını kullanmak istersen repo Settings → Secrets → Actions'a
`KEYSTORE_BASE64` (`base64 -w0 anahtar.jks`), `KEYSTORE_PASSWORD`, `KEY_ALIAS`,
`KEY_PASSWORD` ekle; CI otomatik onları kullanır. (Anahtar değişirse eski
kurulumun üzerine güncelleme yapılamaz, önce uygulamayı kaldırman gerekir.)

## Yerelde derlemek (opsiyonel)

```bash
cd android
./gradlew assembleRelease   # app/build/outputs/apk/release/app-release.apk
```
