# Karakter Evreni — Android uygulaması

`bigfalkon.github.io` sitesini saran, sideload için imzalanmış **gerçek bir APK**.
Native kabuk (Kotlin + WebView) içinde çalışır; PWA/"ana ekrana ekle" değildir.

## APK'yı nasıl alırım? (kod yazmadan)

1. GitHub'da repoda **Actions** sekmesi → **Android APK** workflow'u.
2. Sağdaki **Run workflow** düğmesine bas (branch: `main`).
3. Derleme bitince:
   - **Releases** bölümünde `Karakter Evreni APK vN` sürümü oluşur → telefondan direkt indir.
   - Ya da çalışmanın altındaki `karakter-evreni-apk` artifact'ını indir.
4. Telefonda APK'ya dokun, "bilinmeyen kaynaklardan kuruluma izin ver" de, kur.

`android/` klasöründe bir değişiklik `main`'e gittiğinde de derleme otomatik çalışır
(artifact üretir, release'i sadece elle tetiklemede oluşturur).

## Uygulama ne yapıyor?

- `https://bigfalkon.github.io/index.html` adresini native WebView'da açar.
- **Geri tuşu**: önce açık modalı/menüyü kapatır, sonra sayfa geçmişinde geri gider,
  en sonda "çıkmak için tekrar bas".
- **Aşağı çekip yenileme** (sadece sayfa en üstteyken aktif).
- **İndirmeler**: yedek/JSON ve düzenlenmiş görseller (blob: ve data: URL'ler dahil)
  telefonun *İndirilenler* klasörüne kaydedilir.
- **Dosya seçici**: sayfadaki `<input type="file">` alanları galeri/dosya uygulamasını açar.
- **Dış linkler** Chrome Custom Tab'de açılır, uygulama içinde kaybolmaz.
- **Çevrimdışı** ekranı + "Tekrar dene".
- Splash ekranı, koyu tema (#131318), portre kilidi, uygulama ikonu repodaki ikondan üretildi.

### Android'e uygun hafif UI düzeltmeleri

Siteye dokunmadan, sadece uygulama içinde `assets/native-ui.css` + `assets/native-ui.js`
enjekte edilir (`<html class="android-app">`):

- Dokunma vurgusu/uzun basma metin seçimi kapalı, kaydırma çubukları gizli.
- Butonlar en az 44dp, basınca hafif "ezilme" geri bildirimi.
- Dokunmatikte yapışan hover efektleri etkisiz.
- Diyaloglar telefona uygun (92vh, 24dp köşe).
- Status bar'ı sistem çizdiği için sayfadaki fazladan üst boşluk geri alınır.
- `[data-web-only]` işaretli öğeler uygulamada gizlenir (sadece web'de anlamlı içerikler için).

## İmzalama

Varsayılan olarak repodaki `keystore/sideload.jks` kullanılır
(alias `sideload`, parola `karakterevreni`). Bu **kişisel sideload** içindir; herkese
açık olduğu için Play Store'a yükleme veya güvenlik sınırı olarak kullanma.

Kendi anahtarını kullanmak istersen repo Settings → Secrets → Actions'a şunları ekle:
`KEYSTORE_BASE64` (`base64 -w0 anahtar.jks`), `KEYSTORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD`.
Varsa CI otomatik onları kullanır. (Anahtarı değiştirirsen eski kurulumun üzerine
güncelleme yapılamaz; önce uygulamayı kaldırman gerekir.)

## Yerelde derlemek (opsiyonel)

Android SDK kuruluysa:

```bash
cd android
./gradlew assembleRelease   # app/build/outputs/apk/release/app-release.apk
```

Açılan adresi değiştirmek için: `MainActivity.kt` içindeki `START_URL`.
