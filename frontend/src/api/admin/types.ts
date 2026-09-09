export type AdminProjectDetail = {
  id: string;
  projectName: string;
  projectNumber: string;
  projectStatus: string;
  emissionReduction: string | number | null;
  baselineEmission?: string | number | null;
  baselineEmissions?: string | number | null;
  postProjectEmission: string | number | null;
  postProjectEmissions?: string | number | null;
  repFirstName: string;
  repFirstNameKana: string;
  repLastName: string;
  repLastNameKana: string;
  repEmail: string;
  repPhoneNumber: string;
  repPosition: string;
  picFirstName: string;
  picFirstNameKana: string;
  picLastName: string;
  picLastNameKana: string;
  picEmail: string;
  picPhoneNumber: string;
  picPosition: string;
  dataManagementPic: string;
  companyName: string;
  regionalCategory: string;
  newMembersTotal: number | null;
  withdrawnMembers: number | null;
  cumulativeMembers: number | null;
  membersTotal: number | null;
  methodologyVersion: string;
  methodologyCategory: string;
  methodologyNumber: string;
  methodologyName: string;
  methodologyMainEmission: string;
  methodCombiFeasibility: boolean;
  methodDetails: string;
  operatorCompanyId: string;
  isCompanyJoined: boolean | null;
  hasSiteRelation: boolean | null;
  updatedAt: string;
  createdAt: string;
};

export type AdminProjectDetailResponse = {
  statusCode: string;
  message: string | null;
  errors: unknown;
  project: AdminProjectDetail;
};

export type CreateAdminProjectPayload = {
  project: Array<{
    projectName?: string;
    projectNumber?: string;
    projectStatus?: string;
    // emissionReduction?: number;
    baselineEmissions?: number;
    postProjectEmission?: number;
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
    regionalCategory?: string;
    newMembersTotal?: number;
    withdrawnMembers?: number;
    cumulativeMembers?: number;
    membersTotal?: number;
    methodologyVersion?: string;
    methodCategory?: string;
    methodNumber?: string;
    methodName?: string;
    methodMainEmission?: string;
    methodCombiFeasibility?: boolean;
    methodDetails?: string;
    operatorCompanyId?: string;
  }>;
  operatorCompanyId?: string;
  createdBy: string;
};

export type CreateAdminProjectResponse = {
  statusCode?: string;
  message?: string | null;
  errors?: unknown;
  project?: Array<Partial<AdminProjectDetail> & {
    id?: string;
  }>;
};

export type ProjectPlanAutofillResponse = {
  statusCode?: string;
  message?: string | null;
  errors?: unknown;
  data?: Record<string, unknown>;
};

export type UpdateAdminProjectPayload = {
  processedBy: string;
  projectName: string;
  baselineEmissions: number;
  postProjectEmission: number;
  repFirstName: string;
  repFirstNameKana: string;
  repLastName: string;
  repLastNameKana: string;
  repEmail: string;
  repPhoneNumber: string;
  repPosition: string;
  picFirstName: string;
  picFirstNameKana: string;
  picLastName: string;
  picLastNameKana: string;
  picEmail: string;
  picPhoneNumber: string;
  picPosition: string;
  dataManagementPic: string;
  regionalCategory: string;
  methodologyVersion: string;
  methodCategory: string;
  methodNumber: string;
  methodName: string;
  methodMainEmission: string;
  methodCombiFeasibility: boolean;
  methodDetails: string;
};

export type AdminProjectPayloadFormValues = {
  projectBase?: {
    projectName?: string;
    projectNumber?: string;
    projectStatus?: string;
    // emissionReduction?: number;
    baselineEmissions?: number;
    postProjectEmission?: number;
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
    picDataManagement?: string;
    // companyName?: string;
    regionalCategory?: string;
    newMembersTotal?: number;
    withdrawnMembers?: number;
    cumulativeMembers?: number;
    membersTotal?: number;
    methodologyVersion?: string;
    methodCategory?: string;
    methodNumber?: string;
    methodName?: string;
    methodMainEmission?: string;
    methodCombiFeasibility?: boolean;
    methodDetails?: string;
    operatorCompanyId?: string | number;
  };
};

export type AdminProjectPlanApplicationPayloadFormValues = AdminProjectPayloadFormValues & {
  projectApplication?: Record<string, unknown>;
};

export type CreateAdminProjectPlanApplicationPayload = Record<string, unknown>;

export type PendingCompanyEquipmentItem = {
  installationDate: string;
  manufacturer: string;
  modelNo: string;
  installationAddress: string;
};

export type Methodology = {
  methodology: string;
  hasSubsidy: boolean;
  subsidyName?: string;
  remarks?: string;
  equipmentItems?: PendingCompanyEquipmentItem[];
};

export type CompanyStatus = 'PENDING' | 'ACTIVE' | 'SUSPENDED' | 'REJECTED';

export type CompanySort = 'createdAt' | 'updatedAt' | 'nameJa' | 'companyId';

export type SortOrder = 'asc' | 'desc';

export type SuspensionReasonCode =
  | 'BILLING'
  | 'POLICY_VIOLATION'
  | 'SECURITY'
  | 'ADMINISTRATIVE'
  | 'OTHER';

export type PaymentMethod = 'CREDIT_CARD';

export type AdminCompanyListItem = {
  id: string | null;
  companyId: string;
  nameJA: string;
  nameFurigana: string;
  nameEN: string;
  email: string;
  phoneNumber: string;
  fax: string | null;
  corpNumber: string;
  address: string;
  postalCode: string;
  prefecture: string;
  municipality: string;
  blocknum: string;
  buildingName: string | null;
  representativeFirstName: string;
  representativeLastName: string;
  representativeFirstNameKana: string;
  representativeLastNameKana: string;
  representativePosition: string;
  representativePhoneNumber: string;
  requestedMethodologies: string[];
  activityInformation: string | null;
  status: CompanyStatus;
  isActive: boolean;
  createdAt: string;
  updatedAt: string | null;
};

export type AdminCompanyAccountActionTarget = Pick<
  AdminCompanyListItem,
  'companyId' | 'nameJA' | 'email'
>;

export type Pagination = {
  page: number;
  limit: number;
  totalItems: number;
  totalPages: number;
  nextPageToken: string | null;
  hasMore: boolean;
};

export type AdminCompanyListResponse = {
  statusCode: string;
  message: string;
  companies: AdminCompanyListItem[];
  pagination: Pagination;
};

export type AdminCompanyDetailMasterAccount = {
  username: string;
  fullName: string;
  fullNameKana: string;
  firstName: string;
  lastName: string;
  firstNameKana: string;
  lastNameKana: string;
  phoneNumber: string;
  email: string;
  userPosition: string;
  role: string;
  isActive: boolean;
  createdAt: string;
  updatedAt: string;
  createdBy: string;
  updatedBy: string;
};

export type AdminCompanyDetailCompany = {
  companyId: string;
  corpNumber: string;
  nameJA: string;
  nameFurigana: string;
  nameEN: string;
  email: string;
  phoneNumber: string;
  faxNumber: string;
  address: string;
  postalCode: string;
  prefecture: string;
  municipality: string;
  blocknum: string;
  buildingName: string;
  companyStatus: CompanyStatus;
  subscriptionStatus: string;
  isActive: boolean;
  createdAt: string;
  updatedAt: string;
  createdBy: string;
  updatedBy: string;
};

export type AdminCompanyDetailUser = {
  username: string;
  fullName: string;
  email: string;
  role: string;
  siteName: string;
};

export type AdminCompanyDetailSite = {
  siteName: string;
  picEmail: string;
  createdAt: string;
  address: string;
  postalCode: string;
  prefecture: string;
  municipality: string;
  blocknum: string;
  buildingName: string;
  projectNames: string[];
};

export type AdminCompanyDetailResponse = {
  statusCode: string;
  message: string;
  errors?: Record<string, string>;
  timestamp: string;
  masterAccount: AdminCompanyDetailMasterAccount | null;
  company: AdminCompanyDetailCompany;
  users: AdminCompanyDetailUser[];
  sites: AdminCompanyDetailSite[];
};

export type AdminCompanyPasswordResetLinkResponse = {
  statusCode: string;
  message: string;
  timestamp?: string;
};

export type GetAdminCompaniesParams = {
  status?: CompanyStatus | CompanyStatus[];
  search?: string;
  prefecture?: string;
  createdFrom?: string;
  createdTo?: string;
  page?: number;
  limit?: number;
  sort?: CompanySort;
  sortOrder?: SortOrder;
};

export type CompanySuspension = {
  id: number;
  reasonCode: SuspensionReasonCode;
  reasonDetail: string;
  createdAt: string;
  createdBy: string;
  resolvedAt: string | null;
  resolvedBy: string | null;
  resolutionNote: string | null;
  active: boolean;
};

export type CompanySuspensionListResponse = {
  statusCode?: string;
  message?: string | null;
  suspensions: CompanySuspension[];
  pagination: Pagination;
};

export type SuspendCompanyPayload = {
  reasonCode: SuspensionReasonCode;
  reasonDetail: string;
};

export type ResolveCompanySuspensionPayload = {
  resolutionNote: string;
  paymentMethod?: PaymentMethod;
};

export type CompanyMutationResponse = {
  statusCode?: string;
  message?: string | null;
  errors?: unknown;
  companyStatus?: CompanyStatus;
  activeSuspensionCount?: number;
};

export type PendingCompanyItem = {
  id: string;
  companyId: string;
  status: PendingCompanyStatus;
  nameJA: string;
  nameFurigana?: string;
  nameEN?: string;
  isSuspended?: boolean;
  email: string;
  phoneNumber: string;
  fax: string;
  corpNumber: string;
  address: string;
  postalCode?: string;
  prefecture?: string;
  municipality?: string;
  blocknum?: string;
  buildingName?: string;
  representativeFirstName?: string;
  representativeLastName?: string;
  representativeFirstNameKana?: string;
  representativeLastNameKana?: string;
  representativePosition?: string;
  representativePhoneNumber?: string;
  faxNumber?: string;
  requestedMethodologies?: string[];
  activityInformation?: string;
  methodologies?: Methodology[];
};

export type PendingCompanyStatus = '申請受付' | '却下' | '承認済み' | '停止';

export type PendingCompanyApiItem = {
  id?: string;
  requestId?: string;
  companyId?: string;
  status?: string | null;
  applicationStatus?: string | null;
  statusLabel?: string | null;
  nameJA?: string;
  nameFurigana?: string;
  nameKana?: string;
  companyNameKana?: string;
  nameEN?: string;
  companyNameEnglish?: string;
  companyName?: string;
  isSuspended?: boolean;
  suspended?: boolean;
  email?: string;
  corporateEmail?: string;
  phoneNumber?: string;
  fax?: string | null;
  corpNumber?: string;
  address?: string;
  postalCode?: string | null;
  prefecture?: string | null;
  municipality?: string | null;
  blocknum?: string | null;
  buildingName?: string | null;
  representativeFirstName?: string | null;
  representativeLastName?: string | null;
  representativeFirstNameKana?: string | null;
  representativeLastNameKana?: string | null;
  representativePosition?: string | null;
  representativePhoneNumber?: string | null;
  faxNumber?: string | null;
  requestedMethodologies?: string[];
  activityInformation?: string;
  methodologies?: Array<{
    methodology?: string;
    hasSubsidy?: boolean;
    subsidyName?: string;
    remarks?: string;
    equipmentItems?: Array<{
      installationDate?: string;
      manufacturer?: string;
      modelNo?: string;
      installationAddress?: string;
    }>;
  }>;
};

export type PendingCompaniesResponse = {
  statusCode?: string;
  message?: string | null;
  companies?: PendingCompanyApiItem[];
  companyList?: PendingCompanyApiItem[];
  items?: PendingCompanyApiItem[];
  data?: PendingCompanyApiItem[];
};

export type AdminMutationResponse = {
  statusCode?: string;
  message?: string | null;
  errors?: unknown;
};

export type ProjectStatus =
  | '作成済み'
  | '準備中'
  | '変更中'
  | '妥当性確認処理'
  | '却下'
  | '実施中'
  | '検証中'
  | 'クレジット申請中'
  | '終了';

export type UpdateProjectStatusPayload = {
  id: string;
  processedBy: string;
  companyId: string;
  projectStatus: ProjectStatus;
};

export type UpdateProjectStatusResponse = UpdateProjectStatusPayload;

/** How far one project's plan is filled, for list views. */
export type ProjectPlanProgressSummary = {
  projectId: string;
  totalForms: number;
  completedForms: number;
  /** First page not yet complete; null once everything is done. */
  nextIncompleteSlug?: string | null;
  /** True when the operator may pick any page instead of resuming. */
  complete: boolean;
};

export type ProjectPlanBatchProgressResponse = {
  statusCode?: string;
  message?: string | null;
  errors?: unknown;
  /** Projects the caller cannot see are omitted rather than reported. */
  projects?: ProjectPlanProgressSummary[];
};

/**
 * A provisional upload against a project-plan page. The s3Key still points
 * under tmp/; it becomes permanent only when that page is saved.
 */
export type ProjectPlanFileResponse = {
  statusCode?: string;
  message?: string;
  s3Key?: string;
  fileId?: string;
  originalName?: string;
};
