export type CreateSitePayload = {
  siteName: string;
  siteNameKana: string;
  postalCode: string;
  prefName: string;
  cityName: string;
  townName: string;
  blockName: string;
  address: string;
  picFirstName: string;
  picFirstNameKana: string;
  picLastName: string;
  picLastNameKana: string;
  picEmail: string;
  picPhoneNumber: string;
  picPosition: string;
};

export type UpdateSitePayload = {
  postalCode: string;
  prefName: string;
  cityName: string;
  townName: string;
  blockName: string;
  address: string;
};