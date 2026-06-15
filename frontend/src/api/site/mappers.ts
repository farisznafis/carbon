import type { Site, SiteProject, SiteProjectApiItem, SiteListItem, SiteDashboardApiResponse } from "./types";

function toNumberOrUndefined(value?: string | number | null): number | undefined {
  if (value === null || value === undefined || value === "") return undefined;
  const parsed = typeof value === "number" ? value : Number(value);
  return Number.isNaN(parsed) ? undefined : parsed;
}

// For GET /site/{siteId}/project response (projects.ts)
export function mapSiteProject(item: SiteProjectApiItem): SiteProject {
  return {
    id: item.id,
    projectName: item.projectName ?? "",
    projectStatus: (item.projectStatus as SiteProject["projectStatus"]) ?? "準備中",
    emissionReduction: toNumberOrUndefined(item.emissionReduction),
    baselineEmissions: toNumberOrUndefined(item.baselineEmission),
    postProjectEmission: toNumberOrUndefined(item.postProjectEmission),
    methodologyVersion: item.methodologyVersion ?? "",
    methodCategory: item.methodologyCategory ?? "",
    methodNumber: item.methodologyNumber ?? "",
    methodName: item.methodologyName ?? "",
    methodMainEmission: item.methodologyMainEmission ?? "",
    methodCombiFeasibility: Boolean(item.methodCombiFeasibility),
    isJoined: Boolean(item.isJoined),
    projectNumber: item.projectNumber ?? "",
    repFirstName: item.repFirstName ?? undefined,
    repFirstNameKana: item.repFirstNameKana ?? undefined,
    repLastName: item.repLastName ?? undefined,
    repLastNameKana: item.repLastNameKana ?? undefined,
    repEmail: item.repEmail ?? undefined,
    repPhoneNumber: item.repPhoneNumber ?? undefined,
    repPosition: item.repPosition ?? undefined,
    picFirstName: item.picFirstName ?? undefined,
    picFirstNameKana: item.picFirstNameKana ?? undefined,
    picLastName: item.picLastName ?? undefined,
    picLastNameKana: item.picLastNameKana ?? undefined,
    picEmail: item.picEmail ?? undefined,
    picPhoneNumber: item.picPhoneNumber ?? undefined,
    picPosition: item.picPosition ?? undefined,
    dataManagementPic: item.dataManagementPic ?? undefined,
    companyName: item.companyName ?? undefined,
    regionalCategory: item.regionalCategory ?? undefined,
    newMembersTotal: item.newMembersTotal ?? undefined,
    withdrawnMembers: item.withdrawnMembers ?? undefined,
    cumulativeMembers: item.cumulativeMembers ?? undefined,
    membersTotal: item.membersTotal ?? undefined,
    methodDetails: item.methodDetails ?? undefined,
    operatorCompanyId: item.operatorCompanyId ?? undefined,
    updatedAt: item.updatedAt ?? undefined,
    createdAt: item.createdAt ?? undefined,
  };
}

// For GET /site list response
export function mapSiteListItem(item: SiteListItem): Site {
  return {
    id: item.id,
    siteName: item.siteName,
    postalCode: item.postalCode ?? "",
    prefectureName: item.prefName ?? "",
    cityName: item.cityName ?? "",
    blockName: item.townName ? `${item.townName}${item.blockName ?? ""}` : (item.blockName ?? ""),
    address: item.address ?? "",
    picId: "",
    picFirstName: item.picFirstName ?? "",
    picLastName: item.picLastName ?? "",
    picEmail: "",
    picPhoneNumber: "",
    picPosition: "",
    companyId: "",
    project: item.projects ?? [],
  };
}

// For GET /site/{siteId} response (dashboard.ts)
// using apiSite from SiteDashboardApiResponse to map to Site, because the fields are more complete than SiteListItem 
export function mapSiteDashboard(apiSite: SiteDashboardApiResponse['site']): Site { 
  return {
    id: apiSite.id,
    siteName: apiSite.siteName ?? "",
    postalCode: apiSite.postalCode ?? "",
    prefectureName: apiSite.prefecture ?? "",
    cityName: apiSite.city ?? "",
    blockName: [apiSite.town, apiSite.block].filter(Boolean).join(""),
    address: apiSite.address ?? "",
    picId: "",
    picFirstName: apiSite.picFirstName ?? "",
    picLastName: apiSite.picLastName ?? "",
    picEmail: "",
    picPhoneNumber: "",
    picPosition: "",
    companyId: "",
    project: apiSite.projects ?? [],
  };
}
