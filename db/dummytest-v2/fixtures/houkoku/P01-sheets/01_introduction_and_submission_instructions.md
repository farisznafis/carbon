# Filled sheet — 01_introduction_and_submission_instructions

| Item | Dummy value |
|---|---|
| Project | P01 / P000001 |
| 報告期間 | 2026-09-15 – 2026-09-30 |
| 承認設備 | E01–E05 |
| 算定結果 ER | 5.958 t-CO2 |
| 注意 | テストデータ / 架空書類 / 公式提出不可 |

---

## Complete source-form representation

# はじめに（本資料の説明）

**Sumber:** `hokoku_program_ver.1.5.0` — sheet Excel urutan 3 (visible). **Dokumen:** モニタリング報告書（プログラム型排出削減プロジェクト用）Ver.1.5.0.

**Konvensi:** `________` = area isian; `□` = pilihan; **[自動]** = terisi melalui formula/tautan antar-sheet; **[条件付]** = hanya jika berlaku. Susunan tabel Markdown merekonstruksi formulir dan bagian yang terlihat, bukan menyalin sel/merged-cell atau formula Excel mentah.

Sheet referensi/instruksi, bukan form input.

## 1. ■HP公開について

Sheet dengan awalan `【HP公開】` dipublikasikan pada situs J-Credit setelah proses sertifikasi kredit selesai. **Jangan menuliskan data pribadi pada sheet tersebut.**

## 2. ■様式のVer.について

Periksa versi form yang dapat digunakan pada J-Credit Registry: https://j-creditregistry.go.jp/toppage.html . Versi yang tidak didukung akan menimbulkan error saat upload.

## 3. ■シート名・シート見出しの色について

| Jenis sheet | Instruksi |
| --- | --- |
| Sheet `【HP公開】` | Akan dipublikasikan sesudah sertifikasi kredit. |
| Tab merah / protected | Jangan ubah atau hapus format maupun nama sheet. Bila menduplikasi untuk kebutuhan tertentu, nama sheet hasil copy boleh berbeda; sheet sumber tidak diubah. |

## 4. ■様式のルールについて

| Jenis sel | Arti / cara mengisi |
| --- | --- |
| 入力が必要なセル（自由記入） | Masukkan teks/nilai secara langsung. |
| 入力が必要なセル（プルダウンから選択） | Pilih dari dropdown. |
| 入力が不要なセル（自動入力） | Diisi otomatis; jangan ditimpa. |
| 入力が任意のセル | Isian opsional. |
| 項目名など | Label/teks statis. |

Isi teks berwarna hitam. Dalam perubahan project plan, bagian yang berubah berwarna merah. Registry membatasi jenis karakter yang didukung (S-JIS, NEC/IBM tertentu); hindari karakter tertentu seperti subskrip `CO₂` dan beberapa karakter JIS ekstensi.

## 5. ■複数方法論を用いるプロジェクトについて

Jika memakai lebih dari satu metodologi, buat `【HP公開】クレジット発行内容概要` dan `【HP公開】削減活動数` per metodologi. Template menyediakan ①–⑤; hanya ① terlihat, ②–⑤ hidden. Arsip ini mendokumentasikan **hanya sheet visible**, sebagaimana diminta.

## 6. ■認証権限保有主体変更について

| Kondisi | Pekerjaan yang disebut dalam sumber |
| --- | --- |
| Pemilik/otoritas sertifikasi proyek berubah | Unhide `認証権限保有主体変更届` pada workbook Project Plan, isi form, ubah `登録申請書` ke permohonan perubahan, lalu prosedur Registry. |
| Ada aplikasi bantuan biaya review berjalan | Perubahan tidak dapat dilakukan selama proses dukungan biaya masih berlangsung. |
| Estimasi persetujuan pada instruksi workbook | 3–10 hari kerja (estimasi yang tertulis pada dokumen). |

## 7. ■Ｊ－クレジットを発行する口座について

Akun dengan otorisasi `申請代行者` tidak dapat digunakan sebagai penerima kredit; pastikan akun bertipe `口座保有者` jika akan menerima kredit.

## 8. ■形式的な計画変更および登録情報の変更のみを申請する場合

Jika hanya melakukan perubahan formal/perubahan data registrasi, prosedur dilakukan melalui Registry. Jika bukan formal change, perlu pemeriksaan oleh 審査機関. Instruksi workbook menjelaskan pemilihan `環境省(ダミーの審査機関)` untuk kasus tertentu; ikuti prosedur terbaru saat digunakan.

## 9. ■クレジット認証申請の流れ・提出書類

| 申請 | Metode dalam workbook |
| --- | --- |
| ① 審査費用支援申請 | 申請 elektronik melalui Ｊ－クレジット登録簿システム; baca manual 3.9 dan informasi support. |
| ② クレジット認証申請 | 申請 elektronik melalui Ｊ－クレジット登録簿システム; baca manual 3.9. |

## 10. ●提出書類

| 提出者 | 提出書類 | 作成時期 | 提出形式 / informasi |
| --- | --- | --- | --- |
| 全事業者必須 | モニタリング報告書 | 審査機関による審査受審前 | 本エクセルファイル |
| 全事業者必須 | 個別活動実績報告リスト | 審査機関による審査受審前 | Excel; https://japancredit.go.jp/application/document/ |
| 全事業者必須 | 入会届（ひな形） | 審査機関による審査受審前 | 書式任意; contoh Word tersedia. Jika Web-form, simpan halaman sebagai PDF. |
| 全事業者必須 | 入会規約 | 審査機関による審査受審前 | 書式任意; contoh Word disediakan bersama 入会届; jika Web-form, buat PDF. |
| プロジェクト計画変更がある方のみ | 計画変更届 | Saat terjadi perubahan | Pada Excel Project Plan: isi `【HP公開】計画変更届`; perubahan di Project Plan ditulis merah. |

## 11. ●締切日 / referensi

Bantuan biaya: https://japancredit.go.jp/application/support/#ninsho_sinsa . Deadline per komite sertifikasi: https://japancredit.go.jp/authentication_committee/ . Manual Registry: https://j-creditregistry.go.jp/docs/japancredit_usermanual.pdf .

