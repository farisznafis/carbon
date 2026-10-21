export interface LoginResponse {
  statusCode: string;
  role: 'master' | 'user' | 'admin';
  token: string;
  /** Participating user public ID ("USR-…"). */
  userId?: string | null;
  user?: {
    email?: string | null;
    username?: string | null;
  } | null;
  admin?: {
    /** Admin or operator public ID ("ADM-…" or "OPR-…"). */
    id?: string | null;
    email?: string | null;
    username?: string | null;
    name?: string | null;
    role?: string | null;
    companyId?: string | null;
    siteId?: string | null;
    siteName?: string | null;
  } | null;
  companyName: string;
  siteId?: string | null;
}

export interface SignUpData {
  corporateNumber: string;
  corporateName: string;
  corporateNameKana: string;
  companyNameEnglish: string;
  corporateEmail: string;
  corporatePhoneNumber: string;
  postalCode: string;
  prefecture: string;
  municipality: string;
  buildingName: string;
  blocknum: string;
  lastName: string;
  firstName: string;
  lastNameKana: string;
  firstNameKana: string;
  position: string;
  phoneNumber: string;
  faxNumber: string;
  email: string;
  methodology: string[];
  activityInformation: string;
}

export interface SignUpResponse {
  success: boolean;
  message: string;
  userId: string;
}

export type ChangePasswordPayload = {
  /** Only set when a master changes an employee's password; null for one's own. */
  userId: string | null;
  currentPassword: string | null;
  newPassword: string;
};

export type ChangePasswordResponse = {
  statusCode: string;
  message?: string | null;
};

export type PasswordResetRequestResponse = {
  statusCode: string;
  message?: string | null;
  timestamp?: string;
};

export type ActionLinkExchangeResponse = {
  statusCode: string;
  message?: string | null;
  actionSessionToken: string;
  purpose: 'PASSWORD_RESET' | string;
  expiresInSeconds: number;
};

export type MasterAccountSetupPayload = {
  username: string;
  password: string;
};

export type MasterAccountSetupResponse = {
  statusCode: string;
  message?: string | null;
};

export type PasswordResetCompleteResponse = {
  statusCode: string;
  message?: string | null;
  timestamp?: string;
};
