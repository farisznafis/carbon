export type CreateSitePayload = {
  siteName: string;
  siteNameKana: string;
  postalCode: string;
  prefecture: string;
  municipality: string;
  blocknum: string;
  buildingName: string;
  userId?: string;
};

export type UpdateSitePayload = {
  siteName: string;
  postalCode: string;
  prefecture: string;
  municipality: string;
  blocknum: string;
  buildingName: string;
  picuserId: string | null;
};
