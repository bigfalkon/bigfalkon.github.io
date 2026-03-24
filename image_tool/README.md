# Karakter Evreni — Resim İndirme/Yükleme Aracı

Firestore'daki karakter resimlerini toplu indirin, düzenleyin ve geri yükleyin.

---

## Kurulum

```bash
cd image_tool
pip install -r requirements.txt
```

### Yapılandırma

```bash
# .env.example şablonunu kopyala
cp .env.example .env
```

> Windows'ta: `.env.example` dosyasını kopyalayıp adını `.env` yap.

Sonra `.env` dosyasını bir metin editöründe aç ve doldur:

```env
FIREBASE_SERVICE_ACCOUNT=./service-account.json
IMGBB_API_KEY=buraya_gercek_anahtarini_yaz
```

**Firebase service account almak için:**
1. [Firebase Console](https://console.firebase.google.com) → Proje Ayarları → Hizmet Hesapları
2. "Yeni özel anahtar oluştur" → JSON indir
3. `image_tool/service-account.json` olarak kaydet

**ImgBB API anahtarı için:** https://api.imgbb.com/ (ücretsiz kayıt)

---

## Kullanım

### 1. Resimleri İndir

```bash
# Tüm resimleri indir
python image_tool.py download --output ./images

# Belirli kategori
python image_tool.py download --output ./images --category star1
python image_tool.py download --output ./images --category star2
python image_tool.py download --output ./images --category star3
python image_tool.py download --output ./images --category fusion
python image_tool.py download --output ./images --category dismissed

# Belirli bir AU'nun resimleri
python image_tool.py download --output ./images --category au --au-id "au_001"

# Mevcut dosyaların üzerine yaz
python image_tool.py download --output ./images --force
```

İndirme tamamlandığında klasörde `.manifest.json` dosyası oluşur.
Bu dosya her resmin hash'ini tutar — yükleme sırasında sadece değişenler belirlenir.

### 2. Resimleri Düzenle

`./images` klasöründeki istediğin dosyaları düzenle.
Değiştirmediğin dosyalar upload sırasında otomatik olarak atlanır.

### 3. Yükle ve Firestore'u Güncelle

```bash
# Neyin yükleneceğini önce kontrol et (gerçek yükleme yapmaz)
python image_tool.py upload --input ./images --dry-run

# Sadece değişen dosyaları yükle (önerilen)
python image_tool.py upload --input ./images

# Tüm dosyaları zorla yükle (manifest görmezden gel)
python image_tool.py upload --input ./images --all
```

---

## Dosya Adlandırma Kuralı

| Resim Türü           | Dosya Adı                              |
|----------------------|----------------------------------------|
| 1★ temel             | `{id}_star1.jpg`                       |
| 2★ evrim             | `{id}_star2.jpg`                       |
| 3★ evrim             | `{id}_star3.jpg`                       |
| Füzyon               | `{id}_fusion.jpg`                      |
| AU 3★ varyant        | `{id}_au_{auId}_star3.jpg`            |
| AU füzyon varyant    | `{id}_au_{auId}_fusion.jpg`           |
| Dismissed 1★         | `dismissed_{id}_star1.jpg`            |

Yükleme sırasında dosya adı ayrıştırılarak hangi karakterin hangi alanı güncelleneceği otomatik belirlenir.

---

## Tipik İş Akışı

```bash
# 1. İndir — .manifest.json otomatik oluşturulur
python image_tool.py download --output ./duzenlenecek --category star3

# 2. ./duzenlenecek klasöründeki resimleri düzenle

# 3. Kontrol et (dry-run)
python image_tool.py upload --input ./duzenlenecek --dry-run
# → "3 değişmiş dosya yüklenecek, 47 dosya atlanacak"

# 4. Uygula — sadece değişen 3 dosya yüklenir
python image_tool.py upload --input ./duzenlenecek
```
