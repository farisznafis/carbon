# Prompt: membangun form multi-halaman dari spesifikasi Excel

Dipakai kalau ada fitur berupa **formulir panjang yang dibagi jadi banyak
halaman**, spesifikasinya datang dari Excel, dan pengisinya berhenti-lanjut
beberapa kali. Bukan khusus untuk satu fitur — ganti placeholder di §4.

Alurnya tiga pesan. Jangan langsung minta DTO di pesan pertama: AI harus paham
kerangkanya dulu, kalau tidak ia akan membuat controller baru untuk tiap
halaman dan kerangkanya tidak pernah terbentuk.

| | Kapan | Isi |
|---|---|---|
| Pesan 1 | sekali di awal | briefing + minta kerangka |
| Pesan 2 | tiap halaman | satu sheet Excel |
| Pesan 3 | seperlunya | koreksi kalau AI melenceng |

---

## 1. Pesan pertama — briefing dan kerangka

Salin blok di bawah ini apa adanya, ganti bagian `{...}` lebih dulu.

````
Kamu akan membantu saya membangun backend sebuah formulir multi-halaman.
Kamu tidak punya akses ke repository saya — semua yang kamu butuhkan ada di
pesan ini.

## Konteks

Fitur: {FITUR}
Formulir ini punya sekitar {JUMLAH} halaman. Pengisi mengerjakannya beberapa
kali duduk, jadi tiap halaman harus bisa disimpan sendiri dan dilanjutkan nanti.
Spesifikasi tiap halaman datang dari file Excel resmi yang akan saya kirim satu
per satu setelah kerangkanya jadi.

Stack: Java 17, Spring Boot 3, Spring Validation (Jakarta), Lombok,
{PENYIMPANAN}, dan PostgreSQL untuk data master.
Base package: {PAKET}

## Aturan arsitektur yang tidak boleh dilanggar

SATU controller, SATU service, SATU repository — untuk SEMUA halaman.
Menambah halaman = satu kelas DTO + satu baris di registry. Tidak pernah
endpoint, service, atau repository baru per halaman.

Kalau suatu permintaan sepertinya butuh itu, HENTIKAN dan jelaskan alasannya.
Jangan dibangun diam-diam.

## Invarian

1. ATOMIK PER HALAMAN. Satu halaman disimpan hanya kalau lolos validasi
   seluruhnya. Tidak ada penyimpanan separuh. Ini yang membuat data progres
   bisa dipercaya, dan itu yang membuat fitur "lanjutkan" mendarat di halaman
   yang benar.
2. SERVER PEMILIK NILAI TURUNAN. Apa pun yang dihitung — total, rangka baris,
   jumlah hari — dihitung ulang setiap kali dibaca dan setiap kali disimpan,
   dan MENIMPA apa pun yang dikirim klien. Itu yang membuat aman mengirim
   angka total ke klien.
3. PERHITUNGAN HARUS IDEMPOTEN. Proses simpan menjalankannya dua kali.
4. KUNCI DAN METADATA DIKELOLA SERVER. id, status, updatedAt, updatedBy
   ditandai read-only saat deserialisasi, supaya klien tidak bisa memalsukan
   status "selesai".
5. ARAH DATA SATU JALUR, DAN INI MUTLAK.
   PostgreSQL HANYA DIBACA. Fungsinya dua: menentukan siapa boleh mengakses
   apa, dan memasok data master untuk pengisian otomatis. TIDAK ADA APA PUN
   dari formulir ini yang ditulis balik ke PostgreSQL — termasuk saat pengisi
   menekan submit terakhir.
   SEMUA data formulir disimpan di {PENYIMPANAN}, satu item per halaman.
   Status "selesai" adalah atribut pada item itu, BUKAN kolom di tabel
   PostgreSQL.
   Konsekuensinya: method simpan tetap ditandai @Transactional(readOnly = true),
   karena satu-satunya kerja SQL di situ adalah pembacaan untuk otorisasi.
   Kalau suatu saat ada yang menambahkan tulis ke PostgreSQL, penanda itu
   HARUS dicabut — dan itu tanda bahwa aturan ini sedang dilanggar.

## Yang saya minta sekarang (baru kerangkanya, belum ada halaman)

1. Kelas dasar abstrak untuk semua halaman: memegang kunci dan metadata
   (status, updatedAt, updatedBy, lampiran). Pakai @Getter/@Setter, JANGAN
   @Data — supaya subclass ber-@Data tidak mewarisi kontrak equals/hashCode
   yang bentrok.

2. Interface penanda kosong untuk baris tabel berulang. Ini cara kerangka
   menemukan tabel milik sebuah halaman tanpa mengandalkan konvensi nama.

3. Record FormDefinition — segala yang dibutuhkan untuk menyajikan satu
   halaman:
     slug            segmen URL
     storageKey      kunci penyimpanan
     displayName     label yang dilihat pengguna
     order           urutan; harus berurutan tanpa lompatan
     type            kelas DTO halaman itu
     sqlFields       field yang diisi otomatis dari PostgreSQL
     derivation      nilai yang dihitung server; null untuk kebanyakan halaman
     optional        halaman yang hanya sebagian pengisi yang mengisinya

4. Registry: satu komponen berisi daftar semua halaman, dibangun di
   konstruktor. Gagal saat startup kalau ada slug/kunci ganda atau urutan yang
   bolong. Sediakan lookup by slug dan by storageKey.

5. Repository generik untuk semua halaman. Cache satu tabel per kelas DTO.
   Sediakan find(type, key), save(type, item), dan SATU query yang mengambil
   status semua halaman sekaligus (jangan N kali baca).

6. Service dengan urutan operasi persis begini:

   BACA:
     resolve halaman dari registry -> otorisasi -> baca item tersimpan (atau
     instance baru) -> isi otomatis dari SQL -> jalankan perhitungan ->
     kembalikan data + daftar field read-only + aturan kondisional.

   SIMPAN:
     bind body -> JALANKAN PERHITUNGAN -> validasi -> kalau ada pelanggaran,
     kembalikan 400 dan JANGAN simpan apa pun -> promosikan file yang diupload
     -> baca ulang payload dan JALANKAN PERHITUNGAN LAGI -> set kunci/status/
     audit di server -> simpan.

   Perhitungan dijalankan sebelum validasi supaya nilai kiriman klien untuk
   field turunan tidak pernah sampai ke validasi maupun ke penyimpanan. Dan
   dijalankan lagi setelah promosi file karena pembacaan ulang berasal dari
   body request, yang tidak pernah membawa nilai turunan.

   PROGRES:
     daftar semua halaman beserta statusnya. Hitungan total dan selesai HANYA
     menghitung halaman wajib. Halaman opsional tetap ditampilkan tapi tidak
     ikut dihitung dan tidak pernah jadi tujuan "lanjutkan".

7. Anotasi @RequiredIf: constraint level kelas, repeatable, dengan field(),
   when(), is(). Formulir semacam ini penuh pola "pilih ada/tidak; kalau ada,
   isi juga field ini".

   PENTING: validatornya harus melaporkan pelanggaran ke NODE FIELD yang
   bersangkutan, pakai disableDefaultConstraintViolation() lalu
   buildConstraintViolationWithTemplate(...).addPropertyNode(field). Kalau
   pakai @AssertTrue per DTO, error-nya akan terkunci ke nama METHOD, dan
   kontrak error ke klien langsung rusak.

   Kalau nama properti di anotasi tidak ada, lempar exception. Salah ketik di
   situ tidak boleh lolos diam-diam.

8. Controller: lima endpoint saja untuk semua halaman — baca satu halaman,
   simpan satu halaman, upload file, progres satu induk, progres banyak induk.
   Body di-bind sebagai JsonNode, lalu treeToValue ke tipe DTO. Jangan
   convertValue — itu melempar IllegalArgumentException yang biasanya muncul
   sebagai HTTP 500.

## Dua jebakan yang pasti kamu temui

A. Pengisian otomatis dari SQL JANGAN sekadar menimpa. Jalankan dulu
   binder-nya ke instance sementara, serialisasi, lalu laporkan read-only
   HANYA field yang benar-benar terisi — baru terapkan yang itu saja ke objek
   sebenarnya.
   Kalau ini dilewati, field yang SQL tidak punya datanya akan datang KOSONG
   SEKALIGUS TERKUNCI, dan field wajib dalam keadaan itu membuat halaman
   mustahil disimpan selamanya.

B. Generics. Lookup registry mengembalikan FormDefinition<?>, dan itu tidak
   akan lolos typecheck ke repository.find(def.type(), ...). Tiap batas
   generik di service butuh helper privat:
   private <T extends FormBase> X baca(FormDefinition<T> def, ...)

## Validasi

Pakai anotasi Jakarta standar saja: @NotBlank, @NotNull, @Size, @Email,
@DecimalMin, @DecimalMax, @PositiveOrZero, dan @Pattern dengan regex inline.
JANGAN membuat anotasi validator karakter sendiri. Satu-satunya anotasi custom
adalah @RequiredIf di atas.

## Selesai kalau

- Satu halaman bisa didaftarkan dengan satu DTO + satu baris registry, dan
  langsung dilayani controller bersama tanpa perubahan lain.
- PUT tidak valid mengembalikan 400, dan GET sesudahnya masih menunjukkan
  halaman itu BELUM DIMULAI. Tidak ada yang tersimpan separuh.
- PUT valid mengembalikan 200; GET sesudahnya mengembalikan nilai tersimpan
  dengan field SQL tertimpa dan daftar read-only tidak kosong.
- Menyimpan halaman B tidak merusak halaman A.
- Endpoint progres menunjukkan status tiap halaman dan menunjuk halaman wajib
  pertama yang belum selesai.
- Halaman yang memegang List<Baris> bisa round-trip di penyimpanan dan
  melaporkan error baris sebagai rows[1].namaField.
- Nilai turunan dihitung ulang saat simpan walaupun klien mengirim angka lain.

Tulis itu sebagai test. Setelah selesai, minta saya sheet pertama.
Jangan buat DTO apa pun sebelum saya kirim sheet-nya.
````

---

## 2. Pesan kedua dan seterusnya — kirim satu sheet

Satu sheet per pesan. Jangan dua, jawabannya jadi dangkal.

````
Ini sheet berikutnya.

Nama sheet: {NAMA_SHEET}
Baris {DARI}–{SAMPAI}

Header dan label kolom:
{tempel di sini}

Catatan (※) di sheet:
{tempel di sini, jangan diringkas}

Contoh terisi dari file contoh resmi:
{tempel 1–2 baris nyata, JANGAN dikosongkan}

Kembalikan tepat dua hal, tanpa penjelasan panjang:

A. Kelas DTO-nya. Aturannya:
   - Label Jepang tiap field jadi javadoc-nya, persis seperti di sheet.
   - @JsonProperty di setiap field.
   - JANGAN @Builder atau @AllArgsConstructor — builder Lombok tidak
     menyertakan field superclass, jadi kunci jadi tidak bisa di-set TANPA
     error kompilasi.
   - Kalau sheet punya pilihan ada/tidak yang mengatur field lain, tambahkan
     @RequiredIf di level kelas.

B. Satu baris registry untuk halaman ini.

Kalau sheet ini berisi TABEL berulang:
   Satu baris di sheet = satu objek baris. Jangan pipihkan tabel jadi satu set
   kolom, dan jangan pecah kolom satu tabel ke dua halaman.
   Buat kelas baris yang mengimplementasikan interface penanda, lalu di
   halamannya pakai List<Baris> dengan @Valid dan @NotEmpty.
   Kalau dua tabel berbagi sebagian besar kolom, taruh kolom bersama di kelas
   dasar abstrak; hanya kelas baris konkret yang dianotasi sebagai bean
   penyimpanan.

Kalau sheet menyebut suatu kolom dihitung, atau totalnya selalu satu satuan
tertentu, atau barisnya boleh ditambah — SEBUTKAN itu di jawabanmu dan usulkan
perhitungan di server, jangan biarkan pengisi mengetiknya.

Aturan validasi (ini yang paling sering salah, baca sebelum menulis @Pattern
atau @Size):

1. Field yang DIKOSONGKAN oleh contoh resmi TIDAK BOLEH @NotBlank.
   Spesifikasi database biasanya menandai hampir semuanya wajib. Formulirnya
   tidak. Karena halaman bersifat atomik, satu field salah-wajib membuat
   seluruh halaman mustahil disimpan.

2. @Size diukur dari contoh, jangan ditebak. Ukur nilai terpanjang lalu beri
   kelonggaran.

3. Field berikut TIDAK diberi aturan karakter sama sekali: satuan, simbol,
   rumus, nomor rekening, kode, huruf klasifikasi. Isinya campur aduk antar
   jenis huruf, dan aturan sepihak apa pun akan menolak separuh isi asli
   formulir.

4. Sebelum menambahkan @Pattern, cari nilainya di contoh dan pastikan polanya
   menerima nilai itu.
````

---

## 3. Kalau AI melenceng

Tiga koreksi yang paling sering dibutuhkan. Salin yang relevan.

````
Kamu membuat controller/service/repository baru untuk halaman ini. Jangan.
Arsitekturnya satu controller untuk semua halaman. Kembalikan hanya DTO dan
satu baris registry.
````

````
Kamu menandai field ini wajib, tapi contoh resmi mengosongkannya. Karena
halaman ini atomik, itu membuat seluruh halaman mustahil disimpan. Jadikan
opsional.
````

````
Pola @Pattern itu menolak nilai yang ada di contoh resmi: {NILAI}. Periksa
lagi polanya terhadap contoh, atau hapus aturan karakternya kalau field ini
memang campur jenis huruf.
````

---

## 4. Placeholder yang harus kamu isi

| Placeholder | Contoh |
|---|---|
| `{FITUR}` | プロジェクト計画書 (J-Credit project plan) |
| `{JUMLAH}` | 32 |
| `{PENYIMPANAN}` | AWS SDK v2 DynamoDB Enhanced Client |
| `{PAKET}` | `com.kct.jcredit.core_backend` |
| `{NAMA_SHEET}` | `【HP公開】4.排出削減計画` |

Kalau penyimpanannya bukan dokumen melainkan tabel relasional per halaman,
ganti poin 5 di pesan pertama — sisanya tidak berubah, karena registry, urutan
simpan, dan aturan validasinya tidak bergantung pada jenis penyimpanan.

---

## 5. Kenapa aturan-aturan itu ada

Ini untuk kamu, bukan untuk ditempel ke AI. Semua berasal dari bug nyata.

**Aturan karakter menolak data asli.** Lima kejadian: satuan `t-CO2/年` ditolak
aturan half-width; klasifikasi `Ⅰ Ⅱ Ⅲ` ditolak aturan teks-Jepang karena angka
Romawi di luar polanya; rumus ditolak karena `=` dan `×` tidak ada di pola;
nama orang ditolak karena spasi antara nama depan dan belakang; satu wilayah
ditolak padahal itu satu-satunya nilai resmi untuk wilayah itu.

**Format kosong itu formulirnya; contoh cuma satu cara mengisinya.** Membaca
struktur dari file contoh pernah membuat saya menambahkan tabel yang tidak
pernah diminta. Tapi contohnya tetap wajib dikirim — dari situlah panjang
kolom dan kolom mana yang boleh kosong diketahui.

**Field turunan pernah tidak pernah muncul di layar.** Server menghitungnya
saat simpan lalu mengembalikannya, tapi klien membuang responsnya. Angkanya
baru muncul kalau pengguna keluar dari halaman lalu kembali. Kalau frontend
juga dibangun, pastikan respons simpan dilipat kembali ke form.
