export const paths = {
  home: '/',
  login: '/login',
  adminLogin: '/admin/login',
  forgotPassword: '/forgot-password',
  signup: '/signup',

  masterDashboard: '/master/dashboard',
  masterSiteRegister: '/master/site-register',
  masterSiteEditRoute: '/master/site-edit/:id',
  masterSiteEdit: (id: string | number) => `/master/site-edit/${id}`,
  masterUserRegister: '/master/user-register',
  masterUserProfileRoute: '/master/users/:userId/profile',
  masterUserProfile: (userId: string | number) =>
    `/master/users/${userId}/profile`,
  userEditRoute: '/user-edit/:id',
  userEdit: (id: string | number) => `/user-edit/${id}`,

  siteDashboardRoute: '/sites/:siteId/dashboard',
  userDashboard: '/dashboard',
  siteDashboard: (siteId: string | number) => `/sites/${siteId}/dashboard`,
  siteDetailRoute: '/sites/:id',
  siteDetail: (id: string | number) => `/sites/${id}`,

  legacyDashboardRoute: '/dashboard/:siteId',
  legacyProjectRoute: '/project/:id',
  projectRegisterRoute: '/projects/:id/register',
  projectRegister: (id: string | number) => `/projects/${id}/register`,
  projectEquipmentRoute: '/projects/:id/equipment',
  projectEquipment: (id: string | number) => `/projects/${id}/equipment`,
  equipmentAddRoute: '/equipment/add/:type',
  equipmentAdd: (type: string) => `/equipment/add/${type}`,
  siteProjectDetailRoute: '/sites/:siteId/projects/:projectId',
  siteProjectDetail: (
    siteId: string | number,
    projectId: string | number,
  ) => `/sites/${siteId}/projects/${projectId}`,

  monitoringRegisterRoute: '/monitoring/register/:projectType',
  monitoringRegister: (projectType: string) => `/monitoring/register/${projectType}`,

  profile: '/profile',
  passwordChange: '/password-change',

  adminDashboard: '/admin/dashboard',
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
