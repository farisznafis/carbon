export interface Company {
  id: string;
  nameJA: string;
  nameFurigana: string;
  nameEN: string;
  email: string;
  phoneNumber: string;
  corpNumber: string;
  address: string;
  fax?: string;
  repFirstName: string;
  repLastName: string;
  repFirstNameKana: string;
  repLastNameKana: string;
  repEmail: string;
  repPhoneNumber: string;
  repPosition: string;
  applicationDate: string;
}

export interface User {
  id: string;
  email: string;
  name?: string;
  username?: string;
  role?: 'master' | 'user' | 'admin';
  companyId?: string;
  companyName?: string;
  siteId: string;
  siteName?: string;
}

export interface UserProfile extends User {
  firstName: string;
  lastName: string;
  firstNameKana: string;
  lastNameKana: string;
  phoneNumber?: string;
  position: string;
  password?: string;
}

export type UserList = Omit<UserProfile, 'companyId'>;

export interface UserProfileResponse {
  statusCode: string;
  profile: UserProfile;
  company: Company;
}

export interface UserFormData {
  username: string;
  lastName: string;
  firstName: string;
  lastNameKana: string;
  firstNameKana: string;
  phoneNumber?: string;
  userPosition?: string;
  email: string;
  password?: string;
  passwordConfirmation?: string;
  siteId?: string;
}
