import { apiRequest } from '../_shared/httpClient';
import type {
  AdminCompanyListItem,
  AdminCompanyListResponse,
  CompanyMutationResponse,
  CompanyStatus,
  CompanySuspension,
  CompanySuspensionListResponse,
  GetAdminCompaniesParams,
  Methodology,
  Pagination,
  PendingCompanyItem,
  PendingCompanyStatus,
  ResolveCompanySuspensionPayload,
  SuspendCompanyPayload,
} from './types';

export type {
  AdminCompanyListItem,
  AdminCompanyListResponse,
  CompanySort,
  CompanyStatus,
  CompanySuspension,
  GetAdminCompaniesParams,
  Methodology,
  Pagination,
  PendingCompanyEquipmentItem,
  PendingCompanyItem,
  PendingCompanyStatus,
  ResolveCompanySuspensionPayload,
  SortOrder,
  SuspendCompanyPayload,
  SuspensionReasonCode,
} from './types';

const DEFAULT_PAGINATION: Pagination = {
  page: 1,
  limit: 20,
  totalItems: 0,
  totalPages: 1,
  nextPageToken: null,
  hasMore: false,
};

function appendParam(
  params: URLSearchParams,
  key: string,
  value: string | number | undefined,
) {
  if (value === undefined || value === '') return;
  params.set(key, String(value));
}

function buildCompanyListQuery(params: GetAdminCompaniesParams) {
  const searchParams = new URLSearchParams();
  const status = Array.isArray(params.status)
    ? params.status.join(',')
    : params.status;

  appendParam(searchParams, 'status', status);
  appendParam(searchParams, 'search', params.search?.slice(0, 100));
  appendParam(searchParams, 'prefecture', params.prefecture);
  appendParam(searchParams, 'createdFrom', params.createdFrom);
  appendParam(searchParams, 'createdTo', params.createdTo);
  appendParam(searchParams, 'page', params.page);
  appendParam(searchParams, 'limit', params.limit);
  appendParam(searchParams, 'sort', params.sort);
  appendParam(searchParams, 'sortOrder', params.sortOrder);

  const query = searchParams.toString();
  return query ? `?${query}` : '';
}

function normalizePagination(pagination?: Partial<Pagination>): Pagination {
  return {
    ...DEFAULT_PAGINATION,
    ...pagination,
    page: pagination?.page ?? DEFAULT_PAGINATION.page,
    limit: pagination?.limit ?? DEFAULT_PAGINATION.limit,
    totalItems: pagination?.totalItems ?? DEFAULT_PAGINATION.totalItems,
    totalPages: Math.max(pagination?.totalPages ?? DEFAULT_PAGINATION.totalPages, 1),
    nextPageToken: pagination?.nextPageToken ?? null,
    hasMore: pagination?.hasMore ?? false,
  };
}

function normalizeCompanyListResponse(
  response: AdminCompanyListResponse,
): AdminCompanyListResponse {
  return {
    statusCode: response.statusCode,
    message: response.message,
    companies: Array.isArray(response.companies) ? response.companies : [],
    pagination: normalizePagination(response.pagination),
  };
}

export async function getAdminCompanies(
  params: GetAdminCompaniesParams = {},
): Promise<AdminCompanyListResponse> {
  const response = await apiRequest<AdminCompanyListResponse>(
    `/admin/companies${buildCompanyListQuery(params)}`,
    {
      method: 'GET',
      fallbackErrorMessage: 'Failed to fetch companies',
    },
  );

  return normalizeCompanyListResponse(response);
}

type RawCompanySuspensionListResponse = Partial<CompanySuspensionListResponse> & {
  items?: CompanySuspension[];
  data?: CompanySuspension[];
};

export async function getCompanySuspensions(
  companyId: string,
  params: { activeOnly?: boolean; page?: number; limit?: number } = {},
): Promise<CompanySuspensionListResponse> {
  const searchParams = new URLSearchParams();
  appendParam(
    searchParams,
    'activeOnly',
    params.activeOnly === undefined ? undefined : String(params.activeOnly),
  );
  appendParam(searchParams, 'page', params.page);
  appendParam(searchParams, 'limit', params.limit);

  const query = searchParams.toString();
  const response = await apiRequest<RawCompanySuspensionListResponse>(
    `/admin/companies/${companyId}/suspensions${query ? `?${query}` : ''}`,
    {
      method: 'GET',
      fallbackErrorMessage: 'Failed to fetch company suspensions',
    },
  );

  return {
    statusCode: response.statusCode,
    message: response.message,
    suspensions: response.suspensions ?? response.items ?? response.data ?? [],
    pagination: normalizePagination(response.pagination),
  };
}

export async function approveAdminCompany(
  companyId: string,
): Promise<CompanyMutationResponse> {
  return apiRequest<CompanyMutationResponse>(
    `/admin/companies/${companyId}/approve`,
    {
      method: 'POST',
      fallbackErrorMessage: 'Failed to approve company',
    },
  );
}

export async function rejectAdminCompany(
  companyId: string,
  rejectionReason: string,
): Promise<CompanyMutationResponse> {
  return apiRequest<CompanyMutationResponse>(
    `/admin/companies/${companyId}/reject`,
    {
      method: 'POST',
      body: { rejectionReason },
      fallbackErrorMessage: 'Failed to reject company',
    },
  );
}

export async function suspendAdminCompany(
  companyId: string,
  payload: SuspendCompanyPayload,
): Promise<CompanyMutationResponse> {
  return apiRequest<CompanyMutationResponse>(
    `/admin/companies/${companyId}/suspensions`,
    {
      method: 'POST',
      body: payload,
      fallbackErrorMessage: 'Failed to suspend company',
    },
  );
}

export async function resolveCompanySuspension(
  companyId: string,
  suspensionId: number,
  payload: ResolveCompanySuspensionPayload,
): Promise<CompanyMutationResponse> {
  return apiRequest<CompanyMutationResponse>(
    `/admin/companies/${companyId}/suspensions/${suspensionId}/resolve`,
    {
      method: 'POST',
      body: payload,
      fallbackErrorMessage: 'Failed to resolve company suspension',
    },
  );
}

function toLegacyPendingStatus(status: CompanyStatus): PendingCompanyStatus {
  if (status === 'REJECTED') return '却下';
  if (status === 'ACTIVE') return '承認済み';
  if (status === 'SUSPENDED') return '停止';
  return '申請受付';
}

function toLegacyPendingCompany(item: AdminCompanyListItem): PendingCompanyItem {
  return {
    id: item.id ?? item.companyId,
    companyId: item.companyId,
    status: toLegacyPendingStatus(item.status),
    nameJA: item.nameJA,
    nameFurigana: item.nameFurigana,
    nameEN: item.nameEN,
    isSuspended: item.status === 'SUSPENDED',
    email: item.email,
    phoneNumber: item.phoneNumber,
    fax: item.fax ?? '',
    corpNumber: item.corpNumber,
    address: item.address,
    postalCode: item.postalCode,
    prefecture: item.prefecture,
    municipality: item.municipality,
    blocknum: item.blocknum,
    buildingName: item.buildingName ?? undefined,
    representativeFirstName: item.representativeFirstName,
    representativeLastName: item.representativeLastName,
    representativeFirstNameKana: item.representativeFirstNameKana,
    representativeLastNameKana: item.representativeLastNameKana,
    representativePosition: item.representativePosition,
    representativePhoneNumber: item.representativePhoneNumber,
    faxNumber: item.fax ?? undefined,
    requestedMethodologies: item.requestedMethodologies,
    activityInformation: item.activityInformation ?? '',
    methodologies: [],
  };
}

export async function getPendingCompanies(): Promise<PendingCompanyItem[]> {
  const response = await getAdminCompanies({
    status: ['PENDING', 'REJECTED'],
    page: 1,
    limit: 100,
    sort: 'createdAt',
    sortOrder: 'desc',
  });

  return response.companies.map(toLegacyPendingCompany);
}

export async function approvePendingCompany(companyId: string): Promise<void> {
  await approveAdminCompany(companyId);
}

export async function rejectPendingCompany(
  companyId: string,
  rejectionReason: string,
): Promise<void> {
  await rejectAdminCompany(companyId, rejectionReason);
}
