export interface LoginResponse {
  statusCode: string;
  role: 'master' | 'user' | 'admin';
  token: string;
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
  town: string;
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
