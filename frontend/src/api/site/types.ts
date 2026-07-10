import type { Project } from "../projects/types";

export interface Site {
  id: string;
  siteName: string;
  postalCode: string;
  prefectureName: string;
  cityName: string;
  blockName: string;
  addressDetail?: string;
  address?: string;
  buildingName?: string;
  picId?: string;
  picFirstName: string;
  picLastName: string;
  picEmail: string;
  picPhoneNumber?: string;
  picPosition: string;
  companyId: string;
  project?: string[];
}

export type CreateSiteData = Omit<
  Site,
  'id' | 'picEmail' | 'picPosition' | 'companyId' | 'project'
>;

export interface SiteAddResponse extends Site {
  statusCode: string;
  site: Site;
}

export type SiteFormData = {
  siteName: string;
  siteNameKana: string;
  postalCode: string;
  prefectureName: string;
  cityName: string;
  townName: string;
  blockName: string;
  addressDetail: string;
  picFirstName: string;
  picFirstNameKana: string;
  picLastName: string;
  picLastNameKana: string;
  picEmail: string;
  picPhoneNumber: string;
  picPosition: string;
  userId?: string;
};

export type SiteDashboardMetrics = {
  sumEmissionReduction?: string | null;
  sumEmission?: string | null;
  jcredit?: string | null;
};

export type SiteDashboardApiResponse = {
  statusCode: string;
  message?: string | null;
  errors?: unknown;
  site?: {
    id: string;
    siteName: string;
    postalCode?: string | null;
    prefecture?: string | null;
    prefName?: string | null;
    city?: string | null;
    cityName?: string | null;
    municipality?: string | null;
    town?: string | null;
    block?: string | null;
    blockName?: string | null;
    blocknum?: string | null;
    address?: string | null;
    buildingName?: string | null;
    picFirstName?: string | null;
    picLastName?: string | null;
    projects?: string[] | null;
    data?: SiteDashboardMetrics | null;
  } | null;
};

export type SiteDashboardData = {
  site: Site | null;
  metrics: SiteDashboardMetrics | null;
};

export type SiteProjectsApiResponse = {
  statusCode: string;
  message?: string | null;
  errors?: Record<string, string> | null;
  projects?: SiteProjectApiItem[] | null;
  pagination?: {
    page: number;
    limit: number;
    totalItems: number;
    totalPages: number;
    nextPageToken?: string | null;
    hasMore: boolean;
  } | null;
};

export type SiteProjectApiItem = {
  id: string;
  projectName: string;
  projectNumber: string;
  projectStatus: string;
  emissionReduction?: string | number | null;
  baselineEmission?: string | number | null;
  postProjectEmission?: string | number | null;
  repFirstName?: string | null;
  repFirstNameKana?: string | null;
  repLastName?: string | null;
  repLastNameKana?: string | null;
  repEmail?: string | null;
  repPhoneNumber?: string | null;
  repPosition?: string | null;
  picFirstName?: string | null;
  picFirstNameKana?: string | null;
  picLastName?: string | null;
  picLastNameKana?: string | null;
  picEmail?: string | null;
  picPhoneNumber?: string | null;
  picPosition?: string | null;
  dataManagementPic?: string | null;
  companyName?: string | null;
  regionalCategory?: string | null;
  newMembersTotal?: number | null;
  withdrawnMembers?: number | null;
  cumulativeMembers?: number | null;
  membersTotal?: number | null;
  methodologyVersion?: string | null;
  methodologyCategory?: string | null;
  methodologyNumber?: string | null;
  methodologyName?: string | null;
  methodologyMainEmission?: string | null;
  methodCombiFeasibility?: boolean | null;
  methodDetails?: string | null;
  operatorCompanyId?: number | null;
  isJoined?: boolean | null;
  updatedAt?: string | null;
  createdAt?: string | null;
};

export type SiteProject = Project & {
  isJoined: boolean;
  projectNumber: string;
  repFirstName?: string;
  repFirstNameKana?: string;
  repLastName?: string;
  repLastNameKana?: string;
  repEmail?: string;
  repPhoneNumber?: string;
  repPosition?: string;
  picFirstName?: string;
  picFirstNameKana?: string;
  picLastName?: string;
  picLastNameKana?: string;
  picEmail?: string;
  picPhoneNumber?: string;
  picPosition?: string;
  dataManagementPic?: string;
  companyName?: string;
  regionalCategory?: string;
  newMembersTotal?: number;
  withdrawnMembers?: number;
  cumulativeMembers?: number;
  membersTotal?: number;
  methodDetails?: string;
  operatorCompanyId?: string | number;
  updatedAt?: string;
  createdAt?: string;
};

export type SiteProjectEquipment = {
  tempId?: string | null;
  equipmentId?: number | null;
  methodologyNumber?: string | null;
  isEligible?: boolean | null;
  equipmentOpDate?: string | null;
  nominalMaxOutput?: number | null;
  pcsManufacturer?: string | null;
  pcsModel?: string | null;
  powerCondUnitCount?: number | null;
  deviceNo?: string | null;
  powerCondStandby?: number | null;
  hasBattery?: boolean | null;
  batteryMaker?: string | null;
  batteryModel?: string | null;
  batteryCapacity?: number | null;
  ictEquipmentPowerConsumption?: number | null;
  additionalEquipment?: boolean | null;
  additionalEquipmentDate?: string | null;
  additionalEquipmentType?: string | null;
  additionalUnitCount?: number | null;
  additionalDeviceUniqueNo?: string | null;
  equipmentType?: string | null;
  unitCount?: number | null;
  deviceUniqueNo?: string | null;
  totalInvestment?: number | null;
  hasSubsidy?: boolean | null;
  subsidyName?: string | null;
  subsidyAmount?: number | null;
  preProjectUnitPrice?: number | null;
};

export type SiteProjectEquipmentResponse = {
  statusCode?: string;
  message?: string | null;
  errors?: unknown;
  equipments?: SiteProjectEquipment[] | null;
};

// For GET /site list response
export interface SiteListItem {
  id: string;
  siteName: string;
  postalCode?: string;
  prefecture?: string;
  prefName?: string;
  municipality?: string;
  cityName?: string;
  townName?: string;
  blocknum?: string;
  blockName?: string;
  address?: string;
  buildingName?: string;
  picFirstName?: string;
  picLastName?: string;
  projects?: string[];
}

export interface SitesResponse {
  statusCode: string;
  message?: string;
  siteList: SiteListItem[];
}
