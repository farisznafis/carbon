import { useNavigate } from 'react-router-dom';
import { useAuth } from '@/contexts/AuthContext';
import { UserLoginForm } from '@/components/forms/shared/UserLoginForm';
import type { UserLoginFormData } from '@/lib/schemas/loginSchemas';

const Login = () => {
  const navigate = useNavigate();
  const { login } = useAuth();

  const handleUserLogin = async (data: UserLoginFormData) => {
    const response = await login(data.companyId, data.email, data.password);

    if (response.role === 'master') {
      navigate('/master/dashboard');
      return;
    }

    navigate(`/sites/${response.siteId}/dashboard`);
  };

  return <UserLoginForm onSubmit={handleUserLogin} />;
};

export default Login;
