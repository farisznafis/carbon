export type CreateSitePayload = {
  siteName: string;
  siteNameKana: string;
  postalCode: string;
  prefecture: string;
  municipality: string;
  blocknum: string;
  buildingName: string;
  userId?: number;
};

export type UpdateSitePayload = {
  postalCode: string;
  prefName: string;
  cityName: string;
  townName: string;
  blockName: string;
  address: string;
};
