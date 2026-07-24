import { useEffect, useMemo, useState } from 'react';
import { useLocation, useNavigate } from 'react-router-dom';
import { toast } from '@/hooks/use-toast';
import { useDashboardStore } from '@/stores/dashboardStore';
import { registerSite, validateCreateSite } from '@/api/site';
import { SiteForm, SiteFormData } from '@/features/master/forms/SiteForm';
import { SiteConfirmation } from './SiteConfirmation';
import { logger } from '@/lib/logger';
import { PageBackButton } from '@/components/navigation/PageBackButton';
import { useUsersQuery } from '@/hooks/users/useUsersQuery';
import {
  FormPageContent,
  FormPageTitle,
  FormStandalonePage,
} from '@/components/forms/form-layout';
import {
  createSiteRegistrationFlowState,
  getSiteRegistrationFlowState,
} from '@/features/master/navigation/siteRegistrationFlow';
import { paths } from '@/routes/paths';

const emptySiteValues: SiteFormData = {
  siteName: '',
  siteNameKana: '',
  postalCode: '',
  prefectureName: '',
  cityName: '',
  townName: '',
  blockName: '',
  addressDetail: '',
  picFirstName: '',
  picFirstNameKana: '',
  picLastName: '',
  picLastNameKana: '',
  picEmail: '',
  picPhoneNumber: '',
  picPosition: '',
  userId: '',
};

function SiteRegister() {
  const [step, setStep] = useState<'form' | 'confirm'>('form');
  const location = useLocation();
  const navigate = useNavigate();
  const addSite = useDashboardStore((state) => state.addSite);
  const setUsers = useDashboardStore((state) => state.setUsers);
  const registrationFlowState = getSiteRegistrationFlowState(location.state);
  const returnedUser = registrationFlowState?.createdUser;
  const [formData, setFormData] = useState<SiteFormData>(() => ({
    ...(registrationFlowState?.siteDraft ?? emptySiteValues),
    userId:
      returnedUser?.id ?? registrationFlowState?.siteDraft.userId ?? '',
  }));
  const usersQuery = useUsersQuery();
  const availableUsers = useMemo(
    () => {
      const fetchedUsers = usersQuery.data?.users ?? [];

      return returnedUser &&
        !fetchedUsers.some((user) => user.id === returnedUser.id)
        ? [...fetchedUsers, returnedUser]
        : fetchedUsers;
    },
    [returnedUser, usersQuery.data?.users],
  );
  const selectableUsers = useMemo(
    () =>
      availableUsers.filter(
        (user) =>
          user.role !== 'admin' &&
          (user.role === 'master' || user.siteId === null),
      ),
    [availableUsers],
  );

  useEffect(() => {
    if (availableUsers.length > 0 || usersQuery.data?.users) {
      setUsers(availableUsers);
    }
  }, [availableUsers, setUsers, usersQuery.data?.users]);

  const handleAddUser = (siteDraft: SiteFormData) => {
    setFormData(siteDraft);
    navigate(paths.masterUserRegister, {
      replace: true,
      state: createSiteRegistrationFlowState(siteDraft),
    });
  };

  const handleDraftSubmit = async (values: SiteFormData) => {
    try {
      await validateCreateSite(values);
      setFormData(values);
      setStep('confirm');
    } catch (error) {
      logger.error('Site create validation error:', error);
      toast({
        title: 'エラー',
        description:
          error instanceof Error ? error.message : '拠点登録内容の検証に失敗しました',
        variant: 'destructive',
      });
      throw error;
    }
  };

  const handleFinalSubmit = async () => {
    try {
      const assignee = formData.userId
        ? selectableUsers.find((user) => user.id === formData.userId)
        : null;
      const result = await registerSite(formData, assignee);
      addSite(result.site);

      toast({
        title: '成功',
        description: '拠点を登録しました',
      });

      navigate(paths.masterDashboard);
    } catch (error) {
      logger.error('Error creating site:', error);
      toast({
        title: 'エラー',
        description:
          error instanceof Error ? error.message : '拠点登録に失敗しました',
        variant: 'destructive',
      });
    }
  };

  const handleBack = () => {
    setStep('form');
  };

  if (step === 'confirm') {
    return (
      <SiteConfirmation
        mode='create'
        data={formData}
        originalData={emptySiteValues}
        onBack={handleBack}
        onSubmit={handleFinalSubmit}
      />
    );
  }

  return (
    <FormStandalonePage>
      <PageBackButton
        onClick={() => navigate(-1)}
        label=''
        className='absolute left-4 top-10 h-12 w-12 rounded-full p-0 sm:left-10 lg:left-16'
      />

      <FormPageContent>
        <FormPageTitle>拠点追加</FormPageTitle>

        <SiteForm
          mode='create'
          onChange={setFormData}
          onNext={handleDraftSubmit}
          defaultValues={formData}
          originalData={emptySiteValues}
          userOptions={selectableUsers}
          isUserOptionsLoading={usersQuery.isLoading && !returnedUser}
          userOptionsError={
            usersQuery.isError && !returnedUser
              ? '責任者候補を取得できませんでした'
              : null
          }
          userOptionsHelperText="マスターおよび拠点に紐づいていない社員のみ表示されます。"
          onAddUser={handleAddUser}
          embedded
        />
      </FormPageContent>
    </FormStandalonePage>
  );
}

export default SiteRegister;
