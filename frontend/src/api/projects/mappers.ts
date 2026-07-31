import type { Project, ProjectListItem } from './types';

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
    methodCategory: item.methodologyCategory,
    methodNumber: item.methodologyNumber,
    methodName: item.methodologyName,
    methodMainEmission: item.methodologyMainEmission,
    methodologyVersion: item.methodologyVersion,
    methodCombiFeasibility: item.methodCombiFeasibility,
  };
}
