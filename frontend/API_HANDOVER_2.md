# Frontend API Integration Guide
## Updated: 2026-08-25

## Overview

| English | 日本語 (Japanese) | Indonesia |
|---------|------------------|-----------|
| This document outlines the REST API contract and integration requirements for the GX environmental monitoring system. It is designed for backend developers implementing the required endpoints to match the frontend application's expectations. | このドキュメントは、GX環境監視システムのREST APIコントラクトと統合要件をまとめたものです。フロントエンドアプリケーションの期待に合わせてエンドポイントを実装するバックエンド開発者向けに設計されています。 | Dokumen ini menjelaskan kontrak REST API dan kebutuhan integrasi untuk sistem pemantauan lingkungan GX. Dokumen ini ditujukan untuk backend developer yang mengimplementasikan endpoint agar sesuai dengan ekspektasi aplikasi frontend. |

**Scope**: API contracts and frontend integration points only.

| English | 日本語 (Japanese) | Indonesia |
|---------|------------------|-----------|
| This guide focuses on the API contracts and frontend integration points. | このガイドはAPIコントラクトとフロントエンド統合ポイントに焦点を当てています。 | Panduan ini berfokus pada kontrak API dan titik integrasi frontend. |

**Frontend Stack**: React 18 with TypeScript, Fetch API for all HTTP requests, React Router v6 for routing, Context API for state management.

**Terminology**: The current codebase uses `Site` / `siteId` naming for 拠点 (location). This document follows that convention.

---

## Table of Contents

1. [Architecture Overview](#architecture-overview)
2. [API Folder Structure](#api-folder-structure)
3. [Detailed Module Breakdown](#detailed-module-breakdown)
4. [API Call Map](#api-call-map)
5. [MSW Mock Flow](#msw-mock-flow)
6. [User System Architecture](#user-system-architecture)
7. [Required Endpoints](#required-endpoints)
8. [Error Handling](#error-handling)
9. [Local Development vs ngrok Backend](#local-development-vs-ngrok-backend)
10. [Testing Guide](#testing-guide)

---

## Architecture Overview

### Technology Stack

| English | 日本語 (Japanese) | Indonesia |
|---------|------------------|-----------|
| The frontend uses these technologies to communicate with the backend API. | フロントエンドはバックエンドAPIと通信するために以下の技術を使用しています。 | Frontend menggunakan teknologi berikut untuk berkomunikasi dengan backend API. |

| Technology | Purpose (English) | 用途 (日本語) | Tujuan (Indonesia) |
|------------|-------------------|--------------|---------------------|
| React 18 | UI framework | UIフレームワーク | Framework UI |
| TypeScript | Type-safe JavaScript | 型安全なJavaScript | JavaScript yang aman tipe |
| React Router v6 | Page navigation routing | ページナビゲーションルーティング | Routing navigasi halaman |
| React Context API | Global state management | グローバル状態管理 | Manajemen state global |
| Native Fetch API | HTTP requests | HTTPリクエスト | Permintaan HTTP |
| shadcn-ui + Tailwind CSS | UI components | UIコンポーネント | Komponen UI |

### API Service Location

| English | 日本語 (Japanese) | Indonesia |
|---------|------------------|-----------|
| All API logic is organized in the **`src/api/`** folder with feature-based modules. This is the "post office" where all communication with the backend happens. | すべてのAPIロジックは**`src/api/`**フォルダに機能ベースモジュールとして整理されています。これはバックエンドとのすべての通信が行われる"郵便局"です。 | Seluruh logika API diorganisir di folder **`src/api/`** dengan modul berbasis fitur. Ini adalah "kantor pos" tempat seluruh komunikasi dengan backend terjadi. |

### Implementation Status

| English | 日本語 (Japanese) | Indonesia |
|---------|------------------|-----------|
| ✅ **Mock implementations** are currently in place for the signup flow through MSW. ⚠️ **Action Required**: Replace or extend the mocked responses with actual REST API calls when the backend is ready. | ✅ **モック実装**は現在MSWを通じてサインアップフローで配置されています。⚠️ **対応必要**: バックエンドが準備できたら、モックレスポンスを実際のREST API呼び出しに置き換えるか拡張してください。 | ✅ **Implementasi mock** saat ini sudah tersedia untuk alur signup melalui MSW. ⚠️ **Perlu Tindakan**: Ganti atau perluas respons mock dengan panggilan REST API yang sebenarnya ketika backend sudah siap. |

---

## API Folder Structure

### `src/api/` — The API Layer (Pos Komunikasi dengan Server)

| Folder | English | 日本語 (Japanese) | Indonesia |
|--------|---------|------------------|-----------|
| `_shared/` | **Core engine.** The central fetch wrapper (`apiFetch`, `apiRequest`), error handling classes (`ApiError`), and shared types (`ApiPagination`, `ApiResponse`). This is the only place that should call `fetch` directly. | **コアエンジン。** 中央fetchラッパー（`apiFetch`、`apiRequest`）、エラー処理クラス（`ApiError`）、共有型（`ApiPagination`、`ApiResponse`）。これは`fetch`を直接呼び出すべき唯一の場所です。 | **Mesin inti.** Wrapper fetch pusat (`apiFetch`, `apiRequest`), kelas penanganan error (`ApiError`), dan tipe bersama (`ApiPagination`, `ApiResponse`). Ini adalah satu-satunya tempat yang seharusnya memanggil `fetch` secara langsung. |
| `auth/` | **Login door.** Authentication functions: login, admin login, logout, signup, password change, password reset. The entry point for user access. | **ログインドア。** 認証関数：ログイン、管理者ログイン、ログアウト、サインアップ、パスワード変更、パスワードリセット。ユーザーアクセスのエントリーポイント。 | **Pintu masuk.** Fungsi autentikasi: login, admin login, logout, signup, ubah password, reset password. Titik masuk akses user. |
| `admin/` | **Admin room.** Admin-only operations: pending company approval/rejection, project CRUD, participation reviews, company suspensions. | **管理者ルーム。** 管理者専用操作：保留中企業の承認/却下、プロジェクトCRUD、参加レビュー、企業停止。 | **Ruang admin.** Operasi khusus admin: persetolalan/pengajuan perusahaan pending, CRUD proyek, review partisipasi, suspend perusahaan. |
| `batch/` | **Mass processing.** Batch data operations: emission trends, project emission summaries, batch history. | **一括処理。** バッチデータ操作：排出トレンド、プロジェクト排出サマリー、バッチ履歴。 | **Proses massal.** Operasi data batch: tren emisi, ringkasan emisi proyek, riwayat batch. |
| `company/` | **Company data.** Company profile updates, signup validation, signup revision flow. | **企業データ。** 企業プロフィール更新、サインアップバリデーション、サインアップリビジョンフロー。 | **Data perusahaan.** Update profil perusahaan, validasi signup, alur revisi signup. |
| `monitoring/` | **Data watchtower.** Monitoring data submission, evidence file uploads (ELPV, ELPVR, ELPJ), monitoring records, cancel requests. | **データ見張り塔。** モニタリングデータ送信、証拠ファイルアップロード（ELPV、ELPVR、ELPJ）、モニタリング記録、キャンセルリクエスト。 | **Menara pantau.** Pengiriman data monitoring, upload file bukti (ELPV, ELPVR, ELPJ), catatan monitoring, permintaan pembatalan. |
| `notifications/` | **Notification inbox.** Fetching notifications, unread count, mark as read, delete. | **通知受信箱。** 通知取得、未読数、既読マーク、削除。 | **Kotak notifikasi.** Mengambil notifikasi, jumlah belum dibaca, tandai sudah dibaca, hapus. |
| `profile/` | **User profile card.** Profile data fetching for current user and other users by ID. | **ユーザープロフィールカード。** 現在のユーザーと他のユーザーのIDによるプロフィールデータ取得。 | **Kartu profil user.** Pengambilan data profil untuk user saat ini dan user lain berdasarkan ID. |
| `projects/` | **Project catalog.** Project listing and detail retrieval. | **プロジェクトカタログ。** プロジェクト一覧と詳細取得。 | **Katalog proyek.** Daftar proyek dan pengambilan detail. |
| `site/` | **Site & equipment warehouse.** Site CRUD, site dashboard, equipment management, project association, file uploads, join flow. | **サイト＆設備倉庫。** サイトCRUD、サイトダッシュボード、設備管理、プロジェクト関連付け、ファイルアップロード、参加フロー。 | **Gudang site & peralatan.** CRUD site, dashboard site, manajemen peralatan, asosiasi proyek, upload file, alur bergabung. |
| `users/` | **User management office.** User listing, creation, updates, and deletion. | **ユーザー管理オフィス。** ユーザー一覧、作成、更新、削除。 | **Kantor manajemen user.** Daftar user, pembuatan, update, dan penghapusan. |

### Root Files

| File | English | 日本語 (Japanese) | Indonesia |
|------|---------|------------------|-----------|
| `config.ts` | **Configuration center.** Stores `BASE_URL` and `getAuthHeaders()` helper that reads JWT token from localStorage. If the backend URL changes, this is the first place to update. | **設定センター。** `BASE_URL`とlocalStorageからJWTトークンを読み取る`getAuthHeaders()`ヘルパーを格納。バックエンドURLが変更された場合、最初に更新する場所。 | **Pusat konfigurasi.** Menyimpan `BASE_URL` dan helper `getAuthHeaders()` yang membaca token JWT dari localStorage. Jika URL backend berubah, ini adalah tempat pertama yang perlu diperbarui. |
| `index.ts` | **Public doorway.** Central export file. Other parts of the app import API functions from here instead of importing each module directly. | **公開ドアウェイ。** 中央エクスポートファイル。アプリの他の部分は各モジュールを直接インポートする代わりに、ここからAPI関数をインポートします。 | **Pintu masuk publik.** File ekspor pusat. Bagian aplikasi lain mengimpor fungsi API dari sini alih-alih mengimpor setiap modul secara langsung. |
| `types.ts` | **Compatibility type shelf.** Re-exports domain types for backward compatibility. New code should import from domain modules directly. | **互換性タイプ棚。** 後方互換性のためにドメイン型を再エクスポート。新しいコードはドメインモジュールから直接インポートする必要があります。 | **Rak tipe kompatibilitas.** Mengekspor ulang tipe domain untuk kompatibilitas mundur. Kode baru harus mengimpor langsung dari modul domain. |

---

## Detailed Module Breakdown

### `_shared/` — Core Engine (Mesin Inti)

| File | English | 日本語 (Japanese) | Indonesia |
|------|---------|------------------|-----------|
| `httpClient.ts` | **Central fetch wrapper.** The only file that should call `fetch` directly. Handles BASE_URL prefixing, auth headers, JSON body serialization, FormData handling, and dev-only ngrok headers. Exports: `apiFetch`, `parseJsonResponse`, `apiRequest`, `apiRequestWithMeta`, `apiVoidRequest`. | **中央fetchラッパー。** `fetch`を直接呼び出すべき唯一のファイル。BASE_URLプレフィックス、認証ヘッダー、JSONボディシリアライゼーション、FormData処理、開発用ngrokヘッダーを処理。 | **Wrapper fetch pusat.** Satu-satunya file yang seharusnya memanggil `fetch` secara langsung. Menangani prefix BASE_URL, header autentikasi, serialisasi body JSON, penanganan FormData, dan header ngrok untuk pengembangan. |
| `errors.ts` | **Error toolkit.** `ApiError` class for structured HTTP error metadata, and `getApiErrorMessage()` for fallback messages. | **エラーツールキット。** 構造化されたHTTPエラーメタデータ用の`ApiError`クラスと、フォールバックメッセージ用の`getApiErrorMessage()`。 | **Toolkit error.** Kelas `ApiError` untuk metadata error HTTP terstruktur, dan `getApiErrorMessage()` untuk pesan fallback. |
| `types.ts` | **Shared type definitions.** `ApiErrorPayload` (error response shape), `ApiPagination` (pagination metadata), `ApiResponse<T>` (generic response wrapper). | **共有型定義。** `ApiErrorPayload`（エラーレスポンス形状）、`ApiPagination`（ページネーションメタデータ）、`ApiResponse<T>`（汎用レスポンスラッパー）。 | **Definisi tipe bersama.** `ApiErrorPayload` (bentuk respons error), `ApiPagination` (metadata paginasi), `ApiResponse<T>` (wrapper respons generik). |

---

### `auth/` — Login Door (Pintu Masuk)

| File | English | 日本語 (Japanese) | Indonesia |
|------|---------|------------------|-----------|
| `auth.ts` | **Authentication API calls.** Contains `login()`, `adminLogin()`, `logout()`, `signUp()`, `changePassword()`, `requestPasswordReset()`, `completePasswordReset()`, `exchangeActionLink()`, `exchangePasswordResetActionLink()`. Login and signup use unauthenticated requests; password changes require the current authenticated user token. | **認証API呼び出し。** `login()`、`adminLogin()`、`logout()`、`signUp()`、`changePassword()`、`requestPasswordReset()`、`completePasswordReset()`、`exchangeActionLink()`、`exchangePasswordResetActionLink()`を含む。ログインとサインアップは非認証リクエストを使用。パスワード変更には現在の認証ユーザートークンが必要。 | **Panggilan API autentikasi.** Berisi `login()`, `adminLogin()`, `logout()`, `signUp()`, `changePassword()`, `requestPasswordReset()`, `completePasswordReset()`, `exchangeActionLink()`, `exchangePasswordResetActionLink()`. Login dan signup menggunakan request tanpa autentikasi; ubah password memerlukan token user yang sedang terautentikasi. |
| `types.ts` | **Auth type definitions.** `LoginResponse`, `SignUpData`, `SignUpResponse`, `ChangePasswordPayload`, `ChangePasswordResponse`, `PasswordResetRequestResponse`, `ActionLinkExchangeResponse`, `PasswordResetCompleteResponse`. | **認証型定義。** `LoginResponse`、`SignUpData`、`SignUpResponse`、`ChangePasswordPayload`、`ChangePasswordResponse`、`PasswordResetRequestResponse`、`ActionLinkExchangeResponse`、`PasswordResetCompleteResponse`。 | **Definisi tipe autentikasi.** `LoginResponse`, `SignUpData`, `SignUpResponse`, `ChangePasswordPayload`, `ChangePasswordResponse`, `PasswordResetRequestResponse`, `ActionLinkExchangeResponse`, `PasswordResetCompleteResponse`. |

---

### `admin/` — Admin Room (Ruang Admin)

| File | English | 日本語 (Japanese) | Indonesia |
|------|---------|------------------|-----------|
| `pendingCompanies.ts` | **Company approval workflow.** `getPendingCompanies()`, `approvePendingCompany()`, `rejectPendingCompany()`, `getAdminCompanies()`, `getAdminCompanyDetail()`, `approveAdminCompany()`, `rejectAdminCompany()`, `suspendAdminCompany()`, `resolveCompanySuspension()`, `getCompanySuspensions()`, `sendAdminCompanyPasswordResetLink()`. | **企業承認ワークフロー。** `getPendingCompanies()`、`approvePendingCompany()`、`rejectPendingCompany()`、`getAdminCompanies()`、`getAdminCompanyDetail()`、`approveAdminCompany()`、`rejectAdminCompany()`、`suspendAdminCompany()`、`resolveCompanySuspension()`、`getCompanySuspensions()`、`sendAdminCompanyPasswordResetLink()`。 | **Alur persetujuan perusahaan.** `getPendingCompanies()`, `approvePendingCompany()`, `rejectPendingCompany()`, `getAdminCompanies()`, `getAdminCompanyDetail()`, `approveAdminCompany()`, `rejectAdminCompany()`, `suspendAdminCompany()`, `resolveCompanySuspension()`, `getCompanySuspensions()`, `sendAdminCompanyPasswordResetLink()`. |
| `projects.ts` | **Admin project management.** `getAdminProjectDetail()`, `createAdminProject()`, `updateAdminProject()`, `updateAdminProjectStatus()`, `getAdminProjectPlanApplication()`, `getProjectPlanAutofillByProject()`, `getProjectPlanAutofillDefaults()`, `createAdminProjectPlanApplication()`. | **管理者プロジェクト管理。** `getAdminProjectDetail()`、`createAdminProject()`、`updateAdminProject()`、`updateAdminProjectStatus()`、`getAdminProjectPlanApplication()`、`getProjectPlanAutofillByProject()`、`getProjectPlanAutofillDefaults()`、`createAdminProjectPlanApplication()`。 | **Manajemen proyek admin.** `getAdminProjectDetail()`, `createAdminProject()`, `updateAdminProject()`, `updateAdminProjectStatus()`, `getAdminProjectPlanApplication()`, `getProjectPlanAutofillByProject()`, `getProjectPlanAutofillDefaults()`, `createAdminProjectPlanApplication()`. |
| `participationReviews.ts` | **Participation review system.** `getPendingParticipationReviews()`, `getMyParticipationReviews()`, `getParticipationReviewDetail()`, `approveParticipationReview()`, `rejectParticipationReview()`, `participationReviewChecklistFields`. | **参加レビューシステム。** `getPendingParticipationReviews()`、`getMyParticipationReviews()`、`getParticipationReviewDetail()`、`approveParticipationReview()`、`rejectParticipationReview()`、`participationReviewChecklistFields`。 | **Sistem review partisipasi.** `getPendingParticipationReviews()`, `getMyParticipationReviews()`, `getParticipationReviewDetail()`, `approveParticipationReview()`, `rejectParticipationReview()`, `participationReviewChecklistFields`. |
| `projectPayloadMapper.ts` | **Form-to-API transformer.** Converts admin project form values into API payloads. Exports: `mapAdminProjectPayload`, `mapAdminProjectPlanApplicationPayload`. | **フォーム→API変換器。** 管理者プロジェクトフォーム値をAPIペイロードに変換。 | **Transformasi form-to-API.** Mengkonversi nilai form proyek admin menjadi payload API. |
| `types.ts` | **Admin type definitions.** All admin-specific API and domain types including `PendingCompanyItem`, `AdminProjectDetail`, `AdminProjectPayloadFormValues`, `CreateAdminProjectPayload`, etc. | **管理者型定義。** `PendingCompanyItem`、`AdminProjectDetail`、`AdminProjectPayloadFormValues`、`CreateAdminProjectPayload`など、管理者固有のAPIとドメイン型。 | **Definisi tipe admin.** Semua tipe API dan domain khusus admin termasuk `PendingCompanyItem`, `AdminProjectDetail`, `AdminProjectPayloadFormValues`, `CreateAdminProjectPayload`, dll. |

---

### `batch/` — Mass Processing (Proses Massal)

| File | English | 日本語 (Japanese) | Indonesia |
|------|---------|------------------|-----------|
| `queries.ts` | **Batch data reads.** `getBatchHistory()`, `getEmissionTrend()`, `getProjectEmissionSummary()`. | **バッチデータ読み取り。** `getBatchHistory()`、`getEmissionTrend()`、`getProjectEmissionSummary()`。 | **Pembacaan data batch.** `getBatchHistory()`, `getEmissionTrend()`, `getProjectEmissionSummary()`. |
| `types.ts` | **Batch type definitions.** `BatchHistoryItem`, `BatchHistoryPagination`, `EmissionTrendPoint`, `ProjectEmissionSummary`, `ProjectEmissionBreakdown`, etc. | **バッチ型定義。** `BatchHistoryItem`、`BatchHistoryPagination`、`EmissionTrendPoint`、`ProjectEmissionSummary`、`ProjectEmissionBreakdown`など。 | **Definisi tipe batch.** `BatchHistoryItem`, `BatchHistoryPagination`, `EmissionTrendPoint`, `ProjectEmissionSummary`, `ProjectEmissionBreakdown`, dll. |

---

### `company/` — Company Data (Data Perusahaan)

| File | English | 日本語 (Japanese) | Indonesia |
|------|---------|------------------|-----------|
| `mutations.ts` | **Company update calls.** `updateCompany()`, `validateSignupCompanyInfo()`, `validateSignupMethodology()`. | **企業更新呼び出し。** `updateCompany()`、`validateSignupCompanyInfo()`、`validateSignupMethodology()`。 | **Panggilan update perusahaan.** `updateCompany()`, `validateSignupCompanyInfo()`, `validateSignupMethodology()`. |
| `signupRevision.ts` | **Signup revision flow.** `getSignupRevision()`, `updateSignupRevision()`. | **サインアップリビジョンフロー。** `getSignupRevision()`、`updateSignupRevision()`。 | **Alur revisi signup.** `getSignupRevision()`, `updateSignupRevision()`. |
| `types.ts` | **Company type definitions.** `Company`, `UpdateCompanyResponse`, `ValidateSignupCompanyInfoPayload`, `ValidateSignupMethodologyPayload`, `SignupRevisionRequest`, `SignupRevisionResponse`. | **企業型定義。** `Company`、`UpdateCompanyResponse`、`ValidateSignupCompanyInfoPayload`、`ValidateSignupMethodologyPayload`、`SignupRevisionRequest`、`SignupRevisionResponse`。 | **Definisi tipe perusahaan.** `Company`, `UpdateCompanyResponse`, `ValidateSignupCompanyInfoPayload`, `ValidateSignupMethodologyPayload`, `SignupRevisionRequest`, `SignupRevisionResponse`. |

---

### `monitoring/` — Data Watchtower (Menara Pantau)

| File | English | 日本語 (Japanese) | Indonesia |
|------|---------|------------------|-----------|
| `mutations.ts` | **Monitoring submissions & uploads.** `submitMonitoringData()`, `uploadElpvFile()`, `uploadElpvrFile()`, `uploadElpjBatteryFile()`, `uploadElpjControlFile()`, `validateMonitoringData()`, `createMonitoringCancelRequest()`, `decideMonitoringCancelRequest()`. | **モニタリング送信＆アップロード。** `submitMonitoringData()`、`uploadElpvFile()`、`uploadElpvrFile()`、`uploadElpjBatteryFile()`、`uploadElpjControlFile()`、`validateMonitoringData()`、`createMonitoringCancelRequest()`、`decideMonitoringCancelRequest()`。 | **Pengiriman & upload monitoring.** `submitMonitoringData()`, `uploadElpvFile()`, `uploadElpvrFile()`, `uploadElpjBatteryFile()`, `uploadElpjControlFile()`, `validateMonitoringData()`, `createMonitoringCancelRequest()`, `decideMonitoringCancelRequest()`. |
| `queries.ts` | **Monitoring reads.** `getMonitoringRecent()`, `getMonitoringRecords()`, `getAdminProjectMonitoringRecords()`, `getMonitoringCancelRequests()`. | **モニタリング読み取り。** `getMonitoringRecent()`、`getMonitoringRecords()`、`getAdminProjectMonitoringRecords()`、`getMonitoringCancelRequests()`。 | **Pembacaan monitoring.** `getMonitoringRecent()`, `getMonitoringRecords()`, `getAdminProjectMonitoringRecords()`, `getMonitoringCancelRequests()`. |
| `mappers.ts` | **Data normalizer.** `normalizeRecentMonitoringItem()` — transforms raw API items into frontend-friendly monitoring records. | **データ正規化。** `normalizeRecentMonitoringItem()` — 生APIアイテムをフロントエンド向けモニタリングレコードに変換。 | **Normalizer data.** `normalizeRecentMonitoringItem()` — mengubah item API mentah menjadi catatan monitoring yang ramah frontend. |
| `types.ts` | **Monitoring type definitions.** `MonitoringData`, `SubmitDataResponse`, `MonitoringEvidenceKind`, `MonitoringRecentApiItem`, `MonitoringRecentItem`, `MonitoringRecordListItem`, `MonitoringCancelRequestListItem`, etc. | **モニタリング型定義。** `MonitoringData`、`SubmitDataResponse`、`MonitoringEvidenceKind`、`MonitoringRecentApiItem`、`MonitoringRecentItem`、`MonitoringRecordListItem`、`MonitoringCancelRequestListItem`など。 | **Definisi tipe monitoring.** `MonitoringData`, `SubmitDataResponse`, `MonitoringEvidenceKind`, `MonitoringRecentApiItem`, `MonitoringRecentItem`, `MonitoringRecordListItem`, `MonitoringCancelRequestListItem`, dll. |

---

### `notifications/` — Notification Inbox (Kotak Notifikasi)

| File | English | 日本語 (Japanese) | Indonesia |
|------|---------|------------------|-----------|
| `index.ts` | **All-in-one notification module.** Contains everything: `getNotifications()`, `getUnreadNotificationCount()`, `markNotificationAsRead()`, `deleteNotification()`, plus all types (`UserNotification`, `NotificationPagination`, `NotificationListParams`, `NotificationListResult`). No separate files — everything lives here. | **オールインワン通知モジュール。** すべてがここに含まれる：`getNotifications()`、`getUnreadNotificationCount()`、`markNotificationAsRead()`、`deleteNotification()`、さらにすべての型（`UserNotification`、`NotificationPagination`、`NotificationListParams`、`NotificationListResult`）。別ファイルなし — すべてがここにある。 | **Modul notifikasi all-in-one.** Semua ada di sini: `getNotifications()`, `getUnreadNotificationCount()`, `markNotificationAsRead()`, `deleteNotification()`, ditambah semua tipe (`UserNotification`, `NotificationPagination`, `NotificationListParams`, `NotificationListResult`). Tidak ada file terpisah — semuanya ada di sini. |

---

### `profile/` — User Profile Card (Kartu Profil User)

| File | English | 日本語 (Japanese) | Indonesia |
|------|---------|------------------|-----------|
| `profile.ts` | **Profile read calls.** `getProfile()` (current user), `getUserProfileById()` (any user by ID). | **プロフィール読み取り呼び出し。** `getProfile()`（現在のユーザー）、`getUserProfileById()`（IDによる任意のユーザー）。 | **Panggilan baca profil.** `getProfile()` (user saat ini), `getUserProfileById()` (user mana pun berdasarkan ID). |
| `mappers.ts` | **Profile data transformer.** Maps raw API response into `ProfileViewModel` and `ProfileCompanyViewModel`. | **プロフィールデータ変換器。** 生APIレスポンスを`ProfileViewModel`と`ProfileCompanyViewModel`にマッピング。 | **Transformer data profil.** Memetakan respons API mentah menjadi `ProfileViewModel` dan `ProfileCompanyViewModel`. |
| `types.ts` | **Profile type definitions.** `ProfileApiProfile`, `ProfileApiCompany`, `ProfileApiResponse`. | **プロフィール型定義。** `ProfileApiProfile`、`ProfileApiCompany`、`ProfileApiResponse`。 | **Definisi tipe profil.** `ProfileApiProfile`, `ProfileApiCompany`, `ProfileApiResponse`. |

---

### `projects/` — Project Catalog (Katalog Proyek)

| File | English | 日本語 (Japanese) | Indonesia |
|------|---------|------------------|-----------|
| `queries.ts` | **Project read calls.** `getProjects()` (list all), `getProjectDetails()` (single project by ID). | **プロジェクト読み取り呼び出し。** `getProjects()`（すべて一覧）、`getProjectDetails()`（IDによる単一プロジェクト）。 | **Panggilan baca proyek.** `getProjects()` (daftar semua), `getProjectDetails()` (satu proyek berdasarkan ID). |
| `mappers.ts` | **Project data transformer.** `mapProjectListItem()` — normalizes API items into frontend project objects. | **プロジェクトデータ変換器。** `mapProjectListItem()` — APIアイテムをフロントエンドプロジェクトオブジェクトに正規化。 | **Transformer data proyek.** `mapProjectListItem()` — menormalisasi item API menjadi objek proyek frontend. |
| `types.ts` | **Project type definitions.** `Project`, `ProjectDetail`, `ProjectDetailResponse`, `CompanyProjectRelation`, `Equipment`, `ProjectListItem`, `ProjectListResponse`. | **プロジェクト型定義。** `Project`、`ProjectDetail`、`ProjectDetailResponse`、`CompanyProjectRelation`、`Equipment`、`ProjectListItem`、`ProjectListResponse`。 | **Definisi tipe proyek.** `Project`, `ProjectDetail`, `ProjectDetailResponse`, `CompanyProjectRelation`, `Equipment`, `ProjectListItem`, `ProjectListResponse`. |

---

### `site/` — Site & Equipment Warehouse (Gudang Site & Peralatan)

| File | English | 日本語 (Japanese) | Indonesia |
|------|---------|------------------|-----------|
| `dashboard.ts` | **Site dashboard data.** `getSiteData()` — fetches dashboard metrics for a single site. | **サイトダッシュボードデータ。** `getSiteData()` — 単一サイトのダッシュボードメトリクスを取得。 | **Data dashboard site.** `getSiteData()` — mengambil metrik dashboard untuk satu site. |
| `list.ts` | **Site listing.** `getSites()` — fetches all sites for a company. | **サイト一覧。** `getSites()` — 企業のすべてのサイトを取得。 | **Daftar site.** `getSites()` — mengambil semua site untuk satu perusahaan. |
| `mutations.ts` | **Site CRUD.** `registerSite()`, `updateSite()`, `deleteSite()`, `validateCreateSite()`, `validateUpdateSite()`. | **サイトCRUD。** `registerSite()`、`updateSite()`、`deleteSite()`、`validateCreateSite()`、`validateUpdateSite()`。 | **CRUD site.** `registerSite()`, `updateSite()`, `deleteSite()`, `validateCreateSite()`, `validateUpdateSite()`. |
| `equipment.ts` | **Equipment management.** `getSiteProjectEquipment()`, `getSiteEquipmentList()`, `getSiteEquipmentById()`, `getSiteEquipmentHistory()`, `joinSiteProject()`, `uploadSiteProjectEquipmentFile()`, `updateSiteEquipment()`, `getSiteProjectEquipmentPresignedUrl()`, plus validation functions. | **設備管理。** `getSiteProjectEquipment()`、`getSiteEquipmentList()`、`getSiteEquipmentById()`、`getSiteEquipmentHistory()`、`joinSiteProject()`、`uploadSiteProjectEquipmentFile()`、`updateSiteEquipment()`、`getSiteProjectEquipmentPresignedUrl()`、さらにバリデーション関数。 | **Manajemen peralatan.** `getSiteProjectEquipment()`, `getSiteEquipmentList()`, `getSiteEquipmentById()`, `getSiteEquipmentHistory()`, `joinSiteProject()`, `uploadSiteProjectEquipmentFile()`, `updateSiteEquipment()`, `getSiteProjectEquipmentPresignedUrl()`, ditambah fungsi validasi. |
| `projects.ts` | **Site-project association.** `getSiteProjects()` — fetches projects linked to a site. | **サイト-プロジェクト関連付け。** `getSiteProjects()` — サイトにリンクされたプロジェクトを取得。 | **Asosiasi site-proyek.** `getSiteProjects()` — mengambil proyek yang terhubung ke sebuah site. |
| `mappers.ts` | **Site data transformer.** `mapSiteProject()`, `mapSiteListItem()`, `mapSiteDashboard()`. | **サイトデータ変換器。** `mapSiteProject()`、`mapSiteListItem()`、`mapSiteDashboard()`。 | **Transformer data site.** `mapSiteProject()`, `mapSiteListItem()`, `mapSiteDashboard()`. |
| `payloadMappers.ts` | **Form-to-API transformer for sites.** `buildAddress()`, `mapCreateSitePayload()`, `mapUpdateSitePayload()`. | **サイトのフォーム→API変換器。** `buildAddress()`、`mapCreateSitePayload()`、`mapUpdateSitePayload()`。 | **Transformer form-to-API untuk site.** `buildAddress()`, `mapCreateSitePayload()`, `mapUpdateSitePayload()`. |
| `payloads.ts` | **Site payload types.** `CreateSitePayload`, `UpdateSitePayload`. | **サイトペイロード型。** `CreateSitePayload`、`UpdateSitePayload`。 | **Tipe payload site.** `CreateSitePayload`, `UpdateSitePayload`. |
| `types.ts` | **Site type definitions.** `Site`, `CreateSiteData`, `SiteAddResponse`, `SiteFormData`, `SiteDashboardMetrics`, `SiteDashboardData`, `SiteProject`, `SiteListItem`, `SiteEquipmentHistory`, etc. | **サイト型定義。** `Site`、`CreateSiteData`、`SiteAddResponse`、`SiteFormData`、`SiteDashboardMetrics`、`SiteDashboardData`、`SiteProject`、`SiteListItem`、`SiteEquipmentHistory`など。 | **Definisi tipe site.** `Site`, `CreateSiteData`, `SiteAddResponse`, `SiteFormData`, `SiteDashboardMetrics`, `SiteDashboardData`, `SiteProject`, `SiteListItem`, `SiteEquipmentHistory`, dll. |

---

### `users/` — User Management Office (Kantor Manajemen User)

| File | English | 日本語 (Japanese) | Indonesia |
|------|---------|------------------|-----------|
| `list.ts` | **User listing.** `getUsers()` — fetches all users for a company. | **ユーザー一覧。** `getUsers()` — 企業のすべてのユーザーを取得。 | **Daftar user.** `getUsers()` — mengambil semua user untuk satu perusahaan. |
| `mutations.ts` | **User CRUD.** `registerUser()`, `updateUser()`, `deleteUser()`. | **ユーザーCRUD。** `registerUser()`、`updateUser()`、`deleteUser()`。 | **CRUD user.** `registerUser()`, `updateUser()`, `deleteUser()`. |
| `types.ts` | **User type definitions.** `Company`, `User`, `UserProfile`, `UserList`, `UserProfileResponse`, `UserFormData`. | **ユーザー型定義。** `Company`、`User`、`UserProfile`、`UserList`、`UserProfileResponse`、`UserFormData`。 | **Definisi tipe user.** `Company`, `User`, `UserProfile`, `UserList`, `UserProfileResponse`, `UserFormData`. |

---

## API Call Map

### How a Request Flows (Alur Permintaan)

| Step | English | 日本語 (Japanese) | Indonesia |
|------|---------|------------------|-----------|
| 1 | **Pages** (`src/pages/...`) — User action triggers the request (button click, form submit). | **ページ**（`src/pages/...`）— ユーザーアクションがリクエストをトリガー（ボタンクリック、フォーム送信）。 | **Halaman** (`src/pages/...`) — Aksi user memicu permintaan (klik tombol, kirim form). |
| 2 | **Contexts** (`src/contexts/...`) — If the payload is built from shared form state across multiple steps. | **コンテキスト**（`src/contexts/...`）— ペイロードが複数ステップの共有フォームステートから構築される場合。 | **Konteks** (`src/contexts/...`) — Jika payload dibangun dari state form bersama lintas beberapa langkah. |
| 3 | **API module** (`src/api/...`) — The real request function and contract. | **APIモジュール**（`src/api/...`）— 実際のリクエスト関数とコントラクト。 | **Modul API** (`src/api/...`) — Fungsi permintaan yang sebenarnya dan kontraknya. |
| 4 | **Mocks** (`src/mocks/...`) — Whether the request is being intercepted in development. | **モック**（`src/mocks/...`）— 開発中にリクエストが傍受されるかどうか。 | **Mock** (`src/mocks/...`) — Apakah permintaan diintercept saat pengembangan. |
| 5 | **Backend** — The request reaches the real server. | **バックエンド** — リクエストが実際のサーバーに届く。 | **Backend** — Permintaan sampai ke server yang sebenarnya. |

### Quick Reading Order

| Step | File to Read | English | 日本語 (Japanese) | Indonesia |
|------|-------------|---------|------------------|-----------|
| 1 | `src/pages/...` | See what triggers the action | アクションをトリガーするものを見る | Lihat apa yang memicu aksi |
| 2 | `src/contexts/...` | If payload is built from shared state | ペイロードが共有ステートから構築される場合 | Jika payload dibangun dari state bersama |
| 3 | `src/api/...` | See the real request function and contract | 実際のリクエスト関数とコントラクトを見る | Lihat fungsi permintaan yang sebenarnya dan kontraknya |
| 4 | `src/mocks/...` | See if the request is intercepted in development | 開発中にリクエストが傍受されるか確認 | Lihat apakah permintaan diintercept saat pengembangan |
| 5 | `public/mockServiceWorker.js` | Confirm the mock layer actually starts | モックレイヤーが実際に開始されることを確認 | Konfirmasi bahwa layer mock benar-benar berjalan |

---

## MSW Mock Flow

### Why MSW Is Used

| English | 日本語 (Japanese) | Indonesia |
|---------|------------------|-----------|
| MSW sits between the browser and the network. Instead of letting a request leave the browser, it intercepts the request and returns a controlled response from the frontend layer. This gives us three practical benefits: (1) The UI can be developed against a realistic request/response contract. (2) The Network tab still shows a real fetch call. (3) Backend developers can receive a stable contract example before the real implementation exists. | MSWはブラウザとネットワークの間に位置します。リクエストがブラウザを出る代わりに、リクエストを傍受し、フロントエンド層から制御されたレスポンスを返します。これにより3つの実用的な利点があります：（1）UIは現実的なリクエスト/レスポンスコントラクトに対して開発できます。（2）ネットワークタブにはまだ実際のfetch呼び出しが表示されます。（3）バックエンド開発者は実際の実装が存在する前に安定したコントラクト例を受け取ることができます。 | MSW berada di antara browser dan jaringan. Alih-alih membiarkan permintaan keluar dari browser, MSW mengintercept permintaan dan mengembalikan respons yang dikontrol dari layer frontend. Ini memberikan tiga manfaat praktis: (1) UI dapat dikembangkan terhadap kontrak request/response yang realistis. (2) Tab Network tetap menampilkan panggilan fetch yang nyata. (3) Backend developer bisa menerima contoh kontrak yang stabil sebelum implementasi sebenarnya ada. |

### Current Mocked Endpoint

| English | 日本語 (Japanese) | Indonesia |
|---------|------------------|-----------|
| Only `POST /company/signup` is mocked. The mock returns a success response with a generated user ID so the signup flow can proceed. | `POST /company/signup`のみがモックされています。モックは生成されたユーザーID付きの成功レスポンスを返し、サインアップフローを進行させます。 | Hanya `POST /company/signup` yang di-mock. Mock mengembalikan respons sukses dengan ID user yang di-generate supaya alur signup bisa dilanjutkan. |

### Files Involved

| File | English | 日本語 (Japanese) | Indonesia |
|------|---------|------------------|-----------|
| `src/mocks/handlers.ts` | Defines request handlers. Contains the `POST */company/signup` interception rule. | リクエストハンドラーを定義。`POST */company/signup`傍受ルールを含む。 | Mendefinisikan handler permintaan. Berisi aturan intercept `POST */company/signup`. |
| `src/mocks/browser.ts` | Creates the browser worker with `setupWorker(...handlers)`. | `setupWorker(...handlers)`でブラウザワーカーを作成。 | Membuat browser worker dengan `setupWorker(...handlers)`. |
| `src/mocks/mocks.ts` | Exposes `enableMocking()`. Starts the worker only in development mode. | `enableMocking()`を公開。開発モードでのみワーカーを起動。 | Mengekspos `enableMocking()`. Memulai worker hanya dalam mode pengembangan. |
| `src/main.tsx` | Calls `enableMocking()` during startup so the mock layer is ready before any request. | 起動時に`enableMocking()`を呼び出し、モックレイヤーをリクエスト前に準備。 | Memanggil `enableMocking()` saat startup supaya layer mock siap sebelum ada permintaan. |
| `public/mockServiceWorker.js` | Required service worker file generated by MSW. Without this, mocks cannot start. | MSWが生成した必須サービスワーカーファイル。これがないとモックは起動できません。 | File service worker wajib yang di-generate oleh MSW. Tanpa ini, mock tidak bisa dimulai. |

### Request Flow (Step-by-Step)

| Step | English | 日本語 (Japanese) | Indonesia |
|------|---------|------------------|-----------|
| 1 | Form data is collected in signup pages and stored in context. | フォームデータがサインアップページで収集され、コンテキストに保存される。 | Data form dikumpulkan di halaman signup dan disimpan di konteks. |
| 2 | Confirmation step builds the API payload. | 確認ステップがAPIペイロードを構築する。 | Langkah konfirmasi membangun payload API. |
| 3 | Frontend calls `signUp(payload)` from `src/api/auth.ts`. | フロントエンドが`src/api/auth.ts`から`signUp(payload)`を呼び出す。 | Frontend memanggil `signUp(payload)` dari `src/api/auth.ts`. |
| 4 | `signUp()` uses `fetch()` to send the request. | `signUp()`は`fetch()`を使用してリクエストを送信。 | `signUp()` menggunakan `fetch()` untuk mengirim permintaan. |
| 5 | MSW intercepts the request in the browser. | MSWがブラウザ内でリクエストを傍受する。 | MSW mengintercept permintaan di dalam browser. |
| 6 | `handlers.ts` returns a mocked response. | `handlers.ts`がモックレスポンスを返す。 | `handlers.ts` mengembalikan respons mock. |
| 7 | UI receives the response and continues to completion state. | UIがレスポンスを受信し、完了状態に進む。 | UI menerima respons dan melanjutkan ke status selesai. |

---

## User System Architecture

### Role-Based Access Control

| Role | English | 日本語 (Japanese) | Indonesia |
|------|---------|------------------|-----------|
| **Master User** | First person from a company to sign up. Can manage company sites (拠点), assign users as 担当者 (person in charge) for sites, and view all projects joined by any company site. | 企業からサインアップした最初の人物。企業サイト（拠点）を管理し、サイトの担当者を割り当て、企業サイトが参加したすべてのプロジェクトを表示できます。 | Orang pertama dari perusahaan yang mendaftar. Bisa mengelola site perusahaan, menetapkan user sebagai penanggung jawab untuk site, dan melihat semua proyek yang diikuti oleh site perusahaan mana pun. |
| **Standard User (一般)** | Created by master user. Can only view projects for sites they're assigned to as 担当者. Limited management capabilities. | 管理者ユーザーが作成。担当者として割り当てられたサイトのプロジェクトのみ表示可能。管理機能は制限。 | Dibuat oleh master user. Hanya bisa melihat proyek untuk site di mana mereka ditetapkan sebagai penanggung jawab. Kemampuan manajemen terbatas. |

### Site System (拠点)

| English | 日本語 (Japanese) | Indonesia |
|---------|------------------|-----------|
| Each company can have multiple sites. Each site must have a 担当者 (person in charge). Sites join projects independently. Master users see all sites; standard users see only their assigned sites. | 各企業は複数のサイトを持つことができます。各サイトには担当者が必要です。サイトは独立してプロジェクトに参加します。管理者ユーザーはすべてのサイトを表示；一般ユーザーは割り当てられたサイトのみ表示。 | Setiap perusahaan bisa memiliki multiple site. Setiap site harus punya penanggung jawab. Site bergabung dengan proyek secara independen. Master user melihat semua site; standard user hanya melihat site yang ditetapkan untuk mereka. |

### First Login Flow

| Step | English | 日本語 (Japanese) | Indonesia |
|------|---------|------------------|-----------|
| 1 | User signs up and admin approves. | ユーザーがサインアップし、管理者が承認。 | User mendaftar dan admin menyetujui. |
| 2 | On first login, if user has no sites and no joined projects → redirect to project list. | 初回ログイン時、サイトがなく参加プロジェクトもない場合 → プロジェクト一覧にリダイレクト。 | Saat login pertama, jika user tidak punya site dan tidak ada proyek yang diikuti → redirect ke daftar proyek. |
| 3 | User sees available projects with 「参加する」 button. | 利用可能なプロジェクトと「参加する」ボタンが表示される。 | User melihat proyek yang tersedia dengan tombol 「参加する」. |
| 4 | If user clicks 「参加する」 but has no sites → redirect to create site first. | 「参加する」をクリックしたがサイトがない場合 → まずサイト作成にリダイレクト。 | Jika user mengklik 「参加する」 tapi tidak punya site → redirect ke pembuatan site terlebih dahulu. |

---

## Required Endpoints

### Endpoint Summary Table

| # | Method | Route | Module | English | 日本語 (Japanese) | Indonesia |
|---|--------|-------|--------|---------|------------------|-----------|
| 1 | POST | `/auth/user/login` | auth | User login | ユーザーログイン | Login user |
| 2 | POST | `/auth/admin/login` | auth | Admin login | 管理者ログイン | Login admin |
| 3 | POST | `/company/signup` | auth | Sign up | サインアップ | Daftar akun |
| 4 | POST | `/auth/user/change-password` | auth | Change password | パスワード変更 | Ubah password |
| 5 | POST | `/auth/user/request-password-reset` | auth | Request password reset | パスワードリセットリクエスト | Minta reset password |
| 6 | POST | `/auth/user/complete-password-reset` | auth | Complete password reset | パスワードリセット完了 | Selesaikan reset password |
| 7 | GET | `/project` | projects | List projects | プロジェクト一覧 | Daftar proyek |
| 8 | GET | `/project/:projectId` | projects | Project detail | プロジェクト詳細 | Detail proyek |
| 9 | POST | `/monitoring` | monitoring | Submit monitoring data | モニタリングデータ送信 | Kirim data monitoring |
| 10 | POST | `/monitoring/upload-elpv-evidence` | monitoring | Upload ELPV evidence | ELPV証拠アップロード | Upload bukti ELPV |
| 11 | POST | `/monitoring/upload-elpvr-evidence` | monitoring | Upload ELPVR evidence | ELPVR証拠アップロード | Upload bukti ELPVR |
| 12 | POST | `/monitoring/upload-elpj-battery-specification-evidence` | monitoring | Upload ELPJ battery evidence | ELPJバッテリー証拠アップロード | Upload bukti baterai ELPJ |
| 13 | POST | `/monitoring/upload-elpj-control-evidence` | monitoring | Upload ELPJ control evidence | ELPJ制御証拠アップロード | Upload bukti kontrol ELPJ |
| 14 | GET | `/monitoring` | monitoring | Get recent monitoring | 最新モニタリング取得 | Ambil data monitoring terbaru |
| 15 | GET | `/site` | site | List sites | サイト一覧 | Daftar site |
| 16 | GET | `/site/:siteId` | site | Site dashboard | サイトダッシュボード | Dashboard site |
| 17 | POST | `/site` | site | Create site | サイト作成 | Buat site |
| 18 | PATCH | `/site/:siteId` | site | Update site | サイト更新 | Update site |
| 19 | DELETE | `/site/:siteId` | site | Delete site | サイト削除 | Hapus site |
| 20 | GET | `/site/:siteId/project` | site | Site projects | サイトプロジェクト | Proyek site |
| 21 | GET | `/user` | users | List users | ユーザー一覧 | Daftar user |
| 22 | POST | `/user` | users | Create user | ユーザー作成 | Buat user |
| 23 | PATCH | `/user` | users | Update user | ユーザー更新 | Update user |
| 24 | DELETE | `/user/:userId` | users | Delete user | ユーザー削除 | Hapus user |
| 25 | GET | `/auth/user/profile` | profile | Get my profile | 自分のプロフィール | Ambil profil saya |
| 26 | GET | `/user/profile/:userId` | profile | Get user profile | ユーザープロフィール | Ambil profil user |
| 27 | PATCH | `/company` | company | Update company | 企業更新 | Update perusahaan |
| 28 | GET | `/notification/list` | notifications | List notifications | 通知一覧 | Daftar notifikasi |
| 29 | GET | `/notification/count` | notifications | Unread count | 未読数 | Jumlah belum dibaca |
| 30 | PATCH | `/notification/:id/read` | notifications | Mark as read | 既読マーク | Tandai sudah dibaca |
| 31 | PATCH | `/notification/:id/delete` | notifications | Delete notification | 通知削除 | Hapus notifikasi |
| 32 | GET | `/admin/companies/pending` | admin | Pending companies | 保留中企業 | Perusahaan pending |
| 33 | POST | `/admin/companies/:id/approve` | admin | Approve company | 企業承認 | Setujui perusahaan |
| 34 | POST | `/admin/companies/:id/reject` | admin | Reject company | 企業却下 | Tolak perusahaan |
| 35 | GET | `/admin/companies` | admin | List all companies | 企業一覧 | Daftar semua perusahaan |
| 36 | GET | `/admin/companies/:id` | admin | Company detail | 企業詳細 | Detail perusahaan |
| 37 | POST | `/project` | admin | Create project | プロジェクト作成 | Buat proyek |
| 38 | PATCH | `/project/:id` | admin | Update project | プロジェクト更新 | Update proyek |
| 39 | PATCH | `/project/:id/status` | admin | Update project status | プロジェクトステータス更新 | Update status proyek |
| 40 | GET | `/project/:id` | admin | Admin project detail | 管理者プロジェクト詳細 | Detail proyek admin |
| 41 | GET | `/batch/history` | batch | Batch history | バッチ履歴 | Riwayat batch |
| 42 | GET | `/batch/emission-trend` | batch | Emission trend | 排出トレンド | Tren emisi |
| 43 | GET | `/batch/emission-summary` | batch | Emission summary | 排出サマリー | Ringkasan emisi |

---

### 1. User Login (POST)

**Route**: `POST /auth/user/login`
**Frontend Function**: `login(companyId, identifier, password)` in `src/api/auth/auth.ts`

**Request Contract**:
```http
POST {BASE_URL}/auth/user/login
Content-Type: application/json

{
  "companyId": "company-uuid",
  "identifier": "user@example.com",
  "password": "securePassword123"
}
```

**Expected Response**:
```json
{
  "statusCode": "200",
  "role": "master",
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "userId": "user-uuid-here",
  "user": {
    "id": "user-uuid-here",
    "email": "user@example.com",
    "username": "山田太郎"
  },
  "companyName": "デモ株式会社",
  "siteId": "site-uuid-1"
}
```

**Role Values**: `'master' | 'user' | 'admin'`

**Frontend Integration**: `src/pages/Login.tsx`, `src/contexts/AuthContext.tsx`

**Backend Requirements**:
| English | 日本語 (Japanese) | Indonesia |
|---------|------------------|-----------|
| Verify password hash. Return JWT token with appropriate expiration. Include role, userId, companyName, and siteId. | パスワードハッシュを検証。適切な有効期限のJWTトークンを返却。role、userId、companyName、siteIdを含める。 | Verifikasi hash password. Kembalikan token JWT dengan masa berlaku yang sesuai. Sertakan role, userId, companyName, dan siteId. |

---

### 2. Admin Login (POST)

**Route**: `POST /auth/admin/login`
**Frontend Function**: `adminLogin(identifier, password)` in `src/api/auth/auth.ts`

**Request Contract**:
```http
POST {BASE_URL}/auth/admin/login
Content-Type: application/json

{
  "identifier": "admin@example.com",
  "password": "securePassword123"
}
```

**Expected Response**: Same shape as user login with `role: "admin"`.

---

### 3. Sign Up (POST)

**Route**: `POST /company/signup`
**Frontend Function**: `signUp(userData)` in `src/api/auth/auth.ts`

**Request Contract**:
```http
POST {BASE_URL}/company/signup
Content-Type: application/json

{
  "corporateNumber": "1234567890123",
  "corporateName": "デモ株式会社",
  "corporateNameKana": "デモカブシキガイシャ",
  "companyNameEnglish": "Demo Corporation",
  "corporateEmail": "info@demo.co.jp",
  "corporatePhoneNumber": "03-1234-5678",
  "postalCode": "100-0001",
  "prefecture": "東京都",
  "municipality": "千代田区",
  "blocknum": "丸の内1-1-1",
  "buildingName": "〇〇ビル 5階",
  "lastName": "山田",
  "firstName": "太郎",
  "lastNameKana": "ヤマダ",
  "firstNameKana": "タロウ",
  "position": "環境管理責任者",
  "phoneNumber": "03-1234-5678",
  "faxNumber": "03-1234-5679",
  "email": "yamada@demo.co.jp",
  "methodology": ["EN-R-002", "ISO14001"],
  "activityInformation": "CO2排出量を30%削減"
}
```

**Expected Response**:
```json
{
  "success": true,
  "message": "アカウントが作成されました。管理者の承認をお待ちください。",
  "userId": "user-uuid-here"
}
```

**Backend Requirements**:
| English | 日本語 (Japanese) | Indonesia |
|---------|------------------|-----------|
| First signup from a company → create master user. Hash password before storage. Create company record if new. Send verification email. Mark account as pending admin approval. Return user ID. | 企業からの最初のサインアップ → マスターユーザーを作成。保存前にパスワードをハッシュ化。新規の場合は企業レコードを作成。確認メールを送信。管理者承認待ちとしてマーク。ユーザーIDを返却。 | Pendaftaran pertama dari perusahaan → buat master user. Hash password sebelum disimpan. Buat record perusahaan jika baru. Kirim email verifikasi. Tandai akun sebagai menunggu persetujuan admin. Kembalikan ID user. |

---

### 4. Change Password (POST)

**Route**: `POST /auth/user/change-password`
**Frontend Function**: `changePassword(payload)` in `src/api/auth/auth.ts`

**Request Contract**:
```http
POST {BASE_URL}/auth/user/change-password
Authorization: Bearer {token}
Content-Type: application/json

{
  "userId": "user-uuid",
  "currentPassword": "oldPassword123",
  "newPassword": "newPassword456"
}
```

**Expected Response**:
```json
{
  "statusCode": "200",
  "message": "パスワードが変更されました"
}
```

---

### 5. Request Password Reset (POST)

**Route**: `POST /auth/user/request-password-reset`
**Frontend Function**: `requestPasswordReset(email)` in `src/api/auth/auth.ts`

**Request Contract**:
```http
POST {BASE_URL}/auth/user/request-password-reset
Content-Type: application/json

{
  "email": "user@example.com"
}
```

**Expected Response**:
```json
{
  "statusCode": "200",
  "message": "パスワードリセットメールを送信しました",
  "timestamp": "2024-01-15T14:30:00Z"
}
```

---

### 6. Complete Password Reset (POST)

**Route**: `POST /auth/user/complete-password-reset`
**Frontend Function**: `completePasswordReset(token, newPassword)` in `src/api/auth/auth.ts`

**Request Contract**:
```http
POST {BASE_URL}/auth/user/complete-password-reset
Content-Type: application/json

{
  "actionSessionToken": "reset-token-from-email",
  "newPassword": "newPassword456"
}
```

**Expected Response**:
```json
{
  "statusCode": "200",
  "message": "パスワードがリセットされました",
  "timestamp": "2024-01-15T14:35:00Z"
}
```

---

### 7. List Projects (GET)

**Route**: `GET /project`
**Frontend Function**: `getProjects()` in `src/api/projects/queries.ts`

**Request Contract**:
```http
GET {BASE_URL}/project
Authorization: Bearer {token}
```

**Expected Response**:
```json
{
  "statusCode": "200",
  "data": [
    {
      "id": "proj-uuid-1",
      "name": "環境モニタリングプロジェクトA",
      "description": "CO2排出量削減を目的とした環境モニタリング",
      "methodology": "EN-S-001",
      "lastUpdate": "2024-01-15",
      "status": "active",
      "devices": 8,
      "isJoined": true,
      "locationId": "location-uuid-1",
      "locationName": "東京本社"
    }
  ]
}
```

**Frontend Integration**: `src/pages/Dashboard.tsx`

---

### 8. Get Project Details (GET)

**Route**: `GET /project/:projectId`
**Frontend Function**: `getProjectDetails(projectId)` in `src/api/projects/queries.ts`

**Request Contract**:
```http
GET {BASE_URL}/project/{projectId}
Authorization: Bearer {token}
```

**Expected Response**:
```json
{
  "statusCode": "200",
  "data": {
    "id": "proj-uuid-1",
    "name": "環境モニタリングプロジェクトA",
    "description": "CO2排出量削減を目的とした環境モニタリング",
    "methodology": "EN-S-001",
    "lastUpdate": "2024-01-15",
    "status": "active",
    "devices": 8,
    "companyName": "デモ株式会社",
    "location": "東京都千代田区丸の内1-1-1",
    "deviceList": [
      {
        "id": "dev-001",
        "name": "空気質センサー1",
        "type": "PM2.5モニター",
        "status": "online",
        "lastReading": "2024-01-15 14:30:00"
      }
    ],
    "recentData": [
      {
        "timestamp": "2024-01-15 14:30:00",
        "parameter": "PM2.5",
        "value": 12.5,
        "unit": "μg/m³"
      }
    ],
    "documents": [
      {
        "id": "DOC001",
        "name": "環境影響評価報告書.pdf",
        "type": "PDF",
        "uploadDate": "2024-01-10",
        "size": "2.4 MB"
      }
    ]
  }
}
```

**Frontend Integration**: `src/pages/ProjectDetail.tsx`

---

### 9. Submit Monitoring Data (POST)

**Route**: `POST /monitoring`
**Frontend Function**: `submitMonitoringData(data)` in `src/api/monitoring/mutations.ts`

**Request Contract**:
```http
POST {BASE_URL}/monitoring
Authorization: Bearer {token}
Content-Type: application/json

{
  "projectId": "proj-uuid-1",
  "deviceId": "dev-001",
  "parameter": "PM2.5",
  "value": 12.5,
  "unit": "μg/m³",
  "timestamp": "2024-01-15T14:30:00Z"
}
```

**Expected Response**:
```json
{
  "success": true,
  "message": "モニタリングデータが正常に送信されました",
  "dataId": "data-uuid-here"
}
```

**Frontend Integration**: `src/pages/MonitoringRegister.tsx`

---

### 10-13. Upload Monitoring Evidence Files (POST)

All four upload endpoints follow the same pattern:

| # | Route | Function | Evidence Kind |
|---|-------|----------|---------------|
| 10 | `POST /monitoring/upload-elpv-evidence` | `uploadElpvFile()` | ELPV |
| 11 | `POST /monitoring/upload-elpvr-evidence` | `uploadElpvrFile()` | ELPVR |
| 12 | `POST /monitoring/upload-elpj-battery-specification-evidence` | `uploadElpjBatteryFile()` | ELPJ Battery |
| 13 | `POST /monitoring/upload-elpj-control-evidence` | `uploadElpjControlFile()` | ELPJ Control |

**Request Contract** (same for all):
```http
POST {BASE_URL}/monitoring/upload-{kind}-evidence?projectId={projectId}&siteId={siteId}&description={description}
Authorization: Bearer {token}
Content-Type: multipart/form-data

file: <binary file data>
```

**Expected Response**:
```json
{
  "success": true,
  "message": "ファイルがアップロードされました",
  "fileId": "file-uuid-here"
}
```

---

### 14. Get Recent Monitoring (GET)

**Route**: `GET /monitoring`
**Frontend Function**: `getMonitoringRecent(params)` in `src/api/monitoring/queries.ts`

**Request Contract**:
```http
GET {BASE_URL}/monitoring?projectId={projectId}&siteId={siteId}&limit={limit}
Authorization: Bearer {token}
```

**Expected Response**:
```json
{
  "statusCode": "200",
  "data": [
    {
      "id": "mon-uuid-1",
      "parameter": "PM2.5",
      "value": 12.5,
      "unit": "μg/m³",
      "timestamp": "2024-01-15T14:30:00Z",
      "deviceId": "dev-001",
      "deviceName": "空気質センサー1"
    }
  ]
}
```

---

### 15. List Sites (GET)

**Route**: `GET /site`
**Frontend Function**: `getSites()` in `src/api/site/list.ts`

**Request Contract**:
```http
GET {BASE_URL}/site
Authorization: Bearer {token}
```

**Expected Response**:
```json
{
  "statusCode": "200",
  "data": [
    {
      "id": "site-uuid-1",
      "name": "東京本社",
      "postalCode": "100-0001",
      "prefecture": "東京都",
      "city": "千代田区",
      "address": "丸の内1-1-1",
      "building": "〇〇ビル 5階",
      "personInChargeId": "user-uuid-1",
      "personInChargeName": "山田太郎",
      "joinedProjects": ["proj-uuid-1", "proj-uuid-2"],
      "createdAt": "2024-01-15"
    }
  ]
}
```

**Backend Requirements**:
| English | 日本語 (Japanese) | Indonesia |
|---------|------------------|-----------|
| Master users: return all company sites. Standard users: return only sites they're 担当者 of. Include person in charge details and joined project IDs. | 管理者ユーザー：すべての企業サイトを返却。一般ユーザー：担当者として割り当てられたサイトのみ返却。担当者詳細と参加プロジェクトIDを含める。 | Master user: kembalikan semua site perusahaan. Standard user: kembalikan hanya site di mana mereka adalah penanggung jawab. Sertakan detail penanggung jawab dan ID proyek yang diikuti. |

---

### 16. Get Site Dashboard (GET)

**Route**: `GET /site/:siteId`
**Frontend Function**: `getSiteData(siteId)` in `src/api/site/dashboard.ts`

**Request Contract**:
```http
GET {BASE_URL}/site/{siteId}
Authorization: Bearer {token}
```

**Expected Response**:
```json
{
  "statusCode": "200",
  "data": {
    "id": "site-uuid-1",
    "name": "東京本社",
    "postalCode": "100-0001",
    "prefecture": "東京都",
    "city": "千代田区",
    "address": "丸の内1-1-1",
    "building": "〇〇ビル 5階",
    "personInChargeId": "user-uuid-1",
    "personInChargeName": "山田太郎",
    "personInChargeEmail": "yamada@demo.co.jp",
    "joinedProjects": [
      {
        "id": "proj-uuid-1",
        "name": "環境モニタリングプロジェクトA",
        "status": "active",
        "joinedDate": "2024-01-15"
      }
    ],
    "createdAt": "2024-01-15"
  }
}
```

---

### 17. Create Site (POST)

**Route**: `POST /site`
**Frontend Function**: `registerSite(siteData)` in `src/api/site/mutations.ts`

**Request Contract**:
```http
POST {BASE_URL}/site
Authorization: Bearer {token}
Content-Type: application/json

{
  "siteName": "大阪支店",
  "postalCode": "530-0001",
  "prefecture": "大阪府",
  "city": "大阪市北区",
  "blockName": "梅田1-1-1",
  "addressDetail": "△△ビル 3階",
  "picId": "user-uuid-2"
}
```

**Expected Response**:
```json
{
  "success": true,
  "message": "拠点が正常に登録されました",
  "siteId": "site-uuid-new"
}
```

**Backend Requirements**:
| English | 日本語 (Japanese) | Indonesia |
|---------|------------------|-----------|
| Only master users can create sites. Validate person in charge is a user in the company. Associate site with company. Return site ID. | 管理者のみがサイトを作成可能。担当者が企業のユーザーであることを検証。サイトを企業に関連付け。サイトIDを返却. | Hanya master user yang bisa membuat site. Validasi penanggung jawab adalah user dari perusahaan yang bersangkutan. Hubungkan site dengan perusahaan. Kembalikan ID site. |

---

### 18. Update Site (PATCH)

**Route**: `PATCH /site/:siteId`
**Frontend Function**: `updateSite(siteId, updates)` in `src/api/site/mutations.ts`

**Request Contract**:
```http
PATCH {BASE_URL}/site/{siteId}
Authorization: Bearer {token}
Content-Type: application/json

{
  "siteName": "大阪支店（更新）",
  "postalCode": "530-0001",
  "prefecture": "大阪府",
  "city": "大阪市北区",
  "blockName": "梅田1-1-1",
  "addressDetail": "△△ビル 3階",
  "picId": "user-uuid-3"
}
```

**Expected Response**:
```json
{
  "success": true,
  "message": "拠点情報を更新しました"
}
```

---

### 19. Delete Site (DELETE)

**Route**: `DELETE /site/:siteId`
**Frontend Function**: `deleteSite(siteId)` in `src/api/site/mutations.ts`

**Request Contract**:
```http
DELETE {BASE_URL}/site/{siteId}
Authorization: Bearer {token}
```

**Expected Response**:
```json
{
  "success": true,
  "message": "拠点が削除されました"
}
```

---

### 20. Get Site Projects (GET)

**Route**: `GET /site/:siteId/project`
**Frontend Function**: `getSiteProjects(siteId)` in `src/api/site/projects.ts`

**Request Contract**:
```http
GET {BASE_URL}/site/{siteId}/project
Authorization: Bearer {token}
```

**Expected Response**:
```json
{
  "statusCode": "200",
  "data": [
    {
      "id": "proj-uuid-1",
      "name": "環境モニタリングプロジェクトA",
      "status": "active",
      "joinedDate": "2024-01-15"
    }
  ]
}
```

---

### 21. List Users (GET)

**Route**: `GET /user`
**Frontend Function**: `getUsers()` in `src/api/users/list.ts`

**Request Contract**:
```http
GET {BASE_URL}/user
Authorization: Bearer {token}
```

**Expected Response**:
```json
{
  "statusCode": "200",
  "data": [
    {
      "id": "user-uuid-1",
      "name": "山田太郎",
      "email": "yamada@demo.co.jp",
      "role": "master",
      "assignedLocations": ["location-uuid-1", "location-uuid-2"]
    },
    {
      "id": "user-uuid-2",
      "name": "佐藤花子",
      "email": "sato@demo.co.jp",
      "role": "user",
      "assignedLocations": ["location-uuid-1"]
    }
  ]
}
```

---

### 22. Create User (POST)

**Route**: `POST /user`
**Frontend Function**: `registerUser(userData)` in `src/api/users/mutations.ts`

**Request Contract**:
```http
POST {BASE_URL}/user
Authorization: Bearer {token}
Content-Type: application/json

{
  "name": "新規太郎",
  "email": "shinki@demo.co.jp",
  "role": "user",
  "assignedLocations": ["location-uuid-1"]
}
```

**Expected Response**:
```json
{
  "success": true,
  "message": "ユーザーが作成されました",
  "userId": "user-uuid-new"
}
```

---

### 23. Update User (PATCH)

**Route**: `PATCH /user`
**Frontend Function**: `updateUser(userData)` in `src/api/users/mutations.ts`

**Request Contract**:
```http
PATCH {BASE_URL}/user
Authorization: Bearer {token}
Content-Type: application/json

{
  "userId": "user-uuid-2",
  "name": "佐藤花子（更新）",
  "email": "sato-new@demo.co.jp",
  "role": "user",
  "assignedLocations": ["location-uuid-1", "location-uuid-3"]
}
```

**Expected Response**:
```json
{
  "success": true,
  "message": "ユーザーが更新されました"
}
```

---

### 24. Delete User (DELETE)

**Route**: `DELETE /user/:userId`
**Frontend Function**: `deleteUser(userId)` in `src/api/users/mutations.ts`

**Request Contract**:
```http
DELETE {BASE_URL}/user/{userId}
Authorization: Bearer {token}
```

**Expected Response**:
```json
{
  "success": true,
  "message": "ユーザーが削除されました"
}
```

---

### 25. Get My Profile (GET)

**Route**: `GET /auth/user/profile`
**Frontend Function**: `getProfile()` in `src/api/profile/profile.ts`

**Request Contract**:
```http
GET {BASE_URL}/auth/user/profile
Authorization: Bearer {token}
```

**Expected Response**:
```json
{
  "statusCode": "200",
  "data": {
    "id": "user-uuid-1",
    "email": "yamada@demo.co.jp",
    "name": "山田太郎",
    "role": "master",
    "company": {
      "id": "company-uuid",
      "name": "デモ株式会社"
    }
  }
}
```

---

### 26. Get User Profile by ID (GET)

**Route**: `GET /user/profile/:userId`
**Frontend Function**: `getUserProfileById(userId)` in `src/api/profile/profile.ts`

**Request Contract**:
```http
GET {BASE_URL}/user/profile/{userId}
Authorization: Bearer {token}
```

**Expected Response**: Same shape as "Get My Profile" but for the specified user.

---

### 27. Update Company (PATCH)

**Route**: `PATCH /company`
**Frontend Function**: `updateCompany(data)` in `src/api/company/mutations.ts`

**Request Contract**:
```http
PATCH {BASE_URL}/company
Authorization: Bearer {token}
Content-Type: application/json

{
  "corporateName": "デモ株式会社（更新）",
  "postalCode": "100-0001",
  "prefecture": "東京都",
  "city": "千代田区",
  "blocknum": "丸の内1-1-1",
  "buildingName": "〇〇ビル 5階"
}
```

**Expected Response**:
```json
{
  "statusCode": "200",
  "message": "企業情報が更新されました"
}
```

---

### 28. List Notifications (GET)

**Route**: `GET /notification/list`
**Frontend Function**: `getNotifications(params)` in `src/api/notifications/index.ts`

**Request Contract**:
```http
GET {BASE_URL}/notification/list?unreadOnly=false&page=0&size=10
Authorization: Bearer {token}
```

**Expected Response**:
```json
{
  "statusCode": "200",
  "notifications": [
    {
      "id": "notif-uuid-1",
      "title": "プロジェクト参加承認",
      "message": "プロジェクトAへの参加が承認されました",
      "read": false,
      "operatingUserId": 123,
      "participatingUserId": 456,
      "createdAt": "2024-01-15T14:30:00Z",
      "updatedAt": "2024-01-15T14:30:00Z"
    }
  ],
  "pagination": {
    "page": 0,
    "limit": 10,
    "totalItems": 25,
    "totalPages": 3,
    "nextPageToken": null,
    "hasMore": true
  }
}
```

---

### 29. Get Unread Notification Count (GET)

**Route**: `GET /notification/count`
**Frontend Function**: `getUnreadNotificationCount()` in `src/api/notifications/index.ts`

**Request Contract**:
```http
GET {BASE_URL}/notification/count
Authorization: Bearer {token}
```

**Expected Response**:
```json
{
  "statusCode": "200",
  "unreadCount": 5
}
```

---

### 30. Mark Notification as Read (PATCH)

**Route**: `PATCH /notification/:notificationId/read`
**Frontend Function**: `markNotificationAsRead(notificationId)` in `src/api/notifications/index.ts`

**Request Contract**:
```http
PATCH {BASE_URL}/notification/{notificationId}/read
Authorization: Bearer {token}
```

**Expected Response**: No body (void response).

---

### 31. Delete Notification (PATCH)

**Route**: `PATCH /notification/:notificationId/delete`
**Frontend Function**: `deleteNotification(notificationId)` in `src/api/notifications/index.ts`

**Request Contract**:
```http
PATCH {BASE_URL}/notification/{notificationId}/delete
Authorization: Bearer {token}
```

**Expected Response**: No body (void response).

---

### 32. Get Pending Companies (GET)

**Route**: `GET /admin/companies/pending`
**Frontend Function**: `getPendingCompanies()` in `src/api/admin/pendingCompanies.ts`

**Request Contract**:
```http
GET {BASE_URL}/admin/companies/pending
Authorization: Bearer {token}
```

**Expected Response**:
```json
{
  "statusCode": "200",
  "data": [
    {
      "companyId": "company-uuid-1",
      "companyName": "新規株式会社",
      "corporateNumber": "9876543210987",
      "status": "pending",
      "submittedAt": "2024-01-10T09:00:00Z",
      "masterUser": {
        "name": "新規太郎",
        "email": "shinki@newcorp.co.jp"
      }
    }
  ]
}
```

---

### 33. Approve Company (POST)

**Route**: `POST /admin/companies/:companyId/approve`
**Frontend Function**: `approvePendingCompany(companyId)` / `approveAdminCompany(companyId)` in `src/api/admin/pendingCompanies.ts`

**Request Contract**:
```http
POST {BASE_URL}/admin/companies/{companyId}/approve
Authorization: Bearer {token}
Content-Type: application/json

{
  "companyId": "company-uuid-1"
}
```

**Expected Response**:
```json
{
  "success": true,
  "message": "企業が承認されました"
}
```

---

### 34. Reject Company (POST)

**Route**: `POST /admin/companies/:companyId/reject`
**Frontend Function**: `rejectPendingCompany(companyId, reason)` / `rejectAdminCompany(companyId, reason)` in `src/api/admin/pendingCompanies.ts`

**Request Contract**:
```http
POST {BASE_URL}/admin/companies/{companyId}/reject
Authorization: Bearer {token}
Content-Type: application/json

{
  "rejectionReason": "必要書類が不足しています"
}
```

**Expected Response**:
```json
{
  "success": true,
  "message": "企業が却下されました"
}
```

---

### 35-36. Admin Company List & Detail (GET)

**Route**: `GET /admin/companies` / `GET /admin/companies/:companyId`
**Frontend Functions**: `getAdminCompanies()` / `getAdminCompanyDetail(companyId)` in `src/api/admin/pendingCompanies.ts`

Follows standard GET pattern with Bearer token authentication.

---

### 37-40. Admin Project Management

| # | Method | Route | Function | Purpose |
|---|--------|-------|----------|---------|
| 37 | POST | `/project` | `createAdminProject()` | Create new project |
| 38 | PATCH | `/project/:projectId` | `updateAdminProject()` | Update existing project |
| 39 | PATCH | `/project/:projectId/status` | `updateAdminProjectStatus()` | Change project status |
| 40 | GET | `/project/:projectId` | `getAdminProjectDetail()` | Get project details |

All require Bearer token authentication. Project creation uses `CreateAdminProjectPayload` with methodology and equipment data.

---

### 41-43. Batch Data Endpoints

| # | Route | Function | Purpose |
|---|-------|----------|---------|
| 41 | `GET /batch/history` | `getBatchHistory()` | Batch processing history |
| 42 | `GET /batch/emission-trend` | `getEmissionTrend()` | Emission trend data |
| 43 | `GET /batch/emission-summary` | `getProjectEmissionSummary()` | Project emission summary |

All require Bearer token authentication and return paginated results.

---

## Error Handling

### Standard Error Response Format

All endpoints should return consistent error responses:

```json
{
  "success": false,
  "message": "エラーメッセージ（ユーザーに表示される）",
  "code": "ERROR_CODE",
  "details": {}
}
```

### HTTP Status Codes

| Code | English | 日本語 (Japanese) | Indonesia | Frontend Handling |
|------|---------|------------------|-----------|-------------------|
| 200 | Success | 成功 | Sukses | Process response data |
| 201 | Created | 作成済み | Dibuat | Process response data |
| 400 | Bad Request | 不正なリクエスト | Permintaan tidak valid | Show validation errors |
| 401 | Unauthorized | 認証なし | Tidak terautentikasi | Redirect to login |
| 403 | Forbidden | アクセス禁止 | Akses ditolak | Show "access denied" message |
| 404 | Not Found | 見つかりません | Tidak ditemukan | Show "not found" message |
| 500 | Server Error | サーバーエラー | Error server | Show generic error message |

### Role-Based Access Errors

| English | 日本語 (Japanese) | Indonesia |
|---------|------------------|-----------|
| **403 Forbidden** should be returned when: standard user tries to access master-only endpoints (site management), user tries to access sites/projects they're not authorized for, user tries to modify data they don't own. | **403 Forbidden** は以下の場合に返却：一般ユーザーが管理者専用エンドポイント（サイト管理）にアクセスしようとした場合、権限のないサイト/プロジェクトにアクセスしようとした場合、所有していないデータを変更しようとした場合。 | **403 Forbidden** harus dikembalikan ketika: standard user mencoba mengakses endpoint khusus master (manajemen site), user mencoba mengakses site/proyek yang tidak diizinkan, user mencoba memodifikasi data yang bukan milik mereka. |

---

## Local Development vs ngrok Backend

### Option A: Local Backend

| English | 日本語 (Japanese) | Indonesia |
|---------|------------------|-----------|
| Use this when backend runs on the same machine. | バックエンドが同じマシンで動作している場合に使用。 | Gunakan ini ketika backend berjalan di mesin yang sama. |

| Config File | Setting | Value |
|-------------|---------|-------|
| `vite.config.ts` | proxy target | `http://localhost:8081` |
| `src/api/config.ts` | BASE_URL | `/api` |
| `src/api/auth.ts` | login endpoint | `${BASE_URL}/auth/user/login` |

```typescript
// vite.config.ts
server: {
  proxy: {
    "/api": {
      target: "http://localhost:8081",
      changeOrigin: true,
    },
  }
}
```

### Option B: ngrok Backend

| English | 日本語 (Japanese) | Indonesia |
|---------|------------------|-----------|
| Use this when backend is exposed through ngrok. | バックエンドがngrok経由で公開されている場合に使用。 | Gunakan ini ketika backend diekspos melalui ngrok. |

| Config File | Setting | Value |
|-------------|---------|-------|
| `vite.config.ts` | proxy target | `https://<ngrok-host>.ngrok-free.dev` |
| `src/api/config.ts` | BASE_URL | `/api` |

---

## Testing Guide

### Step 1: Update BASE_URL

```typescript
// In src/api/config.ts
export const BASE_URL = '/api';
```

### Step 2: Test Role-Based Access

| Test | English | 日本語 (Japanese) | Indonesia |
|------|---------|------------------|-----------|
| Master User | Login with email containing "master". Verify access to sites management. Can create/edit sites. Can see all company projects. | "master"を含むメールでログイン。サイト管理へのアクセスを確認。サイトの作成/編集が可能。企業のすべてのプロジェクトを表示可能。 | Login dengan email yang mengandung "master". Verifikasi akses ke manajemen site. Bisa membuat/edit site. Bisa melihat semua proyek perusahaan. |
| Standard User | Login with regular email. Should NOT see site management. Can only see assigned locations. Limited project visibility. | 通常のメールでログイン。サイト管理は表示されない。割り当てられた場所のみ表示。プロジェクトの表示が制限。 | Login dengan email biasa. TIDAK seharusnya melihat manajemen site. Hanya bisa melihat lokasi yang ditetapkan. Visibilitas proyek terbatas. |

### Step 3: Test First Login Flow

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Create user with no sites and no joined projects | — |
| 2 | Login | Redirect to `/projects` (project list) |
| 3 | Click 「参加する」 on any project | — |
| 4 | If no sites | Redirect to `/sites/register` |
| 5 | Create site | Return to project registration |

### Step 4: Test Project Registration Flow

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Navigate to project list | Projects load correctly |
| 2 | Click 「参加する」 button | Registration flow starts |
| 3 | Select location | Step 1 complete ✓ |
| 4 | Add devices with file uploads | Step 2 complete ✓ |
| 5 | Check all 4 agreements | Step 3 complete ✓ |
| 6 | Agree to contract | Step 4 complete ✓ |
| 7 | Agree to final terms | Step 5 complete ✓ |
| 8 | See completion message | Step 6 complete ✓ |

### Step 5: Browser Testing Checklist

```bash
# Run development server
npm run dev
```

| # | Test | Check |
|---|------|-------|
| 1 | Login | Check network tab for API call |
| 2 | Dashboard | Verify projects load |
| 3 | Sites page | Master user only |
| 4 | Project list | Projects display correctly |
| 5 | Project registration | All 6 steps work |
| 6 | Site management | CRUD operations work |
| 7 | Notifications | List, count, mark read, delete |
| 8 | Profile | Data displays correctly |

---

## Maintenance Checklist

When adding or changing API files:

| Step | English | 日本語 (Japanese) | Indonesia |
|------|---------|------------------|-----------|
| 1 | Add network calls through `_shared/httpClient.ts`. | `_shared/httpClient.ts`を通じてネットワーク呼び出しを追加。 | Tambahkan panggilan jaringan melalui `_shared/httpClient.ts`. |
| 2 | Keep domain-specific types in that domain's `types.ts`. | ドメイン固有の型をそのドメインの`types.ts`に保持。 | Simpan tipe domain-spesifik di `types.ts` milik domain tersebut. |
| 3 | Add new public exports through the domain `index.ts`. | ドメイン`index.ts`を通じて新しい公開エクスポートを追加。 | Tambahkan ekspor publik baru melalui `index.ts` domain. |
| 4 | Add root `src/api/index.ts` exports only when a stable public API is needed. | 安定した公開APIが必要な場合のみ、ルート`src/api/index.ts`にエクスポートを追加。 | Tambahkan ekspor `src/api/index.ts` hanya ketika API publik stabil diperlukan. |
| 5 | Update this document when adding, renaming, or removing files under `src/api/`. | `src/api/`以下のファイルを追加、リネーム、削除した場合、このドキュメントを更新。 | Perbarui dokumen ini ketika menambahkan, mengganti nama, atau menghapus file di bawah `src/api/`. |
