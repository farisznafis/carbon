# ProjectPlanController API Map

## API-to-Service Map

| # | Method | API | Controller method | Service/dependency | Request/JWT notes | Operator permission |
|---:|---|---|---|---|---|---|
| 1 | GET | `/projectplan/autofill` | `getCorporationMaster` | `ProjectPlanAutofillService.getCorporationMaster` + `JwtUtil` | JWT supplies companyId, role, username | `PROJECT_READ` |
| 2 | GET | `/projectplan/autofill/{projectId}` | `getProjectMaster` | `ProjectPlanAutofillService.getProjectMaster` + `JwtUtil` | Path projectId plus companyId, role, username from JWT | `PROJECT_READ` |
| 3 | GET | `/projectplan/{projectId}/forms/{formSlug}` | `getForm` | `ProjectPlanFormService.getForm` + `JwtUtil` | Optional integer query `methodology`; JWT context | `PROJECT_READ` |
| 4 | PUT | `/projectplan/{projectId}/forms/{formSlug}` | `saveForm` | `ProjectPlanFormService.saveForm` + `JwtUtil` | Optional integer query `methodology`, raw `JsonNode` body; JWT context | `PROJECT_UPDATE` |
| 5 | POST | `/projectplan/{projectId}/forms/{formSlug}/files` | `uploadFormFile` | `ProjectPlanFormService.uploadFormFile` + `JwtUtil` | Multipart `fieldName` request parameter and `file` request part; JWT context | `PROJECT_UPDATE` |
| 6 | GET | `/projectplan/{projectId}/files/download-url` | `getFileDownloadUrl` | `ProjectPlanFormService.getFileDownloadUrl` + `JwtUtil` | Required `s3Key` query parameter; JWT context | `PROJECT_READ` |
| 7 | GET | `/projectplan/{projectId}/progress` | `getProgress` | `ProjectPlanFormService.getProgress` + `JwtUtil` | Path projectId and JWT context | `PROJECT_READ` |
| 8 | GET | `/projectplan/progress` | `getProgressForProjects` | `ProjectPlanFormService.getProgressForProjects` + `JwtUtil` | `projectIds` query parameter binds to `List<String>`; JWT context | `PROJECT_READ` |

## Service Index

1. `ProjectPlanAutofillService`
   - `GET /projectplan/autofill` (`getCorporationMaster`)
   - `GET /projectplan/autofill/{projectId}` (`getProjectMaster`)
2. `ProjectPlanFormService`
   - `GET /projectplan/{projectId}/forms/{formSlug}` (`getForm`)
   - `PUT /projectplan/{projectId}/forms/{formSlug}` (`saveForm`)
   - `POST /projectplan/{projectId}/forms/{formSlug}/files` (`uploadFormFile`)
   - `GET /projectplan/{projectId}/files/download-url` (`getFileDownloadUrl`)
   - `GET /projectplan/{projectId}/progress` (`getProgress`)
   - `GET /projectplan/progress` (`getProgressForProjects`)
3. `JwtUtil`
   - Each endpoint extracts the token from `Authentication.credentials`.
   - Both services receive companyId, role, and username. The corporation autofill endpoint receives those three values; the project/form/file/progress endpoints additionally receive their route/query/body inputs.

## HTTP, Validation, and Authorization Notes

- All controller responses go through `respond`, which maps the response `statusCode` with `ResponseUtil.getHttpStatus`.
- GET autofill/form/download/progress routes require `PROJECT_READ` for operators; PUT save and POST upload routes require `PROJECT_UPDATE`. Admins are allowed by each method-security expression.
- The controller does not annotate the `JsonNode` save body with `@Valid`. Whole-page schema/conditional validation and atomic persistence are owned by `ProjectPlanFormService`; controller tests should verify exact JSON delegation and response forwarding, while service tests own validation/transaction guarantees.
- File uploads consume multipart form data. `fieldName` is a request parameter and `file` is a required request part. A successful upload is provisional and returns an `s3Key`; promotion occurs when a valid page is saved.
- Download URL lookup delegates project/key ownership checks to the service; provisional or foreign-project keys should be represented by the service error response and converted by the controller.
- Per-project progress includes required/optional form state and resume metadata. Batch progress accepts a list of project IDs; inaccessible projects are filtered by the service contract rather than failing the entire batch.
- Use a method-security-enabled MockMvc test slice for role/permission checks, and MockMvc multipart requests for upload binding.

## Existing Test Coverage

No ProjectPlanController test suite or legacy controller test was found. The outline therefore starts from all eight controller routes and includes success delegation, response status mapping, access denial, multipart/query binding, and resume/batch progress contracts.
