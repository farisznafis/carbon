# Frontend Feature Structure

This folder owns domain-specific UI for the three application sides:

- `admin`: admin-only screens, tables, dialogs, and project registration forms.
- `master`: master user screens, site/user management pages, and their forms.
- `user`: regular user screens such as profile, dashboard, site detail, project detail, project registration, equipment, and monitoring.

## Import Guidelines

- Keep `src/pages` as route entrypoints only. Pages should import from `src/features` and expose the route component shape required by `App.tsx`.
- Put reusable UI primitives and shared components in `src/components`.
- Put role/domain-specific UI in `src/features/admin`, `src/features/master`, or `src/features/user`.
- Prefer domain imports for feature code, for example `@/features/admin/...`, `@/features/master/...`, or `@/features/user/...`.
- Prefer shared imports for reusable UI, for example `@/components/ui/button` or `@/components/data-display/tables/SiteTable`.

## Moved Path Reference

- `src/components/Tables/AdminTables/PendingCompanyDetailDialog.tsx` -> `src/features/admin/dashboard/tables/PendingCompanyDetailDialog.tsx`
- `src/components/Tables/AdminTables/*` -> `src/features/admin/dashboard/tables/*`
- `src/components/Tables/SiteTable.tsx` -> `src/components/data-display/tables/SiteTable.tsx`
- `src/components/Tables/UserTable.tsx` -> `src/components/data-display/tables/UserTable.tsx`
- `src/components/forms/Admin/*` -> `src/features/admin/project-registration/forms/*`
- `src/components/forms/SiteForm.tsx` -> `src/features/master/forms/SiteForm.tsx`
- `src/components/forms/UserForm.tsx` -> `src/features/master/forms/UserForm.tsx`
- `src/components/forms/CompanyForm.tsx` -> `src/features/user/profile/forms/CompanyForm.tsx`
- `src/components/forms/MonitoringForms/*` -> `src/features/user/monitoring/forms/*`
- `src/components/project-register/*` -> `src/features/user/project-registration/components/*`

## Boundary Rules

- Feature barrels (`src/features/*/index.ts`) should expose page-level entrypoints only.
- Do not export internal tables, dialogs, form sections, or mappers from feature root barrels.
- Public/auth pages (`Login`, `Signup`, `ForgotPassword`, `Index`, `Hero`, `NotFound`) can stay in `src/pages` until the public/auth flow is refactored separately.
