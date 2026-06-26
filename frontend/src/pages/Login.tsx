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

    navigate(paths.siteDashboard(response.siteId ?? ''));
  };

  return (
    <UserLoginForm
      onSubmit={handleUserLogin}
      header={
        <header className="text-center">
          <div className="mx-auto mb-5 flex h-16 w-16 items-center justify-center rounded-xl bg-secondary text-primary">
            <LogIn className="h-8 w-8" />
          </div>
          <h1>ログイン</h1>
          <p className="mt-4 text-[20px] font-medium leading-[28px] text-foreground">
            GXデータの登録・管理を続けるにはログインしてください。
          </p>
        </header>
      }
    />
  );
};

export default Login;
