# Karakter Evreni — Resim İndirme/Yükleme Aracı

Firestore'daki karakter resimlerini toplu indirin, düzenleyin ve geri yükleyin.

## Kurulum

```bash
cd image_tool
pip install -r requirements.txt
cp .env.example .env
```

`.env` dosyasını düzenleyerek Firebase service account yolunu ve ImgBB API anahtarını girin.

**Firebase service account almak için:**
1. [Firebase Console](https://console.firebase.google.com) → Proje Ayarları → Hizmet Hesapları
2. "Yeni özel anahtar oluştur" → JSON indir
3. `image_tool/service-account.json` olarak kaydedin

**ImgBB API anahtarı için:** https://api.imgbb.com/

---

## Kullanım

### Resim İndirme

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

### Resim Yükleme

```bash
# Önce neyin güncelleneceğini kontrol et (gerçek yükleme yapmaz)
python image_tool.py upload --input ./images --dry-run

# Gerçek yükleme (ImgBB'ye yükler + Firestore günceller)
python image_tool.py upload --input ./images
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

> Yükleme sırasında dosya adı ayrıştırılarak hangi karakterin hangi alanının güncelleneceği otomatik belirlenir.

---

## Tipik İş Akışı

1. `python image_tool.py download --output ./duzenlenecek --category star3`
2. `./duzenlenecek` klasöründeki resimleri düzenle
3. `python image_tool.py upload --input ./duzenlenecek --dry-run` (kontrol)
4. `python image_tool.py upload --input ./duzenlenecek` (uygula)
