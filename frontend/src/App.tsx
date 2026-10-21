import { Toaster } from "@/components/ui/toaster";
import { lazy, Suspense, useCallback, useEffect, useRef } from "react";
import {
  Routes,
  Route,
  Navigate,
  useLocation,
  useParams,
  useSearchParams,
} from "react-router-dom";
import { AppShell } from "@/components/layout/AppShell";
import { ScrollToTop } from "./components/layout/ScrollToTop";
import { SessionExpiryGuard } from "@/components/layout/SessionExpiryGuard";
import { useAuth } from "@/contexts/AuthContext";
import { ProtectedRoute } from "./contexts/ProtectedRoute";
import { ErrorBoundary } from "@/components/feedback/ErrorBoundary";
import { AdminPermissionProvider } from '@/features/admin/permissions/AdminPermissionContext';
import { AdminBusinessAccessBoundary } from '@/features/admin/permissions/AdminBusinessAccessBoundary';
import { AdminPermissionRoute } from '@/features/admin/permissions/AdminPermissionRoute';
import { AppLoadingScreen, PageSkeleton } from '@/components/feedback/loading';
import { isAuthPagePath, paths } from "@/routes/paths";
import { cn } from "@/lib/utils";
import Lenis from "lenis";
import "lenis/dist/lenis.css";

const Index = lazy(() => import("./pages/Index"));
const Inquiry = lazy(() => import('./features/public/inquiry/InquiryPage'));
const Login = lazy(() => import("./pages/Login"));
const AdminLogin = lazy(() => import("./features/admin/auth/AdminLoginPage"));
const ForgotPassword = lazy(() => import("./pages/ForgotPassword"));
const ResetPassword = lazy(() => import("./pages/ResetPassword"));
const Signup = lazy(() => import("./pages/Signup"));
const SignupRevision = lazy(() =>
  import("./features/public/signup-revision/SignupRevisionPage"),
);
const UserInvitation = lazy(() =>
  import('./features/public/user-invitation/UserInvitationPage'),
);
const ProjectDetail = lazy(() => import("./features/user/project-detail/ProjectDetailPage"));
const MonitoringRegister = lazy(() => import("./features/user/monitoring/MonitoringRegisterPage"));
const SiteRegister = lazy(() => import("./features/master/pages/SiteRegisterPage"));
const SiteEdit = lazy(() => import("./features/master/pages/SiteEditPage"));
const UserEdit = lazy(() => import("./features/master/pages/UserEditPage"));
const MasterUserProfile = lazy(() => import("./features/master/pages/MasterUserProfilePage"));
const ProjectMembership = lazy(() => import("./features/user/project-registration/ProjectMembershipPage"));
const ProjectRegister = lazy(() => import("./features/user/project-registration/ProjectRegisterPage"));
const EquipmentForm = lazy(() => import("./features/user/project-registration/EquipmentFormPage"));
const EquipmentConfirmation = lazy(() =>
  import("./features/user/project-registration/EquipmentConfirmationPage"),
);
const EquipmentEdit = lazy(() =>
  import("./features/user/equipment-edit/EquipmentEditPage"),
);
const MasterDashboard = lazy(() => import("./features/master/pages/MasterDashboardPage"));
const NotFound = lazy(() => import("./pages/NotFound"));
const Dashboard = lazy(() => import("./features/user/dashboard/DashboardPage"));
const PasswordChange = lazy(() => import("./features/user/profile/PasswordChangePage"));
const Profile = lazy(() => import("./features/user/profile/ProfilePage"));
const AdminDashboard = lazy(() => import("./features/admin/dashboard/AdminDashboardPage"));
const AdminOperatorList = lazy(() =>
  import('./features/admin/operators/AdminOperatorListPage'),
);
const AdminOperatorRegister = lazy(() =>
  import('./features/admin/operators/AdminOperatorRegisterPage'),
);
const AdminOperatorProfile = lazy(() =>
  import('./features/admin/operators/AdminOperatorProfilePage'),
);
const AdminOperatorEdit = lazy(() =>
  import('./features/admin/operators/AdminOperatorEditPage'),
);
const AdminCompanyDetail = lazy(() =>
  import("./features/admin/company-detail/AdminCompanyDetailPage"),
);
const AdminCancelRequestDetail = lazy(() =>
  import("./features/admin/monitoring/CancelRequestDetailPage"),
);
const AdminProjectEdit = lazy(() => import("./features/admin/project-edit/AdminProjectEditPage"));
const AdminProjectRegister = lazy(() =>
  import("./features/admin/project-registration/AdminProjectRegisterPage").then((module) => ({
    default: module.AdminProjectRegister,
  })),
);
const ProjectDetailAdmin = lazy(() => import("./features/admin/project-detail/ProjectDetailAdminPage"));
const AdminProjectInformation = lazy(() =>
  import('./features/admin/project-detail/AdminProjectInformationPage'),
);
const AdminMonitoringReport = lazy(() =>
  import("./features/admin/project-detail/AdminMonitoringReportPage"),
);
const AdminProjectMonitoringDetail = lazy(() =>
  import("./features/admin/monitoring/ProjectMonitoringDetailPage"),
);
const AdminParticipationReviewList = lazy(() =>
  import("./features/admin/membership-review/ParticipationReviewListPage"),
);
const AdminParticipationReviewDetail = lazy(() =>
  import("./features/admin/membership-review/ParticipationReviewDetailPage"),
);
const AdminHelpFaqPage = lazy(() => import("./features/master/help/AdminHelpFaqPage"));

function RequireAdmin({ children }: { children: JSX.Element }) {
  const { isAuthenticated, user } = useAuth();
  return isAuthenticated && user?.role === 'admin'
    ? <AdminBusinessAccessBoundary>{children}</AdminBusinessAccessBoundary>
    : <Navigate to={paths.adminLogin} replace />;
}

function RequireMaster({ children }: { children: JSX.Element }) {
  const { user } = useAuth();

  if (user?.role === 'master') {
    return children;
  }

  const fallbackPath =
    user?.role === 'admin'
      ? paths.adminDashboard
      : user?.siteId
        ? paths.siteDashboard(user.siteId)
        : paths.userDashboard;

  return <Navigate to={fallbackPath} replace />;
}

/** プロジェクト作成. An existing project is changed on the edit page, not here. */
function AdminProjectRegisterRoute() {
  return (
    <AdminPermissionRoute permission="PROJECT_CREATE" title="プロジェクト追加" description="新しいプロジェクトを登録します。">
      <AdminProjectRegister />
    </AdminPermissionRoute>
  );
}

function LegacyDashboardRedirect() {
  const { siteId } = useParams();
  return <Navigate to={paths.siteDashboard(siteId ?? "")} replace />;
}

function LegacyProjectRedirect() {
  const { id } = useParams();
  const [searchParams] = useSearchParams();
  const siteId = searchParams.get("siteId");

  if (siteId) {
    return <Navigate to={paths.siteProjectDetail(siteId, id ?? "")} replace />;
  }

  return <ProjectDetail />;
}

const App = () => {
  const location = useLocation();
  const isAuthPage = isAuthPagePath(location.pathname);
  const { loading: authLoading } = useAuth();
  const lenisRef = useRef<Lenis | null>(null);

  /** Synchronizes Lenis with route-level native scroll resets. */
  const resetSmoothScrollPosition = useCallback(() => {
    lenisRef.current?.scrollTo(0, {
      immediate: true,
      force: true,
    });
  }, []);

  useEffect(() => {
    if (authLoading) return;

    const lenis = new Lenis({
      autoRaf: true,
      autoToggle: true,
      anchors: true,
      allowNestedScroll: true,
      naiveDimensions: true,
      stopInertiaOnNavigate: true,
    });
    lenisRef.current = lenis;

    return () => {
      lenisRef.current = null;
      lenis.destroy();
    };
  }, [authLoading]);

  if (authLoading) {
    return <AppLoadingScreen />;
  }

  return (
    <ErrorBoundary>
      <AdminPermissionProvider>
      <div
        className={cn(
          'flex flex-col bg-background text-foreground',
          isAuthPage ? 'h-dvh overflow-hidden' : 'min-h-dvh',
        )}
      >
        <Toaster />
        <ScrollToTop onRouteScrollReset={resetSmoothScrollPosition} />
        <SessionExpiryGuard />
        <AppShell>
        <div
          className={cn(
            'min-h-0 min-w-0 flex-1',
            isAuthPage && 'relative overflow-hidden',
          )}
        >
            <Suspense fallback={<PageSkeleton />}>
              <Routes>
          <Route path={paths.home} element={<Index />} />
          <Route path={paths.contact} element={<Inquiry />} />
          <Route path={paths.login} element={<Login />} />
          <Route path={paths.adminLogin} element={<AdminLogin />} />
          <Route path={paths.forgotPassword} element={<ForgotPassword />} />
          <Route path={paths.resetPassword} element={<ResetPassword />} />
          <Route path={paths.signup} element={<Signup />} />
          <Route path={paths.signupRevision} element={<SignupRevision />} />
          <Route path={paths.userInvitation} element={<UserInvitation />} />
          <Route element={<ProtectedRoute />}>
            <Route path={paths.masterDashboard} element={<MasterDashboard />} />
            <Route path={paths.masterSiteRegister} element={<SiteRegister />} />
            <Route path={paths.masterSiteEditRoute} element={<SiteEdit />} />
            <Route path={paths.masterUserProfileRoute} element={<MasterUserProfile />} />
            <Route path={paths.userEditRoute} element={<UserEdit />} />
            <Route path={paths.userDashboard} element={<Dashboard />} />
            <Route path={paths.siteDashboardRoute} element={<Dashboard />} />
            <Route path={paths.legacyDashboardRoute} element={<LegacyDashboardRedirect />} />
            <Route
              path={paths.projectMembershipRoute}
              element={
                <RequireMaster>
                  <ProjectMembership />
                </RequireMaster>
              }
            />
            <Route
              path={paths.projectRegisterRoute}
              element={
                <RequireMaster>
                  <ProjectRegister />
                </RequireMaster>
              }
            />
            <Route
              path={paths.projectEquipmentRoute}
              element={
                <RequireMaster>
                  <EquipmentForm />
                </RequireMaster>
              }
            />
            <Route
              path={paths.projectEquipmentConfirmationRoute}
              element={
                <RequireMaster>
                  <EquipmentConfirmation />
                </RequireMaster>
              }
            />
            <Route path={paths.siteProjectDetailRoute} element={<ProjectDetail />} />
            <Route
              path={paths.siteProjectEquipmentEditRoute}
              element={<EquipmentEdit />}
            />
            <Route path={paths.legacyProjectRoute} element={<LegacyProjectRedirect />} />
            <Route
              path={paths.monitoringRegisterRoute}
              element={<MonitoringRegister />}
            />
            <Route path={paths.siteDetailRoute} element={<LegacyDashboardRedirect />} />
            <Route path={paths.profile} element={<Profile />} />
          </Route>

          <Route
            path={paths.adminDashboard}
            element={
              <RequireAdmin>
                <AdminDashboard />
              </RequireAdmin>
            }
          />
          {[paths.adminProjectList, paths.adminCompanyList, paths.adminMethodologyList].map(
            (path) => (
              <Route
                key={path}
                path={path}
                element={
                  <RequireAdmin>
                    <AdminDashboard />
                  </RequireAdmin>
                }
              />
            ),
          )}
          <Route
            path={paths.adminMonitoring}
            element={
              <RequireAdmin>
                <Navigate to={`${paths.adminDashboard}?panel=cancel`} replace />
              </RequireAdmin>
            }
          />
          <Route
            path={paths.adminOperatorList}
            element={
              <RequireAdmin>
                <AdminOperatorList />
              </RequireAdmin>
            }
          />
          <Route
            path={paths.adminOperatorRegister}
            element={
                  <RequireAdmin>
                    <AdminPermissionRoute permission="OPERATOR_USER_CREATE" title="運用者追加" description="新しい運用者を登録します。">
                      <AdminOperatorRegister />
                    </AdminPermissionRoute>
              </RequireAdmin>
            }
          />
          <Route
            path={paths.adminOperatorProfileRoute}
            element={
                  <RequireAdmin>
                    <AdminPermissionRoute permission="OPERATOR_USER_READ" title="運用者プロフィール" description="運用者の登録情報を確認します。" allowSelfOperator>
                      <AdminOperatorProfile />
                    </AdminPermissionRoute>
              </RequireAdmin>
            }
          />
          <Route
            path={paths.adminOperatorEditRoute}
            element={
                  <RequireAdmin>
                    <AdminPermissionRoute permission="OPERATOR_USER_UPDATE" title="運用者更新" description="運用者の登録情報を更新します。" allowSelfOperator>
                      <AdminOperatorEdit />
                    </AdminPermissionRoute>
              </RequireAdmin>
            }
          />
          <Route
            path={paths.adminApplicationDetailRoute}
            element={
              <RequireAdmin>
                <AdminDashboard />
              </RequireAdmin>
            }
          />
          <Route
            path={paths.adminCompanyDetailRoute}
            element={
                  <RequireAdmin>
                    <AdminPermissionRoute permission="PARTICIPATING_COMPANY_READ" title="参加企業詳細" description="参加企業の登録情報を確認します。">
                      <AdminCompanyDetail />
                    </AdminPermissionRoute>
              </RequireAdmin>
            }
          />
          <Route
            path={paths.adminCancelRequestDetailRoute}
            element={
                  <RequireAdmin>
                    <AdminPermissionRoute permission="MONITORING_CANCEL" title="取消依頼詳細" description="取消依頼の内容を確認します。">
                      <AdminCancelRequestDetail />
                    </AdminPermissionRoute>
              </RequireAdmin>
            }
          />
          <Route
            path={paths.adminProjectRegister}
            element={
                  <RequireAdmin>
                    <AdminProjectRegisterRoute />
              </RequireAdmin>
            }
          />
          <Route
            path={paths.adminProjectDetailRoute}
            element={
                  <RequireAdmin>
                    <AdminPermissionRoute permission="PROJECT_READ" title="プロジェクト詳細" description="登録済みプロジェクトの進捗、排出量、関連データを確認します。">
                      <ProjectDetailAdmin />
                    </AdminPermissionRoute>
              </RequireAdmin>
            }
          />
          <Route
            path={paths.adminProjectInformationRoute}
            element={
                  <RequireAdmin>
                    <AdminPermissionRoute permission="PROJECT_READ" title="プロジェクト詳細情報" description="登録済みプロジェクトの詳細情報を確認します。">
                      <AdminProjectInformation />
                    </AdminPermissionRoute>
              </RequireAdmin>
            }
          />
          <Route
            path={paths.adminProjectMembershipRoute}
            element={
                  <RequireAdmin>
                    <AdminPermissionRoute permission="PARTICIPATING_COMPANY_APPLICATION_REVIEW" title="入会届一覧" description="レビュー対象となる入会届を確認します。">
                      <AdminParticipationReviewList />
                    </AdminPermissionRoute>
              </RequireAdmin>
            }
          />
          <Route
            path={paths.adminProjectMembershipDetailRoute}
            element={
                  <RequireAdmin>
                    <AdminPermissionRoute permission="PARTICIPATING_COMPANY_APPLICATION_REVIEW" title="入会届詳細" description="入会届の内容と審査状況を確認します。">
                      <AdminParticipationReviewDetail />
                    </AdminPermissionRoute>
              </RequireAdmin>
            }
          />
          <Route
            path={paths.adminProjectMonitoringDetailRoute}
            element={
                  <RequireAdmin>
                    <AdminPermissionRoute permission="MONITORING_READ" title="モニタリングデータ詳細" description="登録されたモニタリングデータを確認します。">
                      <AdminProjectMonitoringDetail />
                    </AdminPermissionRoute>
              </RequireAdmin>
            }
          />
          <Route
            path={paths.adminProjectEditRoute}
            element={
                  <RequireAdmin>
                    <AdminPermissionRoute permission="PROJECT_UPDATE" title="プロジェクト更新" description="登録済みプロジェクトの情報を更新します。">
                      <AdminProjectEdit />
                    </AdminPermissionRoute>
              </RequireAdmin>
            }
          />
          <Route
            path={paths.adminProjectMonitoringReportRoute}
            element={
                  <RequireAdmin>
                    <AdminPermissionRoute permission="MONITORING_READ" title="モニタリングデータ報告書" description="モニタリング結果の報告書を確認します。">
                      <AdminMonitoringReport />
                    </AdminPermissionRoute>
              </RequireAdmin>
            }
          />
          <Route path={paths.passwordChange} element={<PasswordChange />} />
          <Route path="*" element={<NotFound />} />
          <Route
            path={paths.adminHelpFaq}
            element={
              <AdminHelpFaqPage />
            }
          />
              </Routes>
            </Suspense>
        </div>
        </AppShell>
      </div>
      </AdminPermissionProvider>
    </ErrorBoundary>
  );
};

export default App;
