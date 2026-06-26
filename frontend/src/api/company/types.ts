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
};

export type ValidateSignupCompanyInfoResponse = {
  statusCode?: string;
  message?: string;
  errors?: Record<string, string>;
  success?: boolean;
  valid?: boolean;
};
