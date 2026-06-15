import type { User } from '../users/types';

export interface LoginResponse {
  statusCode: string;
  token: string;
  user: User;
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
  city: string;
  address: string;
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
