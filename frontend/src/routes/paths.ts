export const paths = {
  home: '/',
  login: '/login',
  adminLogin: '/admin/login',
  forgotPassword: '/forgot-password',
  signup: '/signup',

  masterDashboard: '/master/dashboard',
  masterSiteRegister: '/master/site-register',
  masterSiteEditRoute: '/master/site-edit/:siteId',
  masterSiteEdit: (siteId: string) => `/master/site-edit/${siteId}`,
  masterUserRegister: '/master/user-register',
  masterUserProfileRoute: '/master/users/:userId/profile',
  masterUserProfile: (userId: string | number) =>
    `/master/users/${userId}/profile`,
  userEditRoute: '/user-edit/:id',
  userEdit: (id: string | number) => `/user-edit/${id}`,

  siteDashboardRoute: '/sites/:siteId/dashboard',
  userDashboard: '/dashboard',
  siteDashboard: (siteId: string) => `/sites/${siteId}/dashboard`,
  siteDetailRoute: '/sites/:siteId',
  siteDetail: (siteId: string) => `/sites/${siteId}`,

  legacyDashboardRoute: '/dashboard/:siteId',
  legacyProjectRoute: '/project/:id',
  projectMembershipRoute: '/projects/:siteId/register',
  projectMembership: (siteId: string) =>
    `/projects/${siteId}/register`,
  projectRegisterRoute: '/projects/:siteId/:projectId/register',
  projectRegister: (
    siteId: string,
    projectId: string | number,
  ) => `/projects/${siteId}/${projectId}/register`,
  projectEquipmentRoute: '/projects/:siteId/:projectId/equipment',
  projectEquipment: (
    siteId: string,
    projectId: string | number,
  ) => `/projects/${siteId}/${projectId}/equipment`,
  projectEquipmentConfirmationRoute:
    '/projects/:siteId/:projectId/equipment/confirmation',
  projectEquipmentConfirmation: (
    siteId: string,
    projectId: string | number,
  ) => `/projects/${siteId}/${projectId}/equipment/confirmation`,
  equipmentAddRoute: '/equipment/add/:type',
  equipmentAdd: (type: string) => `/equipment/add/${type}`,
  siteProjectDetailRoute: '/sites/:siteId/projects/:projectId',
  siteProjectDetail: (
    siteId: string,
    projectId: string | number,
  ) => `/sites/${siteId}/projects/${projectId}`,

  monitoringRegisterRoute: '/monitoring/register/:projectType',
  monitoringRegister: (projectType: string) => `/monitoring/register/${projectType}`,

  profile: '/profile',
  passwordChange: '/password-change',

  adminDashboard: '/admin/dashboard',
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
  adminProjectDetail: (projectId: string | number) => `/admin/projects/${projectId}`,
  adminProjectEditRoute: '/admin/projects/:projectId/edit',
  adminProjectEdit: (projectId: string | number) => `/admin/projects/${projectId}/edit`,
  adminProjectPlanRoute: '/admin/projects/:projectId/project-plan',
  adminProjectPlan: (projectId: string | number) =>
    `/admin/projects/${projectId}/project-plan`,
  adminProjectMonitoringReportRoute:
    '/admin/projects/:projectId/monitoring-report',
  adminProjectMonitoringReport: (projectId: string | number) =>
    `/admin/projects/${projectId}/monitoring-report`,
} as const;
