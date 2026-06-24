export type ProfileApiErrorMap = Record<string, string> | null;

export interface ProfileApiCompany {
  phoneNumber: string | null;
  nameJA: string | null;
  nameFurigana: string | null;
  nameEN: string | null;
  address: string | null;
  corpNumber: string | null;
  fax: string | null;
  email: string | null;
}

export interface ProfileApiProfile {
  companyId: number | null;
  username: string | null;
  firstName: string | null; lastName: string | null;
  firstNameKana: string | null;
  lastNameKana: string | null;
  email: string | null;
  userPosition: string | null;
  phoneNumber: string | null;
  company: ProfileApiCompany | null;
}

export interface ProfileApiResponse {
  statusCode: string;
  message: string | null;
  errors: ProfileApiErrorMap;
  timestamp: string;
  profile: ProfileApiProfile;
}
