export type CreateUserInvitationPayload = {
  email: string;
  siteId: string | null;
};

export type CreateUserInvitationResponse = {
  statusCode: string;
  message?: string | null;
  timestamp?: string;
};

export type UserInvitationContext = {
  email: string;
  companyName: string;
  siteId: string | null;
  siteName: string | null;
};

export type CompleteUserInvitationPayload = {
  username: string;
  firstName: string;
  firstNameKana: string;
  lastName: string;
  lastNameKana: string;
  phoneNumber: string;
  userPosition: string;
  password: string;
};

export type CompleteUserInvitationResponse = {
  statusCode: string;
  message?: string | null;
  timestamp?: string;
};
