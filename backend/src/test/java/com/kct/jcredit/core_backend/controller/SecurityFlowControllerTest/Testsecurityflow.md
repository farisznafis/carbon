# SecurityFlowController API Map

## API-to-Service Map

| # | Method | API | Controller method | Service call | Input/identity notes | Controller HTTP behavior |
|---:|---|---|---|---|---|---|
| 1 | POST | `/auth/action-links/exchange` | `exchangeActionLink` | `SecurityAccountService.exchangeActionLink` | `ActionLinkExchangeRequest.token`; public | Always returns HTTP 200 for a normal service response |
| 2 | POST | `/auth/master-account/setup` | `setupMasterAccount` | `SecurityAccountService.setupMasterAccount` | `X-Action-Session`, `username`, `password`; public | Returns HTTP 200 for a normal service response |
| 3 | POST | `/auth/password-reset/request` | `requestPasswordReset` | `SecurityAccountService.requestPasswordReset` | `ForgotPasswordRequest`; public | Returns HTTP 200 for a normal service response; service uses a generic response to avoid account enumeration |
| 4 | POST | `/auth/password-reset/complete` | `completePasswordReset` | `SecurityAccountService.completePasswordReset` | `X-Action-Session`, `newPassword`; public | Maps response `statusCode` through `ResponseUtil.getHttpStatus` |
| 5 | POST | `/auth/user/login/email-otp/verify` | `verifyParticipatingLogin` | `SecurityAccountService.verifyParticipatingLogin` | `OtpVerifyRequest.challengeId` and `code`; public | Returns HTTP 200 for a normal service response; typed `LoginResponse` |
| 6 | POST | `/auth/admin/login/email-otp/verify` | `verifyOperatingLogin` | `SecurityAccountService.verifyOperatingLogin` | `OtpVerifyRequest.challengeId` and `code`; public | Returns HTTP 200 for a normal service response; typed `AdminLoginResponse` |
| 7 | POST | `/auth/user/login/email-otp/resend` | `resendParticipatingLogin` | `SecurityAccountService.resendLoginOtp` | `OtpResendRequest.challengeId`, fixed purpose `PARTICIPATING_LOGIN`; public | Returns HTTP 200 for a normal service response |
| 8 | POST | `/auth/admin/login/email-otp/resend` | `resendOperatingLogin` | `SecurityAccountService.resendLoginOtp` | `OtpResendRequest.challengeId`, fixed purpose `OPERATING_LOGIN`; public | Returns HTTP 200 for a normal service response |
| 9 | POST | `/auth/email-otp/settings/challenge` | `startEmailOtpSetting` | `SecurityAccountService.startEmailOtpSetting` + `JwtUtil.extractUserId` + `JwtUtil.extractRole` | Authenticated JWT plus `enabled` and `currentPassword` | Requires authentication; returns HTTP 200 for a normal service response |
| 10 | POST | `/auth/email-otp/settings/confirm` | `confirmEmailOtpSetting` | `SecurityAccountService.confirmEmailOtpSetting` + `JwtUtil.extractUserId` + `JwtUtil.extractRole` | Authenticated JWT plus `challengeId` and `code` | Requires authentication; returns HTTP 200 for a normal service response |

## Service Index

1. `SecurityAccountService`
   - `exchangeActionLink`
   - `setupMasterAccount`
   - `requestPasswordReset`
   - `completePasswordReset`
   - `verifyParticipatingLogin`
   - `verifyOperatingLogin`
   - `resendLoginOtp` for participating and operating purposes
   - `startEmailOtpSetting`
   - `confirmEmailOtpSetting`
2. `JwtUtil`
   - Used only for the authenticated email-OTP settings routes to extract user ID and role.

## Validation, Authentication, and Status Notes

- `ActionLinkExchangeRequest.token` is required and nonblank.
- `MasterAccountSetupRequest.username` and `password` are validated; username has length and custom-format constraints, while password uses the security-password validator.
- `ForgotPasswordRequest.email` is required and must be an email address.
- `CompletePasswordResetRequest.newPassword` is required and uses the security-password validator.
- `OtpVerifyRequest` requires a UUID challenge ID and a six-digit numeric code. `OtpResendRequest` requires a challenge ID.
- `EmailOtpSettingChallengeRequest.enabled` must be nonnull and `currentPassword` nonblank.
- `completePasswordReset` is the only endpoint that converts response status codes with `ResponseUtil`; other endpoints return HTTP 200 for normal service responses. Exceptions should be checked against the configured global exception handler.
- The email-OTP settings challenge and confirmation endpoints require an authenticated principal. Test unauthenticated 401 behavior and verify JWT user ID/role forwarding through MockMvc with Spring Security enabled.
- Use MockMvc for `@Valid` and required `X-Action-Session` header behavior. Direct controller calls do not invoke MVC validation or authorization.
