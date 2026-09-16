# API Documentation (just in case if needed)

Base URL: `http://<host>:8081/api`
Default Port: `8081`
Context Path: `/api`

---

## API Endpoints 

| Method | Endpoint | Description | Auth Required | Request Body |
|--------|----------|-------------|---------------|--------------|
| | | **Authentication** | | |
| POST | `/auth/user/login` | User login | No | `username, password` |
| POST | `/auth/admin/login` | Admin/Operator login | No | `username, password` |
| GET | `/auth/user/profile` | Get user profile | Yes (user) | - |
| POST | `/auth/user/change-password` | Change password | Yes | `currentPassword, newPassword` |
| POST | `/auth/dummyUser` | Create test user | No | `username, password, email, companyId` |
| POST | `/auth/dummyOperator` | Create test operator | No | `username, password, email, companyId` |
| | | **Site** | | |
| GET | `/site` | Get all sites for company | Yes (master) | Query: `companyId` |
| GET | `/site/{siteId}` | Get site details | Yes (user) | - |
| GET | `/site/{siteId}/project` | Get projects for site | Yes (user) | - |
| POST | `/site` | Create new site | Yes (master) | `siteName, address, prefecture, city` |
| PATCH | `/site/{siteId}` | Update site | Yes (master) | `siteName, address, prefecture, city` |
| | | **Project** | | |
| GET | `/project` | List projects (paginated) | Yes (operator) | Query: `page, limit, status, methodology, etc.` |
| POST | `/project` | Create new project | Yes (operator) | `projectNumber, projectName, status, methodology` |
| GET | `/project/{projectId}` | Get project details | Yes (operator) | - |
| PATCH | `/project/{projectId}` | Update project | Yes (operator) | `projectName, status, methodology` |
| POST | `/project/{projectId}/solar/register` | Register solar data (multipart) | Yes (operator) | `data` JSON + files |
| GET | `/project/dynamodb` | Test DynamoDB connection | No | - |
| | | **Site-Project** | | |
| POST | `/site/project` | Link site to project | Yes (master) | `siteId, projectId` |
| | | **Company** | | |
| PATCH | `/company` | Update company info | Yes (master) | `companyId, companyName, address, contactEmail, contactPhone` |
| | | **User** | | |
| GET | `/user` | Get all users for company | Yes (master) | - |
| POST | `/user` | Create new user | Yes (master) | `username, password, email, role, siteIdList` |
| PATCH | `/user` | Update user info | Yes | `username, email, role, siteIdList` |
| DELETE | `/user/site` | Remove user-site link | Yes (master) | `username, siteId` |
| | | **Monitoring** | | |
| POST | `/monitoring` | Register monitoring record | Yes (master) | `projectId, siteId, elpvEvidence, elpvrEvidence, elpjBatterySpecificationEvidence, elpjControlEvidence, elpv, elpvr, elpj` |
| GET | `/monitoring` | Get latest monitoring record | Yes | Query: `projectId, siteId` |
| POST | `/monitoring/{projectId}/upload-elpv-evidence` | Upload ELPV evidence | Yes | `siteId, description, file` |
| POST | `/monitoring/{projectId}/upload-elpvr-evidence` | Upload ELPVR evidence | Yes | `siteId, description, file` |
| POST | `/monitoring/{projectId}/upload-elpj-battery-specification-evidence` | Upload ELPJ battery spec | Yes | `siteId, description, file` |
| POST | `/monitoring/{projectId}/upload-elpj-control-evidence` | Upload ELPJ control evidence | Yes | `siteId, description, file` |

---

## Roles

| Role | Description |
|------|-------------|
| `master` | Full access to sites, users, companies, monitoring |
| `user` | Read access to assigned sites |
| `operator` | CRUD access to projects |

---

## Status Codes

| Code | HTTP Status | Description |
|------|-------------|-------------|
| SUCCESS | 200 | Successful operation |
| CREATED | 201 | Resource created |
| BAD_REQUEST | 400 | Invalid request |
| UNAUTHORIZED | 401 | Authentication required |
| FORBIDDEN | 403 | Access denied |
| NOT_FOUND | 404 | Resource not found |
| INTERNAL_ERROR | 500 | Server error |