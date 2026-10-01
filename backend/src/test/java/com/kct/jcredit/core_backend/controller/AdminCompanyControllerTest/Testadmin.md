1	GET	/admin/companies	getCompanies	adminCompanyQueryService
2	POST	/admin/companies/{companyId}/approve	approveCompany	companyApprovalService
3	POST	/admin/companies/{companyId}/reject	rejectCompany	companyApprovalService
4	POST	/admin/companies/{companyId}/master-setup-link/resend	resendMasterSetupLink	companyApprovalService
5	POST	/admin/companies/{companyId}/password-reset-link	requestMasterPasswordReset	securityAccountService（+ jwtUtil）
6	GET	/admin/companies/{companyId}	getCompanyDetail	adminCompanyQueryService
7	POST	/admin/companies/{companyId}/suspensions	createSuspension	companyLifecycleService
8	GET	/admin/companies/{companyId}/suspensions	getSuspensions	companyLifecycleService
9	POST	/admin/companies/{companyId}/suspensions/{suspensionId}/resolve	resolveSuspension	companyLifecycleService
10	GET	/admin/companies/participate/project/{projectId}	getParticipationsByProject	reviewService
11	GET	/admin/companies/participate/pending	getPendingReviews	reviewService
12	GET	/admin/companies/participate/open	getPendingReviewsForMe	reviewService（+ jwtUtil）
13	GET	/admin/companies/participations/approved	getApprovedParticipations	reviewService
14	GET	/admin/companies/participations/rejected	getRejectedParticipations	reviewService
15	GET	/admin/companies/participate/{participationId}	getReviewDetail	reviewService
16	POST	/admin/companies/participation/{participationId}/approve	approveReview	reviewService（+ jwtUtil）
17	POST	/admin/companies/participation/{participationId}/reject	rejectReview	reviewService（+ jwtUtil）
18	GET	/admin/companies/operator-user	getOperatorUsers	operatorUserService（+ jwtUtil）
19	POST	/admin/companies/operator-user	createUser	operatorUserService（+ jwtUtil）
20	PATCH	/admin/companies/operator-user/{userId}	updateOperatorUser	operatorUserService（+ jwtUtil）
21	GET	/admin/companies/operator-user/{userId}	getOperatorUserProfile	（Service呼び出しなし、jwtUtilのみで権限チェック→未取得コード）


Service別 API一覧
1. AdminCompanyQueryService
GET /admin/companies（getCompanies）
GET /admin/companies/{companyId}（getCompanyDetail）
2. CompanyApprovalService
POST /admin/companies/{companyId}/approve（approveCompany）
POST /admin/companies/{companyId}/reject（rejectCompany）
POST /admin/companies/{companyId}/master-setup-link/resend（resendMasterSetupLink）
3. SecurityAccountService
POST /admin/companies/{companyId}/password-reset-link（requestMasterPasswordReset）
4. CompanyLifecycleService
POST /admin/companies/{companyId}/suspensions（createSuspension）
GET /admin/companies/{companyId}/suspensions（getSuspensions）
POST /admin/companies/{companyId}/suspensions/{suspensionId}/resolve（resolveSuspension）
5. ParticipationReviewService
GET /admin/companies/participate/project/{projectId}（getParticipationsByProject）
GET /admin/companies/participate/pending（getPendingReviews）
GET /admin/companies/participate/open（getPendingReviewsForMe）
GET /admin/companies/participations/approved（getApprovedParticipations）
GET /admin/companies/participations/rejected（getRejectedParticipations）
GET /admin/companies/participate/{participationId}（getReviewDetail）
POST /admin/companies/participation/{participationId}/approve（approveReview）
POST /admin/companies/participation/{participationId}/reject（rejectReview）
6. OperatorUserService
GET /admin/companies/operator-user（getOperatorUsers）
POST /admin/companies/operator-user（createUser）
PATCH /admin/companies/operator-user/{userId}（updateOperatorUser）
GET /admin/companies/operator-user/{userId}（getOperatorUserProfile）※未確定