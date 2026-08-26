export type AdminOperatorPermissionCode =
  | 'PROJECT_CREATE'
  | 'PROJECT_READ'
  | 'PROJECT_UPDATE'
  | 'PROJECT_STATUS_UPDATE'
  | 'PROJECT_APPLICATION_OUTPUT'
  | 'PROJECT_VIEW'
  | 'PARTICIPATING_COMPANY_READ'
  | 'PARTICIPATING_COMPANY_APPLICATION_REVIEW'
  | 'PARTICIPATING_COMPANY_SUSPEND'
  | 'MASTER_USER_PASSWORD_RESET'
  | 'MONITORING_READ'
  | 'MONITORING_DELETE'
  | 'MONITORING_CANCEL'
  | 'MONITORING_BATCH_HISTORY_READ'
  | 'MONITORING_BATCH_ERROR_READ'
  | 'OPERATOR_USER_READ'
  | 'OPERATOR_USER_CREATE'
  | 'OPERATOR_USER_UPDATE'
  | 'METHODOLOGY_READ'
  | 'APPLICATION_FORM_READ';

export type AdminOperatorApiItem = {
  id?: string | number | null;
  username?: string | null;
  name?: string | null;
  role?: string | null;
  email?: string | null;
  firstName?: string | null;
  firstNameKana?: string | null;
  lastName?: string | null;
  lastNameKana?: string | null;
  phoneNumber?: string | null;
  userPosition?: string | null;
  siteId?: string | number | null;
  siteName?: string | null;
  companyId?: string | null;
  permissions?: string[] | null;
};

export type AdminOperator = {
  id: string;
  username: string;
  name: string;
  role: string;
  email: string;
  firstName: string;
  firstNameKana: string;
  lastName: string;
  lastNameKana: string;
  phoneNumber: string;
  userPosition: string;
  siteId: string | null;
  siteName: string;
  companyId: string;
  permissions: string[];
};

export type AdminOperatorListApiResponse = {
  statusCode?: string;
  message?: string;
  users?: AdminOperatorApiItem[] | null;
};

export type AdminOperatorListResponse = {
  statusCode: string;
  message: string;
  users: AdminOperator[];
};

export type AdminOperatorProfileApiResponse = {
  statusCode?: string;
  message?: string;
  profile?: AdminOperatorApiItem | null;
};

export type CreateAdminOperatorPayload = {
  username: string;
  firstName: string;
  firstNameKana: string;
  lastName: string;
  lastNameKana: string;
  email: string;
  phoneNumber: string;
  userPosition: string;
  password: string;
  role: 'operator';
  permissionCodes: string[];
  siteId?: string | number;
};

export type CreateAdminOperatorResponse = {
  statusCode?: string;
  message?: string;
  user?: AdminOperatorApiItem | null;
};
