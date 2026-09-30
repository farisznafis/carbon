# PartiCompanyController API Map

## API-to-Service Map

| # | Method | API | Controller method | Service/dependency | Request/JWT/header notes |
|---:|---|---|---|---|---|
| 1 | PATCH | `/company` | `updateCompany` | `PartiCompanyService.updateCompany` + `JwtUtil.extractCompanyId` | JWT credentials supply `companyId`; `UpdatePartiCompanyRequest` body; `master` role |
| 2 | POST | `/company/signup` | `signup` | `PartiCompanyRegisterService.register` | `PartiCompanyRegisterRequest` body; public; returns HTTP 201 |
| 3 | POST | `/company/signup/email-verification/start` | `startSignupEmailVerification` | `CompanySecurityFlowService.startSignupEmailVerification` | `EmailOtpStartRequest.email`; public |
| 4 | POST | `/company/signup/email-verification/resend` | `resendSignupEmailVerification` | `CompanySecurityFlowService.resendSignupEmailVerification` | `OtpResendRequest.challengeId`; public |
| 5 | POST | `/company/signup/email-verification/verify` | `verifySignupEmail` | `CompanySecurityFlowService.verifySignupEmail` | `OtpVerifyRequest.challengeId` and `code`; public |
| 6 | GET | `/company/signup/revision` | `getRejectedSignupRevision` | `CompanySecurityFlowService.getRevisionDraft` | Required `X-Action-Session` header; public |
| 7 | PUT | `/company/signup/revision` | `reviseRejectedSignup` | `CompanySecurityFlowService.reviseRejectedSignup` | Required `X-Action-Session` header and `PartiCompanyRegisterRequest` body; public |
| 8 | POST | `/company/signup/company-info/validate` | `validateSignupCompanyInfo` | `PartiCompanyRegisterService.validateCompanyInfo` | `PartiCompanyInfoValidateRequest` body; returns validation-success `BaseResponse` |
| 9 | POST | `/company/signup/methodology/validate` | `validateSignupMethodology` | Controller only; no service call | `PartiCompanyMethodologyValidateRequest` body; returns validation-success `BaseResponse` |
| 10 | POST | `/company/{companyId}/deletion-request` | `createDeletionRequest` | `CompanyDeletionService.createDeletionRequest` + `JwtUtil.extractUsername` | Path `companyId`, JWT actor, `CreateDeletionRequest.reason`; `master` role |
| 11 | POST | `/company/{companyId}/deletion-approve` | `approveDeletionRequest` | `CompanyDeletionService.approveDeletionRequest` + `JwtUtil.extractUsername` | Path `companyId`, JWT actor; `admin` role; body is not used by the service call |
| 12 | POST | `/company/{companyId}/deletion-reject` | `rejectDeletionRequest` | `CompanyDeletionService.rejectDeletionRequest` + `JwtUtil.extractUsername` | Path `companyId`, JWT actor, `RejectDeletionRequest.rejectionReason`; `admin` role |

## Service Index

1. `PartiCompanyService`
   - `PATCH /company` (`updateCompany`)
2. `PartiCompanyRegisterService`
   - `POST /company/signup` (`signup`)
   - `POST /company/signup/company-info/validate` (`validateSignupCompanyInfo`)
3. `CompanySecurityFlowService`
   - `POST /company/signup/email-verification/start` (`startSignupEmailVerification`)
   - `POST /company/signup/email-verification/resend` (`resendSignupEmailVerification`)
   - `POST /company/signup/email-verification/verify` (`verifySignupEmail`)
   - `GET /company/signup/revision` (`getRejectedSignupRevision`)
   - `PUT /company/signup/revision` (`reviseRejectedSignup`)
4. `CompanyDeletionService`
   - `POST /company/{companyId}/deletion-request` (`createDeletionRequest`)
   - `POST /company/{companyId}/deletion-approve` (`approveDeletionRequest`)
   - `POST /company/{companyId}/deletion-reject` (`rejectDeletionRequest`)
5. Controller-only operation
   - `POST /company/signup/methodology/validate` (`validateSignupMethodology`)

## HTTP and Validation Notes

- `updateCompany`, the three deletion endpoints, and their status-code conversion use `ResponseUtil.getHttpStatus`.
- `signup` returns HTTP 201. Email-verification and revision endpoints return HTTP 200 with the delegated response body.
- Both validation endpoints return HTTP 200 with `BaseResponse.success(MSG_VALIDATION_SUCCESS)` when valid. `@Valid` failures should be tested through MockMvc so MVC validation is exercised.
- `CreateDeletionRequest` currently has no Bean Validation constraints; `ApproveDeletionRequest` is empty. `RejectDeletionRequest.rejectionReason` is `@NotBlank`.
- `master` authorization applies to company update and deletion-request creation; `admin` authorization applies to deletion approval and rejection.

## Legacy Test Coverage and Gaps

The existing root-level `PartiCompanyControllerTest.java` covers company update, signup, email-verification start/resend/verify, revision submission, both validation endpoints, and deletion request/approve/reject success or selected error results. The dedicated outline adds the missing revision-draft GET case, request-validation cases, explicit JWT/header forwarding assertions, and HTTP status conversion cases for deletion responses.
