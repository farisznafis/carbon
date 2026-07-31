import { useNavigate } from 'react-router-dom';
import { useAuth } from '@/contexts/AuthContext';
import { UserLoginForm } from '@/components/forms/shared/UserLoginForm';
import type { UserLoginFormData } from '@/lib/schemas/loginSchemas';
import { paths } from '@/routes/paths';

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

  return <UserLoginForm onSubmit={handleUserLogin} />;
};

export default Login;
