# Form action audit

## Tujuan

Dokumen ini mencatat standardisasi action commit pada form, confirmation, review, dan decision dialog. Audit awal menemukan 22 implementasi form langsung, 14 pemakai `FormActions`, serta beberapa confirmation/review yang masih menyusun tombol secara manual.

Seluruh action commit production sekarang memakai API deklaratif `FormActions`. Komponen ini menjadi sumber tunggal untuk layout responsif, tinggi tombol, variant, loading label, dan pencegahan submit ganda.

## Standar

| Context | Ukuran | Penggunaan |
| --- | --- | --- |
| `standard` | `md` / 40px | Form aplikasi, review, dan confirmation page |
| `auth` | `lg` / 48px | Login dan password flow; tetap memakai adaptasi viewport pendek |
| `compact` | `sm` / 32px | Dialog dan modal |

- Satu action memenuhi lebar container.
- Dua atau tiga action menjadi kolom sama lebar pada desktop dan bertumpuk pada mobile.
- `leadingAction` menyediakan kolom terpisah untuk destructive icon action seperti delete.
- `pendingActionId` mengganti label action aktif dan menonaktifkan seluruh group.
- Urutan semantik adalah kembali/batal di kiri dan action utama di kanan. Reject berada di kiri approve.

## Inventaris production yang dimigrasikan

| Area | Surface | Context | Status |
| --- | --- | --- | --- |
| Shared | `components/forms/shared/LoginForm.tsx` | auth | Migrated |
| Shared | `components/forms/shared/AddressForm/AddressForm.tsx` | standard | Migrated; legacy shared form tanpa caller production saat audit |
| Shared | `components/feedback/Confirmation.tsx` | standard / compact | Migrated |
| Shared | `components/feedback/Confirmation/ConfirmationLayout.tsx` | standard | Migrated |
| Shared | `components/feedback/ConfirmationData/FullPageConfirmationModal.tsx` | compact | Migrated |
| Shared | `components/feedback/ConfirmationModal.tsx` | compact | Migrated |
| Shared | `components/feedback/DeleteAlertDialog/DeleteAlertDialog.tsx` | compact | Migrated |
| Session | `components/layout/SessionExpiryGuard.tsx` | compact | Migrated |
| Session | `components/layout/SessionTimeoutOverlay.tsx` | compact | Migrated |
| Auth | `pages/ForgotPassword.tsx` | auth | Migrated |
| Auth | `pages/ResetPassword.tsx` | auth | Migrated |
| Signup | `pages/signup/SignupStep1.tsx` | standard | Migrated |
| Signup | `pages/signup/SignupStep2.tsx` | standard | Migrated |
| Signup | `pages/signup/SignupStep3.tsx` | standard | Migrated |
| Signup revision | `features/public/signup-revision/SignupRevisionPage.tsx` | standard | Migrated status actions; retry tetap utility action |
| Master | `features/master/forms/SiteForm.tsx` | standard | Migrated dengan `leadingAction` delete |
| Master | `features/master/forms/UserForm.tsx` | standard | Migrated dengan `leadingAction` delete |
| Master | `features/master/pages/SiteConfirmation.tsx` | standard | Migrated |
| Master | `features/master/pages/UserConfirmation.tsx` | standard | Migrated |
| Profile | `features/user/profile/forms/ProfileForm.tsx` | standard | Migrated |
| Profile | `features/user/profile/forms/CompanyForm.tsx` | standard | Migrated |
| Profile | `features/user/profile/PasswordChangePage.tsx` | standard | Migrated |
| Site | `features/user/sites/SiteDetailPage.tsx` | standard | Migrated edit actions |
| Equipment | `features/user/project-registration/EquipmentFormPage.tsx` | standard | Migrated solar dan other equipment actions |
| Equipment | `features/user/project-registration/EquipmentConfirmationPage.tsx` | standard | Migrated |
| Equipment | `features/user/equipment-edit/EquipmentEditPage.tsx` | standard | Migrated |
| Equipment | `features/user/equipment-edit/EquipmentEditConfirmation.tsx` | standard | Migrated |
| Monitoring | `features/user/monitoring/forms/solar/MonitoringSolarForm.tsx` | standard | Migrated |
| Monitoring | `features/user/monitoring/forms/MonitoringConfirmation.tsx` | standard | Migrated |
| Monitoring | `features/user/project-detail/components/MonitoringCancellationDialog.tsx` | compact | Migrated decision action; row actions dikecualikan |
| Project registration | `features/user/project-registration/components/EquipmentPage.tsx` | standard | Migrated legacy form |
| Project registration | `features/user/project-registration/components/EquipmentRegistrationStep.tsx` | standard | Migrated |
| Project registration | `features/user/project-registration/components/DynamicAgreementsStep.tsx` | standard | Migrated |
| Project registration | `features/user/project-registration/components/ContractStep.tsx` | standard | Migrated |
| Project registration | `features/user/project-registration/components/FinalTermsStep.tsx` | standard | Migrated |
| Project registration | `features/user/project-registration/components/ProjectRegistrationReviewStep.tsx` | standard | Migrated |
| Admin project | `features/admin/project-registration/AdminProjectRegisterPage.tsx` | standard | Migrated |
| Admin project | `features/admin/project-edit/AdminProjectEditPage.tsx` | standard | Migrated |
| Admin project | `features/admin/project-detail/AdminProjectPlanApplicationPage.tsx` | standard | Migrated review navigation/actions |
| Admin membership | `features/admin/membership-review/ParticipationReviewDetailPage.tsx` | standard / compact | Migrated page decision dan confirmation dialog |
| Admin monitoring | `features/admin/monitoring/CancelRequestDetailPage.tsx` | standard / compact | Migrated page decision dan confirmation dialog |
| Admin company | `features/admin/dashboard/tables/PendingCompanyApplicationDetail.tsx` | standard | Migrated decision actions |
| Admin company | `features/admin/dashboard/tables/PendingCompanyActionDialog.tsx` | compact | Migrated |
| Admin company | `features/admin/dashboard/tables/CompanySuspendDialog.tsx` | compact | Migrated |
| Admin company | `features/admin/dashboard/tables/CompanySuspensionResolveDialog.tsx` | compact | Migrated |
| Admin company | `features/admin/company-detail/components/AdminMasterPasswordResetDialog.tsx` | compact | Migrated |
| Admin project | `features/admin/dashboard/tables/ProjectCompletionDialog.tsx` | compact | Migrated |

`ProjectRegistrationStepActions` dihapus karena hanya membungkus pola yang sekarang sudah ditangani langsung oleh shared `FormActions`.

## Pengecualian yang disengaja

Komponen berikut tidak dimigrasikan karena bukan form commit action:

- Retry/reload dan error recovery.
- Postal-code search.
- Clear icon, password visibility, dan field-level controls.
- File select/upload trigger.
- Table-row actions, pagination, sort, tab, dan dropdown controls.
- Floating/page back navigation.
- Dashboard, navbar, notification, help, dan guide navigation.
- Completion-state navigation seperti `SignupStep4` dan `CompletionStep`.
- Story dan test fixture yang sengaja mendemonstrasikan primitive `Button` secara langsung.
- Primitive internal `DialogFooter`, `AlertDialogFooter`, `SheetFooter`, dan `DrawerFooter`.

## Pengecekan regresi

- Cari native submit production tanpa shared action: `rg -l --glob '*.tsx' 'type=.submit.' src`.
- Cari dialog production yang belum memakai shared action: bandingkan hasil `rg -l '(AlertDialogFooter|DialogFooter)' src` dengan `rg -l '<FormActions' src`.
- Cari wrapper lama: `rg 'ProjectRegistrationStepActions' src`.
- Jalankan Vitest targeted, ESLint, `npx tsc --noEmit`, `git diff --check`, dan production build.
