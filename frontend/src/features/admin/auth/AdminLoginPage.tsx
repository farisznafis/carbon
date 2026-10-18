import { useNavigate } from 'react-router-dom';
import { AdminLoginForm } from '@/features/admin/auth/components/AdminLoginForm';
import { useAuth } from '@/contexts/AuthContext';
import type { AdminLoginFormData } from '@/lib/schemas/loginSchemas';
import { paths } from '@/routes/paths';

export default function AdminLoginPage() {
  const navigate = useNavigate();
  const { adminLogin } = useAuth();

  const handleAdminLogin = async (data: AdminLoginFormData) => {
    try {
      await adminLogin(data.identifier, data.password);
      navigate(paths.adminDashboard);
    } catch (error) {
      throw new Error(
        error instanceof Error ? error.message : '無効な管理者認証情報です',
      );
    }
  };

  return <AdminLoginForm onSubmit={handleAdminLogin} />;
}
