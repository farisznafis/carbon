import { useEffect, useState } from 'react';
import { useQueryClient } from '@tanstack/react-query';
import { useLocation, useNavigate, useParams } from 'react-router-dom';
import type { Site } from '@/api/site';
import type { UserList } from '@/api/users';
import { deleteUser, updateUser, validateUpdateUser } from '@/api/users/mutations';
import {
  FormPageContent,
  FormPageTitle,
  FormStandalonePage,
} from '@/components/forms/form-layout';
import { PageBackButton } from '@/components/navigation/PageBackButton';
import { UserForm, UserFormData } from '@/features/master/forms/UserForm';
import UserConfirmation from '@/features/master/pages/UserConfirmation';
import { toast } from '@/hooks/use-toast';
import { useSitesQuery } from '@/hooks/sites/useSitesQuery';
import { logger } from '@/lib/logger';
import { useDashboardStore } from '@/stores/dashboardStore';

const mapUserToForm = (user: UserList | undefined): UserFormData => {
  const userWithPosition = user as
    | (UserList & { userPosition?: string })
    | undefined;

  return {
    username: user?.username ?? '',
    lastName: user?.lastName ?? '',
    firstName: user?.firstName ?? '',
    lastNameKana: user?.lastNameKana ?? '',
    firstNameKana: user?.firstNameKana ?? '',
    phoneNumber: user?.phoneNumber ?? '',
    userPosition: user?.position ?? userWithPosition?.userPosition ?? '',
    email: user?.email ?? '',
    password: '',
    passwordConfirmation: '',
    siteId: user?.siteId ?? '',
  };
};

const mapUserEditPayload = (
  formValues: UserFormData,
  isMasterUser: boolean,
) => {
  const {
    password: _password,
    passwordConfirmation: _passwordConfirmation,
    username: _username,
    ...payload
  } = formValues;

  return isMasterUser
    ? {
        ...payload,
        siteId: null,
      }
    : payload;
};

export default function UserEdit() {
  const [step, setStep] = useState<'form' | 'confirm'>('form');
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const location = useLocation();
  const { id } = useParams();
  const initialUser = useDashboardStore((state) => state.getUser(id));
  const { editUser, editSite, deleteUserStore, setSites } = useDashboardStore();
  const sitesQuery = useSitesQuery();
  const [formData, setFormData] = useState<UserFormData>(() =>
    mapUserToForm(initialUser),
  );
  const mode: 'edit' | 'profile' =
    location.state?.mode === 'profile' ? 'profile' : 'edit';
  const isMasterUser = initialUser?.role === 'master';

  useEffect(() => {
    if (sitesQuery.data?.sites) {
      setSites(sitesQuery.data.sites);
    }
  }, [setSites, sitesQuery.data?.sites]);

  const handleDraftSubmit = async (values: UserFormData) => {
    if (!id || !initialUser) return;

    const payloadWithMasterSiteRule = mapUserEditPayload(
      values,
      Boolean(isMasterUser),
    );

    try {
      await validateUpdateUser(id, payloadWithMasterSiteRule, initialUser);
      setFormData(values);
      setStep('confirm');
    } catch (error) {
      logger.error('User update validation error:', error);
      toast({
        title: 'エラー',
        description:
          error instanceof Error ? error.message : '社員更新内容の検証に失敗しました',
        variant: 'destructive',
      });
      throw error;
    }
  };

  const handleFinalSubmit = async () => {
    if (!id || !formData || !initialUser) return;

    const payloadWithMasterSiteRule = mapUserEditPayload(
      formData,
      Boolean(isMasterUser),
    );

    try {
      const { user: updatedUser, updatedSites } = await updateUser(
        id,
        payloadWithMasterSiteRule,
        initialUser,
      );
      const updatedSiteId = payloadWithMasterSiteRule.siteId || null;
      const updatedSite = updatedSiteId
        ? sitesQuery.data?.sites.find((site) => site.id === updatedSiteId)
        : undefined;
      const mergedUpdatedUser: UserList = {
        ...initialUser,
        ...updatedUser,
        siteId: updatedSiteId,
        siteName: updatedSiteId ? (updatedUser.siteName ?? updatedSite?.siteName) : undefined,
      };

      editUser(id, mergedUpdatedUser);
      queryClient.setQueryData<{ statusCode: string; users: UserList[] }>(
        ['users'],
        (current) =>
          current
            ? {
                ...current,
                users: current.users.map((user) =>
                  user.id === id ? { ...user, ...mergedUpdatedUser } : user,
                ),
              }
            : current,
      );

      if (updatedSites?.length) {
        updatedSites.forEach((site) => editSite(site.id, site));
      }

      toast({
        title: '更新完了',
        description: '社員を更新しました',
      });

      navigate('/master/dashboard');
    } catch (error) {
      logger.error('Error updating user:', error);
      toast({
        title: 'エラー',
        description:
          error instanceof Error ? error.message : '社員更新に失敗しました',
        variant: 'destructive',
      });
    }
  };

  const handleDelete = async () => {
    if (!id) return;

    try {
      const result = await deleteUser(
        id,
        initialUser?.siteId ? ({ id: initialUser.siteId } as Site) : undefined,
      );

      deleteUserStore(id);

      if (result.site) {
        editSite(result.site.id, result.site);
      }

      toast({
        title: '削除完了',
        description: '社員を削除しました',
      });

      navigate('/master/dashboard');
    } catch (error) {
      logger.error(error);
      toast({
        title: 'エラー',
        description: '削除中にエラーが発生しました',
        variant: 'destructive',
      });
    }
  };

  const handleConfirmBack = () => {
    setStep('form');
  };

  const handleFormBack = () => {
    navigate(-1);
  };

  if (!id || !initialUser) {
    return <div>読み込み中...</div>;
  }

  if (step === 'confirm') {
    return (
      <UserConfirmation
        mode="edit"
        data={formData}
        originalData={mapUserToForm(initialUser)}
        onBack={handleConfirmBack}
        onSubmit={handleFinalSubmit}
        hideSiteSection={isMasterUser}
        hideUsernameField
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
        <FormPageTitle>
          {mode === 'profile' ? 'プロフィール編集' : '社員更新'}
        </FormPageTitle>

        <UserForm
          mode={mode}
          defaultValues={formData}
          originalData={mapUserToForm(initialUser)}
          onChange={setFormData}
          onNext={handleDraftSubmit}
          onBack={handleFormBack}
          onDelete={handleDelete}
          siteOptions={sitesQuery.data?.sites ?? []}
          isSiteOptionsLoading={sitesQuery.isLoading}
          siteOptionsError={
            sitesQuery.isError ? '拠点を取得できませんでした' : null
          }
          hideSiteSection={isMasterUser}
          hideUsernameField
          embedded
        />
      </FormPageContent>
    </FormStandalonePage>
  );
}
