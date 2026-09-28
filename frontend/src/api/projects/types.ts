export interface Project {
  id: string;
  companyId?: string;
  operatorCompanyId?: string | number;
  companyName?: string;
  siteName?: string;
  projectName: string;
  projectStatus: string;
  emissionReduction?: number;
  updatedAt?: string | null;
  createdAt?: string | null;
  baselineEmissions?: number;
  postProjectEmission?: number;
  methodologyVersion: string;
  methodCategory: string;
  methodNumber: string;
  methodName: string;
  methodMainEmission: string;
  methodCombiFeasibility: boolean;
}

export interface ProjectDetail {
  id: string;
  siteId: string | number | null;
  projectName: string;
  projectNumber: string;
  projectStatus: 'ACTIVE' | 'PENDING';
  emissionReduction?: number | string | null;
  baselineEmission?: number | string | null;
  postProjectEmission?: number | string | null;
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
  newMembersTotal?: number;
  withdrawnMembers?: number;
  cumulativeMembers?: number;
  membersTotal?: number;
  methodologyVersion: string;
  methodologyCategory: string;
  methodologyNumber: string;
  methodologyName: string;
  methodologyMainEmission: string;
  methodCombiFeasibility: boolean;
  methodDetails: string;
  operatorCompanyId: number;
  isCompanyJoined: boolean | null;
  hasSiteRelation: boolean | null;
  updatedAt: string;
  createdAt: string;
}

export interface ProjectDetailResponse {
  statusCode: string;
  message: string | null;
  errors: unknown;
  project: ProjectDetail;
}

export interface CompanyProjectRelation {
  id: string;
  projectID: string;
  siteID: string;
}

export interface Equipment {
  id: string;
  installationDate: string;
  manufacturer: string;
  modelNo: string;
  installationAddress: string;
  model: string;
}

export type ProjectMembershipPdfDownload = {
  pdfUrl: string;
  expiresIn: number;
};

export type ProjectListItem = {
  id: string;
  companyId?: string | number | null;
  operatorCompanyId?: string | number | null;
  companyName?: string | null;
  siteName?: string | null;
  projectName: string;
  projectStatus: string;
  emissionReduction?: number | string | null;
  updatedAt?: string | null;
  createdAt?: string | null;
  methodologyCategory: string;
  methodologyNumber: string;
  methodologyName: string;
  methodologyMainEmission: string;
  methodologyVersion: string;
  methodCombiFeasibility: boolean;
};

export type ProjectListResponse = {
  statusCode: string;
  message: string | null;
  errors: unknown;
  projects: ProjectListItem[];
  pagination?: {
    page: number;
    limit: number;
    totalItems: number;
    totalPages: number;
    nextPageToken: string | null;
    hasMore: boolean;
  };
};

export type ProjectMemberTermsInfo = {
  statusCode: string;
  message: string | null;
  projectName: string;
  operatorNameJa: string;
  adminRepName: string;
  projectOverview: string;
  usePurpose: string;
  startedAt: string;
};
