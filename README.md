# Zikir Sholat (APK)

Aplikasi Android berupa WebView yang memuat aplikasi Zikir Sholat dari `app/src/main/assets/index.html`.

## Cara membuat APK lewat GitHub
1. Unggah seluruh isi folder ini ke repositori GitHub (cabang `main`).
   Pastikan folder `.github/workflows/build.yml` ikut terunggah.
2. Buka tab **Actions**. Build berjalan otomatis (3-5 menit). Jika tidak, pilih "Build APK" lalu "Run workflow".
3. Setelah hijau, buka run tersebut, turun ke **Artifacts**, unduh `waktu-sholat-apk` (berisi `waktu-sholat.apk`).
4. Ekstrak, pasang di ponsel Android 8.1 atau lebih baru. Izinkan "pasang dari sumber tidak dikenal" bila diminta.

## Catatan
- Font dimuat dari Google Fonts, perlu internet saat pertama dibuka. Tanpa itu dipakai font bawaan ponsel.
