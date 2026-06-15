import { useNavigate } from 'react-router-dom';
import { AdminLoginForm } from '@/features/admin/auth/components/AdminLoginForm';
import { useAuth } from '@/contexts/AuthContext';
import type { AdminLoginFormData } from '@/lib/schemas/loginSchemas';

export default function AdminLoginPage() {
  const navigate = useNavigate();
  const { adminLogin } = useAuth();

  const handleAdminLogin = async (data: AdminLoginFormData) => {
    try {
      await adminLogin(data.identifier, data.password);
      navigate('/admin/dashboard');
    } catch (error) {
      throw new Error(
        error instanceof Error
          ? error.message
          : '無効な運営者・管理者認証情報です',
      );
    }
  };

  return <AdminLoginForm onSubmit={handleAdminLogin} />;
}
