import { apiFetch, apiVoidRequest, parseJsonResponse } from '../_shared/httpClient';
import type {
  AdminMutationResponse,
  Methodology,
  PendingCompaniesResponse,
  PendingCompanyApiItem,
  PendingCompanyItem,
} from './types';

export type {
  Methodology,
  PendingCompanyEquipmentItem,
  PendingCompanyItem,
} from './types';

function resolvePendingCompanyId(item: PendingCompanyApiItem, index: number): string {
  return item.companyId ?? item.id ?? `pending-company-${index}`;
}

function resolveCompanyId(item: PendingCompanyApiItem, index: number): string {
  return item.companyId ?? item.requestId ?? `company-${index}`;
}

function resolveCompanyName(item: PendingCompanyApiItem): string {
  return item.nameJA ?? item.companyName ?? '-';
}

function resolveNameFurigana(item: PendingCompanyApiItem): string {
  return item.nameFurigana ?? item.nameKana ?? item.companyNameKana ?? '-';
}

function resolveNameEN(item: PendingCompanyApiItem): string {
  return item.nameEN ?? item.companyNameEnglish ?? '-';
}

function resolveEmail(item: PendingCompanyApiItem): string {
  return item.email ?? item.corporateEmail ?? '-';
}

function resolvePhoneNumber(item: PendingCompanyApiItem): string {
  return item.phoneNumber ?? '-';
}

function resolveFax(item: PendingCompanyApiItem): string {
  return item.fax ?? '-';
}

function resolveCorpNumber(item: PendingCompanyApiItem): string {
  return item.corpNumber ?? '-';
}

function resolveAddress(item: PendingCompanyApiItem): string {
  return item.address ?? '-';
}

function resolveIsSuspended(item: PendingCompanyApiItem): boolean {
  return item.isSuspended ?? item.suspended ?? false;
}

function resolveMethodologies(
  item: PendingCompanyApiItem,
): Methodology[] {
  if (!item.methodologies || !Array.isArray(item.methodologies)) {
    return [];
  }

  return item.methodologies
    .filter((m) => m.methodology) // Filter out invalid entries
    .map((m) => ({
      methodology: m.methodology || '',
      hasSubsidy: m.hasSubsidy ?? false,
      subsidyName: m.subsidyName,
      remarks: m.remarks,
      equipmentItems: (m.equipmentItems || []).map((eq) => ({
        installationDate: eq.installationDate || '',
        manufacturer: eq.manufacturer || '',
        modelNo: eq.modelNo || '',
        installationAddress: eq.installationAddress || '',
      })),
    }));
}

export async function getPendingCompanies(): Promise<PendingCompanyItem[]> {
  const response = await apiFetch('/admin/companies/pending', {
    method: 'GET',
  });
  const { data: json, rawBody } = await parseJsonResponse<PendingCompaniesResponse>(response);

  if (!response.ok) {
    throw new Error(
      json?.message || rawBody || `Failed to fetch pending companies: HTTP ${response.status}`,
    );
  }

  const items =
    json?.companies ??
    json?.companyList ??
    json?.items ??
    json?.data ??
    [];

  if (!Array.isArray(items)) {
    throw new Error(json?.message || 'Invalid pending companies response');
  }

  return items.map((item, index) => ({
    id: resolvePendingCompanyId(item, index),
    companyId: resolveCompanyId(item, index),
    nameJA: resolveCompanyName(item),
    nameFurigana: resolveNameFurigana(item),
    nameEN: resolveNameEN(item),
    email: resolveEmail(item),
    phoneNumber: resolvePhoneNumber(item),
    fax: resolveFax(item),
    corpNumber: resolveCorpNumber(item),
    address: resolveAddress(item),
    methodologies: resolveMethodologies(item),
  }));
}

/**
 * Approve a pending company request.
 * Adjust the endpoint/path if your backend differs.
 */
export async function approvePendingCompany(companyId: string): Promise<void> {
  await apiVoidRequest(`/admin/companies/${companyId}/approve`, {
    method: 'POST',
    body: { companyId },
    fallbackErrorMessage: 'Failed to approve company',
  });
}

/**
 * Reject a pending company request with a reason.
 * Adjust the endpoint/path/body if your backend differs.
 */
export async function rejectPendingCompany(companyId: string, rejectionReason: string): Promise<void> {
  await apiVoidRequest(`/admin/companies/${companyId}/reject`, {
    method: 'POST',
    body: { rejectionReason },
    fallbackErrorMessage: 'Failed to reject company',
  });
}
