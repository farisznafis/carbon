# API Layer Documentation

This document describes every file under `src/api/`. It is intended for developers who need to understand the API layer structure, public imports, internal helpers, and ownership boundaries.

## Import Guidelines

- Prefer domain-level imports for application code:
  - `@/api/auth`
  - `@/api/projects`
  - `@/api/monitoring`
  - `@/api/site`
  - `@/api/users`
  - `@/api/company`
  - `@/api/admin`
- Use the root `@/api` barrel only as a compatibility/public convenience entrypoint.
- Avoid importing internal files such as `mappers.ts`, `payloadMappers.ts`, `queries.ts`, or `mutations.ts` from UI code unless there is a specific reason.
- Do not import UI/components/forms into the API layer. Shared input and payload types should live in API/domain modules.

## Architecture Rules

- All HTTP calls should go through `_shared/httpClient.ts`.
- Domain modules own their own request, response, and view/domain types.
- Mapper files should only transform data. They should not perform network calls.
- Payload mapper files convert frontend/domain input into API request payloads.
- `src/api/index.ts` is a public compatibility barrel, not the source of domain ownership.
- `src/api/types.ts` is a compatibility type barrel and should not become a god types file again.

## Root Files

### `API_DOCUMENTATION.md`

- **Purpose:** Documents the structure, responsibilities, exports, and import guidelines for every file in the API layer.
- **Main exports:** None.
- **Boundary:** Documentation only.
- **Notes:** Update this file whenever API files are added, renamed, removed, or significantly repurposed.

### `config.ts`

- **Purpose:** Defines shared API configuration.
- **Main exports:** `BASE_URL`, `getAuthHeaders`.
- **Boundary:** Shared infrastructure used by `_shared/httpClient.ts`.
- **Notes:** `getAuthHeaders` reads auth tokens from `localStorage` and returns headers for authenticated requests.

### `index.ts`

- **Purpose:** Public compatibility barrel for stable API functions and types.
- **Main exports:** Auth functions, project functions, monitoring functions, site functions, profile functions, user functions, company functions, admin functions, and public domain types.
- **Boundary:** Should expose stable public API only.
- **Notes:** Prefer domain imports in new code. Keep this file explicit to avoid duplicate export ambiguity.

### `types.ts`

- **Purpose:** Compatibility barrel for legacy/shared API types.
- **Main exports:** Re-exported domain types plus `ErrorResponse` and `DeviceData`.
- **Boundary:** Compatibility layer only.
- **Notes:** New domain types should be added to their owning module first.

## `_shared`

### `_shared/errors.ts`

- **Purpose:** Shared API error utilities.
- **Main exports:** `ApiError`, `getApiErrorMessage`.
- **Boundary:** Shared infrastructure.
- **Notes:** Use this for consistent HTTP error metadata and fallback error messages.

### `_shared/httpClient.ts`

- **Purpose:** Central fetch wrapper for all API requests.
- **Main exports:** `apiFetch`, `parseJsonResponse`, `apiRequest`, `apiRequestWithMeta`, `apiVoidRequest`.
- **Boundary:** Shared infrastructure.
- **Notes:** This is the only API-layer file that should call `fetch` directly. It centralizes `BASE_URL`, auth headers, JSON body handling, FormData handling, and dev-only ngrok headers.

### `_shared/types.ts`

- **Purpose:** Shared response and error shape definitions.
- **Main exports:** `ApiErrorPayload`, `ApiPagination`, `ApiResponse<T>`.
- **Boundary:** Cross-domain shared types only.
- **Notes:** Keep domain-specific types out of this file.

## `admin`

### `admin/index.ts`

- **Purpose:** Public barrel for admin API operations.
- **Main exports:** Pending company actions, admin project functions, and admin types.
- **Boundary:** Public admin module entrypoint.
- **Notes:** Prefer importing admin API from `@/api/admin`.

### `admin/pendingCompanies.ts`

- **Purpose:** Fetches pending companies and handles approve/reject actions.
- **Main exports:** `getPendingCompanies`, `approvePendingCompany`, `rejectPendingCompany`.
- **Boundary:** Admin pending-company queries and mutations.
- **Notes:** Uses admin-specific mapping helpers internally to normalize backend response variants.

### `admin/projectPayloadMapper.ts`

- **Purpose:** Converts admin project form values into the create-project API payload.
- **Main exports:** `mapAdminProjectPayload`.
- **Boundary:** Payload mapping only.
- **Notes:** This currently bridges form-shaped data to API payload shape. If form ownership changes later, this mapper can move closer to the admin project registration feature.

### `admin/projects.ts`

- **Purpose:** Admin project detail and creation API calls.
- **Main exports:** `getAdminProjectDetail`, `createAdminProject`.
- **Boundary:** Admin project queries and mutations.
- **Notes:** Uses shared HTTP helpers and admin-specific types.

### `admin/types.ts`

- **Purpose:** Admin-specific API and domain type definitions.
- **Main exports:** Admin project types, pending company types, mutation response types.
- **Boundary:** Type-only admin module file.
- **Notes:** Uses domain-specific names such as `PendingCompanyEquipmentItem` to avoid conflicts with auth/signup equipment types.

## `auth`

### `auth/auth.ts`

- **Purpose:** Authentication API calls.
- **Main exports:** `login`, `adminLogin`, `logout`, `signUp`, `changePassword`.
- **Boundary:** Auth-related mutations only.
- **Notes:** Login and signup use unauthenticated requests; password changes require the current authenticated user token.

### `auth/index.ts`

- **Purpose:** Public barrel for auth API.
- **Main exports:** Auth functions and auth/signup types.
- **Boundary:** Public auth module entrypoint.

### `auth/types.ts`

- **Purpose:** Auth and signup type definitions.
- **Main exports:** `LoginResponse`, `EquipmentItem`, `MethodologyRequest`, `SignUpData`, `SignUpResponse`, `ChangePasswordPayload`, `ChangePasswordResponse`.
- **Boundary:** Type-only auth module file.
- **Notes:** `EquipmentItem` here belongs to signup methodology payloads.

## `company`

### `company/index.ts`

- **Purpose:** Public barrel for company API.
- **Main exports:** `updateCompany`, `Company`, `UpdateCompanyResponse`.
- **Boundary:** Public company module entrypoint.

### `company/mutations.ts`

- **Purpose:** Company mutation API calls.
- **Main exports:** `updateCompany`.
- **Boundary:** Company-specific mutations.
- **Notes:** This was separated from `users` because company updates are a separate domain concern.

### `company/types.ts`

- **Purpose:** Company module type exports.
- **Main exports:** `Company`, `UpdateCompanyResponse`.
- **Boundary:** Type-only company module file.
- **Notes:** Reuses the existing `Company` type from the users domain for compatibility.

## `monitoring`

### `monitoring/index.ts`

- **Purpose:** Public barrel for monitoring API.
- **Main exports:** Monitoring query, mutation, mapper, and monitoring types.
- **Boundary:** Public monitoring module entrypoint.

### `monitoring/mappers.ts`

- **Purpose:** Normalizes monitoring API items into frontend-friendly monitoring records.
- **Main exports:** `normalizeRecentMonitoringItem`.
- **Boundary:** Mapping only.

### `monitoring/mutations.ts`

- **Purpose:** Monitoring submission and evidence upload API calls.
- **Main exports:** `submitMonitoringData`, `uploadElpvFile`, `uploadElpvrFile`, `uploadElpjBatteryFile`, `uploadElpjControlFile`.
- **Boundary:** Monitoring mutations and upload helpers.
- **Notes:** Upload variants delegate to a shared internal upload implementation.

### `monitoring/queries.ts`

- **Purpose:** Monitoring read API calls.
- **Main exports:** `getMonitoringRecent`.
- **Boundary:** Monitoring queries only.

### `monitoring/types.ts`

- **Purpose:** Monitoring request, response, and normalized item types.
- **Main exports:** `MonitoringData`, `SubmitDataResponse`, `MonitoringEvidenceKind`, `MonitoringRecentApiItem`, `MonitoringRecentItem`, `MonitoringRecentResponse`.
- **Boundary:** Type-only monitoring module file.

## `profile`

### `profile/index.ts`

- **Purpose:** Public barrel for profile API.
- **Main exports:** `getProfile`, `ProfileViewModel`, profile API response types.
- **Boundary:** Public profile module entrypoint.

### `profile/mappers.ts`

- **Purpose:** Maps profile API responses into the profile view model.
- **Main exports:** `ProfileViewModel`, `mapProfileApiResponseToProfileViewModel`.
- **Boundary:** Mapping only.

### `profile/profile.ts`

- **Purpose:** Profile read API call.
- **Main exports:** `getProfile`.
- **Boundary:** Profile queries only.

### `profile/types.ts`

- **Purpose:** Profile API response type definitions.
- **Main exports:** `ProfileApiErrorMap`, `ProfileApiCompany`, `ProfileApiProfile`, `ProfileApiResponse`.
- **Boundary:** Type-only profile module file.

## `projects`

### `projects/index.ts`

- **Purpose:** Public barrel for project API.
- **Main exports:** `getProjectDetails`, `getProjects`, project types.
- **Boundary:** Public project module entrypoint.

### `projects/mappers.ts`

- **Purpose:** Maps project list API items into frontend project objects.
- **Main exports:** `mapProjectListItem`.
- **Boundary:** Mapping only.

### `projects/queries.ts`

- **Purpose:** Project read API calls.
- **Main exports:** `getProjectDetails`, `getProjects`.
- **Boundary:** Project queries only.

### `projects/types.ts`

- **Purpose:** Project-related API and domain type definitions.
- **Main exports:** `Project`, `ProjectDetail`, `ProjectDetailResponse`, `CompanyProjectRelation`, `Equipment`, `ProjectListItem`, `ProjectListResponse`.
- **Boundary:** Type-only project module file.

## `site`

### `site/dashboard.ts`

- **Purpose:** Fetches dashboard data for a single site.
- **Main exports:** `getSiteData`.
- **Boundary:** Site dashboard query.

### `site/index.ts`

- **Purpose:** Public barrel for site API.
- **Main exports:** Site queries, mutations, project query, and public site types.
- **Boundary:** Public site module entrypoint.
- **Notes:** Internal mappers and payload types are intentionally not exposed here.

### `site/list.ts`

- **Purpose:** Fetches a company's site list.
- **Main exports:** `getSites`.
- **Boundary:** Site list query.

### `site/mappers.ts`

- **Purpose:** Maps site-related API response objects into domain/view objects.
- **Main exports:** `mapSiteProject`, `mapSiteListItem`, `mapSiteDashboard`.
- **Boundary:** Response mapping only.

### `site/mutations.ts`

- **Purpose:** Site create, update, and delete API calls.
- **Main exports:** `registerSite`, `updateSite`, `deleteSite`.
- **Boundary:** Site mutations.

### `site/payloadMappers.ts`

- **Purpose:** Converts site form data into site API payloads.
- **Main exports:** `buildAddress`, `mapCreateSitePayload`, `mapUpdateSitePayload`.
- **Boundary:** Payload mapping only.

### `site/payloads.ts`

- **Purpose:** Site request payload type definitions.
- **Main exports:** `CreateSitePayload`, `UpdateSitePayload`.
- **Boundary:** Type-only payload file.

### `site/projects.ts`

- **Purpose:** Fetches projects associated with a site.
- **Main exports:** `getSiteProjects`, `SiteProject`.
- **Boundary:** Site project query.

### `site/types.ts`

- **Purpose:** Site-related API, domain, dashboard, list, and project type definitions.
- **Main exports:** `Site`, `CreateSiteData`, `SiteAddResponse`, `SiteFormData`, `SiteDashboardMetrics`, `SiteDashboardApiResponse`, `SiteDashboardData`, `SiteProjectsApiResponse`, `SiteProjectApiItem`, `SiteProject`, `SiteListItem`, `SitesResponse`.
- **Boundary:** Type-only site module file.

## `users`

### `users/index.ts`

- **Purpose:** Public barrel for user API.
- **Main exports:** `getUsers`, `registerUser`, `updateUser`, `deleteUser`, user/company types.
- **Boundary:** Public users module entrypoint.
- **Notes:** Company mutation APIs live in `company`, not `users`.

### `users/list.ts`

- **Purpose:** Fetches the user list.
- **Main exports:** `getUsers`.
- **Boundary:** User list query.

### `users/mutations.ts`

- **Purpose:** User create, update, and delete API calls.
- **Main exports:** `registerUser`, `updateUser`, `deleteUser`.
- **Boundary:** User mutations only.

### `users/types.ts`

- **Purpose:** User, company, profile, and user form type definitions.
- **Main exports:** `Company`, `User`, `UserProfile`, `UserList`, `UserProfileResponse`, `UserFormData`.
- **Boundary:** Type-only users module file.

## Maintenance Checklist

When adding or changing API files:

1. Add network calls through `_shared/httpClient.ts`.
2. Keep domain-specific types in that domain's `types.ts`.
3. Add new public exports through the domain `index.ts`.
4. Add root `src/api/index.ts` exports only when a stable public API is needed.
5. Update this document when adding, renaming, or removing files under `src/api/`.
