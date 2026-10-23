import { useNavigate } from 'react-router-dom';
import { AdminLoginForm } from '@/features/admin/auth/components/AdminLoginForm';
import { useAuth } from '@/contexts/AuthContext';
import type { AdminLoginFormData } from '@/lib/schemas/loginSchemas';
import { paths } from '@/routes/paths';

export default function AdminLoginPage() {
  const navigate = useNavigate();
  const { adminLogin } = useAuth();

  const handleAdminLogin = async (data: AdminLoginFormData) => {
    // Errors propagate as-is: AdminLoginForm reads the ApiError status and field errors.
    await adminLogin(data.identifier, data.password);
    navigate(paths.adminDashboard);
  };

  return <AdminLoginForm onSubmit={handleAdminLogin} />;
}
