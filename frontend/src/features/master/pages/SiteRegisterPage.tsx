import { useEffect, useMemo, useState } from 'react';
import { useQueryClient } from '@tanstack/react-query';
import { useLocation, useNavigate } from 'react-router-dom';
import { toast } from '@/hooks/use-toast';
import { useDashboardStore } from '@/stores/dashboardStore';
import { registerSite, validateCreateSite, type Site } from '@/api/site';
import { SiteForm, SiteFormData } from '@/features/master/forms/SiteForm';
import { SiteConfirmation } from './SiteConfirmation';
import { logger } from '@/lib/logger';
import { useUsersQuery } from '@/hooks/users/useUsersQuery';
import {
  FormPageContent,
  FormPageTitle,
  FormStandalonePage,
} from '@/components/forms/form-layout';
import { usePageBreadcrumb } from '@/components/navigation/pageBreadcrumbContext';
import {
  getSiteRegistrationReturnTarget,
} from '@/features/master/navigation/siteRegistrationFlow';
import { paths } from '@/routes/paths';
import { UserInvitationDialog } from '@/features/master/user-invitations/UserInvitationDialog';

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
  const [isInvitationDialogOpen, setIsInvitationDialogOpen] = useState(false);
  const location = useLocation();
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const addSite = useDashboardStore((state) => state.addSite);
  const setUsers = useDashboardStore((state) => state.setUsers);
  const registrationReturnTarget = getSiteRegistrationReturnTarget(
    location.state,
  );
  const [formData, setFormData] = useState<SiteFormData>(emptySiteValues);
  const usersQuery = useUsersQuery();
  const availableUsers = useMemo(
    () => usersQuery.data?.users ?? [],
    [usersQuery.data?.users],
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

  /** Preserves the site draft and opens the invitation without creating a user inline. */
  const handleAddUser = (siteDraft: SiteFormData) => {
    setFormData(siteDraft);
    setIsInvitationDialogOpen(true);
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
      queryClient.setQueryData<{ sites: Site[] }>(['sites'], (current) => ({
        sites: [
          ...(current?.sites ?? []).filter(
            (site) => site.siteId !== result.site.siteId,
          ),
          result.site,
        ],
      }));

      toast({
        title: '成功',
        description: '拠点を登録しました',
      });

      if (registrationReturnTarget) {
        navigate(registrationReturnTarget.path, {
          replace: true,
          state: {
            ...registrationReturnTarget.state,
            createdSiteId: result.site.siteId,
          },
        });
        return;
      }

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

  /** Returns to the originating form with its preserved draft when available. */
  const handlePageBack = () => {
    if (registrationReturnTarget) {
      navigate(registrationReturnTarget.path, {
        replace: true,
        state: registrationReturnTarget.state,
      });
      return;
    }

    navigate(-1);
  };

  usePageBreadcrumb([
    registrationReturnTarget
      ? { label: 'プロジェクト参加申請', onClick: handlePageBack }
      : { label: '拠点', to: `${paths.masterDashboard}?tab=sites` },
    { label: '拠点追加' },
  ]);

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
      <FormPageContent>
        <FormPageTitle>拠点追加</FormPageTitle>

        <SiteForm
          mode='create'
          onChange={setFormData}
          onNext={handleDraftSubmit}
          defaultValues={formData}
          originalData={emptySiteValues}
          userOptions={selectableUsers}
          isUserOptionsLoading={usersQuery.isLoading}
          userOptionsError={
            usersQuery.isError
              ? '責任者候補を取得できませんでした'
              : null
          }
          userOptionsHelperText="マスターおよび拠点に紐づいていない社員のみ表示されます。"
          onAddUser={handleAddUser}
          embedded
        />
      </FormPageContent>
      <UserInvitationDialog
        open={isInvitationDialogOpen}
        onOpenChange={setIsInvitationDialogOpen}
      />
    </FormStandalonePage>
  );
}

export default SiteRegister;
