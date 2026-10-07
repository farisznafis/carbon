import { useCallback, useEffect, useMemo, useState, type ReactNode } from 'react';
import {
  useLocation,
  useNavigate,
  useParams,
} from 'react-router-dom';
import { ApiError, isForbiddenApiError } from '@/api/_shared/errors';
import type {
  AdminCompanyListItem,
  CompanyStatus,
  Pagination,
} from '@/api/admin';
import {
  approveAdminCompany,
  getAdminCompanies,
  getOpenParticipationReviews,
  rejectAdminCompany,
  type ParticipationReviewDetail,
} from '@/api/admin';
import {
  getBatchHistory,
  type BatchHistoryItem,
  type BatchHistoryPagination,
} from '@/api/batch';
import {
  getMonitoringCancelRequests,
} from '@/api/monitoring';
import {
  updateAdminProjectStatus,
  type ProjectStatus,
} from '@/api/admin/projects';
import type { Project } from '@/api/projects';
import { PaginationControls } from '@/components/data-display/Pagination/PaginationControls';
import {
  adminPageContainerClassName,
  dashboardEmptyStateClassName,
} from '@/components/layout/DashboardLayout';
import { AdminPageHeader } from '@/components/layout/AdminPageHeader';
import { AdminTableCard } from '@/components/layout/AdminTableCard';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '@/components/ui/select';
import { Tabs, TabsContent } from '@/components/ui/tabs';
import { useAuth } from '@/contexts/AuthContext';
import { AdminCompanyApplicationsTable } from '@/features/admin/dashboard/tables/AdminCompanyApplicationsTable';
import { PendingCompanyActionDialog } from '@/features/admin/dashboard/tables/PendingCompanyActionDialog';
import { PendingCompanyApplicationDetail } from '@/features/admin/dashboard/tables/PendingCompanyApplicationDetail';
import { ProjectTable } from '@/features/admin/dashboard/tables/ProjectTable';
import {
  AdminDashboardOverview,
  type AdminHomePanel,
} from './AdminDashboardOverview';
import { AdminProjectOverviewCards } from './AdminProjectOverviewCards';
import {
  normalizeProjectStatus,
  PROJECT_STATUS_PRESENTATIONS,
} from './projectStatusPresentation';
import { PageSkeleton, SkeletonTable } from '@/components/feedback/loading';
import { useProjectsQuery } from '@/hooks/projects/useProjectsQuery';
import { useToast } from '@/hooks/use-toast';
import { usePagination } from '@/hooks/usePagination';
import { paths } from '@/routes/paths';
import { getAdminRouteFromLegacyTab } from '@/features/admin/navigation/adminNavigationConfig';
import { adminHomeProjectNavigationState } from '@/features/admin/navigation/adminProjectNavigation';
import { Search, TriangleAlert } from 'lucide-react';
import { useAdminPermissions } from '@/features/admin/permissions/useAdminPermissions';
import { PermissionDeniedNotice } from '@/features/admin/permissions/PermissionDeniedNotice';
import { NoticeBanner } from '@/components/feedback/NoticeBanner';
import { ActionButton } from '@/components/actions';
import { PageTabsList, PageTabsTrigger } from '@/components/navigation/PageTabs';

type AdminDashboardTabValue =
  | 'overview'
  | 'companies'
  | 'projects'
  | 'operators'
  | 'methodologies';
type ProjectManagementView = 'analytics' | 'management';

type PendingCompanyActionMode = 'approve' | 'reject';
type ApplicationDetailNavigationState = {
  company?: AdminCompanyListItem;
  reviewMode?: boolean;
  returnTo?: string;
};

const dashboardContainer = adminPageContainerClassName;

const APPLICATION_STATUSES: CompanyStatus[] = ['PENDING', 'REJECTED'];
const PARTICIPATING_STATUSES: CompanyStatus[] = ['ACTIVE', 'SUSPENDED'];
const HOME_APPLICATION_SUMMARY_LIMIT = 3;
const ALL_PROJECT_FILTER = 'ALL';
const PROJECT_VIEW_QUERY_PARAM = 'view';
const FORBIDDEN_SECTION_ERROR = '__FORBIDDEN_SECTION__';

const DEFAULT_PAGINATION: Pagination = {
  page: 1,
  limit: 20,
  totalItems: 0,
  totalPages: 1,
  nextPageToken: null,
  hasMore: false,
};

const DEFAULT_BATCH_HISTORY_PAGINATION: BatchHistoryPagination = {
  page: 1,
  limit: 10,
  totalItems: 0,
  totalPages: 1,
  nextPageToken: null,
  hasMore: false,
};

const adminDashboardTabs: Array<{
  value: AdminDashboardTabValue;
  label: string;
}> = [
  // { value: 'overview', label: '概要' },
  { value: 'companies', label: '企業一覧・申請管理' },
  { value: 'projects', label: 'プロジェクト管理' },
  { value: 'operators', label: '運用者一覧' },
  { value: 'methodologies', label: '方法論一覧' },
];

/** Resolves the route-driven dashboard section while legacy tab URLs redirect. */
function getAdminDashboardSection(
  pathname: string,
  legacyTab: string | null,
): AdminDashboardTabValue {
  if (pathname === paths.adminProjectList) return 'projects';
  if (pathname === paths.adminCompanyList) return 'companies';
  if (pathname === paths.adminMethodologyList) return 'methodologies';
  if (
    pathname === paths.adminDashboard &&
    (legacyTab === 'overview' ||
      adminDashboardTabs.some((tab) => tab.value === legacyTab))
  ) {
    return legacyTab as AdminDashboardTabValue;
  }

  return 'overview';
}

function isStringRecord(value: unknown): value is Record<string, string> {
  return (
    Boolean(value) &&
    typeof value === 'object' &&
    !Array.isArray(value) &&
    Object.values(value as Record<string, unknown>).every(
      (entry) => typeof entry === 'string',
    )
  );
}

function getApiMessage(error: unknown, fallback: string) {
  if (isForbiddenApiError(error)) return FORBIDDEN_SECTION_ERROR;
  if (error instanceof ApiError) return error.message || fallback;
  if (error instanceof Error) return error.message || fallback;
  return fallback;
}

function getApiFieldErrors(error: unknown) {
  if (error instanceof ApiError && isStringRecord(error.payload?.errors)) {
    return error.payload.errors;
  }

  return {};
}

/** Builds a stable filter value from one project's methodology identity. */
function getProjectMethodologyFilterValue(project: Project) {
  return `${project.methodNumber}::${project.methodName}`;
}

/** Builds the human-readable methodology option shown in the filter. */
function getProjectMethodologyFilterLabel(project: Project) {
  return [project.methodNumber, project.methodName].filter(Boolean).join(' ');
}

/** Checks whether a project matches the normalized free-text query. */
function matchesProjectSearch(project: Project, normalizedQuery: string) {
  if (!normalizedQuery) return true;

  return [
    project.projectName,
    project.methodCategory,
    project.methodNumber,
    project.methodologyVersion,
    project.methodName,
  ].some((field) => field.toLocaleLowerCase('ja-JP').includes(normalizedQuery));
}

function AdminDashboardSection({
  title,
  description,
  action,
  children,
}: {
  title: string;
  description: string;
  action?: ReactNode;
  children: ReactNode;
}) {
  return (
    <section className="space-y-4">
      <AdminPageHeader
        title={title}
        description={description}
        action={action}
        headingAs="h2"
      />
      {children}
    </section>
  );
}

function AdminDashboardEmptyState({ children }: { children: ReactNode }) {
  return (
    <div className={dashboardEmptyStateClassName}>
      {children}
    </div>
  );
}

/** Renders the route-selected admin dashboard section. */
const AdminDashboard = () => {
  const navigate = useNavigate();
  const location = useLocation();
  const { applicationId } = useParams();
  const { toast } = useToast();
  const { user } = useAuth();
  const adminPermissions = useAdminPermissions();
  const canReadProjects = adminPermissions.hasPermission('PROJECT_READ');
  const canReadCompanies = adminPermissions.hasPermission(
    'PARTICIPATING_COMPANY_READ',
  );
  const canReviewCompanies = adminPermissions.hasPermission(
    'PARTICIPATING_COMPANY_APPLICATION_REVIEW',
  );
  const canCancelMonitoring = adminPermissions.hasPermission(
    'MONITORING_CANCEL',
  );
  const canReadBatchHistory = adminPermissions.hasPermission(
    'MONITORING_BATCH_HISTORY_READ',
  );
  const projectsQuery = useProjectsQuery(canReadProjects);
  const projects = useMemo(() => projectsQuery.data ?? [], [projectsQuery.data]);

  const [applicationCompaniesLoading, setApplicationCompaniesLoading] = useState(true);
  const [applicationCompaniesError, setApplicationCompaniesError] = useState<string | null>(null);
  const [participatingCompaniesLoading, setParticipatingCompaniesLoading] = useState(true);
  const [participatingCompaniesError, setParticipatingCompaniesError] = useState<string | null>(null);
  const [cancelRequestItems, setCancelRequestItems] = useState<
    Awaited<ReturnType<typeof getMonitoringCancelRequests>>
  >([]);
  const [cancelRequestLoading, setCancelRequestLoading] = useState(true);
  const [cancelRequestError, setCancelRequestError] = useState<string | null>(
    null,
  );
  const [batchHistoryItems, setBatchHistoryItems] = useState<BatchHistoryItem[]>(
    [],
  );
  const [batchHistoryPagination, setBatchHistoryPagination] =
    useState<BatchHistoryPagination>(DEFAULT_BATCH_HISTORY_PAGINATION);
  const [batchHistoryLoading, setBatchHistoryLoading] = useState(true);
  const [batchHistoryError, setBatchHistoryError] = useState<string | null>(
    null,
  );
  const [openMembershipReviews, setOpenMembershipReviews] = useState<
    ParticipationReviewDetail[]
  >([]);
  const [openMembershipReviewsError, setOpenMembershipReviewsError] = useState<
    string | null
  >(null);
  const [applicationCompanies, setApplicationCompanies] = useState<
    AdminCompanyListItem[]
  >([]);
  const [homePendingApplications, setHomePendingApplications] = useState<
    AdminCompanyListItem[]
  >([]);
  const [homePendingApplicationTotal, setHomePendingApplicationTotal] = useState(0);
  const [homePendingApplicationsLoading, setHomePendingApplicationsLoading] = useState(true);
  const [homePendingApplicationsError, setHomePendingApplicationsError] = useState<string | null>(null);
  const [homeRejectedApplications, setHomeRejectedApplications] = useState<
    AdminCompanyListItem[]
  >([]);
  const [homeRejectedApplicationTotal, setHomeRejectedApplicationTotal] = useState(0);
  const [homeRejectedApplicationsLoading, setHomeRejectedApplicationsLoading] = useState(true);
  const [homeRejectedApplicationsError, setHomeRejectedApplicationsError] = useState<string | null>(null);
  const [participatingCompanies, setParticipatingCompanies] = useState<
    AdminCompanyListItem[]
  >([]);
  const [applicationPagination, setApplicationPagination] =
    useState<Pagination>(DEFAULT_PAGINATION);
  const [participatingPagination, setParticipatingPagination] =
    useState<Pagination>(DEFAULT_PAGINATION);
  const [applicationPage, setApplicationPage] = useState(1);
  const [participatingPage, setParticipatingPage] = useState(1);
  const [companyLimit, setCompanyLimit] = useState(20);
  const [submitting, setSubmitting] = useState(false);
  const [actionFieldErrors, setActionFieldErrors] = useState<
    Record<string, string>
  >({});
  const [updatingProjectStatusId, setUpdatingProjectStatusId] = useState<
    string | null
  >(null);
  const [projectStatusFilter, setProjectStatusFilter] = useState(
    ALL_PROJECT_FILTER,
  );
  const [projectMethodologyFilter] = useState(
    ALL_PROJECT_FILTER,
  );
  const [projectSearchQuery, setProjectSearchQuery] = useState('');
  const [pendingDialogOpen, setPendingDialogOpen] = useState(false);
  const [pendingDialogMode, setPendingDialogMode] =
    useState<PendingCompanyActionMode | null>(null);
  const [selectedCompany, setSelectedCompany] =
    useState<AdminCompanyListItem | null>(null);
  const [unroutedDetailCompany, setUnroutedDetailCompany] =
    useState<AdminCompanyListItem | null>(null);
  const [unroutedReviewMode, setUnroutedReviewMode] = useState(false);
  const [rejectReason, setRejectReason] = useState('');

  const requestedTab = new URLSearchParams(location.search).get('tab');
  const requestedPanel = new URLSearchParams(location.search).get('panel');
  const requestedProjectView = new URLSearchParams(location.search).get(
    PROJECT_VIEW_QUERY_PARAM,
  );
  const activeProjectView: ProjectManagementView =
    requestedProjectView === 'analytics' ? 'analytics' : 'management';
  const activePanel: AdminHomePanel | null =
    requestedPanel === 'cancel' || requestedPanel === 'batch'
      ? requestedPanel
      : null;
  const activeSection = getAdminDashboardSection(
    location.pathname,
    requestedTab,
  );

  /** Keeps project subtab selection in the URL without changing the route. */
  const handleProjectViewChange = (view: string) => {
    if (view !== 'analytics' && view !== 'management') return;
    if (view === activeProjectView) return;

    const searchParams = new URLSearchParams(location.search);
    if (view === 'analytics') {
      searchParams.set(PROJECT_VIEW_QUERY_PARAM, view);
    } else {
      searchParams.delete(PROJECT_VIEW_QUERY_PARAM);
    }

    navigate(
      { pathname: location.pathname, search: searchParams.toString(), hash: location.hash },
      { state: location.state, preventScrollReset: true },
    );
  };

  useEffect(() => {
    if (location.pathname !== paths.adminDashboard || !requestedTab) return;

    const legacyRoute = getAdminRouteFromLegacyTab(requestedTab);
    if (legacyRoute) {
      navigate(legacyRoute, { replace: true });
    }
  }, [location.pathname, navigate, requestedTab]);

  /** Reloads the application-company list independently from participating companies. */
  const reloadApplicationCompanies = useCallback(async () => {
    if (!canReviewCompanies) {
      setApplicationCompanies([]);
      setApplicationCompaniesLoading(false);
      setApplicationCompaniesError(null);
      return;
    }

    setApplicationCompaniesLoading(true);
    setApplicationCompaniesError(null);

    try {
      const applications = await getAdminCompanies({
        limit: companyLimit,
        status: APPLICATION_STATUSES,
        page: applicationPage,
        sort: 'createdAt',
        sortOrder: 'desc',
      });
      setApplicationCompanies(applications.companies);
      setApplicationPagination(applications.pagination);
    } catch (error) {
      setApplicationCompaniesError(
        getApiMessage(error, '企業一覧の取得に失敗しました。'),
      );
    } finally {
      setApplicationCompaniesLoading(false);
    }
  }, [applicationPage, canReviewCompanies, companyLimit]);

  /** Reloads participating companies without blocking application reviews. */
  const reloadParticipatingCompanies = useCallback(async () => {
    if (!canReadCompanies) {
      setParticipatingCompanies([]);
      setParticipatingCompaniesLoading(false);
      setParticipatingCompaniesError(null);
      return;
    }

    setParticipatingCompaniesLoading(true);
    setParticipatingCompaniesError(null);

    try {
      const participating = await getAdminCompanies({
        limit: companyLimit,
        status: PARTICIPATING_STATUSES,
        page: participatingPage,
      });
      setParticipatingCompanies(participating.companies);
      setParticipatingPagination(participating.pagination);
    } catch (error) {
      setParticipatingCompaniesError(
        getApiMessage(error, '参加企業一覧の取得に失敗しました。'),
      );
    } finally {
      setParticipatingCompaniesLoading(false);
    }
  }, [canReadCompanies, companyLimit, participatingPage]);

  /** Refreshes both company groups after a company mutation. */
  const reloadCompanyLists = useCallback(async () => {
    await Promise.all([
      reloadApplicationCompanies(),
      reloadParticipatingCompanies(),
    ]);
  }, [reloadApplicationCompanies, reloadParticipatingCompanies]);

  /** Loads the latest PENDING applications and their actual API total for Home. */
  const reloadHomePendingApplications = useCallback(async () => {
    if (!canReviewCompanies) {
      setHomePendingApplications([]);
      setHomePendingApplicationTotal(0);
      setHomePendingApplicationsLoading(false);
      setHomePendingApplicationsError(null);
      return;
    }

    setHomePendingApplicationsLoading(true);
    setHomePendingApplicationsError(null);
    setHomePendingApplications([]);
    setHomePendingApplicationTotal(0);

    try {
      const response = await getAdminCompanies({
        status: 'PENDING',
        page: 1,
        limit: HOME_APPLICATION_SUMMARY_LIMIT,
        sort: 'createdAt',
        sortOrder: 'desc',
      });
      setHomePendingApplications(response.companies);
      setHomePendingApplicationTotal(response.pagination.totalItems);
    } catch (error) {
      setHomePendingApplicationsError(
        getApiMessage(error, '確認待ちの企業申請を取得できませんでした。'),
      );
    } finally {
      setHomePendingApplicationsLoading(false);
    }
  }, [canReviewCompanies]);

  /** Loads the latest REJECTED applications and their actual API total for Home. */
  const reloadHomeRejectedApplications = useCallback(async () => {
    if (!canReviewCompanies) {
      setHomeRejectedApplications([]);
      setHomeRejectedApplicationTotal(0);
      setHomeRejectedApplicationsLoading(false);
      setHomeRejectedApplicationsError(null);
      return;
    }

    setHomeRejectedApplicationsLoading(true);
    setHomeRejectedApplicationsError(null);
    setHomeRejectedApplications([]);
    setHomeRejectedApplicationTotal(0);

    try {
      const response = await getAdminCompanies({
        status: 'REJECTED',
        page: 1,
        limit: HOME_APPLICATION_SUMMARY_LIMIT,
        sort: 'createdAt',
        sortOrder: 'desc',
      });
      setHomeRejectedApplications(response.companies);
      setHomeRejectedApplicationTotal(response.pagination.totalItems);
    } catch (error) {
      setHomeRejectedApplicationsError(
        getApiMessage(error, '却下済みの企業申請を取得できませんでした。'),
      );
    } finally {
      setHomeRejectedApplicationsLoading(false);
    }
  }, [canReviewCompanies]);

  useEffect(() => {
    if (location.pathname !== paths.adminDashboard) return;

    void reloadHomePendingApplications();
    void reloadHomeRejectedApplications();
  }, [
    location.pathname,
    reloadHomePendingApplications,
    reloadHomeRejectedApplications,
  ]);

  useEffect(() => {
    if (!canReadProjects || !canReviewCompanies) {
      setOpenMembershipReviews([]);
      setOpenMembershipReviewsError(null);
      return;
    }

    let isActive = true;

    /** Loads OPEN membership reviews used to prioritize the project table. */
    const preloadOpenMembershipReviews = async () => {
      setOpenMembershipReviewsError(null);

      try {
        const reviews = await getOpenParticipationReviews();
        if (isActive) setOpenMembershipReviews(reviews);
      } catch (error) {
        if (!isActive) return;
        setOpenMembershipReviewsError(
          getApiMessage(error, '入会届の優先順位を取得できませんでした。'),
        );
      }
    };

    void preloadOpenMembershipReviews();

    return () => {
      isActive = false;
    };
  }, [canReadProjects, canReviewCompanies]);

  useEffect(() => {
    if (!canCancelMonitoring) {
      setCancelRequestItems([]);
      setCancelRequestLoading(false);
      setCancelRequestError(null);
      return;
    }

    let isActive = true;

    const preloadCancelRequests = async () => {
      setCancelRequestLoading(true);
      setCancelRequestError(null);

      try {
        const response = await getMonitoringCancelRequests();
        if (!isActive) return;
        setCancelRequestItems(response);
      } catch (error) {
        if (!isActive) return;
        setCancelRequestError(
          getApiMessage(error, '取消一覧の取得に失敗しました。'),
        );
      } finally {
        if (isActive) {
          setCancelRequestLoading(false);
        }
      }
    };

    void preloadCancelRequests();

    return () => {
      isActive = false;
    };
  }, [canCancelMonitoring]);

  useEffect(() => {
    if (!canReadBatchHistory) {
      setBatchHistoryItems([]);
      setBatchHistoryLoading(false);
      setBatchHistoryError(null);
      return;
    }

    let isActive = true;

    const preloadBatchHistory = async () => {
      setBatchHistoryLoading(true);
      setBatchHistoryError(null);

      try {
        const response = await getBatchHistory(
          DEFAULT_BATCH_HISTORY_PAGINATION.page,
          DEFAULT_BATCH_HISTORY_PAGINATION.limit,
        );
        if (!isActive) return;
        setBatchHistoryItems(response.items);
        setBatchHistoryPagination(response.pagination);
      } catch (error) {
        if (!isActive) return;
        setBatchHistoryError(
          getApiMessage(error, 'バッチ履歴の取得に失敗しました。'),
        );
      } finally {
        if (isActive) setBatchHistoryLoading(false);
      }
    };

    void preloadBatchHistory();

    return () => {
      isActive = false;
    };
  }, [canReadBatchHistory]);

  useEffect(() => {
    void reloadApplicationCompanies();
  }, [reloadApplicationCompanies]);

  useEffect(() => {
    void reloadParticipatingCompanies();
  }, [reloadParticipatingCompanies]);

  const resetCompanyPages = () => {
    setApplicationPage(1);
    setParticipatingPage(1);
  };

  const handleCompanyLimitChange = (value: number) => {
    setCompanyLimit(value);
    resetCompanyPages();
  };

  const filteredProjects = useMemo(() => {
    const normalizedQuery = projectSearchQuery
      .trim()
      .toLocaleLowerCase('ja-JP');

    return projects.filter((project) => {
      const matchesStatus =
        projectStatusFilter === ALL_PROJECT_FILTER ||
        normalizeProjectStatus(project.projectStatus) === projectStatusFilter;
      const matchesMethodology =
        projectMethodologyFilter === ALL_PROJECT_FILTER ||
        getProjectMethodologyFilterValue(project) ===
          projectMethodologyFilter;

      return (
        matchesStatus &&
        matchesMethodology &&
        matchesProjectSearch(project, normalizedQuery)
      );
    });
  }, [
    projectMethodologyFilter,
    projectSearchQuery,
    projectStatusFilter,
    projects,
  ]);

  const {
    currentPage: projectPage,
    totalPages: projectTotalPages,
    totalItems: projectTotalItems,
    itemsPerPage: projectItemsPerPage,
    paginatedItems: paginatedProjects,
    goToPage: goToProjectPage,
    setCurrentPage: setProjectPage,
    setItemsPerPage: setProjectItemsPerPage,
  } = usePagination(filteredProjects);

  const handleEditProject = (project: Project) => {
    adminPermissions.guardAction('PROJECT_READ', () => {
      navigate(paths.adminProjectDetail(project.id));
    });
  };

  /** Opens project detail while preserving Admin Home as the contextual back destination. */
  const openHomeProjectDetail = (project: Project) => {
    adminPermissions.guardAction('PROJECT_READ', () => {
      navigate(paths.adminProjectDetail(project.id), {
        state: adminHomeProjectNavigationState,
      });
    });
  };

  const handleChangeProjectStatus = async (
    project: Project,
    status: ProjectStatus,
  ) => {
    if (project.projectStatus === status || updatingProjectStatusId) return;
    if (!adminPermissions.hasPermission('PROJECT_STATUS_UPDATE')) {
      adminPermissions.guardAction('PROJECT_STATUS_UPDATE', () => undefined);
      return;
    }

    setUpdatingProjectStatusId(project.id);
    try {
      const companyId =
        project.companyId ?? project.operatorCompanyId ?? user?.companyId ?? '';
      const projectStatusPayload = {
        id: project.id,
        processedBy: user?.id ? String(user.id) : 'admin',
        companyId: String(companyId),
        projectStatus: status,
      };

      await updateAdminProjectStatus(project.id, projectStatusPayload);
      await projectsQuery.refetch();
      toast({
        title: 'ステータスを更新しました',
        description: `${project.projectName}: ${status}`,
        variant: 'success',
      });
    } catch (error) {
      toast({
        title: 'ステータス更新に失敗しました',
        description: getApiMessage(
          error,
          'プロジェクトステータスを更新できませんでした。',
        ),
        variant: 'destructive',
      });
    } finally {
      setUpdatingProjectStatusId(null);
    }
  };

  const routeApplicationCompany = useMemo(() => {
    if (!applicationId) return null;
    return (
      applicationCompanies.find((company) => company.id === applicationId) ??
      null
    );
  }, [applicationCompanies, applicationId]);
  const applicationNavigationState = location.state as
    | ApplicationDetailNavigationState
    | null;
  const detailCompany = applicationId
    ? applicationNavigationState?.company?.id === applicationId
      ? applicationNavigationState.company
      : routeApplicationCompany
    : unroutedDetailCompany;
  const detailReviewMode = applicationId
    ? applicationNavigationState?.reviewMode ?? true
    : unroutedReviewMode;
  const companyDetailReturnTo =
    applicationNavigationState?.returnTo === paths.adminDashboard
      ? paths.adminDashboard
      : paths.adminCompanyList;

  /** Opens a company application detail while preserving its source page. */
  const openCompanyDetails = (item: AdminCompanyListItem) => {
    const requiredPermission =
      item.status === 'ACTIVE' || item.status === 'SUSPENDED'
        ? 'PARTICIPATING_COMPANY_READ'
        : 'PARTICIPATING_COMPANY_APPLICATION_REVIEW';
    if (!adminPermissions.hasPermission(requiredPermission)) {
      adminPermissions.guardAction(requiredPermission, () => undefined);
      return;
    }

    if (item.status === 'ACTIVE' || item.status === 'SUSPENDED') {
      navigate(paths.adminCompanyDetail(item.companyId));
      return;
    }

    if (item.id) {
      navigate(paths.adminApplicationDetail(item.id), {
        state: { company: item, reviewMode: false, returnTo: location.pathname },
      });
      return;
    }
    setUnroutedDetailCompany(item);
    setUnroutedReviewMode(false);
  };

  /** Opens a pending application with review actions available. */
  const openCompanyReview = (item: AdminCompanyListItem) => {
    if (
      !adminPermissions.hasPermission(
        'PARTICIPATING_COMPANY_APPLICATION_REVIEW',
      )
    ) {
      adminPermissions.guardAction(
        'PARTICIPATING_COMPANY_APPLICATION_REVIEW',
        () => undefined,
      );
      return;
    }

    if (item.id) {
      navigate(paths.adminApplicationDetail(item.id), {
        state: { company: item, reviewMode: true, returnTo: location.pathname },
      });
      return;
    }
    setUnroutedDetailCompany(item);
    setUnroutedReviewMode(true);
  };

  /** Opens actionable applications in review mode and historical ones read-only. */
  const openHomeCompanyApplication = (item: AdminCompanyListItem) => {
    if (item.status === 'PENDING') {
      openCompanyReview(item);
      return;
    }

    openCompanyDetails(item);
  };

  /** Returns to the source list and clears details without a route ID. */
  const closeCompanyDetail = () => {
    if (applicationId) {
      navigate(companyDetailReturnTo);
      return;
    }
    setUnroutedDetailCompany(null);
    setUnroutedReviewMode(false);
  };

  const openApproveCompany = (item: AdminCompanyListItem) => {
    if (
      !adminPermissions.hasPermission(
        'PARTICIPATING_COMPANY_APPLICATION_REVIEW',
      )
    ) {
      adminPermissions.guardAction(
        'PARTICIPATING_COMPANY_APPLICATION_REVIEW',
        () => undefined,
      );
      return;
    }

    setSelectedCompany(item);
    setPendingDialogMode('approve');
    setRejectReason('');
    setActionFieldErrors({});
    setPendingDialogOpen(true);
  };

  const openRejectCompany = (item: AdminCompanyListItem) => {
    if (
      !adminPermissions.hasPermission(
        'PARTICIPATING_COMPANY_APPLICATION_REVIEW',
      )
    ) {
      adminPermissions.guardAction(
        'PARTICIPATING_COMPANY_APPLICATION_REVIEW',
        () => undefined,
      );
      return;
    }

    setSelectedCompany(item);
    setPendingDialogMode('reject');
    setRejectReason('');
    setActionFieldErrors({});
    setPendingDialogOpen(true);
  };

  const openCancelRequestDetail = (
    request: Awaited<ReturnType<typeof getMonitoringCancelRequests>>[number],
  ) => {
    adminPermissions.guardAction('MONITORING_CANCEL', () => {
      navigate(paths.adminCancelRequestDetail(request.id), {
        state: { cancelRequest: request },
      });
    });
  };

  /** Opens or closes a Home monitoring panel while keeping it addressable by URL. */
  const handleHomePanelChange = (panel: AdminHomePanel | null) => {
    if (panel) {
      const requiredPermission =
        panel === 'cancel'
          ? 'MONITORING_CANCEL'
          : 'MONITORING_BATCH_HISTORY_READ';
      adminPermissions.guardAction(requiredPermission, () => {
        navigate(`${paths.adminDashboard}?panel=${panel}`);
      });
      return;
    }

    navigate(paths.adminDashboard, { replace: true });
  };

  /** Opens the OPEN membership list for the selected project. */
  const openProjectMemberships = (project: Project) => {
    adminPermissions.guardAction(
      'PARTICIPATING_COMPANY_APPLICATION_REVIEW',
      () =>
        navigate(paths.adminProjectMembership(project.id, 'OPEN'), {
          state: adminHomeProjectNavigationState,
        }),
    );
  };

  const handlePendingDialogOpenChange = (open: boolean) => {
    setPendingDialogOpen(open);

    if (!open) {
      setSelectedCompany(null);
      setPendingDialogMode(null);
      setRejectReason('');
      setActionFieldErrors({});
    }
  };

  const handleConfirmPendingCompany = async () => {
    if (!selectedCompany || !pendingDialogMode) return;

    setSubmitting(true);
    setActionFieldErrors({});
    try {
      if (pendingDialogMode === 'approve') {
        await approveAdminCompany(selectedCompany.companyId);
      } else {
        await rejectAdminCompany(selectedCompany.companyId, rejectReason);
      }
      await Promise.all([
        reloadCompanyLists(),
        ...(location.pathname === paths.adminDashboard
          ? [
              reloadHomePendingApplications(),
              reloadHomeRejectedApplications(),
            ]
          : []),
      ]);
      handlePendingDialogOpenChange(false);
      closeCompanyDetail();
      toast({
        title:
          pendingDialogMode === 'approve'
            ? '申請を承認しました'
            : '申請を却下しました',
        variant: pendingDialogMode === 'approve' ? 'success' : 'warning',
      });
    } catch (error) {
      setActionFieldErrors(getApiFieldErrors(error));
      setApplicationCompaniesError(
        getApiMessage(
          error,
          pendingDialogMode === 'approve'
            ? '承認に失敗しました。'
            : '却下に失敗しました。',
        ),
      );
    } finally {
      setSubmitting(false);
    }
  };

  if (
    applicationId &&
    (!canReviewCompanies || applicationCompaniesError === FORBIDDEN_SECTION_ERROR)
  ) {
    return (
      <main className={`${dashboardContainer} space-y-6 py-8`}>
        <header>
          <h1 className="text-form-title font-bold text-foreground">企業申請詳細</h1>
          <p className="mt-2 text-muted-foreground">企業申請の内容を確認します。</p>
        </header>
        <PermissionDeniedNotice permission="PARTICIPATING_COMPANY_APPLICATION_REVIEW" />
      </main>
    );
  }

  if (applicationId || unroutedDetailCompany) {
    return (
      <>
        {detailCompany ? (
          <PendingCompanyApplicationDetail
            item={detailCompany}
            reviewMode={detailReviewMode}
            onBack={closeCompanyDetail}
            onApprove={openApproveCompany}
            onReject={openRejectCompany}
          />
        ) : (
          applicationCompaniesLoading ? (
            <PageSkeleton />
          ) : (
            <div className="flex min-h-screen items-center justify-center bg-background px-6 text-small text-foreground">
              申請情報を取得できませんでした。
            </div>
          )
        )}
        <PendingCompanyActionDialog
          open={pendingDialogOpen}
          mode={pendingDialogMode}
          item={selectedCompany}
          rejectReason={rejectReason}
          fieldErrors={actionFieldErrors}
          onOpenChange={handlePendingDialogOpenChange}
          onRejectReasonChange={setRejectReason}
          onConfirm={handleConfirmPendingCompany}
          isSubmitting={submitting}
        />
      </>
    );
  }

  const projectsError = projectsQuery.error instanceof Error
    ? projectsQuery.error.message
    : projectsQuery.isError
      ? 'プロジェクトを取得できませんでした。'
      : null;
  const projectSectionAllowed = canReadProjects && !isForbiddenApiError(projectsQuery.error);
  const applicationSectionAllowed =
    canReviewCompanies &&
    applicationCompaniesError !== FORBIDDEN_SECTION_ERROR &&
    homePendingApplicationsError !== FORBIDDEN_SECTION_ERROR &&
    homeRejectedApplicationsError !== FORBIDDEN_SECTION_ERROR;
  const participatingSectionAllowed =
    canReadCompanies && participatingCompaniesError !== FORBIDDEN_SECTION_ERROR;
  const cancelSectionAllowed =
    canCancelMonitoring && cancelRequestError !== FORBIDDEN_SECTION_ERROR;
  const batchSectionAllowed =
    canReadBatchHistory && batchHistoryError !== FORBIDDEN_SECTION_ERROR;

  return (
    <div className="flex min-h-[calc(100dvh-var(--app-header-height))] flex-col bg-background">
      <Tabs value={activeSection} className="flex flex-1 flex-col bg-background">
        <main className={`${dashboardContainer} flex-1 space-y-section py-page-y`}>
          {/* <TabsContent
            value="overview"
            className="m-0 animate-in fade-in-0 slide-in-from-bottom-4 duration-500"
          >
            <AdminDashboardOverview
              projects={projects}
              projectsLoading={projectsQuery.isLoading}
              projectsError={projectsError}
              openMembershipReviews={openMembershipReviews}
              openMembershipReviewsError={openMembershipReviewsError}
              applicationTotal={applicationPagination.totalItems}
              applicationsLoading={applicationCompaniesLoading}
              applicationsError={applicationCompaniesError}
              pendingApplications={homePendingApplications}
              pendingApplicationTotal={homePendingApplicationTotal}
              pendingApplicationsLoading={homePendingApplicationsLoading}
              pendingApplicationsError={homePendingApplicationsError}
              rejectedApplications={homeRejectedApplications}
              rejectedApplicationTotal={homeRejectedApplicationTotal}
              rejectedApplicationsLoading={homeRejectedApplicationsLoading}
              rejectedApplicationsError={homeRejectedApplicationsError}
              participatingCompanyTotal={participatingPagination.totalItems}
              participatingCompaniesLoading={participatingCompaniesLoading}
              participatingCompaniesError={participatingCompaniesError}
              cancelRequests={cancelRequestItems}
              cancelRequestsLoading={cancelRequestLoading}
              cancelRequestsError={cancelRequestError}
              batchHistory={batchHistoryItems}
              batchHistoryPagination={batchHistoryPagination}
              batchHistoryLoading={batchHistoryLoading}
              batchHistoryError={batchHistoryError}
              activePanel={activePanel}
              onPanelChange={handleHomePanelChange}
              onOpenProjectDetail={openHomeProjectDetail}
              onOpenProjectMembership={openProjectMemberships}
              onOpenCompanyApplication={openHomeCompanyApplication}
              onOpenCancelRequest={openCancelRequestDetail}
              onNavigateProjects={() =>
                adminPermissions.guardAction('PROJECT_READ', () =>
                  navigate(paths.adminProjectList),
                )
              }
              onNavigateCompanies={() =>
                adminPermissions.guardAction(
                  'PARTICIPATING_COMPANY_READ',
                  () => navigate(paths.adminCompanyList),
                )
              }
              onNavigateApplications={() =>
                adminPermissions.guardAction(
                  'PARTICIPATING_COMPANY_APPLICATION_REVIEW',
                  () => navigate(paths.adminCompanyList),
                )
              }
              canReadProjects={projectSectionAllowed}
              canReadCompanies={participatingSectionAllowed}
              canReviewCompanies={applicationSectionAllowed}
              canCancelMonitoring={cancelSectionAllowed}
              canReadBatchHistory={batchSectionAllowed}
            />
          </TabsContent> */}

          <TabsContent
            value="overview"
            className="m-0 animate-in fade-in-0 slide-in-from-bottom-4 duration-500"
          >
            <AdminDashboardOverview
              projects={projects}
              projectsLoading={projectsQuery.isLoading}
              projectsError={projectsError}
              openMembershipReviews={openMembershipReviews}
              openMembershipReviewsError={openMembershipReviewsError}
              applicationTotal={applicationPagination.totalItems}
              applicationsLoading={applicationCompaniesLoading}
              applicationsError={applicationCompaniesError}
              pendingApplications={homePendingApplications}
              pendingApplicationTotal={homePendingApplicationTotal}
              pendingApplicationsLoading={homePendingApplicationsLoading}
              pendingApplicationsError={homePendingApplicationsError}
              rejectedApplications={homeRejectedApplications}
              rejectedApplicationTotal={homeRejectedApplicationTotal}
              rejectedApplicationsLoading={homeRejectedApplicationsLoading}
              rejectedApplicationsError={homeRejectedApplicationsError}
              participatingCompanyTotal={participatingPagination.totalItems}
              participatingCompaniesLoading={participatingCompaniesLoading}
              participatingCompaniesError={participatingCompaniesError}
              cancelRequests={cancelRequestItems}
              cancelRequestsLoading={cancelRequestLoading}
              cancelRequestsError={cancelRequestError}
              batchHistory={batchHistoryItems}
              batchHistoryPagination={batchHistoryPagination}
              batchHistoryLoading={batchHistoryLoading}
              batchHistoryError={batchHistoryError}
              activePanel={activePanel}
              onPanelChange={handleHomePanelChange}
              onOpenProjectDetail={openHomeProjectDetail}
              onOpenProjectMembership={openProjectMemberships}
              onOpenCompanyApplication={openHomeCompanyApplication}
              onOpenCancelRequest={openCancelRequestDetail}
              onNavigateProjects={() =>
                adminPermissions.guardAction('PROJECT_READ', () =>
                  navigate(paths.adminProjectList),
                )
              }
              onNavigateCompanies={() =>
                adminPermissions.guardAction(
                  'PARTICIPATING_COMPANY_READ',
                  () => navigate(paths.adminCompanyList),
                )
              }
              onNavigateApplications={() =>
                adminPermissions.guardAction(
                  'PARTICIPATING_COMPANY_APPLICATION_REVIEW',
                  () => navigate(paths.adminCompanyList),
                )
              }
              canReadProjects={projectSectionAllowed}
              canReadCompanies={participatingSectionAllowed}
              canReviewCompanies={applicationSectionAllowed}
              canCancelMonitoring={cancelSectionAllowed}
              canReadBatchHistory={batchSectionAllowed}
            />
          </TabsContent>

          <TabsContent
            value="companies"
            className="m-0 space-y-10 animate-in fade-in-0 slide-in-from-bottom-4 duration-500"
          >
            <AdminPageHeader
              title="企業管理"
              description="参加申請の確認と参加企業の情報を管理します。"
            />

            <AdminTableCard
              title="参加申請中の企業"
              count={applicationPagination.totalItems}
              description="参加申請を受け付けた企業です。内容を確認し、承認または却下を行ってください。"
              notice={
                applicationSectionAllowed ? (
                  <NoticeBanner
                    tone='warning'
                    title='管理者による対応が必要です'
                  >
                    ステータスが「申請受付」の企業について、申請内容を確認し、承認または却下を行ってください。
                  </NoticeBanner>
                ) : undefined
              }
              footer={
                applicationSectionAllowed &&
                !applicationCompaniesLoading &&
                !applicationCompaniesError ? (
                  <PaginationControls
                    currentPage={applicationPagination.page}
                    totalPages={applicationPagination.totalPages}
                    totalItems={applicationPagination.totalItems}
                    itemsPerPage={companyLimit}
                    rowsPerPageOptions={[10, 20, 50, 100]}
                    onPageChange={setApplicationPage}
                    onItemsPerPageChange={handleCompanyLimitChange}
                  />
                ) : undefined
              }
            >
              {!applicationSectionAllowed ? (
                <PermissionDeniedNotice permission="PARTICIPATING_COMPANY_APPLICATION_REVIEW" />
              ) : applicationCompaniesLoading ? (
                <SkeletonTable columns={5} rows={6} minWidth={980} />
              ) : applicationCompaniesError ? (
                <AdminDashboardEmptyState>
                  {applicationCompaniesError}
                </AdminDashboardEmptyState>
              ) : (
                <AdminCompanyApplicationsTable
                  items={applicationCompanies}
                  emptyMessage="参加申請中の企業はありません。"
                  onDetails={openCompanyDetails}
                  onReview={openCompanyReview}
                />
              )}
            </AdminTableCard>

            <AdminTableCard
              title="参加企業"
              count={participatingPagination.totalItems}
              description="承認済みの参加企業です。企業情報と運用状況を確認できます。"
              footer={
                participatingSectionAllowed &&
                !participatingCompaniesLoading &&
                !participatingCompaniesError ? (
                  <PaginationControls
                    currentPage={participatingPagination.page}
                    totalPages={participatingPagination.totalPages}
                    totalItems={participatingPagination.totalItems}
                    itemsPerPage={companyLimit}
                    rowsPerPageOptions={[10, 20, 50, 100]}
                    onPageChange={setParticipatingPage}
                    onItemsPerPageChange={handleCompanyLimitChange}
                  />
                ) : undefined
              }
            >
              {!participatingSectionAllowed ? (
                <PermissionDeniedNotice permission="PARTICIPATING_COMPANY_READ" />
              ) : participatingCompaniesLoading ? (
                <SkeletonTable columns={5} rows={6} minWidth={980} />
              ) : participatingCompaniesError ? (
                <AdminDashboardEmptyState>{participatingCompaniesError}</AdminDashboardEmptyState>
              ) : (
                <AdminCompanyApplicationsTable
                  items={participatingCompanies}
                  emptyMessage="参加企業はありません。"
                  onDetails={openCompanyDetails}
                  onReview={openCompanyReview}
                />
              )}
            </AdminTableCard>
          </TabsContent>

          <TabsContent
            value="projects"
            className="m-0 animate-in fade-in-0 slide-in-from-bottom-4 duration-500"
          >
            <section className="space-y-4">
              <AdminPageHeader
                title="プロジェクト管理"
                description="登録済みプロジェクトの詳細確認、更新、ステータス変更を行えます。"
              />
              {!projectSectionAllowed ? (
                <PermissionDeniedNotice permission="PROJECT_READ" />
              ) : (
                <Tabs
                  value={activeProjectView}
                  onValueChange={handleProjectViewChange}
                  className="min-w-0"
                >
                  <PageTabsList aria-label="プロジェクト管理の表示">
                    <PageTabsTrigger value="management">管理</PageTabsTrigger>
                    <PageTabsTrigger value="analytics">アナリティクス</PageTabsTrigger>
                  </PageTabsList>

                  <TabsContent value="analytics" className="m-0 pt-5">
                    <AdminProjectOverviewCards
                      projects={projects}
                      projectsLoading={projectsQuery.isLoading}
                      projectsError={projectsError}
                    />
                  </TabsContent>

                  <TabsContent value="management" className="m-0 pt-5">
                    <AdminTableCard
                      title="プロジェクト一覧"
                      count={projectTotalItems}
                      description="計画書の記入が完了していないプロジェクトはハイライト表示されます。"
                      action={
                        <ActionButton
                          kind="add"
                          onClick={() =>
                            adminPermissions.guardAction('PROJECT_CREATE', () =>
                              navigate(paths.adminProjectRegister),
                            )
                          }
                        >
                          プロジェクトを追加
                        </ActionButton>
                      }
                      filters={
                        <>
                          <Select
                            value={projectStatusFilter}
                            onValueChange={(value) => {
                              setProjectStatusFilter(value);
                              setProjectPage(1);
                            }}
                          >
                            <SelectTrigger>
                              <SelectValue placeholder="ステータス" />
                            </SelectTrigger>
                            <SelectContent>
                              <SelectItem value={ALL_PROJECT_FILTER}>
                                すべてのステータス
                              </SelectItem>
                              {PROJECT_STATUS_PRESENTATIONS.map((presentation) => (
                                <SelectItem
                                  key={presentation.status}
                                  value={presentation.status}
                                >
                                  {presentation.status}
                                </SelectItem>
                              ))}
                            </SelectContent>
                          </Select>
                        </>
                      }
                      search={
                        <>
                          <Search
                            className="pointer-events-none absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-muted-foreground"
                            aria-hidden="true"
                          />
                          <Input
                            type="search"
                            value={projectSearchQuery}
                            onChange={(event) => {
                              setProjectSearchQuery(event.target.value);
                              setProjectPage(1);
                            }}
                            placeholder="プロジェクト名・方法論で検索"
                            aria-label="プロジェクトを検索"
                            className="h-10 bg-card pl-9"
                          />
                        </>
                      }
                      footer={
                        !projectsQuery.isLoading && !projectsError ? (
                          <PaginationControls
                            currentPage={projectPage}
                            totalPages={projectTotalPages}
                            totalItems={projectTotalItems}
                            itemsPerPage={projectItemsPerPage}
                            onPageChange={goToProjectPage}
                            onItemsPerPageChange={setProjectItemsPerPage}
                          />
                        ) : undefined
                      }
                    >
                      {projectsQuery.isLoading ? (
                        <SkeletonTable columns={8} rows={6} minWidth={1390} />
                      ) : projectsError ? (
                        <AdminDashboardEmptyState>
                          {projectsError}
                        </AdminDashboardEmptyState>
                      ) : (
                        <ProjectTable
                          projects={paginatedProjects}
                          onEdit={handleEditProject}
                          onStatusChange={handleChangeProjectStatus}
                          updatingProjectStatusId={updatingProjectStatusId}
                        />
                      )}
                    </AdminTableCard>
                  </TabsContent>
                </Tabs>
              )}
            </section>
          </TabsContent>

          <TabsContent
            value="operators"
            className="m-0 animate-in fade-in-0 slide-in-from-bottom-4 duration-500"
          >
            <AdminDashboardSection
              title="運用者一覧"
              description="登録済み運用者のアカウント情報と所属拠点を確認できます。"
            >
              <AdminDashboardEmptyState>
                表示できる運用者データがありません。
              </AdminDashboardEmptyState>
            </AdminDashboardSection>
          </TabsContent>

          <TabsContent
            value="methodologies"
            className="m-0 space-y-10 animate-in fade-in-0 slide-in-from-bottom-4 duration-500"
          >
            <section className="space-y-4">
              <AdminPageHeader
                title="方法論一覧"
                description="利用可能な方法論の一覧をここで管理できるようにします。"
              />
              {adminPermissions.hasPermission('METHODOLOGY_READ') ? (
                <AdminDashboardEmptyState>
                  表示できる方法論データがありません。
                </AdminDashboardEmptyState>
              ) : (
                <PermissionDeniedNotice permission="METHODOLOGY_READ" />
              )}
            </section>

            {/* <AdminDashboardSection
              title="申請書様式"
              description="申請に使用する帳票様式を確認します。"
            >
              {adminPermissions.hasPermission('APPLICATION_FORM_READ') ? (
                <AdminDashboardEmptyState>
                  表示できる申請書様式データがありません。
                </AdminDashboardEmptyState>
              ) : (
                <PermissionDeniedNotice permission="APPLICATION_FORM_READ" />
              )}
            </AdminDashboardSection> */}
          </TabsContent>

        </main>
      </Tabs>

      <PendingCompanyActionDialog
        open={pendingDialogOpen}
        mode={pendingDialogMode}
        item={selectedCompany}
        rejectReason={rejectReason}
        fieldErrors={actionFieldErrors}
        onOpenChange={handlePendingDialogOpenChange}
        onRejectReasonChange={setRejectReason}
        onConfirm={handleConfirmPendingCompany}
        isSubmitting={submitting}
      />

    </div>
  );
};

export default AdminDashboard;
