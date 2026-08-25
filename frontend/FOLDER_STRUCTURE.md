# Frontend Project Folder Structure

## Root Level Folders

| Folder | English | Japanese | Indonesia |
|--------|---------|----------|-----------|
| `.storybook/` | Folder for Storybook configuration, enabling component development and visual testing in isolation. | コンポーネント開発と独立したビジュアルテストを可能にするStorybook設定用フォルダ。 | Folder untuk konfigurasi Storybook, memungkinkan pengembangan komponen dan visual testing terisolasi. |
| `archive/` | Folder for storing deprecated or backup code files no longer in active use, keeping the codebase clean. | 使用されなくなった非推奨またはバックアップコードを保存し、コードベースを整理するためのフォルダ。 | Folder untuk menyimpan file kode usang atau cadangan yang tidak lagi digunakan aktif, menjaga kebersihan codebase. |
| `cypress/` | Folder for Cypress end-to-end and component test files, fixtures, and support utilities. | Cypressエンドツーエンド・コンポーネントテスト、フィクスチャ、サポートユーティリティ用フォルダ。 | Folder untuk file test Cypress end-to-end dan component, fixture, dan utilitas pendukung. |
| `docs/` | Folder for project documentation including API contract changelog and index. | APIコントラクト変更履歴とインデックスを含むプロジェクト文書用フォルダ。 | Folder untuk dokumentasi proyek termasuk changelog dan indeks kontrak API. |
| `public/` | Folder for static assets served directly without processing (favicon, mock service worker). | 処理なしで直接配信される静的アセット（ファビコン、モックサービスワーカー）用フォルダ。 | Folder untuk aset statis yang disajikan langsung tanpa pemrosesan (favicon, mock service worker). |
| `src/` | Main source code folder containing all application logic, components, and features. | すべてのアプリケーションロジック、コンポーネント、機能を含むメインソースコードフォルダ。 | Folder kode sumber utama berisi semua logika aplikasi, komponen, dan fitur. |

---

## src/ Subfolders

| Folder | English | Japanese | Indonesia |
|--------|---------|----------|-----------|
| `api/` | Folder for centralized API layer: HTTP client, shared types, and domain-specific API modules. | HTTPクライアント、共有型、ドメイン固有APIモジュールを持つ集中APIレイヤー用フォルダ。 | Folder untuk lapisan API terpusat: HTTP client, tipe bersama, dan modul API domain-spesifik. |
| `components/` | Folder for reusable UI components organized by category (ui, forms, inputs, layout, etc.). | カテゴリ別に整理された再利用可能なUIコンポーネント用フォルダ（ui、forms、inputs、layout等）。 | Folder untuk komponen UI reusable yang terorganisir per kategori (ui, forms, inputs, layout, dll). |
| `contexts/` | Folder for React Context providers managing global state (auth, forms, routing, user data). | グローバル状態（認証、フォーム、ルーティング、ユーザーデータ）を管理するReact Contextプロバイダー用フォルダ。 | Folder untuk provider React Context yang mengelola state global (auth, forms, routing, data user). |
| `features/` | Folder for feature-based modules encapsulating business logic, components, and hooks per domain. | ドメインごとにビジネスロジック、コンポーネント、フックをカプセル化した機能ベースモジュール用フォルダ。 | Folder untuk modul berbasis fitur yang mengenkapsulasi logika bisnis, komponen, dan hook per domain. |
| `hooks/` | Folder for custom React hooks shared across the application and domain-specific hooks. | アプリケーション全体で共有されるカスタムReactフックとドメイン固有フック用フォルダ。 | Folder untuk custom React hooks yang dibagikan di seluruh aplikasi dan hook domain-spesifik. |
| `lib/` | Folder for utility libraries: validations, schemas, constants, storage, logger, error handling. | バリデーション、スキーマ、定数、ストレージ、ロガー、エラーハンドリング等のユーティリティライブラリ用フォルダ。 | Folder untuk library utilitas: validasi, skema, konstanta, storage, logger, penanganan error. |
| `pages/` | Folder for route-level page components representing full routes (auth, dashboard, landing). | 完全なルートを表すページレベルコンポーネント用フォルダ（認証、ダッシュボード、ランディング）。 | Folder untuk komponen level halaman yang merepresentasikan route penuh (auth, dashboard, landing). |
| `routes/` | Folder for route configuration defining path constants for type-safe navigation. | 型安全なナビゲーションのためのパス定数を定義するルート設定用フォルダ。 | Folder untuk konfigurasi route yang mendefinisikan konstanta path untuk navigasi type-safe. |
| `stores/` | Folder for Zustand state management stores (profile, notifications, forms, dashboard). | Zustand状態管理ストア用フォルダ（プロフィール、通知、フォーム、ダッシュボード）。 | Folder untuk store manajemen state Zustand (profile, notifikasi, form, dashboard). |
| `stories/` | Folder for Storybook stories documenting and testing UI components in isolation. | UIコンポーネントを独立して文書化・テストするStorybookストーリー用フォルダ。 | Folder untuk Storybook stories yang mendokumentasikan dan menguji komponen UI terisolasi. |
| `utils/` | Folder for general utility functions shared across the application. | アプリケーション全体で共有される汎用ユーティリティ関数用フォルダ。 | Folder untuk fungsi utilitas umum yang dibagikan di seluruh aplikasi. |

---

## Detailed Subdirectory Breakdown

### src/api/ — API Layer by Domain

| Subfolder | English | Japanese | Indonesia |
|-----------|---------|----------|-----------|
| `_shared/` | Folder for core HTTP client, shared error types, and base types used across all API modules. | 全APIモジュールで共有されるコアHTTPクライアント、エラー型、基本型用フォルダ。 | Folder untuk HTTP client inti, tipe error bersama, dan tipe dasar yang digunakan di semua modul API. |
| `auth/` | Folder for authentication API: login, register, token refresh, password reset flows. | 認証API用フォルダ：ログイン、登録、トークンリフレッシュ、パスワードリセットフロー。 | Folder untuk API autentikasi: login, register, refresh token, alur reset password. |
| `admin/` | Folder for admin-only APIs: pending companies, participation reviews, project payload mappers. | 管理者専用API用フォルダ：保留中企業、参加レビュー、プロジェクトペイロードマッパー。 | Folder untuk API khusus admin: perusahaan tertunda, review partisipasi, mapper payload proyek. |
| `batch/` | Folder for batch processing APIs handling bulk operations. | バルク操作を扱うバッチ処理API用フォルダ。 | Folder untuk API pemrosesan batch yang menangani operasi bulk. |
| `company/` | Folder for company registration and management APIs. | 企業登録と管理API用フォルダ。 | Folder untuk API registrasi dan manajemen perusahaan. |
| `monitoring/` | Folder for monitoring APIs providing dashboard metrics and alerts. | ダッシュボードメトリクスとアラートを提供する監視API用フォルダ。 | Folder untuk API monitoring yang menyediakan metrik dashboard dan alert. |
| `notifications/` | Folder for notification APIs fetching and managing user notifications. | ユーザー通知を取得・管理する通知API用フォルダ。 | Folder untuk API notifikasi yang mengambil dan mengelola notifikasi pengguna. |
| `profile/` | Folder for user profile APIs handling profile fetching and data transformation. | プロフィール取得とデータ変換を扱うユーザープロフィールAPI用フォルダ。 | Folder untuk API profil pengguna yang menangani pengambilan profil dan transformasi data. |
| `projects/` | Folder for project APIs handling project listings and details. | プロジェクト一覧と詳細を扱うプロジェクトAPI用フォルダ。 | Folder untuk API proyek yang menangani daftar dan detail proyek. |
| `site/` | Folder for site/equipment APIs: dashboard, equipment CRUD, project associations. | サイト/設備API用フォルダ：ダッシュボード、設備CRUD、プロジェクト関連付け。 | Folder untuk API site/peralatan: dashboard, CRUD peralatan, asosiasi proyek. |
| `users/` | Folder for user management APIs: listing, mutations (create/update/delete). | ユーザー管理API用フォルダ：一覧表示、ミューテーション（作成/更新/削除）。 | Folder untuk API manajemen pengguna: listing, mutations (create/update/delete). |

---

### src/components/ — UI Components by Category

| Subfolder | English | Japanese | Indonesia |
|-----------|---------|----------|-----------|
| `ui/` | Folder for 55+ base UI primitives (button, card, dialog, table, tabs, toast) built with Radix UI + Tailwind CSS. | Radix UI + Tailwind CSSで構築された55以上の基本UIプリミティブ用フォルダ。 | Folder untuk 55+ primitif UI dasar (button, card, dialog, table, tabs, toast) dibangun dengan Radix UI + Tailwind CSS. |
| `forms/` | Folder for form layout utilities and shared form components. | フォームレイアウトユーティリティと共有フォームコンポーネント用フォルダ。 | Folder untuk utilitas layout form dan komponen form bersama. |
| `inputs/` | Folder for specialized input components (FileSelect, FileUpload, PasswordInput, InteractiveBlocks, PreviousValueField). | 特殊化された入力コンポーネント用フォルダ。 | Folder untuk komponen input khusus. |
| `layout/` | Folder for page-level layout components (DashboardLayout, Navbar, AccountMenu, FormPageWrapper, SessionGuards). | ページレベルレイアウトコンポーネント用フォルダ。 | Folder untuk komponen layout level halaman. |
| `navigation/` | Folder for navigation utilities (PageBackButton, ScrollToTopButton). | ナビゲーションユーティリティ用フォルダ。 | Folder untuk utilitas navigasi. |
| `feedback/` | Folder for user feedback components (Confirmation dialogs, ErrorBoundary, Notifications, InlineHelp, StatusBadge). | ユーザーフィードバックコンポーネント用フォルダ。 | Folder untuk komponen feedback pengguna. |
| `data-display/` | Folder for data visualization components (DataGridTable, DataTable, ContributionTimeline, Pagination, tables). | データ可視化コンポーネント用フォルダ。 | Folder untuk komponen visualisasi data. |
| `files/` | Folder for file handling components (PresignedFileLink for secure file access). | ファイル処理コンポーネント用フォルダ（安全なファイルアクセス用PresignedFileLink）。 | Folder untuk komponen penanganan file (PresignedFileLink untuk akses file aman). |
| `address/` | Folder for Japanese address formatting and display components. | 日本語住所のフォーマットと表示コンポーネント用フォルダ。 | Folder untuk komponen format dan tampilan alamat Jepang. |

---

### src/contexts/ — React Context Providers

| Folder | English | Japanese | Indonesia |
|--------|---------|----------|-----------|
| (no subfolders) | Folder for 6 React Context providers managing global state: authentication, form wizard, route protection, signup flow, site context, user management. | グローバル状態を管理する6つのReact Contextプロバイダー用フォルダ：認証、フォームウィザード、ルート保護、サインアップフロー、サイトコンテキスト、ユーザー管理。 | Folder untuk 6 provider React Context yang mengelola state global: autentikasi, form wizard, proteksi route, alur signup, konteks site, manajemen user. |

---

### src/features/ — Feature-Based Modules (Domain-Driven)

| Subfolder | English | Japanese | Indonesia |
|-----------|---------|----------|-----------|
| `admin/` | Folder for admin panel features: company-detail, dashboard, membership-review, monitoring, project-detail, project-edit, project-registration. | 管理パネル機能用フォルダ：企業詳細、ダッシュボード、メンバーシップレビュー、監視、プロジェクト詳細、編集、登録。 | Folder untuk fitur panel admin: company-detail, dashboard, membership-review, monitoring, project-detail, project-edit, project-registration. |
| `master/` | Folder for master data management: components, forms, help, navigation, pages for reference data. | マスターデータ管理用フォルダ：参照データ用のコンポーネント、フォーム、ヘルプ、ナビゲーション、ページ。 | Folder untuk manajemen data master: components, forms, help, navigation, pages untuk data referensi. |
| `public/` | Folder for public-facing features: signup-revision flow for external users. | 公開向け機能用フォルダ：外部ユーザー向けサインアップリビジョンフロー。 | Folder untuk fitur publik: alur signup-revision untuk pengguna eksternal. |
| `user/` | Folder for user-facing features: dashboard, equipment, monitoring, profile, project-detail, project-registration, sites. | ユーザー向け機能用フォルダ：ダッシュボード、設備、監視、プロフィール、プロジェクト詳細、プロジェクト登録、サイト。 | Folder untuk fitur pengguna: dashboard, equipment, monitoring, profile, project-detail, project-registration, sites. |

---

### src/hooks/ — Custom React Hooks

| Subfolder | English | Japanese | Indonesia |
|-----------|---------|----------|-----------|
| (root) | Folder for shared hooks: useMobile, useToast, useErrorHandler, useNotifications, usePagination, useRedirectIfLoggedIn, useUnsavedChangesWarning. | 共有フック用フォルダ：useMobile、useToast、useErrorHandler、useNotifications、usePagination、useRedirectIfLoggedIn、useUnsavedChangesWarning。 | Folder untuk hook bersama: useMobile, useToast, useErrorHandler, useNotifications, usePagination, useRedirectIfLoggedIn, useUnsavedChangesWarning. |
| `admin/` | Folder for admin-specific hooks (useAdminCompanyDetailQuery). | 管理者固有フック用フォルダ。 | Folder untuk hook khusus admin. |
| `projects/` | Folder for project-related hooks (useProjectsQuery). | プロジェクト関連フック用フォルダ。 | Folder untuk hook terkait proyek. |
| `sites/` | Folder for site-related hooks (useSitesQuery). | サイト関連フック用フォルダ。 | Folder untuk hook terkait site. |
| `users/` | Folder for user-related hooks (useUsersQuery, useUpdateUserMutation). | ユーザー関連フック用フォルダ。 | Folder untuk hook terkait user. |

---

### src/lib/ — Utility Libraries & Configuration

| Subfolder | English | Japanese | Indonesia |
|-----------|---------|----------|-----------|
| `constants/` | Folder for 9 constant files centralizing dropdown/form options: prefectures, regionals, project_types, methodology, formOptions, emission_helpers, reviewer_names, select_types. | ドロップダウン/フォームオプションを一元管理する9つの定数ファイル用フォルダ。 | Folder untuk 9 file konstanta yang memusatkan opsi dropdown/form: prefectures, regionals, project_types, methodology, formOptions, emission_helpers, reviewer_names, select_types. |
| `schemas/` | Folder for Zod validation schemas used with React Hook Form (login, company). | React Hook Formで使用するZodバリデーションスキーマ用フォルダ（ログイン、企業）。 | Folder untuk skema validasi Zod yang digunakan dengan React Hook Form (login, company). |
| (root) | Folder for validation helpers, equipment validations, storage utilities, general utils, logger, error classes, error handling docs. | バリデーションヘルパー、設備バリデーション、ストレージユーティリティ、汎用ユーティリティ、ロガー、エラークラス、エラーハンドリング文書用フォルダ。 | Folder untuk helper validasi, validasi peralatan, utilitas storage, utils umum, logger, class error, dokumen penanganan error. |

---

### src/pages/ — Route-Level Page Components

| Subfolder | English | Japanese | Indonesia |
|-----------|---------|----------|-----------|
| `signup/` | Folder for 4-step signup wizard components (company info, admin user, site, confirmation). | 4ステップサインアップウィザードコンポーネント用フォルダ（企業情報、管理ユーザー、サイト、確認）。 | Folder untuk komponen wizard signup 4-step (info perusahaan, user admin, site, konfirmasi). |
| (root) | Folder for auth pages (Login, ResetPassword, ForgotPassword), landing pages (Index, Hero), and 404 page. | 認証ページ、ランディングページ、404ページ用フォルダ。 | Folder untuk halaman auth (Login, ResetPassword, ForgotPassword), landing (Index, Hero), dan halaman 404. |

---

### src/routes/ — Route Configuration

| Folder | English | Japanese | Indonesia |
|--------|---------|----------|-----------|
| (no subfolders) | Folder for route path constants: paths.ts (public routes), adminPaths.ts (admin-only routes) enabling type-safe navigation. | 型安全なナビゲーションを可能にするルートパス定数用フォルダ：paths.ts（公開ルート）、adminPaths.ts（管理者専用ルート）。 | Folder untuk konstanta path route: paths.ts (route publik), adminPaths.ts (route admin-only) yang memungkinkan navigasi type-safe. |

---

### src/stores/ — Zustand State Management

| Folder | English | Japanese | Indonesia |
|--------|---------|----------|-----------|
| (no subfolders) | Folder for 5 Zustand stores managing client state: ProfileStore (user profile), notificationStore (toasts/alerts), FormStore (multi-step forms), dashboardStore (dashboard UI state). | クライアント状態を管理する5つのZustandストア用フォルダ：ProfileStore（ユーザープロフィール）、notificationStore（トースト/アラート）、FormStore（マルチステップフォーム）、dashboardStore（ダッシュボードUI状態）。 | Folder untuk 5 store Zustand yang mengelola state client: ProfileStore (profil user), notificationStore (toast/alert), FormStore (multi-step form), dashboardStore (state UI dashboard). |

---

### src/stories/ — Storybook Component Stories

| Subfolder | English | Japanese | Indonesia |
|-----------|---------|----------|-----------|
| `assets/` | Folder for static assets used in stories (SVGs, PNGs, AVIF images). | ストーリーで使用する静的アセット用フォルダ（SVG、PNG、AVIF画像）。 | Folder untuk aset statis yang digunakan di story (SVG, PNG, gambar AVIF). |
| (root) | Folder for component stories (Button, Header, Page) with variants/states and Storybook configuration (Configure.mdx). | コンポーネントストーリー用フォルダ（Button、Header、Page）バリエーション/状態付きとStorybook設定（Configure.mdx）。 | Folder untuk story komponen (Button, Header, Page) dengan varian/state dan konfigurasi Storybook (Configure.mdx). |

---

### src/utils/ — General Utilities

| Folder | English | Japanese | Indonesia |
|--------|---------|----------|-----------|
| (no subfolders) | Folder for utility functions: getHomePath.ts returns the appropriate home route based on user role. | ユーザー役割に基づいて適切なホームルートを返すユーティリティ関数用フォルダ：getHomePath.ts。 | Folder untuk fungsi utilitas: getHomePath.ts mengembalikan home route yang sesuai berdasarkan role user. |