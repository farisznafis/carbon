import { useNavigate } from 'react-router-dom';
import { useAuth } from '@/contexts/AuthContext';
import { UserLoginForm } from '@/components/forms/shared/UserLoginForm';
import type { UserLoginFormData } from '@/lib/schemas/loginSchemas';
import { paths } from '@/routes/paths';
import { LogIn } from 'lucide-react';

const Login = () => {
  const navigate = useNavigate();
  const { login } = useAuth();

  const handleUserLogin = async (data: UserLoginFormData) => {
    const response = await login(data.companyId, data.email, data.password);

    if (response.role === 'master') {
      navigate(paths.masterDashboard);
      return;
    }

    if (response.siteId) {
      navigate(paths.siteDashboard(response.siteId));
      return;
    }

    navigate(paths.userDashboard);
  };

  return (
    <UserLoginForm
      onSubmit={handleUserLogin}
      header={
        <header className="text-center">
          <div className="mx-auto mb-4 flex h-12 w-12 items-center justify-center rounded-xl bg-secondary text-primary">
            <LogIn className="h-6 w-6" />
          </div>
          <h2>ログイン</h2>
          <p className="mt-3 text-sm font-medium leading-6 text-muted-foreground">
            GXデータの登録・管理を続けるにはログインしてください。
          </p>
        </header>
      }
    />
  );
};

export default Login;
