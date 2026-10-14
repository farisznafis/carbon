import type { Project } from "../projects/types";

export interface Site {
  siteId: string;
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
  'siteId' | 'picEmail' | 'picPosition' | 'companyId' | 'project'
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

export type SiteDashboardApiData = SiteDashboardMetrics & {
  siteId?: string | null;
  siteName?: string | null;
  postalCode?: string | null;
  prefecture?: string | null;
  municipality?: string | null;
  blocknum?: string | null;
  buildingName?: string | null;
  picFirstName?: string | null;
  picLastName?: string | null;
  projects?: string[] | null;
};

export type SiteDashboardApiResponse = {
  statusCode: string;
  message?: string | null;
  errors?: unknown;
  site?: {
    siteId: string;
    /** @deprecated Use siteId. */
    id?: string;
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
    data?: SiteDashboardApiData | null;
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

export type SiteProjectParticipationStatus =
  | 'OPEN'
  | 'APPROVED'
  | 'REJECTED';

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
  participationId?: number | null;
  participationStatus?: SiteProjectParticipationStatus | null;
  pdfStatus?: string | null;
  isCompanyJoined?: boolean | null;
  hasSiteRelation?: boolean | null;
  startedAt?: string | null;
  updatedAt?: string | null;
  createdAt?: string | null;
};

export type MyProjectParticipationApiItem = {
  participationId: number;
  siteId: string;
  siteName: string;
  status: string;
  pdfStatus: string;
};

export type MyProjectParticipation = MyProjectParticipationApiItem;

export type SiteProject = Project & {
  participationId?: number;
  participationStatus?: SiteProjectParticipationStatus;
  pdfStatus?: string;
  isCompanyJoined: boolean;
  hasSiteRelation: boolean;
  projectNumber: string;
  startedAt?: string;
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
  siteId?: string | null;
  methodologyNumber?: string | null;
  isEligible?: boolean | null;
  equipmentOpDate?: string | null;
  nominalMaxOutput?: number | null;
  pcsManufacturer?: string | null;
  pcsModel?: string | null;
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
  changeDate?: string | null;
  previousEquipmentId?: number | null;
  evidenceDocuments?: string[] | null;
  files?: SiteProjectEquipmentFile[] | null;
};

export type SiteProjectEquipmentFile = {
  id?: number | null;
  fileType?: string | null;
  fileId?: string | null;
  s3Key?: string | null;
  fileName?: string | null;
};

export type SiteProjectEquipmentApiFile = SiteProjectEquipmentFile & {
  FileType?: string | null;
  file_type?: string | null;
  FileId?: string | number | null;
  file_id?: string | number | null;
  S3Key?: string | null;
  s3key?: string | null;
  s3_key?: string | null;
  FileName?: string | null;
  originalName?: string | null;
};

export type SiteProjectEquipmentApiItem = Omit<
  SiteProjectEquipment,
  'equipmentId' | 'powerCondStandby' | 'files'
> & {
  id?: number | null;
  equipmentId?: number | null;
  pcsStandby?: number | null;
  powerCondStandby?: number | null;
  files?: SiteProjectEquipmentApiFile[] | null;
  Files?: SiteProjectEquipmentApiFile[] | null;
};

export type SiteProjectEquipmentResponse = {
  statusCode?: string;
  message?: string | null;
  errors?: unknown;
  equipments?: SiteProjectEquipmentApiItem[] | null;
};

export type SiteEquipmentListResponse =
  | SiteProjectEquipmentApiItem[]
  | {
      statusCode?: string;
      message?: string | null;
      errors?: unknown;
      equipments?: SiteProjectEquipmentApiItem[] | null;
    };

export type SiteProjectEquipmentDetailResponse = SiteProjectEquipmentApiItem & {
  statusCode?: string;
  message?: string | null;
  errors?: Record<string, string> | null;
  timestamp?: string;
  siteId?: string | null;
};

export type SiteEquipmentHistoryApiItem = {
  id?: number | null;
  changeDate?: string | null;
  previousEquipmentId?: number | null;
  monitoringStartDate?: string | null;
  monitoringEndDate?: string | null;
  isActive?: boolean | null;
  registeredBy?: string | null;
  registeredAt?: string | null;
  equipment?: SiteProjectEquipmentApiItem | null;
};

export type SiteEquipmentHistory = {
  id: number | null;
  changeDate: string | null;
  previousEquipmentId: number | null;
  monitoringStartDate: string | null;
  monitoringEndDate: string | null;
  isActive: boolean | null;
  registeredBy: string | null;
  registeredAt: string | null;
  equipment: SiteProjectEquipment;
};

/** Complete replacement snapshot accepted by POST /sites/{siteId}/equipment/{equipmentId}/change. */
export type SiteProjectEquipmentChangeRequest = {
  changeDate: string;
  nominalMaxOutput: number | null;
  pcsManufacturer: string;
  pcsModel: string;
  deviceNo: string;
  pcsStandby: number | null;
  hasBattery: boolean | null;
  batteryMaker: string | null;
  batteryModel: string | null;
  batteryCapacity: number | null;
  totalInvestment: number | null;
  hasSubsidy: boolean | null;
  subsidyName: string | null;
  subsidyAmount: number | null;
  preProjectUnitPrice: number | null;
  files: SiteProjectEquipmentValidationFile[];
};

export type SiteProjectEquipmentFileType =
  | 'solar_contract'
  | 'panel_catalog'
  | 'pcs_spec'
  | 'battery_catalog'
  | 'construction_invoice'
  | 'subsidy_notice'
  | 'electricity_bill';

export type SiteProjectEquipmentValidationFile = {
  fileType: SiteProjectEquipmentFileType;
  s3Key: string;
  fileName: string;
  fileId: string;
};

export type SiteProjectEquipmentValidationItem = {
  tempId: string;
  siteId: string | null;
  equipmentOpDate: string | null;
  nominalMaxOutput: number | null;
  pcsManufacturer: string | null;
  pcsModel: string | null;
  deviceNo: string | null;
  pcsStandby: number | null;
  hasBattery: boolean | null;
  batteryMaker?: string | null;
  batteryModel?: string | null;
  batteryCapacity?: number | null;
  totalInvestment: number | null;
  hasSubsidy: boolean | null;
  subsidyName?: string | null;
  subsidyAmount?: number | null;
  preProjectUnitPrice: number | null;
  files: SiteProjectEquipmentValidationFile[];
};

/** Request accepted by POST /site/{siteId}/{projectId}/equipment. */
export type SiteProjectEquipmentCreateRequest = {
  tempId: string;
  siteId: string;
  equipmentOpDate: string | null;
  nominalMaxOutput: number | null;
  pcsManufacturer: string | null;
  pcsModel: string | null;
  deviceNo: string | null;
  pcsStandby: number | null;
  hasBattery: boolean | null;
  batteryMaker: string | null;
  batteryModel: string | null;
  batteryCapacity: number | null;
  totalInvestment: number | null;
  hasSubsidy: boolean | null;
  subsidyName: string | null;
  subsidyAmount: number | null;
  preProjectUnitPrice: number | null;
  files: SiteProjectEquipmentValidationFile[];
};

/** Response returned after adding equipment to an existing site project. */
export type SiteProjectEquipmentCreateResponse =
  SiteProjectEquipmentDetailResponse;

export type SiteProjectEquipmentValidationResponse = {
  statusCode?: string;
  message?: string | null;
  errors?: Record<string, string> | null;
  timestamp?: string;
};

export type SiteProjectJoinConfirmationItems = {
  isAgreeDataUse: boolean | null;
  isAgreeExtraData: boolean | null;
  isAgreeTransfer: boolean | null;
  isNotRegistered: boolean | null;
  isKeidanrenParticipation: boolean | null;
  isEmitter: boolean | null;
  emitterCode?: string;
  isAdjustedEmissionsAdded?: boolean;
  isSubjectToPeriodicReport: boolean;
  isPeriodicReportTypeOne?: boolean;
  energyBusinessCode?: string;
  factoryCode?: string;
};

export type SiteProjectJoinConfirmationItemsRequest =
  SiteProjectJoinConfirmationItems & {
    id: string;
    processedBy: string;
    companyId: string;
  };

export type SiteProjectConfirmationItemsValidationRequest = {
  id: string;
  processedBy: string;
  companyId: string;
  isAgreeDataUse: boolean | null;
  isAgreeExtraData: boolean | null;
  isAgreeTransfer: boolean | null;
  isNotRegistered: boolean | null;
  isKeidanrenParticipation: boolean | null;
  isEmitter: boolean | null;
  emitterCode: string;
  isAdjustedEmissionsAdded: boolean | null;
  isSubjectToPeriodicReport: boolean | null;
  isPeriodicReportTypeOne: boolean | null;
  energyBusinessCode: string;
  factoryCode: string;
};

export type SiteProjectConfirmationItemsValidationResponse = {
  statusCode?: string;
  message?: string | null;
  errors?: Record<string, string> | null;
  timestamp?: string;
};

export type SiteProjectJoinSustainabilityCommitmentRequest = {
  companyName: string;
  repName: string;
  repRole: string;
  hasConsent: boolean;
};

export type SiteProjectJoinSustainabilityCommitmentResponse = {
  statusCode?: string;
  message?: string | null;
  errors?: Record<string, string> | null;
  timestamp?: string;
};

export type SiteProjectSustainabilityCommitmentDefaultResponse = {
  statusCode?: string;
  message?: string | null;
  errors?: Record<string, string> | null;
  timestamp?: string;
  companyName?: string | null;
  repName?: string | null;
  repRole?: string | null;
  hasConsent?: boolean | null;
  isAgreeTerms?: boolean | null;
};

export type SiteProjectFinalAgreementValidationRequest = {
  isAgreeTerms: boolean;
};

export type SiteProjectFinalAgreementValidationResponse = {
  statusCode?: string;
  message?: string | null;
  errors?: Record<string, string> | null;
  timestamp?: string;
};

export type SiteProjectJoinRequest = {
  equipment: SiteProjectEquipmentValidationItem;
  confirmationItems: SiteProjectJoinConfirmationItemsRequest;
  sustainabilityCommitment: SiteProjectJoinSustainabilityCommitmentRequest;
  finalAgreement: SiteProjectFinalAgreementValidationRequest;
};

export type SiteProjectJoinResponse = {
  statusCode?: string;
  message?: string | null;
  participationId?: number;
  equipmentId?: number;
  siteProjectRelationId?: number;
};

export type SiteProjectEquipmentFileUploadResponse = {
  statusCode?: string;
  s3Key?: string;
  fileId?: string;
  originalName?: string;
};

// For GET /site list response
export interface SiteListItem {
  siteId: string;
  /** @deprecated Use siteId. */
  id?: string;
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
