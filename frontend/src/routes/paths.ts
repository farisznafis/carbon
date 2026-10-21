export const paths = {
  home: '/',
  contact: '/contact',
  login: '/login',
  adminLogin: '/admin/login',
  forgotPassword: '/forgot-password',
  resetPassword: '/reset-password',
  signup: '/signup',
  signupRevision: '/signup/revision',
  userInvitation: '/user/invitation',
  /** Emailed to the master after approval when FEATURE_MASTER_SETUP_LINK is on; the token rides in the fragment. */
  masterAccountSetup: '/account/setup',

  masterDashboard: '/master/dashboard',
  masterDashboardMembership: '/master/dashboard?tab=membership',
  masterSiteRegister: '/master/site-register',
  masterSiteEditRoute: '/master/site-edit/:siteId',
  masterSiteEdit: (siteId: string) => `/master/site-edit/${siteId}`,
  masterUserProfileRoute: '/master/users/:userId/profile',
  masterUserProfile: (userId: string | number) =>
    `/master/users/${userId}/profile`,
  userEditRoute: '/user-edit/:id',
  userEdit: (id: string | number) => `/user-edit/${id}`,

  siteDashboardRoute: '/sites/:siteId/dashboard',
  userDashboard: '/dashboard',
  siteDashboard: (siteId: string) => `/sites/${siteId}/dashboard`,
  siteDetailRoute: '/sites/:siteId',

  legacyDashboardRoute: '/dashboard/:siteId',
  legacyProjectRoute: '/project/:id',
  projectMembershipRoute: '/projects/:siteId/register',
  projectMembership: (siteId: string) =>
    `/projects/${siteId}/register`,
  projectRegisterRoute: '/projects/:siteId/:projectId/register',
  projectRegister: (
    siteId: string,
    projectId: string,
  ) => `/projects/${siteId}/${projectId}/register`,
  projectEquipmentRoute: '/projects/:siteId/:projectId/equipment',
  projectEquipment: (
    siteId: string,
    projectId: string,
  ) => `/projects/${siteId}/${projectId}/equipment`,
  projectEquipmentConfirmationRoute:
    '/projects/:siteId/:projectId/equipment/confirmation',
  projectEquipmentConfirmation: (
    siteId: string,
    projectId: string,
  ) => `/projects/${siteId}/${projectId}/equipment/confirmation`,
  siteProjectDetailRoute: '/sites/:siteId/projects/:projectId',
  siteProjectDetail: (
    siteId: string,
    projectId: string,
  ) => `/sites/${siteId}/projects/${projectId}`,
  siteProjectEquipmentEditRoute:
    '/sites/:siteId/projects/:projectId/equipment/:equipmentId/edit',
  siteProjectEquipmentEdit: (
    siteId: string,
    projectId: string,
    equipmentId: string | number,
  ) =>
    `/sites/${encodeURIComponent(siteId)}/projects/${encodeURIComponent(projectId)}/equipment/${encodeURIComponent(String(equipmentId))}/edit`,

  monitoringRegisterRoute: '/monitoring/register/:projectType',
  monitoringRegister: (projectType: string) => `/monitoring/register/${projectType}`,

  profile: '/profile',
  passwordChange: '/password-change',
  passwordChangeForUser: (userId: string) =>
    `/password-change?userId=${encodeURIComponent(userId)}`,

  adminDashboard: '/admin/dashboard',
  adminProjectList: '/admin/projects',
  adminCompanyList: '/admin/companies',
  adminOperatorList: '/admin/operators',
  adminOperatorRegister: '/admin/operators/new',
  adminOperatorProfileRoute: '/admin/operators/:userId',
  adminOperatorProfile: (userId: string) =>
    `/admin/operators/${encodeURIComponent(userId)}`,
  adminOperatorEditRoute: '/admin/operators/:userId/edit',
  adminOperatorEdit: (userId: string) =>
    `/admin/operators/${encodeURIComponent(userId)}/edit`,
  adminOperatorPermissionEdit: (userId: string) =>
    `/admin/operators/${encodeURIComponent(userId)}/edit#permissions`,
  adminMethodologyList: '/admin/methodologies',
  adminMonitoring: '/admin/monitoring',
  adminCompanyDetailRoute: '/admin/companies/:companyId',
  adminCompanyDetail: (companyId: string | number) =>
    `/admin/companies/${companyId}`,
  adminApplicationDetailRoute: '/admin/applications/:applicationId',
  adminApplicationDetail: (applicationId: string | number) =>
    `/admin/applications/${applicationId}`,
  adminCancelRequestDetailRoute:
    '/admin/monitoring/cancel-requests/:cancelRequestId',
  adminCancelRequestDetail: (cancelRequestId: string | number) =>
    `/admin/monitoring/cancel-requests/${cancelRequestId}`,
  adminProjectRegister: '/admin/project-register',
  adminProjectDetailRoute: '/admin/projects/:projectId',
  adminProjectDetail: (projectId: string) => `/admin/projects/${projectId}`,
  /** Opens one tab of the project detail page, carrying any extra query state for that tab. */
  adminProjectDetailTab: (
    projectId: string,
    tab: 'overview' | 'monitoring' | 'membership' | 'companies',
    params?: Record<string, string>,
  ) =>
    `/admin/projects/${projectId}?${new URLSearchParams({ tab, ...params }).toString()}`,
  adminProjectInformationRoute: '/admin/projects/:projectId/information',
  adminProjectInformation: (projectId: string) =>
    `/admin/projects/${encodeURIComponent(projectId)}/information`,
  /** Legacy list URL; it now redirects to the project detail 入会届 tab. */
  adminProjectMembershipRoute: '/admin/projects/:projectId/memberships',
  /** The project's membership list, which lives in the project detail 入会届 tab. */
  adminProjectMembership: (projectId: string, status?: string) =>
    `/admin/projects/${projectId}?${new URLSearchParams({
      tab: 'membership',
      ...(status ? { status } : {}),
    }).toString()}`,
  adminProjectMembershipDetailRoute:
    '/admin/projects/:projectId/memberships/:participationId',
  adminProjectMembershipDetail: (
    projectId: string,
    participationId: string | number,
    status?: string,
  ) =>
    `/admin/projects/${projectId}/memberships/${participationId}${status ? `?status=${encodeURIComponent(status)}` : ''}`,
  adminProjectMonitoringDetailRoute:
    '/admin/projects/:projectId/monitoring/:monitoringId',
  adminProjectMonitoringDetail: (
    projectId: string,
    monitoringId: string | number,
  ) => `/admin/projects/${projectId}/monitoring/${monitoringId}`,
  adminProjectEditRoute: '/admin/projects/:projectId/edit',
  adminProjectEdit: (projectId: string) => `/admin/projects/${projectId}/edit`,
  adminProjectMonitoringReportRoute:
    '/admin/projects/:projectId/monitoring-report',
  adminProjectMonitoringReport: (projectId: string) =>
    `/admin/projects/${projectId}/monitoring-report`,

  // Admin Help & FAQ
  adminHelpFaq: '/admin/help-faq',
} as const;

const normalizePathname = (pathname: string) =>
  pathname.length > 1 ? pathname.replace(/\/+$/, '') : pathname;

export const isLoginPath = (pathname: string) => {
  const normalizedPathname = normalizePathname(pathname);

  return (
    normalizedPathname === paths.login ||
    normalizedPathname === paths.adminLogin
  );
};

export const isAuthPagePath = (pathname: string) => {
  const normalizedPathname = normalizePathname(pathname);

  return (
    isLoginPath(normalizedPathname) ||
    normalizedPathname === paths.forgotPassword
  );
};
