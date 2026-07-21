import type { Project, ProjectListItem } from './types';

export function mapProjectListItem(item: ProjectListItem): Project {
  return {
    id: String(item.id),
    companyId: item.companyId != null ? String(item.companyId) : undefined,
    operatorCompanyId:
      item.operatorCompanyId != null ? String(item.operatorCompanyId) : undefined,
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
