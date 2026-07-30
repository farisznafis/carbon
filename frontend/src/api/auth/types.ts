export interface LoginResponse {
  statusCode: string;
  role: 'master' | 'user' | 'admin';
  token: string;
  id?: string | number | null;
  userId?: string | number | null;
  user?: {
    id?: string | number | null;
    userId?: string | number | null;
    email?: string | null;
    username?: string | null;
  } | null;
  companyName: string;
  siteId?: string | number | null;
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
  userId: string;
  currentPassword: string;
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

export type PasswordResetCompleteResponse = {
  statusCode: string;
  message?: string | null;
  timestamp?: string;
};
