#!/usr/bin/env python3
"""
image_tool.py — Karakter Evreni Image Download/Upload CLI Tool

Kullanım:
  python image_tool.py download --output ./images
  python image_tool.py download --output ./images --category star1
  python image_tool.py download --output ./images --category au --au-id "au_001"
  python image_tool.py upload --input ./images
  python image_tool.py upload --input ./images --dry-run
"""

import argparse
import base64
import hashlib
import json
import os
import re
import sys
import time
from pathlib import Path
from urllib.parse import urlparse

try:
    import requests
except ImportError:
    sys.exit("Hata: 'requests' paketi yüklü değil. 'pip install -r requirements.txt' çalıştırın.")

try:
    from dotenv import load_dotenv
except ImportError:
    sys.exit("Hata: 'python-dotenv' paketi yüklü değil. 'pip install -r requirements.txt' çalıştırın.")

try:
    import firebase_admin
    from firebase_admin import credentials, firestore
except ImportError:
    sys.exit("Hata: 'firebase-admin' paketi yüklü değil. 'pip install -r requirements.txt' çalıştırın.")

try:
    from tqdm import tqdm
    HAS_TQDM = True
except ImportError:
    HAS_TQDM = False

# ─────────────────────────────────────────────
# YAPILANDIRMA
# ─────────────────────────────────────────────

load_dotenv(dotenv_path=Path(__file__).parent / ".env")

FIREBASE_SERVICE_ACCOUNT = os.getenv("FIREBASE_SERVICE_ACCOUNT", "./service-account.json")
IMGBB_API_KEY = os.getenv("IMGBB_API_KEY", "")
CHARACTERS_COLLECTION = os.getenv("CHARACTERS_COLLECTION", "characters")
DISMISSED_COLLECTION = os.getenv("DISMISSED_COLLECTION", "dismissed")

VALID_CATEGORIES = ["all", "star1", "star2", "star3", "fusion", "dismissed", "au"]
MANIFEST_FILENAME = ".manifest.json"

# ─────────────────────────────────────────────
# FİRESTORE BAĞLANTISI
# ─────────────────────────────────────────────

_db = None


def get_db():
    global _db
    if _db is not None:
        return _db

    sa_path = Path(FIREBASE_SERVICE_ACCOUNT)
    if not sa_path.exists():
        sys.exit(
            f"Hata: Firebase service account dosyası bulunamadı: {sa_path}\n"
            "  1. Firebase Console → Proje Ayarları → Hizmet Hesapları → Yeni özel anahtar oluştur\n"
            "  2. İndirilen JSON dosyasını .env içinde FIREBASE_SERVICE_ACCOUNT ile belirtin."
        )

    if not firebase_admin._apps:
        cred = credentials.Certificate(str(sa_path))
        firebase_admin.initialize_app(cred)

    _db = firestore.client()
    return _db


# ─────────────────────────────────────────────
# FİRESTORE YARDIMCILARI
# ─────────────────────────────────────────────

def fetch_collection(collection_name):
    """Firestore koleksiyonundaki tüm belgeleri döndürür."""
    db = get_db()
    docs = db.collection(collection_name).stream()
    result = []
    for doc in docs:
        data = doc.to_dict()
        data["_doc_id"] = doc.id
        data["_collection"] = collection_name
        result.append(data)
    return result


def get_all_characters():
    """Tüm karakterleri ve dismissed karakterleri döndürür."""
    print("Firestore'dan karakterler yükleniyor...")
    characters = fetch_collection(CHARACTERS_COLLECTION)
    dismissed = fetch_collection(DISMISSED_COLLECTION)
    print(f"  {len(characters)} karakter, {len(dismissed)} dismissed bulundu.")
    return characters, dismissed


def find_doc_by_char_id(char_id, characters, dismissed):
    """id alanına göre karakter belgesini döndürür."""
    for c in characters:
        if c.get("id") == char_id:
            return c
    for c in dismissed:
        if c.get("id") == char_id:
            return c
    return None


def update_image_url(doc_id, collection, field_path, new_url):
    """
    Firestore belgesindeki tek bir alanı günceller.
    field_path örnekleri:
      "imageUrl"
      "evolutions.0.imageUrl"
      "auData.au_001.star3ImageUrl"
    """
    db = get_db()
    doc_ref = db.collection(collection).document(doc_id)

    # Noktalı yolu Firestore field path formatına çevir
    parts = field_path.split(".")
    if len(parts) == 1:
        doc_ref.update({field_path: new_url})
    elif parts[0] == "evolutions":
        # evolutions bir dizi — get + update gerekir
        idx = int(parts[1])
        doc = doc_ref.get()
        data = doc.to_dict()
        evolutions = data.get("evolutions", [])
        while len(evolutions) <= idx:
            evolutions.append({})
        evolutions[idx][parts[2]] = new_url
        doc_ref.update({"evolutions": evolutions})
    else:
        # Noktalı yol (örn. auData.au_001.star3ImageUrl)
        doc_ref.update({field_path: new_url})


# ─────────────────────────────────────────────
# DOSYA ADI ↔ ALAN YOL EŞLEŞMESİ
# ─────────────────────────────────────────────

# Regex: dosya adından bilgi çıkarma
RE_DISMISSED_STAR1 = re.compile(r"^dismissed_(.+)_star1\.\w+$")
RE_STAR = re.compile(r"^(.+)_star(\d)\.\w+$")
RE_FUSION = re.compile(r"^(.+)_fusion\.\w+$")
RE_AU_STAR3 = re.compile(r"^(.+)_au_(.+)_star3\.\w+$")
RE_AU_FUSION = re.compile(r"^(.+)_au_(.+)_fusion\.\w+$")


def parse_filename(filename):
    """
    Dosya adından (char_id, collection, field_path) döndürür.
    Tanınamayan dosyalar için None döner.

    Örnekler:
      "abc_star1.jpg"             → ("abc", "characters", "imageUrl")
      "abc_star2.jpg"             → ("abc", "characters", "evolutions.0.imageUrl")
      "abc_star3.jpg"             → ("abc", "characters", "evolutions.1.imageUrl")
      "abc_fusion.jpg"            → ("abc", "characters", "imageUrl")
      "abc_au_myau_star3.jpg"     → ("abc", "characters", "auData.myau.star3ImageUrl")
      "abc_au_myau_fusion.jpg"    → ("abc", "characters", "auData.myau.fusionImageUrl")
      "dismissed_abc_star1.jpg"   → ("abc", "dismissed", "imageUrl")
    """
    # Önce AU fusion (daha spesifik) kontrol et
    m = RE_AU_FUSION.match(filename)
    if m:
        char_id, au_id = m.group(1), m.group(2)
        return char_id, CHARACTERS_COLLECTION, f"auData.{au_id}.fusionImageUrl"

    m = RE_AU_STAR3.match(filename)
    if m:
        char_id, au_id = m.group(1), m.group(2)
        return char_id, CHARACTERS_COLLECTION, f"auData.{au_id}.star3ImageUrl"

    m = RE_DISMISSED_STAR1.match(filename)
    if m:
        char_id = m.group(1)
        return char_id, DISMISSED_COLLECTION, "imageUrl"

    m = RE_FUSION.match(filename)
    if m:
        char_id = m.group(1)
        return char_id, CHARACTERS_COLLECTION, "imageUrl"

    m = RE_STAR.match(filename)
    if m:
        char_id, star = m.group(1), int(m.group(2))
        if star == 1:
            return char_id, CHARACTERS_COLLECTION, "imageUrl"
        elif star == 2:
            return char_id, CHARACTERS_COLLECTION, "evolutions.0.imageUrl"
        elif star == 3:
            return char_id, CHARACTERS_COLLECTION, "evolutions.1.imageUrl"

    return None


# ─────────────────────────────────────────────
# İNDİRME LOJİĞİ
# ─────────────────────────────────────────────

def get_extension_from_url(url):
    """URL'den dosya uzantısını çıkarır, bulamazsa .jpg döner."""
    parsed = urlparse(url)
    path = parsed.path
    ext = Path(path).suffix.lower()
    if ext in (".jpg", ".jpeg", ".png", ".gif", ".webp"):
        return ext
    return ".jpg"


def build_image_list(characters, dismissed, category, au_id=None):
    """
    Verilen kategoriye göre (url, filename) çiftlerinin listesini döndürür.
    """
    items = []

    def add(url, filename):
        if url and url.strip():
            items.append((url.strip(), filename))

    def process_char(c, prefix=""):
        char_id = c.get("id", c.get("_doc_id", "unknown"))
        is_fusion = c.get("isFusion", False)

        if category in ("all", "star1"):
            url = c.get("imageUrl", "")
            if url:
                ext = get_extension_from_url(url)
                if is_fusion:
                    add(url, f"{char_id}_fusion{ext}")
                else:
                    add(url, f"{prefix}{char_id}_star1{ext}")

        if category in ("all", "star2", "star3"):
            for evo in c.get("evolutions", []):
                star = evo.get("star", 0)
                url = evo.get("imageUrl", "")
                if url and category in ("all", f"star{star}"):
                    ext = get_extension_from_url(url)
                    add(url, f"{prefix}{char_id}_star{star}{ext}")

        if category in ("all", "fusion") and is_fusion:
            url = c.get("imageUrl", "")
            if url:
                ext = get_extension_from_url(url)
                add(url, f"{char_id}_fusion{ext}")

    if category == "dismissed":
        for c in dismissed:
            char_id = c.get("id", c.get("_doc_id", "unknown"))
            url = c.get("imageUrl", "")
            if url:
                ext = get_extension_from_url(url)
                add(url, f"dismissed_{char_id}_star1{ext}")
        return items

    if category == "au":
        if not au_id:
            sys.exit("Hata: AU kategorisi için --au-id belirtmelisiniz.")
        for c in characters:
            char_id = c.get("id", c.get("_doc_id", "unknown"))
            au_data = c.get("auData", {})
            if au_id in au_data:
                entry = au_data[au_id]
                s3_url = entry.get("star3ImageUrl", "")
                f_url = entry.get("fusionImageUrl", "")
                if s3_url:
                    ext = get_extension_from_url(s3_url)
                    add(s3_url, f"{char_id}_au_{au_id}_star3{ext}")
                if f_url:
                    ext = get_extension_from_url(f_url)
                    add(f_url, f"{char_id}_au_{au_id}_fusion{ext}")
        return items

    # all, star1, star2, star3, fusion
    for c in characters:
        process_char(c)

    if category == "all":
        for c in dismissed:
            char_id = c.get("id", c.get("_doc_id", "unknown"))
            url = c.get("imageUrl", "")
            if url:
                ext = get_extension_from_url(url)
                add(url, f"dismissed_{char_id}_star1{ext}")

    return items


def md5_hash(filepath):
    """Dosyanın MD5 hash'ini döndürür."""
    h = hashlib.md5()
    with open(filepath, "rb") as f:
        for chunk in iter(lambda: f.read(65536), b""):
            h.update(chunk)
    return h.hexdigest()


def load_manifest(directory):
    """manifest.json varsa yükler, yoksa boş dict döner."""
    path = Path(directory) / MANIFEST_FILENAME
    if path.exists():
        try:
            return json.loads(path.read_text(encoding="utf-8"))
        except (json.JSONDecodeError, OSError):
            return {}
    return {}


def save_manifest(directory, manifest):
    """manifest.json dosyasını kaydeder."""
    path = Path(directory) / MANIFEST_FILENAME
    path.write_text(json.dumps(manifest, indent=2, ensure_ascii=False), encoding="utf-8")


def download_image(url, output_path, skip_existing=True):
    """Bir URL'yi dosyaya indirir. skip_existing=True ise mevcut dosyaları atlar."""
    if skip_existing and output_path.exists():
        return "skipped"
    try:
        resp = requests.get(url, timeout=30, headers={"User-Agent": "Mozilla/5.0"})
        resp.raise_for_status()
        output_path.write_bytes(resp.content)
        return "ok"
    except requests.RequestException as e:
        return f"error: {e}"


def cmd_download(args):
    """İndirme komutu."""
    category = args.category
    au_id = args.au_id
    output_dir = Path(args.output)
    output_dir.mkdir(parents=True, exist_ok=True)

    if category not in VALID_CATEGORIES:
        sys.exit(f"Hata: Geçersiz kategori '{category}'. Geçerli seçenekler: {VALID_CATEGORIES}")

    characters, dismissed = get_all_characters()
    items = build_image_list(characters, dismissed, category, au_id)

    if not items:
        print("İndirilecek resim bulunamadı.")
        return

    print(f"\n{len(items)} resim indirilecek → {output_dir}")

    # Mevcut manifest'i yükle (varsa); force modunda sıfırla
    manifest = {} if args.force else load_manifest(output_dir)

    ok = skipped = errors = 0
    iterator = tqdm(items, unit="resim") if HAS_TQDM else items

    for url, filename in iterator:
        out_path = output_dir / filename
        status = download_image(url, out_path, skip_existing=not args.force)
        if status == "ok":
            ok += 1
            manifest[filename] = md5_hash(out_path)
            if not HAS_TQDM:
                print(f"  ✓ {filename}")
        elif status == "skipped":
            skipped += 1
            # Mevcut dosyanın hash'ini kaydet (manifest'te yoksa)
            if filename not in manifest and out_path.exists():
                manifest[filename] = md5_hash(out_path)
            if not HAS_TQDM:
                print(f"  ~ {filename} (atlandı)")
        else:
            errors += 1
            print(f"  ✗ {filename}: {status}")

    save_manifest(output_dir, manifest)
    print(f"\nTamamlandı: {ok} indirildi, {skipped} atlandı, {errors} hata.")
    print(f"Manifest kaydedildi: {output_dir / MANIFEST_FILENAME}")


# ─────────────────────────────────────────────
# YÜKLEME LOJİĞİ
# ─────────────────────────────────────────────

def upload_to_imgbb(filepath, api_key):
    """
    Dosyayı ImgBB'ye yükler ve yeni URL'yi döndürür.
    Hata durumunda exception fırlatır.
    """
    if not api_key:
        raise ValueError(
            "ImgBB API anahtarı bulunamadı. .env dosyasına IMGBB_API_KEY ekleyin.\n"
            "API anahtarı için: https://api.imgbb.com/"
        )

    with open(filepath, "rb") as f:
        image_data = base64.b64encode(f.read()).decode("utf-8")

    resp = requests.post(
        "https://api.imgbb.com/1/upload",
        data={"key": api_key, "image": image_data, "name": Path(filepath).stem},
        timeout=60,
    )
    resp.raise_for_status()
    result = resp.json()

    if not result.get("success"):
        raise RuntimeError(f"ImgBB yanıtı başarısız: {result}")

    return result["data"]["url"]


def cmd_upload(args):
    """Yükleme komutu."""
    input_dir = Path(args.input)
    dry_run = args.dry_run
    upload_all = args.all

    if not input_dir.exists():
        sys.exit(f"Hata: Klasör bulunamadı: {input_dir}")

    image_files = sorted(
        f for f in input_dir.iterdir()
        if f.is_file() and f.suffix.lower() in (".jpg", ".jpeg", ".png", ".gif", ".webp")
    )

    if not image_files:
        print("Klasörde resim dosyası bulunamadı.")
        return

    # Manifest yükle — değişiklik tespiti için
    manifest = load_manifest(input_dir)
    if manifest:
        print(f"Manifest bulundu ({len(manifest)} kayıt). Sadece değişen dosyalar yüklenecek.")
        print("  (Tümünü yüklemek için --all kullanın)\n")
    else:
        if not upload_all:
            print("Uyarı: Manifest bulunamadı (.manifest.json).")
            print("  Önce 'download' çalıştırırsanız sadece değişen dosyalar yüklenir.")
            print("  Şu an klasördeki TÜM resimler yüklenecek.\n")

    # Hangi dosyaların değiştiğini belirle
    to_upload = []
    unchanged = []
    for filepath in image_files:
        if upload_all or not manifest:
            to_upload.append(filepath)
        else:
            current_hash = md5_hash(filepath)
            if manifest.get(filepath.name) != current_hash:
                to_upload.append(filepath)
            else:
                unchanged.append(filepath)

    print(f"{len(to_upload)} değişmiş dosya yüklenecek, {len(unchanged)} dosya atlanacak.")

    if not to_upload:
        print("Yüklenecek değişiklik yok.")
        return

    if not dry_run:
        characters, dismissed = get_all_characters()
    else:
        characters, dismissed = [], []

    ok = skipped = errors = 0

    for filepath in to_upload:
        parsed = parse_filename(filepath.name)
        if not parsed:
            print(f"  ? {filepath.name} — dosya adı tanınamadı, atlandı.")
            skipped += 1
            continue

        char_id, collection, field_path = parsed

        if dry_run:
            print(f"  [DRY-RUN] {filepath.name}")
            print(f"           id={char_id}, koleksiyon={collection}, alan={field_path}")
            ok += 1
            continue

        # Karakter belgesini bul
        doc = find_doc_by_char_id(char_id, characters, dismissed)
        if not doc:
            print(f"  ✗ {filepath.name} — id='{char_id}' Firestore'da bulunamadı.")
            errors += 1
            continue

        # ImgBB'ye yükle
        try:
            print(f"  ↑ {filepath.name} yükleniyor...", end=" ", flush=True)
            new_url = upload_to_imgbb(filepath, IMGBB_API_KEY)
            print(f"OK → {new_url[:60]}...")
        except Exception as e:
            print(f"HATA: {e}")
            errors += 1
            continue

        # Firestore güncelle
        try:
            update_image_url(doc["_doc_id"], collection, field_path, new_url)
            print(f"    ✓ Firestore güncellendi: {collection}/{doc['_doc_id']}.{field_path}")
            # Manifest'i yeni hash ile güncelle (bir dahaki upload'da atlanır)
            manifest[filepath.name] = md5_hash(filepath)
            ok += 1
        except Exception as e:
            print(f"    ✗ Firestore güncelleme hatası: {e}")
            errors += 1

        # Rate limiting — ImgBB ücretsiz tier için küçük bekleme
        time.sleep(0.5)

    if not dry_run and ok > 0:
        save_manifest(input_dir, manifest)

    if dry_run:
        print(f"\n[DRY-RUN] {ok} dosya yüklenecekti, {skipped} atlanacaktı.")
    else:
        print(f"\nTamamlandı: {ok} güncellendi, {len(unchanged)} değişmemiş atlandı, {errors} hata.")


# ─────────────────────────────────────────────
# CLI
# ─────────────────────────────────────────────

def main():
    parser = argparse.ArgumentParser(
        description="Karakter Evreni — Resim İndirme/Yükleme Aracı",
        formatter_class=argparse.RawDescriptionHelpFormatter,
        epilog="""
Örnekler:
  python image_tool.py download --output ./images
  python image_tool.py download --output ./images --category star3
  python image_tool.py download --output ./images --category fusion
  python image_tool.py download --output ./images --category au --au-id "au_001"
  python image_tool.py download --output ./images --category dismissed
  python image_tool.py upload --input ./images --dry-run
  python image_tool.py upload --input ./images
        """,
    )

    subparsers = parser.add_subparsers(dest="command", required=True)

    # ── download ──
    dl = subparsers.add_parser("download", help="Resimleri indir")
    dl.add_argument(
        "--output", "-o", default="./images",
        help="İndirilen resimlerin kaydedileceği klasör (varsayılan: ./images)"
    )
    dl.add_argument(
        "--category", "-c", default="all",
        choices=VALID_CATEGORIES,
        help="Kategori filtresi (varsayılan: all)"
    )
    dl.add_argument(
        "--au-id", default=None,
        help="AU kategorisi için AU kimliği (--category au ile kullanılır)"
    )
    dl.add_argument(
        "--force", "-f", action="store_true",
        help="Mevcut dosyaların üzerine yaz"
    )

    # ── upload ──
    ul = subparsers.add_parser("upload", help="Düzenlenmiş resimleri yükle ve Firestore'u güncelle")
    ul.add_argument(
        "--input", "-i", default="./images",
        help="Yüklenecek resimlerin bulunduğu klasör (varsayılan: ./images)"
    )
    ul.add_argument(
        "--dry-run", action="store_true",
        help="Gerçek yükleme yapmadan hangi işlemlerin yapılacağını göster"
    )
    ul.add_argument(
        "--all", action="store_true",
        help="Manifest olsa bile klasördeki TÜM resimleri yükle"
    )

    args = parser.parse_args()

    if args.command == "download":
        cmd_download(args)
    elif args.command == "upload":
        cmd_upload(args)


if __name__ == "__main__":
    main()
