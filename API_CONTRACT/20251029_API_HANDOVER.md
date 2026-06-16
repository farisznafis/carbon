# API Integration Handover Documentation

## Overview
This document provides backend developers with clear instructions on how to integrate REST APIs with the existing front-end application for the GX environmental monitoring system with multi-location, role-based access control.

---

## Table of Contents
1. [Architecture Overview](#architecture-overview)
2. [User System Architecture](#user-system-architecture)
3. [API Service Module](#api-service-module)
4. [Recommended Backend Structure](#recommended-backend-structure)
5. [Required Endpoints](#required-endpoints)
6. [Integration Examples](#integration-examples)
7. [Error Handling](#error-handling)
8. [Testing Guide](#testing-guide)

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
├── index.ts           # Central export point for all API functions
├── config.ts          # BASE_URL and shared utilities
├── types.ts           # TypeScript interfaces for all API responses
├── auth.ts            # Authentication functions (login, signUp)
├── projects.ts        # Project-related functions
└── monitoring.ts      # Monitoring data functions
```

This architecture:
- Encapsulates all HTTP request logic by feature
- Provides type-safe interfaces for all API responses
- Handles authentication headers automatically
- Includes comprehensive inline documentation
- Makes it easy to find and update specific API calls

### Current Implementation Status
✅ **Mock implementations** are currently in place for all endpoints  
⚠️ **Action Required**: Replace mock responses with actual REST API calls

---

## User System Architecture

### Role-Based Access Control

The system implements a company-based user structure with two roles:

#### Master User (マスターユーザー)
- First person from a company to sign up
- Can manage company locations (拠点)
- Can assign users as 担当者 (person in charge) for locations
- Can view all projects joined by any company location

#### 一般 User (Standard User)
- Created by master user
- Can only view projects for locations they're assigned to as 担当者
- Limited management capabilities

### Location System (拠点)
- Each company can have multiple locations
- Each location must have a 担当者 (person in charge)
- Locations join projects independently
- Master users see all locations; 一般 users see only their assigned locations

### First Login Flow
1. User signs up and admin approves
2. On first login, if user has no locations and no joined projects → redirect to project list
3. User sees available projects with 「参加する」 button
4. If user clicks 「参加する」 but has no locations → redirect to create location first

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

## Recommended Backend Structure

### API Folder Organization

We recommend organizing your backend with an `api/` folder structure to improve code organization, maintainability, and onboarding:

```
backend/
├── api/
│   ├── auth/
│   │   ├── login.handler.js        # POST /auth/login
│   │   └── signup.handler.js       # POST /auth/signup
│   ├── projects/
│   │   ├── list.handler.js         # GET /projects
│   │   ├── available.handler.js    # GET /projects/available
│   │   ├── details.handler.js      # GET /projects/{id}
│   │   └── register.handler.js     # POST /projects/{id}/register
│   ├── locations/
│   │   ├── list.handler.js         # GET /locations
│   │   ├── details.handler.js      # GET /locations/{id}
│   │   ├── create.handler.js       # POST /locations
│   │   └── update.handler.js       # PUT /locations/{id}
│   ├── users/
│   │   └── company.handler.js      # GET /users/company
│   └── monitoring/
│       └── data.handler.js         # POST /monitoring/data
├── middleware/
│   ├── auth.middleware.js          # JWT verification
│   └── role.middleware.js          # Role-based access control
├── models/
│   ├── user.model.js
│   ├── company.model.js
│   ├── location.model.js
│   └── project.model.js
└── utils/
    ├── validation.js
    └── response.js
```

### Benefits of This Structure

1. **Clear Separation**: Each endpoint has its own handler file
2. **Easy Navigation**: Developers can quickly find specific route logic
3. **Scalability**: Adding new endpoints doesn't clutter existing files
4. **Team Collaboration**: Multiple developers can work on different routes without conflicts
5. **Testing**: Easier to write unit tests for individual handlers
6. **Maintenance**: Simpler to update or refactor specific endpoints

### Route Grouping

Endpoints are organized by domain:

- **`/auth`** - Authentication and authorization
- **`/projects`** - Project management and registration
- **`/locations`** - Location (拠点) management
- **`/users`** - User and company user management
- **`/monitoring`** - Environmental monitoring data submission

---

## Required Endpoints

All endpoints below map to the recommended `api/` folder structure. Each section includes the handler file location for easy reference.

---

### 1. Login (POST)

**Handler**: `api/auth/login.handler.js`  
**Route**: `POST /auth/login`  
**Frontend**: `login(email, password)` from `src/api/auth.ts`

**Expected Request**:
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
    "locations": ["location-uuid-1", "location-uuid-2"],
    "hasLocations": true,
    "hasJoinedProjects": true
  }
}
```

**User Role Values**: `'master' | '一般'`

**Implementation Location**: `src/api/auth.ts` - `login()` function

**Frontend Integration**: `src/pages/Login.tsx` and `src/contexts/AuthContext.tsx`

**Backend Requirements**:
- Verify password hash
- Return JWT token with appropriate expiration
- Include all user fields including role and location associations
- Set `hasLocations` and `hasJoinedProjects` flags for first-login detection

---

### 2. Sign Up (POST)

**Handler**: `api/auth/signup.handler.js`  
**Route**: `POST /auth/signup`  
**Frontend**: `signUp(userData)` from `src/api/auth.ts`

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

**Handler**: `api/projects/list.handler.js`  
**Route**: `GET /projects`  
**Frontend**: `getDashboardProjects()` from `src/api/projects.ts`

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

**Handler**: `api/projects/details.handler.js`  
**Route**: `GET /projects/{projectId}`  
**Frontend**: `getProjectDetails(projectId)` from `src/api/projects.ts`

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

**Handler**: `api/monitoring/data.handler.js`  
**Route**: `POST /monitoring/data`  
**Frontend**: `submitMonitoringData(data)` from `src/api/monitoring.ts`

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

### 6. Get Company Locations (GET)

**Handler**: `api/locations/list.handler.js`  
**Route**: `GET /locations`  
**Frontend**: `getCompanyLocations()` - **Needs to be added to `src/api/` (create `locations.ts`)**

**Expected Request**:
```http
GET {BASE_URL}/locations
Authorization: Bearer {token}
```

**Expected Response**:
```json
[
  {
    "id": "location-uuid-1",
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

**Frontend Integration**: `src/pages/Locations.tsx`

**Backend Requirements**:
- Master users: return all company locations
- 一般 users: return only locations they're 担当者 of
- Include person in charge details
- Include list of joined project IDs

**Notes**: This endpoint needs to be added to frontend - create `src/api/locations.ts` with `getCompanyLocations()` function

---

### 7. Get Location Details (GET)

**Handler**: `api/locations/details.handler.js`  
**Route**: `GET /locations/{locationId}`  
**Frontend**: `getLocationDetails(locationId)` - **Needs to be added to `src/api/locations.ts`**

**Expected Request**:
```http
GET {BASE_URL}/locations/{locationId}
Authorization: Bearer {token}
```

**Expected Response**:
```json
{
  "id": "location-uuid-1",
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

**Frontend Integration**: `src/pages/LocationDetail.tsx`

**Backend Requirements**:
- Verify user has access to location
- Include full project details for joined projects
- Include person in charge information

**Notes**: This endpoint needs to be added to `src/api/locations.ts`

---

### 8. Create Location (POST)

**Handler**: `api/locations/create.handler.js`  
**Route**: `POST /locations`  
**Frontend**: `createLocation(locationData)` - **Needs to be added to `src/api/locations.ts`**

**Expected Request**:
```http
POST {BASE_URL}/locations
Authorization: Bearer {token}
Content-Type: application/json

{
  "name": "大阪支店",
  "postalCode": "530-0001",
  "prefecture": "大阪府",
  "city": "大阪市北区",
  "address": "梅田1-1-1",
  "building": "△△ビル 3階",
  "personInChargeId": "user-uuid-2"
}
```

**Expected Response**:
```json
{
  "success": true,
  "message": "拠点が正常に登録されました",
  "locationId": "location-uuid-new"
}
```

**Frontend Integration**: `src/pages/LocationRegister.tsx`

**Backend Requirements**:
- Only master users can create locations
- Validate person in charge is a user in the company
- Associate location with company
- Return location ID

**Notes**: This endpoint needs to be added to `src/api/locations.ts`

---

### 9. Update Location (PUT)

**Handler**: `api/locations/update.handler.js`  
**Route**: `PUT /locations/{locationId}`  
**Frontend**: `updateLocation(locationId, updates)` - **Needs to be added to `src/api/locations.ts`**

**Expected Request**:
```http
PUT {BASE_URL}/locations/{locationId}
Authorization: Bearer {token}
Content-Type: application/json

{
  "name": "大阪支店",
  "postalCode": "530-0001",
  "prefecture": "大阪府",
  "city": "大阪市北区",
  "address": "梅田1-1-1",
  "building": "△△ビル 3階",
  "personInChargeId": "user-uuid-3"
}
```

**Expected Response**:
```json
{
  "success": true,
  "message": "拠点情報を更新しました"
}
```

**Frontend Integration**: `src/pages/LocationDetail.tsx` (edit mode)

**Backend Requirements**:
- Only master users can update locations
- Validate person in charge change
- Update location record

**Notes**: This endpoint needs to be added to `src/api/locations.ts`

---

### 10. Get Company Users (GET)

**Handler**: `api/users/company.handler.js`  
**Route**: `GET /users/company`  
**Frontend**: `getCompanyUsers()` - **Needs to be added to `src/api/` (create `users.ts`)**

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

**Frontend Integration**: `src/pages/LocationRegister.tsx`, `src/pages/LocationDetail.tsx` (for person in charge dropdown)

**Backend Requirements**:
- Return all users in the authenticated user's company
- Include role and assigned locations
- Master users see all; 一般 users see only themselves

**Notes**: This endpoint needs to be added to frontend - create `src/api/users.ts` with `getCompanyUsers()` function

---

### 11. Get Available Projects (GET)

**Handler**: `api/projects/available.handler.js`  
**Route**: `GET /projects/available`  
**Frontend**: `getAvailableProjects()` from `src/api/projects.ts`

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
- Mark `isJoined: true` if any company location has joined
- Include application deadline and requirements
- Show only projects where deadline hasn't passed

**Implementation Location**: `src/api/projects.ts` - `getAvailableProjects()` function

---

### 12. Submit Project Registration (POST)

**Handler**: `api/projects/register.handler.js`  
**Route**: `POST /projects/{projectId}/register`  
**Frontend**: `submitProjectRegistration(registrationData)` - **Needs to be added to `src/api/projects.ts`**

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
- Validate location belongs to user's company
- Store all device information and uploaded files
- Record all agreements
- Mark application as pending review
- Send confirmation email
- Associate location with project (pending approval)

**File Upload**: `verificationFile` for each device (PDF, JPG, PNG formats)

**Notes**: This endpoint needs to be added to `src/api/projects.ts`

---

## Integration Examples

### Example 1: Login with Role-Based Data

**File**: `src/pages/Login.tsx` & `src/contexts/AuthContext.tsx`

```typescript
import { login } from '@/api';

const response = await login(email, password);

// Response includes role and location data:
// {
//   success: true,
//   token: string,
//   user: {
//     id, email, name,
//     companyId, companyName,
//     role: 'master' | '一般',
//     locations: string[],
//     hasLocations: boolean,
//     hasJoinedProjects: boolean
//   }
// }

// Store complete user object
localStorage.setItem('user', JSON.stringify(response.user));

// First login detection
if (!response.user.hasLocations && !response.user.hasJoinedProjects) {
  navigate('/projects'); // Show available projects
}
```

---

### Example 2: Location Management (Master User)

**File**: `src/pages/Locations.tsx`

```typescript
import { getCompanyLocations } from '@/api';

// Master user sees all company locations
const locations = await getCompanyLocations();

// Each location shows:
// - Name and address
// - Person in charge (担当者)
// - Number of joined projects
```

**File**: `src/pages/LocationRegister.tsx`

```typescript
import { getCompanyUsers, createLocation } from '@/api';

// Create new location with person in charge
const users = await getCompanyUsers();

// Select from dropdown (shows both master and 一般 users)
const newLocation = {
  name: "大阪支店",
  ...addressData,
  personInChargeId: selectedUserId
};

await createLocation(newLocation);
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
if (!user.hasLocations) {
  navigate('/locations/register');
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
- 一般 user tries to access master-only endpoints (location management)
- User tries to access locations/projects they're not authorized for
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

1. Create user with `hasLocations: false` and `hasJoinedProjects: false`
2. Login → should redirect to `/projects` (project list)
3. Click 「参加する」 on any project
4. If no locations → redirect to `/locations/register`
5. Create location → return to project registration

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
# 3. Locations page (master user only)
# 4. Project list
# 5. Project registration flow
# 6. Location management
```

---

## Quick Reference Checklist

### Backend Developer Tasks

**Setup & Structure**
- [ ] Set up `api/` folder with recommended structure
- [ ] Update `BASE_URL` in `src/api/config.ts`
- [ ] Implement authentication middleware (`middleware/auth.middleware.js`)
- [ ] Implement role-based access control middleware (`middleware/role.middleware.js`)

**Authentication Routes** (`api/auth/`)
- [ ] `login.handler.js` - POST `/auth/login` - with role data
- [ ] `signup.handler.js` - POST `/auth/signup` - create master user for first signup

**Project Routes** (`api/projects/`)
- [ ] `list.handler.js` - GET `/projects` - role-based filtering
- [ ] `details.handler.js` - GET `/projects/{id}`
- [ ] `available.handler.js` - GET `/projects/available` - **NEW**
- [ ] `register.handler.js` - POST `/projects/{id}/register` - **NEW** with file upload

**Location Routes** (`api/locations/`) - **ALL NEW**
- [ ] `list.handler.js` - GET `/locations` - role-based filtering
- [ ] `details.handler.js` - GET `/locations/{id}`
- [ ] `create.handler.js` - POST `/locations` - master only
- [ ] `update.handler.js` - PUT `/locations/{id}` - master only

**User Routes** (`api/users/`) - **NEW**
- [ ] `company.handler.js` - GET `/users/company` - return company users

**Monitoring Routes** (`api/monitoring/`)
- [ ] `data.handler.js` - POST `/monitoring/data`

**Infrastructure**
- [ ] Set up JWT token generation with role claims
- [ ] Configure CORS for frontend domain
- [ ] Set up file upload handling (multipart/form-data)
- [ ] Test all endpoints with Postman/cURL

### Frontend Files Reference

**Core API Module**:
- ✅ `src/api/` - All API logic organized by feature
  - ✅ `index.ts` - Central export point
  - ✅ `config.ts` - BASE_URL and shared utilities
  - ✅ `types.ts` - TypeScript type definitions
  - ✅ `auth.ts` - Authentication functions
  - ✅ `projects.ts` - Project operations
  - ✅ `monitoring.ts` - Monitoring data submission
  - ⚠️ `locations.ts` - Location operations (**TO BE CREATED**)
  - ⚠️ `users.ts` - User operations (**TO BE CREATED**)

**Authentication**:
- ✅ `src/contexts/AuthContext.tsx` - Updated with role system
- ✅ `src/pages/Login.tsx` - Login page
- ✅ `src/pages/Signup.tsx` - 4-step signup flow

**Dashboard & Projects**:
- ✅ `src/pages/Dashboard.tsx` - Dashboard with first-login detection
- ✅ `src/pages/ProjectList.tsx` - Available projects (**NEW**)
- ✅ `src/pages/ProjectDetail.tsx` - Project details with documents
- ✅ `src/pages/ProjectRegister.tsx` - 6-step registration flow (**NEW**)

**Locations** (**ALL NEW**):
- ✅ `src/pages/Locations.tsx` - Location list (master only)
- ✅ `src/pages/LocationRegister.tsx` - Create location form
- ✅ `src/pages/LocationDetail.tsx` - Location details with edit

**Monitoring**:
- ✅ `src/pages/MonitoringRegister.tsx` - Submit monitoring data

**Navigation**:
- ✅ `src/components/Navbar.tsx` - Role-based menu items

---

## New Endpoints Summary

The following endpoints are **NEW** and need to be implemented:

| Handler | Route | Method | Description |
|---------|-------|--------|-------------|
| `api/locations/list.handler.js` | `/locations` | GET | List company locations |
| `api/locations/details.handler.js` | `/locations/{id}` | GET | Get location details |
| `api/locations/create.handler.js` | `/locations` | POST | Create location (master only) |
| `api/locations/update.handler.js` | `/locations/{id}` | PUT | Update location (master only) |
| `api/users/company.handler.js` | `/users/company` | GET | List company users |
| `api/projects/available.handler.js` | `/projects/available` | GET | List available projects to join |
| `api/projects/register.handler.js` | `/projects/{id}/register` | POST | Submit project registration (with file upload) |

All other endpoints from the original documentation remain the same but with updated response structures to include role and location data.

---

## Example cURL Commands for Testing

### Login
```bash
curl -X POST https://your-api.com/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"master@demo.co.jp","password":"test123"}'
```

### Get Company Locations
```bash
curl -X GET https://your-api.com/v1/locations \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

### Create Location (Master Only)
```bash
curl -X POST https://your-api.com/v1/locations \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "大阪支店",
    "postalCode": "530-0001",
    "prefecture": "大阪府",
    "city": "大阪市北区",
    "address": "梅田1-1-1",
    "personInChargeId": "user-uuid-2"
  }'
```

### Get Available Projects
```bash
curl -X GET https://your-api.com/v1/projects/available \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

### Submit Project Registration
```bash
curl -X POST https://your-api.com/v1/projects/proj-uuid-1/register \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -F "locationId=location-uuid-1" \
  -F "devices[0][oldDeviceName]=旧空調機" \
  -F "devices[0][oldDeviceId]=OLD-001" \
  -F "devices[0][verificationFile]=@/path/to/file.pdf" \
  -F "agreements[dataAccuracy]=true" \
  -F "contractAgreed=true" \
  -F "finalTermsAgreed=true"
```

---

## Database Schema Suggestions

### users table
```sql
- id (uuid, primary key)
- company_id (uuid, foreign key)
- email (string, unique)
- password_hash (string)
- name (string)
- role (enum: 'master', '一般')
- created_at (timestamp)
- approved (boolean)
```

### companies table
```sql
- id (uuid, primary key)
- corporate_number (string)
- name (string)
- name_kana (string)
- name_english (string)
- postal_code (string)
- prefecture (string)
- city (string)
- address (string)
- building (string)
- created_at (timestamp)
```

### locations table
```sql
- id (uuid, primary key)
- company_id (uuid, foreign key)
- name (string)
- postal_code (string)
- prefecture (string)
- city (string)
- address (string)
- building (string)
- person_in_charge_id (uuid, foreign key to users)
- created_at (timestamp)
```

### project_locations table (join table)
```sql
- id (uuid, primary key)
- project_id (uuid, foreign key)
- location_id (uuid, foreign key)
- joined_date (timestamp)
- status (enum: 'pending', 'approved', 'active', 'inactive')
- registration_data (jsonb) - stores device info, agreements
```

### methodologies table
```sql
- id (uuid, primary key)
- user_id (uuid, foreign key to users)
- methodology_code (string) - e.g., 'EN-R-002', 'ISO14001'
- has_subsidy (boolean)
- subsidy_name (string, nullable)
- created_at (timestamp)
```

### equipment_items table
```sql
- id (uuid, primary key)
- methodology_id (uuid, foreign key to methodologies)
- installation_date (date)
- manufacturer (string)
- model (string)
- created_at (timestamp)
```

**Schema Notes**:
- During signup, users can create multiple methodology entries
- Each methodology can have multiple equipment items (1-to-many relationship)
- Each methodology has exactly one subsidy section
- Equipment items are linked to their parent methodology via `methodology_id`

---

**Last Updated**: 2025-10-28  
**Frontend Version**: 2.1.0  
**Frontend API Structure**: Feature-based modules in `src/api/`  
**System**: Multi-location, role-based environmental monitoring platform  
**Contact**: Backend Integration Team
