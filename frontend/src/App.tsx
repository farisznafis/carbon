import { Toaster } from "@/components/ui/toaster";
import { Toaster as Sonner } from "@/components/ui/sonner";
import { lazy, Suspense, useEffect } from "react";
import {
  Routes,
  Route,
  Navigate,
  useParams,
  useSearchParams,
} from "react-router-dom";
import { Navbar } from "@/components/layout/Navbar";
import { ScrollToTop } from "./components/layout/ScrollToTop";
import { SessionExpiryGuard } from "@/components/layout/SessionExpiryGuard";
import { useAuth } from "@/contexts/AuthContext";
import { ProtectedRoute } from "./contexts/ProtectedRoute";
import { ErrorBoundary } from "@/components/feedback/ErrorBoundary";
import { paths } from "@/routes/paths";
import Lenis from "lenis";
import "lenis/dist/lenis.css";

const Index = lazy(() => import("./pages/Index"));
const Login = lazy(() => import("./pages/Login"));
const AdminLogin = lazy(() => import("./features/admin/auth/AdminLoginPage"));
const ForgotPassword = lazy(() => import("./pages/ForgotPassword"));
const Signup = lazy(() => import("./pages/Signup"));
const ProjectDetail = lazy(() => import("./features/user/project-detail/ProjectDetailPage"));
const MonitoringRegister = lazy(() => import("./features/user/monitoring/MonitoringRegisterPage"));
const SiteRegister = lazy(() => import("./features/master/pages/SiteRegisterPage"));
const SiteEdit = lazy(() => import("./features/master/pages/SiteEditPage"));
const UserRegister = lazy(() => import("./features/master/pages/UserRegisterPage"));
const UserEdit = lazy(() => import("./features/master/pages/UserEditPage"));
const MasterUserProfile = lazy(() => import("./features/master/pages/MasterUserProfilePage"));
const SiteDetail = lazy(() => import("./features/user/sites/SiteDetailPage"));
const ProjectMembership = lazy(() => import("./features/user/project-registration/ProjectMembershipPage"));
const ProjectRegister = lazy(() => import("./features/user/project-registration/ProjectRegisterPage"));
const EquipmentForm = lazy(() => import("./features/user/project-registration/EquipmentFormPage"));
const EquipmentConfirmation = lazy(() =>
  import("./features/user/project-registration/EquipmentConfirmationPage"),
);
const MasterDashboard = lazy(() => import("./features/master/pages/MasterDashboardPage"));
const NotFound = lazy(() => import("./pages/NotFound"));
const Dashboard = lazy(() => import("./features/user/dashboard/DashboardPage"));
const PasswordChange = lazy(() => import("./features/user/profile/PasswordChangePage"));
const EquipmentPage = lazy(() =>
  import("./features/user/project-registration/components/EquipmentPage").then((module) => ({
    default: module.EquipmentPage,
  })),
);
const Profile = lazy(() => import("./features/user/profile/ProfilePage"));
const AdminDashboard = lazy(() => import("./features/admin/dashboard/AdminDashboardPage"));
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
const AdminProjectPlanApplication = lazy(() =>
  import("./features/admin/project-detail/AdminProjectPlanApplicationPage"),
);
const AdminMonitoringReport = lazy(() =>
  import("./features/admin/project-detail/AdminMonitoringReportPage"),
);
const AdminProjectMonitoringDetail = lazy(() =>
  import("./features/admin/monitoring/ProjectMonitoringDetailPage"),
);
const AdminHelpFaqPage = lazy(() => import("./features/master/help/AdminHelpFaqPage"));

function RequireAdmin({ children }: { children: JSX.Element }) {
  const { isAuthenticated } = useAuth();
  return isAuthenticated ? children : <Navigate to={paths.adminLogin} replace />;
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
  useEffect(() => {
    const lenis = new Lenis({
      autoRaf: true,
      autoToggle: true,
      anchors: true,
      allowNestedScroll: true,
      naiveDimensions: true,
      stopInertiaOnNavigate: true,
    });

    return () => {
      lenis.destroy();
    };
  }, []);

  return (
    <ErrorBoundary>
      <div className="min-h-screen bg-background text-foreground">
        <Toaster />
        <Sonner />
        <Navbar />
        <ScrollToTop />
        <SessionExpiryGuard />
        <Suspense fallback={<div className="p-6 text-sm text-muted-foreground">Loading...</div>}>
          <Routes>
          <Route path={paths.home} element={<Index />} />
          <Route path={paths.login} element={<Login />} />
          <Route path={paths.adminLogin} element={<AdminLogin />} />
          <Route path={paths.forgotPassword} element={<ForgotPassword />} />
          <Route path={paths.signup} element={<Signup />} />
          <Route element={<ProtectedRoute />}>
            <Route path={paths.masterDashboard} element={<MasterDashboard />} />
            <Route path={paths.masterSiteRegister} element={<SiteRegister />} />
            <Route path={paths.masterSiteEditRoute} element={<SiteEdit />} />
            <Route path={paths.masterUserRegister} element={<UserRegister />} />
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
            <Route path={paths.equipmentAddRoute} element={<EquipmentPage />} />
            <Route path={paths.siteProjectDetailRoute} element={<ProjectDetail />} />
            <Route path={paths.legacyProjectRoute} element={<LegacyProjectRedirect />} />
            <Route
              path={paths.monitoringRegisterRoute}
              element={<MonitoringRegister />}
            />
            <Route path={paths.siteDetailRoute} element={<SiteDetail />} />
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
                <AdminCompanyDetail />
              </RequireAdmin>
            }
          />
          <Route
            path={paths.adminCancelRequestDetailRoute}
            element={
              <RequireAdmin>
                <AdminCancelRequestDetail />
              </RequireAdmin>
            }
          />
          <Route
            path={paths.adminProjectRegister}
            element={
              <RequireAdmin>
                <AdminProjectRegister />
              </RequireAdmin>
            }
          />
          <Route
            path={paths.adminProjectDetailRoute}
            element={
              <RequireAdmin>
                <ProjectDetailAdmin />
              </RequireAdmin>
            }
          />
          <Route
            path={paths.adminProjectMonitoringDetailRoute}
            element={
              <RequireAdmin>
                <AdminProjectMonitoringDetail />
              </RequireAdmin>
            }
          />
          <Route
            path={paths.adminProjectEditRoute}
            element={
              <RequireAdmin>
                <AdminProjectEdit />
              </RequireAdmin>
            }
          />
          <Route
            path={paths.adminProjectPlanRoute}
            element={
              <RequireAdmin>
                <AdminProjectPlanApplication />
              </RequireAdmin>
            }
          />
          <Route
            path={paths.adminProjectMonitoringReportRoute}
            element={
              <RequireAdmin>
                <AdminMonitoringReport />
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
    </ErrorBoundary>
  );
};

export default App;
