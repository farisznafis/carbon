# Frontend API Integration Guide
## Updated: 2026-05-08

## Overview
This document outlines the REST API contract and integration requirements for the GX environmental monitoring system.

**Scope**: This guide focuses on the API contracts and frontend integration points. It is designed for backend developers implementing the required endpoints to match the frontend application's expectations.

**Frontend Stack**: React 18 with TypeScript, Fetch API for all HTTP requests, React Router v6 for routing, Context API for state management.

**Terminology**: The current codebase uses `Site` / `siteId` naming for 拠点. This document follows that convention.

---

## Table of Contents
1. [Architecture Overview](#architecture-overview)
2. [User System Architecture](#user-system-architecture)
3. [API Service Module](#api-service-module)
4. [Required Endpoints](#required-endpoints)
5. [Integration Examples](#integration-examples)
6. [Error Handling](#error-handling)
7. [Testing Guide](#testing-guide)

---

## Architecture Overview

### Technology Stack
- **Frontend**: React 18 with TypeScript
- **Routing**: React Router v6
- **State Management**: React Context API
- **HTTP Client**: Native Fetch API
- **UI Framework**: shadcn-ui with Tailwind CSS

### API Service Location
All API logic is organized in the **`src/api/`** folder with feature-based modules:

**Frontend API Structure:**
```
src/api/
├── auth.ts            # Authentication functions (login, signUp)
├── config.ts          # BASE_URL and shared utilities
├── dashboard.ts       # Dashboard-related API calls (legacy)
├── index.ts           # Central export point for all API functions
├── monitoring.ts      # Monitoring data functions
├── projects.ts        # Project-related functions
├── siteApi.ts         # Site-related API calls (legacy)
├── sites.ts           # Site-related API calls (new)
├── types.ts           # TypeScript interfaces for all API responses
├── userApi.ts         # User management API calls (legacy)
└── users.ts           # User management API calls (new)
```

### Current Implementation Status
✅ **Mock implementations** are currently in place for the signup flow through MSW  
⚠️ **Action Required**: Replace or extend the mocked responses with actual REST API calls when the backend is ready

---

## API Call Map

This section explains, one by one, which folders and files are involved when the frontend makes an API call. It is written for teams outside the frontend group so they can quickly identify where request logic lives, where the data contract is defined, and where the mock layer intercepts calls during development.

### Core API Layer
#### `src/api/index.ts`

This is the central export file. Other parts of the app import API functions from here instead of importing each module directly. In practice, this file acts like the public doorway for the API layer.

#### `src/api/types.ts`

This file defines the request and response shapes used across the API layer. It is the most important file for contract alignment because it tells the frontend what the backend is expected to return. If the response shape changes, this file usually needs to change as well.


### Other API Modules
#### `src/api/auth.ts`
`POST /auth/login`
`POST /auth/signup`
`POST /auth/admin/login`
`POST /logout`

This file contains authentication-related API calls such as `login()` and `signUp()`. It is the main file involved when a user logs in or creates an account. In the current setup, `signUp()` is the important live path because it sends the request that MSW intercepts during development.
```
page corelated: 
- `src/pages/Login.tsx` (calls `login()`)
- `src/pages/SignupStep3.tsx` (calls `signUp()`)
```

#### `src/api/config.ts`

This file stores shared API configuration such as `BASE_URL` and reusable header helpers. If the backend URL changes, this is the first place the frontend team updates. If auth headers are needed, this file is also part of that flow.
```
page corelated: 
- All API calls that require authentication will use the `getAuthHeaders()` function defined here to include the JWT token in the request headers.
```

#### `src/api/dashboard.ts`

This file contains dashboard-related API calls and is used by screens that display dashboard data.
```
page corelated: 
- `src/contexts/DashboardContext.tsx` (calls `getDashboardProjects()`)
- `src/stores/dashboardStore.tsx` (calls `getDashboardProjects()`)
```


#### `src/api/monitoring.ts`
`POST /monitoring/data`

This file contains the API call for sending monitoring data. It is used by the monitoring registration screen and is the place to look when a new data submission is sent to the backend.
```
page corelated: 
- `src/pages/MonitoringRegister.tsx` (calls `submitMonitoringData()`)
```

#### `src/api/projects.ts`
`GET /projects`
`GET /projects/available`
`GET /projects/{id}`

This file contains project-related API calls such as dashboard project listing, available project listing, and project detail retrieval. Any screen that shows project cards or project details will usually depend on this module.
```
page corelated: 
- `src/pages/Dashboard.tsx` (calls `getDashboardProjects()`)
- `src/pages/ProjectRegister.tsx` (calls `getAvailableProjects()`, `registerProject()`)
- `src/hooks/projects/useProjectQuery.ts` (calls `getProjectDetails()`)
```

#### `src/api/siteApi.ts`
`GET /sites/{siteId}`
`PATCH /sites/{siteId}`
`POST /sites`
`DELETE /sites/{siteId}`

This file contains the existing site-related API stubs and mock logic. It is currently the main site-specific API file in the codebase and is used by screens that create, edit, or fetch site information.
```
page corelated: 
- `src/pages/Dashboard.tsx` (calls `getDashboardProjects()` which may include site info)
- `src/pages/master/SiteRegister.tsx` (calls `registerSite()`)
- `src/pages/master/SiteEdit.tsx` (calls `updateSite()`)
```

#### `src/api/sites.ts`
`GET /sites/{siteId}`

This file is the newer site API module exported from `src/api/index.ts`. If the codebase continues migrating toward the newer site flow, this file is where site API operations should live. At the moment, it coexists with `siteApi.ts`, so both files matter when tracing site-related calls.

#### `src/api/userApi.ts`
`GET /users/{id}`
`GET /profile`
`POST /users`
`GET /company/profile`
`PATCH /profile`
`DELETE /users/{id}`
`PATCH /company`

This file contains user-management API logic and stubs. It is relevant when the frontend needs to fetch or update user records that are tied to a company or site.
```
page corelated: 
- `src/pages/Profile.tsx` (calls `getUserProfile()`, `updateUserProfile()`)
- `src/pages/master/UserEdit.tsx` (calls `getUserById()`, `updateUser()`)
- `src/pages/users/useUserQuery.ts` (calls `getUserById()`)
- `src/pages/users/useUpdateUserMutation.ts` (calls `updateUser()`) 
```

#### `src/api/users.ts`
`GET /users`

This is another user-related module in the API folder. If a screen imports company users or dropdown data, this file may be part of that flow. Because the codebase has more than one user API module, it is important to check which one a page actually imports before changing anything.


### `src/pages/` and `src/contexts/`

These folders do not define API requests themselves, but they decide when the requests are triggered.

- Pages such as `src/pages/Login.tsx`, `src/pages/Signup.tsx`, `src/pages/ProjectList.tsx`, and the site registration/detail screens are where the user action happens.
- Context files such as `src/contexts/AuthContext.tsx` and `src/contexts/SignupContext.tsx` store state that is passed into the API payload.
- In other words: pages collect the data, context keeps it alive across steps, and the API module sends it.

### `src/mocks/handlers.ts`

This file defines the MSW request handlers. It is where the frontend tells the browser, "if a request matches this route, return this mocked response instead of going to the real backend."

### `src/mocks/browser.ts`

This file creates the browser worker that MSW uses to intercept requests. It is the browser-side bootstrap for the mocking layer.

### `src/mocks/mocks.ts`

This file decides when MSW starts. In this codebase it only starts in development mode, which means the mock layer is not part of production behavior.

### `public/mockServiceWorker.js`

This is the worker script that the browser loads to activate MSW. If this file is missing, the mock layer cannot start correctly.

### `src/main.tsx`

This is the application entry point. It calls `enableMocking()` before the app renders so the mock layer is ready before any API request is made.

### Quick Reading Order

If you want to trace one API call from start to finish, read the files in this order:

1. `src/pages/...` to see what triggers the action.
2. `src/contexts/...` if the payload is built from shared form state.
3. `src/api/...` to see the real request function and contract.
4. `src/mocks/...` to see whether the request is being intercepted in development.
5. `public/mockServiceWorker.js` and `src/main.tsx` to confirm the mock layer actually starts.

---

## MSW Mock Flow

The current frontend uses Mock Service Worker (MSW) to simulate API behavior in development. This exists so the frontend team can continue integration work, validation, and UI testing even when the backend is not fully available yet.

### Why MSW Is Used

MSW sits between the browser and the network. Instead of letting a request leave the browser, it intercepts the request and returns a controlled response from the frontend layer.

This gives us three practical benefits:
- The UI can be developed against a realistic request/response contract.
- The Network tab still shows a real fetch call from the app, so we can verify that the API layer is actually being executed.
- Backend developers can receive a stable contract example before the real implementation exists.

### Current Mocked Endpoint

At the moment, the mocked flow covers:
- `POST /auth/signup`

The mock returns a success response with a generated user ID so the signup flow can proceed to the completion step.

### Files Involved

The MSW setup is split into small files so the responsibilities stay clear:

**`src/mocks/handlers.ts`**
- Defines the request handlers.
- Contains the `POST */auth/signup` interception rule.
- Reads the request body with `await request.json()`.
- Returns a mocked JSON response with `HttpResponse.json(...)`.

**`src/mocks/browser.ts`**
- Creates the browser worker with `setupWorker(...handlers)`.
- This is the browser-side entry point for MSW.

**`src/mocks/mocks.ts`**
- Exposes `enableMocking()`.
- Starts the worker only in development mode.
- Uses `onUnhandledRequest: "bypass"` so any endpoint that is not mocked will continue to the real network.

**`src/main.tsx`**
- Calls `enableMocking()` during application startup.
- This ensures the mock layer is prepared before the app begins handling requests in the browser.

**`public/mockServiceWorker.js`**
- Required service worker file generated by MSW.
- Without this file, the browser cannot register the worker, and the mock layer will not activate.

### Request Flow

When the user submits the signup form, the flow is:

1. The form data is collected in the signup pages and stored in context.
2. The confirmation step builds the API payload.
3. The frontend calls `signUp(payload)` from `src/api/auth.ts`.
4. `signUp()` uses `fetch()` to send the request.
5. MSW intercepts the request in the browser.
6. `handlers.ts` returns a mocked response.
7. The UI receives the response and continues to the completion state.

From the UI perspective, this behaves like a normal HTTP request. The difference is that the response is generated locally by MSW instead of by a real backend service.

### How To Verify The Mock Is Working

You can confirm the mock is active by checking the following:

- Open the browser DevTools Network tab and submit the signup form.
- Confirm that the request is sent by the frontend API layer.
- Check the browser console for the log message from `handlers.ts`.
- Verify that the response status is `201` and the payload contains `success: true`.

If the mock is not active, the request will behave like a normal network request and may fail if the backend URL is not reachable.

### What The Backend Team Should Know

MSW is not the final API implementation. It is a temporary frontend-side integration layer used to validate the contract.

For backend implementation, the important outputs are:
- The request body shape expected by `POST /auth/signup`.
- The success response contract returned to the frontend.
- The fact that the frontend currently depends on this response shape to advance the signup flow.

When the real backend is ready, the mock handler can be removed or expanded into additional endpoints with the same request/response shape.

### Practical Limitation

Right now only the signup flow is mocked in MSW. Other API functions still follow the existing code paths in the frontend and may remain mock/stub based until they are fully implemented.

---

## User System Architecture

### Role-Based Access Control

The system implements a company-based user structure with two roles:

#### Master User (マスターユーザー)
- First person from a company to sign up
- Can manage company sites (拠点)
- Can assign users as 担当者 (person in charge) for sites
- Can view all projects joined by any company site

#### 一般 User (Standard User)
- Created by master user
- Can only view projects for sites they're assigned to as 担当者
- Limited management capabilities

### Site System (拠点)
- Each company can have multiple sites
- Each site must have a 担当者 (person in charge)
- Sites join projects independently
- Master users see all sites; 一般 users see only their assigned sites

### First Login Flow
1. User signs up and admin approves
2. On first login, if user has no sites and no joined projects → redirect to project list
3. User sees available projects with 「参加する」 button
4. If user clicks 「参加する」 but has no sites → redirect to create site first

---

## Frontend API Module

### File: `src/api/config.ts`

This file contains shared configuration that backend developers need to update.

### Configuration Section

```typescript
// Update this with your actual API base URL
export const BASE_URL = 'https://api.example.com/v1';
```

**TODO**: Replace `'https://api.example.com/v1'` with your production API URL

### Feature Modules

Each API feature has its own module:
- **`src/api/auth.ts`** - Authentication functions (login, signUp)
- **`src/api/projects.ts`** - Project operations (getDashboardProjects, getAvailableProjects, getProjectDetails)
- **`src/api/monitoring.ts`** - Monitoring data submission (submitMonitoringData)
- **`src/api/types.ts`** - All TypeScript type definitions

### Authentication Headers

The service automatically includes authentication tokens in requests:

```typescript
const getAuthHeaders = (): HeadersInit => {
  const user = localStorage.getItem('user');
  const token = user ? JSON.parse(user).token : null;
  
  return {
    'Content-Type': 'application/json',
    ...(token && { 'Authorization': `Bearer ${token}` }),
  };
};
```

**Note**: The front-end stores the JWT token in `localStorage` under the key `'user'` after successful login. The `getAuthHeaders()` function in `src/api/config.ts` automatically includes this token in all authenticated requests.



---

## Required Endpoints

This section defines all endpoints required by the frontend application, with request/response contracts and integration notes.

---

### 1. Login (POST)

**Route**: `POST /auth/login`  
**Frontend Function**: `login(email, password)` in `src/api/auth.ts`

**Request Contract**:
```http
POST {BASE_URL}/auth/login
Content-Type: application/json

{
  "email": "user@example.com",
  "password": "securePassword123"
}
```

**Expected Response**:
```json
{
  "success": true,
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "user": {
    "id": "user-uuid-here",
    "email": "user@example.com",
    "name": "山田太郎",
    "companyId": "company-uuid",
    "companyName": "デモ株式会社",
    "role": "master",
    "siteId": "site-uuid-1",
    "siteName": "東京本社",
    "hasJoinedProjects": true
  }
}
```

**Role Values**: `'master' | '一般'`

**Frontend Uses**: `src/pages/Login.tsx`, `src/contexts/AuthContext.tsx`

**Backend Implementation Notes**:
- Verify password hash
- Return JWT token with appropriate expiration
- Include all user fields including role and location associations
- Set `siteId`, `siteName`, and `hasJoinedProjects` flags for first-login detection

---

### 2. Sign Up (POST)

**Route**: `POST /auth/signup`  
**Frontend Function**: `signUp(userData)` in `src/api/auth.ts`

**Expected Request**:
```http
POST {BASE_URL}/auth/signup
Content-Type: application/json

{
  "corporateNumber": "1234567890123",
  "companyName": "デモ株式会社",
  "companyNameKana": "デモカブシキガイシャ",
  "companyNameEnglish": "Demo Corporation",
  "postalCode": "100-0001",
  "prefecture": "東京都",
  "city": "千代田区",
  "address": "丸の内1-1-1",
  "building": "〇〇ビル 5階",
  "lastName": "山田",
  "firstName": "太郎",
  "lastNameKana": "ヤマダ",
  "firstNameKana": "タロウ",
  "position": "環境管理責任者",
  "phoneNumber": "03-1234-5678",
  "faxNumber": "03-1234-5679",
  "email": "yamada@demo.co.jp",
  "methodologies": [
    {
      "id": "methodology-1234567890",
      "methodology": "EN-R-002",
      "equipmentItems": [
        {
          "installationDate": "2024-01-15",
          "manufacturer": "○○製作所",
          "model": "ABC-1234"
        },
        {
          "installationDate": "2024-02-01",
          "manufacturer": "△△工業",
          "model": "XYZ-5678"
        }
      ],
      "hasSubsidy": "yes",
      "subsidyName": "省エネ設備導入補助金"
    },
    {
      "id": "methodology-9876543210",
      "methodology": "ISO14001",
      "equipmentItems": [
        {
          "installationDate": "2024-03-10",
          "manufacturer": "□□電機",
          "model": "DEF-9012"
        }
      ],
      "hasSubsidy": "no"
    }
  ],
  "goals": "CO2排出量を30%削減"
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

**Implementation Location**: `src/api/auth.ts` - `signUp()` function

**Frontend Integration**: `src/pages/Signup.tsx` with 4-step form flow

**Backend Requirements**:
- First signup from a company → create master user
- Hash password before storage
- Create company record if new
- Store all methodology and equipment information:
  - Each methodology entry includes methodology code, multiple equipment items, and optional subsidy information
  - Equipment items contain: installationDate, manufacturer, model
  - Subsidy information: hasSubsidy ('yes' or 'no') and subsidyName (required if hasSubsidy is 'yes')
- Send verification email
- Mark account as pending admin approval
- Return user ID

**Data Structure Notes**:
- `methodologies` array: Contains one or more methodology forms
- Each methodology can have multiple `equipmentItems` (minimum 1)
- Each methodology has exactly one subsidy section (not repeatable)
- All equipment items within a methodology share the same methodology code

---

### 3. Get Dashboard Projects (GET)

**Route**: `GET /projects`  
**Frontend Function**: `getDashboardProjects()` in `src/api/projects.ts`

**Expected Request**:
```http
GET {BASE_URL}/projects
Authorization: Bearer {token}
```

**Expected Response**:
```json
[
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
```

**Status Values**: `'active' | 'maintenance' | 'inactive'`

**Implementation Location**: `src/api/projects.ts` - `getDashboardProjects()` function

**Frontend Integration**: `src/pages/Dashboard.tsx`

**Backend Requirements**:
- Master users: return all projects for any company location
- 一般 users: return only projects for locations they're 担当者 of
- Include `isJoined`, `locationId`, and `locationName` if location has joined
- Sort by most recent update

---

### 4. Get Project Details (GET)

**Route**: `GET /projects/{projectId}`  
**Frontend Function**: `getProjectDetails(projectId)` in `src/api/projects.ts`

**Expected Request**:
```http
GET {BASE_URL}/projects/{projectId}
Authorization: Bearer {token}
```

**Expected Response**:
```json
{
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
```

**Implementation Location**: `src/api/projects.ts` - `getProjectDetails()` function

**Frontend Integration**: `src/pages/ProjectDetail.tsx`

**Backend Requirements**:
- Verify user has permission to access project
- Include device list with current status
- Include recent monitoring data (24-48 hours)
- Include uploaded documents

---

### 5. Submit Monitoring Data (POST)

**Route**: `POST /monitoring/data`  
**Frontend Function**: `submitMonitoringData(data)` in `src/api/monitoring.ts`

**Expected Request**:
```http
POST {BASE_URL}/monitoring/data
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

**Implementation Location**: `src/api/monitoring.ts` - `submitMonitoringData()` function

**Frontend Integration**: `src/pages/MonitoringRegister.tsx`

**Backend Requirements**:
- Validate all fields
- Verify device belongs to accessible project
- Store with server timestamp
- Trigger compliance alerts if needed

---

### 6. Get Company Sites (GET)

**Route**: `GET /sites`  
**Frontend Function**: `getSites()` - already exported from `src/api/sites.ts`

**Expected Request**:
```http
GET {BASE_URL}/sites
Authorization: Bearer {token}
```

**Expected Response**:
```json
[
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
    "personInChargeEmail": "yamada@demo.co.jp",
    "joinedProjects": ["proj-uuid-1", "proj-uuid-2"],
    "createdAt": "2024-01-15"
  }
]
```

**Frontend Integration**: `src/pages/SiteDetail.tsx`, `src/pages/SiteRegister.tsx`, `src/pages/SiteEdit.tsx`

**Backend Requirements**:
- Master users: return all company sites
- 一般 users: return only sites they're 担当者 of
- Include person in charge details
- Include list of joined project IDs

**Notes**: This endpoint should match the existing `Site` model in `src/api/types.ts` and `src/api/siteApi.ts`.

---

### 7. Get Site Details (GET)

**Route**: `GET /sites/{siteId}`  
**Frontend Function**: `getSiteData(siteId)` in `src/api/siteApi.ts`

**Expected Request**:
```http
GET {BASE_URL}/sites/{siteId}
Authorization: Bearer {token}
```

**Expected Response**:
```json
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
```

**Frontend Integration**: `src/pages/SiteDetail.tsx`

**Backend Requirements**:
- Verify user has access to location
- Include full project details for joined projects
- Include person in charge information

**Notes**: This endpoint should align with the existing `Site` detail flow in the frontend.

---

### 8. Create Site (POST)

**Route**: `POST /sites`  
**Frontend Function**: `registerSite(siteData)` in `src/api/siteApi.ts`

**Expected Request**:
```http
POST {BASE_URL}/sites
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

**Frontend Integration**: `src/pages/SiteRegister.tsx`

**Backend Requirements**:
- Only master users can create sites
- Validate person in charge is a user in the company
- Associate site with company
- Return site ID

**Notes**: This endpoint should map to the existing `registerSite()` flow.

---

### 9. Update Site (PUT)

**Route**: `PUT /sites/{siteId}`  
**Frontend Function**: `updateSite(siteId, updates)` in `src/api/siteApi.ts`

**Expected Request**:
```http
PUT {BASE_URL}/sites/{siteId}
Authorization: Bearer {token}
Content-Type: application/json

{
  "siteName": "大阪支店",
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

**Frontend Integration**: `src/pages/SiteDetail.tsx` (edit mode)

**Backend Requirements**:
- Only master users can update sites
- Validate person in charge change
- Update site record

**Notes**: This endpoint should map to the existing `updateSite()` flow.

---

### 10. Get Company Users (GET)

**Route**: `GET /users/company`  
**Frontend Function**: `getCompanyUsers()` - **Needs to be added to `src/api/users.ts`**

**Expected Request**:
```http
GET {BASE_URL}/users/company
Authorization: Bearer {token}
```

**Expected Response**:
```json
[
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
    "role": "一般",
    "assignedLocations": ["location-uuid-1"]
  }
]
```

**Frontend Integration**: `src/pages/SiteRegister.tsx`, `src/pages/SiteDetail.tsx` (for person in charge dropdown)

**Backend Requirements**:
- Return all users in the authenticated user's company
- Include role and assigned sites
- Master users see all; 一般 users see only themselves

**Notes**: This endpoint needs to be added to frontend - create `src/api/users.ts` with `getCompanyUsers()` function

---

### 11. Get Available Projects (GET)

**Route**: `GET /projects/available`  
**Frontend Function**: `getAvailableProjects()` in `src/api/projects.ts`

**Expected Request**:
```http
GET {BASE_URL}/projects/available
Authorization: Bearer {token}
```

**Expected Response**:
```json
[
  {
    "id": "proj-uuid-1",
    "name": "環境モニタリングプロジェクトA",
    "description": "CO2排出量削減を目的とした環境モニタリングプロジェクト",
    "methodology": "EN-S-001",
    "deadline": "2024-03-31",
    "requirements": [
      "設備情報の提出",
      "測定データの定期報告",
      "年次レポートの作成"
    ],
    "isJoined": false
  }
]
```

**Frontend Integration**: `src/pages/ProjectList.tsx`

**Backend Requirements**:
- Return all projects available for the company to join
- Mark `isJoined: true` if any company site has joined
- Include application deadline and requirements
- Show only projects where deadline hasn't passed

**Implementation Location**: `src/api/projects.ts` - `getAvailableProjects()` function

---

### 12. Submit Project Registration (POST)

**Route**: `POST /projects/{projectId}/register`  
**Frontend Function**: `submitProjectRegistration(registrationData)` - **Needs to be added to `src/api/projects.ts`**

**Expected Request**:
```http
POST {BASE_URL}/projects/{projectId}/register
Authorization: Bearer {token}
Content-Type: multipart/form-data

{
  "locationId": "location-uuid-1",
  "devices": [
    {
      "oldDeviceName": "旧空調機",
      "oldDeviceId": "OLD-001",
      "oldDeviceType": "空調設備",
      "newDeviceName": "新型空調機",
      "newDeviceId": "NEW-001",
      "newDeviceType": "高効率空調設備",
      "verificationFile": <File>
    }
  ],
  "agreements": {
    "dataAccuracy": true,
    "compliance": true,
    "reporting": true,
    "inspection": true
  },
  "contractAgreed": true,
  "finalTermsAgreed": true
}
```

**Expected Response**:
```json
{
  "success": true,
  "message": "プロジェクト参加申請が完了しました",
  "registrationId": "registration-uuid"
}
```

**Frontend Integration**: `src/pages/ProjectRegister.tsx` (6-step flow)

**Backend Requirements**:
- Validate site belongs to user's company
- Store all device information and uploaded files
- Record all agreements
- Mark application as pending review
- Send confirmation email
- Associate site with project (pending approval)

**File Upload**: `verificationFile` for each device (PDF, JPG, PNG formats)

**Notes**: This endpoint needs to be added to `src/api/projects.ts`

---

## Integration Examples

### Example 1: Login with Role-Based Data

**File**: `src/pages/Login.tsx` & `src/contexts/AuthContext.tsx`

```typescript
import { login } from '@/api';

const response = await login(email, password);

// Response includes role and site data:
// {
//   success: true,
//   token: string,
//   user: {
//     id, email, name,
//     companyId, companyName,
//     role: 'master' | '一般',
//     siteId: string,
//     siteName: string,
//     hasJoinedProjects: boolean
//   }
// }

// Store complete user object
localStorage.setItem('user', JSON.stringify(response.user));

// First login detection
if (!response.user.siteId && !response.user.hasJoinedProjects) {
  navigate('/projects'); // Show available projects
}
```

---

### Example 2: Site Management (Master User)

**File**: `src/pages/SiteDetail.tsx`

```typescript
import { getSites } from '@/api';

// Master user sees all company sites
const sites = await getSites();

// Each site shows:
// - Name and address
// - Person in charge (担当者)
// - Number of joined projects
```

**File**: `src/pages/SiteRegister.tsx`

```typescript
import { getCompanyUsers } from '@/api';
import { registerSite } from '@/api/siteApi';

// Create new site with person in charge
const users = await getCompanyUsers();

// Select from dropdown (shows both master and 一般 users)
const newSite = {
  siteName: "大阪支店",
  ...addressData,
  personInChargeId: selectedUserId
};

await registerSite(newSite);
```

---

### Example 3: Project Registration Flow

**File**: `src/pages/ProjectList.tsx`

```typescript
import { getAvailableProjects } from '@/api';

// Show available projects
const projects = await getAvailableProjects();

// User clicks 「参加する」
// Check if user has locations first
if (!user.siteId) {
  navigate('/sites/register');
  return;
}

// Otherwise start registration flow
navigate(`/projects/${projectId}/register`);
```

**File**: `src/pages/ProjectRegister.tsx`

```typescript
import { submitProjectRegistration } from '@/api';

// 6-step registration process:
// Step 1: Select location
// Step 2: Enter device details + upload verification files
// Step 3: Agree to 4 checkboxes
// Step 4: Read and agree to contract
// Step 5: Read and agree to final terms
// Step 6: Show completion message

const registrationData = {
  locationId: selectedLocationId,
  devices: [...deviceData],
  agreements: { ... },
  contractAgreed: true,
  finalTermsAgreed: true
};

await submitProjectRegistration(projectId, registrationData);
```

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

| Code | Meaning | Frontend Handling |
|------|---------|-------------------|
| 200 | Success | Process response data |
| 201 | Created | Process response data |
| 400 | Bad Request | Show validation errors |
| 401 | Unauthorized | Redirect to login |
| 403 | Forbidden | Show "access denied" message |
| 404 | Not Found | Show "not found" message |
| 500 | Server Error | Show generic error message |

### Role-Based Access Errors

**403 Forbidden** should be returned when:
- 一般 user tries to access master-only endpoints (site management)
- User tries to access sites/projects they're not authorized for
- User tries to modify data they don't own

---

## Testing Guide

### Step 1: Update BASE_URL

```typescript
// In src/api/config.ts
export const BASE_URL = 'https://your-api-url.com/v1';
```

### Step 2: Test Role-Based Access

1. **Test as Master User**:
   - Login with email containing "master" (triggers mock master user)
   - Verify access to `/locations` (拠点管理 in navbar)
   - Can create/edit locations
   - Can see all company projects

2. **Test as 一般 User**:
   - Login with regular email
   - Should NOT see 拠点管理 in navbar
   - Can only see assigned locations
   - Limited project visibility

### Step 3: Test First Login Flow

1. Create user with `siteId: null` and `hasJoinedProjects: false`
2. Login → should redirect to `/projects` (project list)
3. Click 「参加する」 on any project
4. If no sites → redirect to `/sites/register`
5. Create site → return to project registration

### Step 4: Test Project Registration Flow

1. Navigate to project list
2. Click 「参加する」 button
3. Complete all 6 steps:
   - Step 1: Select location ✓
   - Step 2: Add devices with file uploads ✓
   - Step 3: Check all 4 agreements ✓
   - Step 4: Agree to contract ✓
   - Step 5: Agree to final terms ✓
   - Step 6: See completion message ✓
4. Verify validation at each step

### Step 5: Browser Testing Checklist

```bash
# Run development server
npm run dev

# Test in browser:
# 1. Login (check network tab for API call)
# 2. Dashboard (verify projects load)
# 3. Sites page (master user only)
# 4. Project list
# 5. Project registration flow
# 6. Site management
```

---
<!-- 
## New Endpoints Summary

The following endpoints are **NEW** and need to be implemented:

| Handler | Route | Method | Description |
|---------|-------|--------|-------------|
| `api/sites/list.handler.js` | `/sites` | GET | List company sites |
| `api/sites/details.handler.js` | `/sites/{id}` | GET | Get site details |
| `api/sites/create.handler.js` | `/sites` | POST | Create site (master only) |
| `api/sites/update.handler.js` | `/sites/{id}` | PUT | Update site (master only) |
| `api/users/company.handler.js` | `/users/company` | GET | List company users |
| `api/projects/available.handler.js` | `/projects/available` | GET | List available projects to join |
| `api/projects/register.handler.js` | `/projects/{id}/register` | POST | Submit project registration (with file upload) |

All other endpoints from the original documentation remain the same but with updated response structures to include role and location data. -->


## Local Development vs ngrok Backend

Frontend can be connected to backend in two supported ways:

### Option A. Local backend
Use this when backend runs on the same machine.

- Frontend base URL: `/api`
- Local Backend target: `http://localhost:8081`
- Backend endpoint example: `https://wannabe-kilowatt-overeater.ngrok-free.dev/auth/user/login`

What should be changed:
- `vite.config.ts` proxy target (CORS passed!)
```ts
server: {
    host: "::",
    port: 8080,
    cors: true,
    allowedHosts: true,
    middlewareMode: false,
    proxy: {
      "/api": {
        target: "https://wannabe-kilowatt-overeater.ngrok-free.dev",
        target: "http://localhost:8081",
        changeOrigin: true,
      },
    }
  },
```

- `src/api/config.ts` base URL
```ts
export const BASE_URL = '/api';
```

- `src/api/auth.ts` login function
```ts
const response = await fetch(`${BASE_URL}/auth/user/login`, {...
```

| Environment | BASE_URL | Vite proxy target | Notes |
|---|---|---|---|
| Local backend | `/api` | `http://localhost:8081` | Use when backend runs locally |
| ngrok backend | `/api` | `https://<ngrok-host>.ngrok-free.dev` | Use when backend is exposed through ngrok |