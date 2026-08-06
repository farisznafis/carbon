import type { Company } from '../users';

export type { Company } from '../users';

export type UpdateCompanyResponse = {
  statusCode: string;
  company: Company;
};

export type ValidateSignupCompanyInfoPayload = {
  corporateNumber: string;
  corporateName: string;
  corporateNameKana: string;
  companyNameEnglish: string;
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
};

export type ValidateSignupCompanyInfoResponse = {
  statusCode?: string;
  message?: string;
  errors?: Record<string, string>;
  success?: boolean;
  valid?: boolean;
};

export type ValidateSignupMethodologyPayload = {
  methodology: string[];
  activityInformation: string;
};

export type ValidateSignupMethodologyResponse = {
  statusCode?: string;
  message?: string;
  errors?: Record<string, string>;
  success?: boolean;
  valid?: boolean;
};

export type SignupRevisionRequest = {
  emailVerificationChallengeId?: string;
  corporateNumber: string;
  corporateName: string;
  corporateNameKana: string;
  companyNameEnglish: string;
  corporatePhoneNumber: string;
  postalCode: string;
  prefecture: string;
  municipality: string;
  blocknum: string;
  buildingName: string;
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
};

export type SignupRevisionResponse = SignupRevisionRequest & {
  rejectionReason?: string | null;
};

export type SignupRevisionUpdateResponse = {
  statusCode?: string;
  message?: string | null;
  errors?: Record<string, string> | null;
  timestamp?: string;
};
