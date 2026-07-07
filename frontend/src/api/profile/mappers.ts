import type { ProfileApiProfile, ProfileApiResponse } from './types';

export type ProfileCompanyViewModel = {
  phoneNumber: string;
  nameJA: string;
  nameFurigana: string;
  nameEN: string;
  address: string;
  corpNumber: string;
  fax: string | null;
  email: string;
};

export type ProfileViewModel = {
  companyId?: number;
  username?: string;
  role?: string;
  siteId?: string;
  siteName?: string;
  firstName: string;
  lastName: string;
  firstNameKana: string;
  lastNameKana: string;
  email: string;
  userPosition: string;
  phoneNumber: string;
  company: ProfileCompanyViewModel | null;
};

function normalizeText(value: string | null | undefined): string {
  return value?.trim() || '-';
}

function mapCompany(profile: ProfileApiProfile): ProfileCompanyViewModel | null {
  if (!profile.company) {
    return null;
  }

  return {
    phoneNumber: normalizeText(profile.company.phoneNumber),
    nameJA: normalizeText(profile.company.nameJA),
    nameFurigana: normalizeText(profile.company.nameFurigana),
    nameEN: normalizeText(profile.company.nameEN),
    address: normalizeText(profile.company.address),
    corpNumber: normalizeText(profile.company.corpNumber),
    fax: profile.company.fax?.trim() || null,
    email: normalizeText(profile.company.email),
    // username: normalizeText(profile.company.username),
  };
}

export function mapProfileApiResponseToProfileViewModel(
  response: ProfileApiResponse,
): ProfileViewModel {
  const profile = response.profile;

  return {
    companyId: profile.companyId,
    username: normalizeText(profile.username),
    role: profile.role?.trim() || undefined,
    siteId: profile.siteId != null ? String(profile.siteId) : '',
    siteName: profile.siteName?.trim() || undefined,
    firstName: normalizeText(profile.firstName),
    lastName: normalizeText(profile.lastName),
    firstNameKana: normalizeText(profile.firstNameKana),
    lastNameKana: normalizeText(profile.lastNameKana),
    email: normalizeText(profile.email),
    userPosition: normalizeText(profile.userPosition),
    phoneNumber: normalizeText(profile.phoneNumber),
    company: mapCompany(profile),
  };
}
