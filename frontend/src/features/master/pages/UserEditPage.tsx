import { useEffect, useState } from 'react';
import { useQueryClient } from '@tanstack/react-query';
import { useLocation, useNavigate, useParams } from 'react-router-dom';
import type { UserList } from '@/api/users';
import { updateUser, validateUpdateUser } from '@/api/users/mutations';
import {
  FormPageContent,
  FormPageTitle,
  FormStandalonePage,
} from '@/components/forms/form-layout';
import { usePageBreadcrumb } from '@/components/navigation/pageBreadcrumbContext';
import { useAuth } from '@/contexts/AuthContext';
import { formatPersonName } from '@/features/master/components/masterDashboardUtils';
import { UserForm, UserFormData } from '@/features/master/forms/UserForm';
import UserConfirmation from '@/features/master/pages/UserConfirmation';
import { toast } from '@/hooks/use-toast';
import { useSitesQuery } from '@/hooks/sites/useSitesQuery';
import { logger } from '@/lib/logger';
import { paths } from '@/routes/paths';
import { useDashboardStore } from '@/stores/dashboardStore';
import { getHomePath } from '@/utils/getHomePath';

type UserEditLocationState = {
  mode?: 'edit' | 'profile';
  returnTo?: string;
  user?: UserList;
};

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
  const { user } = useAuth();
  const locationState = location.state as UserEditLocationState | null;
  const storedUser = useDashboardStore((state) => state.getUser(id));
  const routedUser =
    locationState?.user?.id === id ? locationState.user : undefined;
  const initialUser = storedUser ?? routedUser;
  const { editUser, editSite, setSites } = useDashboardStore();
  const sitesQuery = useSitesQuery();
  const [formData, setFormData] = useState<UserFormData>(() =>
    mapUserToForm(initialUser),
  );
  const mode: 'edit' | 'profile' =
    locationState?.mode === 'profile' ? 'profile' : 'edit';
  const isMasterUser = initialUser?.role === 'master';
  const formattedUserName = formatPersonName(
    initialUser?.lastName,
    initialUser?.firstName,
  );
  const accountName =
    formattedUserName === '-'
      ? initialUser?.username || ''
      : formattedUserName;
  const profilePath =
    mode === 'profile' || !id ? paths.profile : paths.masterUserProfile(id);

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
        ? sitesQuery.data?.sites.find((site) => site.siteId === updatedSiteId)
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
        updatedSites.forEach((site) => editSite(site.siteId, site));
      }

      toast({
        title: '更新完了',
        description: '社員を更新しました',
      });

      navigate(
        locationState?.returnTo ??
          (mode === 'profile' ? paths.profile : paths.masterDashboard),
      );
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

  const handleConfirmBack = () => {
    setStep('form');
  };

  const handleFormBack = () => {
    if (mode === 'profile') {
      navigate(paths.profile);
      return;
    }

    navigate(-1);
  };

  usePageBreadcrumb([
    ...(mode === 'profile'
      ? [
          { label: 'ホーム', to: getHomePath(user) },
          { label: accountName || 'プロファイル', to: paths.profile },
        ]
      : [
          { label: '社員', to: `${paths.masterDashboard}?tab=users` },
          { label: accountName || '社員', to: profilePath },
        ]),
    step === 'confirm'
      ? { label: '編集', onClick: handleConfirmBack }
      : { label: '編集' },
    ...(step === 'confirm' ? [{ label: '確認' }] : []),
  ]);

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
      <FormPageContent>
        <div>
          <FormPageTitle as="h2">
            {mode === 'profile' ? 'プロファイルの編集' : '社員情報の編集'}
          </FormPageTitle>
          <p className="mt-2 text-sm leading-6 text-muted-foreground sm:text-base">
            {mode === 'profile'
              ? '登録されている個人情報とアカウント情報を編集します。'
              : '社員の個人情報とアカウント情報を編集します。'}
          </p>
        </div>

        <UserForm
          mode={mode}
          defaultValues={formData}
          originalData={mapUserToForm(initialUser)}
          onChange={setFormData}
          onNext={handleDraftSubmit}
          onBack={handleFormBack}
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
