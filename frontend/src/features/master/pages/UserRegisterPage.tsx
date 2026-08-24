import { useEffect, useState } from 'react';
import { useLocation, useNavigate } from 'react-router-dom';
import { registerUser, validateCreateUser } from '@/api/users/mutations';
import {
  FormPageContent,
  FormPageTitle,
  FormStandalonePage,
} from '@/components/forms/form-layout';
import { PageBackButton } from '@/components/navigation/PageBackButton';
import { UserForm, UserFormData } from '@/features/master/forms/UserForm';
import UserConfirmation from '@/features/master/pages/UserConfirmation';
import {
  createSiteRegistrationFlowState,
  getSiteRegistrationFlowState,
} from '@/features/master/navigation/siteRegistrationFlow';
import { toast } from '@/hooks/use-toast';
import { useSitesQuery } from '@/hooks/sites/useSitesQuery';
import { logger } from '@/lib/logger';
import { useDashboardStore } from '@/stores/dashboardStore';
import { paths } from '@/routes/paths';

const emptyUserValues: UserFormData = {
  username: '',
  lastName: '',
  firstName: '',
  lastNameKana: '',
  firstNameKana: '',
  userPosition: '',
  phoneNumber: '',
  siteId: '',
  email: '',
  password: '',
};

export default function UserRegister() {
  const [step, setStep] = useState<'form' | 'confirm'>('form');
  const location = useLocation();
  const navigate = useNavigate();
  const { addUser, editSite, setSites } = useDashboardStore();
  const [formData, setFormData] = useState<UserFormData>();
  const sitesQuery = useSitesQuery();
  const registrationFlowState = getSiteRegistrationFlowState(location.state);
  const isSiteRegistrationFlow = Boolean(registrationFlowState);

  useEffect(() => {
    if (sitesQuery.data?.sites) {
      setSites(sitesQuery.data.sites);
    }
  }, [setSites, sitesQuery.data?.sites]);

  const handleDraftSubmit = async (values: UserFormData) => {
    try {
      await validateCreateUser(values);
      setFormData(values);
      setStep('confirm');
    } catch (error) {
      logger.error('User create validation error:', error);
      toast({
        title: 'エラー',
        description:
          error instanceof Error ? error.message : '社員登録内容の検証に失敗しました',
        variant: 'destructive',
      });
      throw error;
    }
  };

  const handleFinalSubmit = async () => {
    if (!formData) return;

    try {
      const res = await registerUser(formData);
      addUser(res.user);
      if (res.site?.siteId) {
        editSite(res.site.siteId, res.site);
      }

      toast({
        title: '成功',
        description: '社員を登録しました',
      });

      if (registrationFlowState) {
        navigate(paths.masterSiteRegister, {
          replace: true,
          state: createSiteRegistrationFlowState(
            registrationFlowState.siteDraft,
            res.user,
            registrationFlowState.returnTarget,
          ),
        });
        return;
      }

      navigate(paths.masterDashboard);
    } catch (error) {
      logger.error('Error creating user:', error);
      toast({
        title: 'エラー',
        description:
          error instanceof Error ? error.message : '社員登録に失敗しました',
        variant: 'destructive',
      });
    }
  };

  const handleConfirmBack = () => {
    setStep('form');
  };

  const handleFormBack = () => {
    if (registrationFlowState) {
      navigate(paths.masterSiteRegister, {
        replace: true,
        state: createSiteRegistrationFlowState(
          registrationFlowState.siteDraft,
          undefined,
          registrationFlowState.returnTarget,
        ),
      });
      return;
    }

    navigate(-1);
  };

  if (step === 'confirm') {
    return (
      <UserConfirmation
        mode="create"
        data={formData}
        originalData={emptyUserValues}
        onBack={handleConfirmBack}
        onSubmit={handleFinalSubmit}
        hideSiteSection={isSiteRegistrationFlow}
      />
    );
  }

  return (
    <FormStandalonePage>
      <PageBackButton
        onClick={handleFormBack}
        label=""
        className="absolute left-4 top-10 h-12 w-12 rounded-full p-0 sm:left-10 lg:left-16"
      />

      <FormPageContent>
        <FormPageTitle>社員追加</FormPageTitle>

        <UserForm
          mode="create"
          onChange={setFormData}
          onNext={handleDraftSubmit}
          onBack={handleFormBack}
          defaultValues={formData || emptyUserValues}
          originalData={emptyUserValues}
          siteOptions={sitesQuery.data?.sites ?? []}
          isSiteOptionsLoading={sitesQuery.isLoading}
          siteOptionsError={sitesQuery.isError ? '拠点を取得できませんでした' : null}
          hideSiteSection={isSiteRegistrationFlow}
          embedded
        />
      </FormPageContent>
    </FormStandalonePage>
  );
}
