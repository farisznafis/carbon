import type { Project, ProjectListItem } from './types';

/** Converts nullable numeric API values into a safe finite number. */
function normalizeOptionalNumber(value: number | string | null | undefined) {
  if (value === null || value === undefined || value === '') return undefined;

  const normalizedValue = Number(value);
  return Number.isFinite(normalizedValue) ? normalizedValue : undefined;
}

/** Maps one project-list API record into the frontend project model. */
export function mapProjectListItem(item: ProjectListItem): Project {
  return {
    id: item.id,
    companyId: item.companyId != null ? String(item.companyId) : undefined,
    operatorCompanyId:
      item.operatorCompanyId != null ? String(item.operatorCompanyId) : undefined,
    companyName: item.companyName ?? undefined,
    siteName: item.siteName ?? undefined,
    projectName: item.projectName,
    projectStatus: item.projectStatus as Project['projectStatus'],
    emissionReduction: normalizeOptionalNumber(item.emissionReduction),
    updatedAt: item.updatedAt ?? null,
    createdAt: item.createdAt ?? null,
    methodCategory: item.methodologyCategory,
    methodNumber: item.methodologyNumber,
    methodName: item.methodologyName,
    methodMainEmission: item.methodologyMainEmission,
    methodologyVersion: item.methodologyVersion,
    methodCombiFeasibility: item.methodCombiFeasibility,
  };
}
