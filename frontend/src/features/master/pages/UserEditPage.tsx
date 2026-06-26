import { useEffect, useState } from 'react';
import { useLocation, useNavigate, useParams } from 'react-router-dom';
import type { Site } from '@/api/site';
import type { UserList } from '@/api/users';
import { deleteUser, updateUser } from '@/api/users/mutations';
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
  const userWithPosition = user as (UserList & { userPosition?: string }) | undefined;

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

export default function UserEdit() {
  const [step, setStep] = useState<'form' | 'confirm'>('form');
  const navigate = useNavigate();
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

  useEffect(() => {
    if (sitesQuery.data?.sites) {
      setSites(sitesQuery.data.sites);
    }
  }, [setSites, sitesQuery.data?.sites]);

  const handleDraftSubmit = () => {
    setStep('confirm');
  };

  const handleFinalSubmit = async () => {
    if (!id || !formData || !initialUser) return;

    const {
      password: _password,
      passwordConfirmation: _passwordConfirmation,
      ...payload
    } = formData;

    try {
      const { user: updatedUser, updatedSites } = await updateUser(id, payload);
      editUser(id, updatedUser);

      if (updatedSites?.length) {
        updatedSites.forEach((site) => editSite(site.id, site));
      }

      toast({
        title: '成功',
        description: 'ユーザーを更新しました',
      });

      navigate('/master/dashboard');
    } catch (error) {
      logger.error('Error updating user:', error);
      toast({
        title: 'エラー',
        description: 'ユーザー更新に失敗しました',
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
        title: '成功',
        description: 'ユーザーを削除しました',
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
          {mode === 'profile' ? 'プロフィール編集' : 'ユーザー更新'}
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
          siteOptionsError={sitesQuery.isError ? '拠点を取得できませんでした' : null}
          embedded
        />
      </FormPageContent>
    </FormStandalonePage>
  );
}
