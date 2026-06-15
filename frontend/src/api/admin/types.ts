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
  isJoined: boolean | null;
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
    id?: string | number;
  }>;
};

export type ProjectPlanApplicationResponse = {
  statusCode?: string;
  message?: string | null;
  errors?: unknown;
  application?: unknown;
  projectApplication?: unknown;
  data?: unknown;
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

export type PendingCompanyItem = {
  id: string;
  companyId: string;
  nameJA: string;
  nameFurigana?: string;
  nameEN?: string;
  isSuspended?: boolean;
  email: string;
  phoneNumber: string;
  fax: string;
  corpNumber: string;
  address: string;
  methodologies?: Methodology[];
};

export type PendingCompanyApiItem = {
  id?: string;
  requestId?: string;
  companyId?: string;
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
  id: string | number;
  processedBy: string;
  companyId: string;
  projectStatus: ProjectStatus;
};

export type UpdateProjectStatusResponse = UpdateProjectStatusPayload;
