import { useNavigate, useParams, useLocation } from 'react-router-dom';
import { UserForm, UserFormData } from '@/features/master/forms/UserForm';
import { deleteUser, updateUser } from '@/api/users/mutations';
import { toast } from '@/hooks/use-toast';
import { useDashboardStore } from '@/stores/dashboardStore';
import { useEffect, useState } from 'react';
import { userSchema } from '@/lib/validations';
import UserConfirmation from './UserConfirmation';
import type { Site } from '@/api/site';
import { logger } from '@/lib/logger';
import { useSitesQuery } from '@/hooks/sites/useSitesQuery';

export default function UserEdit() {
  const [step, setStep] = useState<"form" | "confirm">("form");
  const navigate = useNavigate();
  const location = useLocation(); 
  const { id } = useParams();
  const initialUser = useDashboardStore(s => s.getUser(id));
  const { editUser, editSite, deleteUserStore, setSites } = useDashboardStore();
  const sitesQuery = useSitesQuery();

  useEffect(() => {
    if (sitesQuery.data?.sites) {
      setSites(sitesQuery.data.sites);
    }
  }, [setSites, sitesQuery.data?.sites]);

  const mapUserToForm = (user: typeof initialUser): UserFormData => ({
    username: user?.username ?? '',
    lastName: user?.lastName ?? '',
    firstName: user?.firstName ?? '',
    lastNameKana: user?.lastNameKana ?? '',
    firstNameKana: user?.firstNameKana ?? '',
    phoneNumber: user?.phoneNumber ?? '',
    userPosition: user?.position ?? (user as any)?.userPosition ?? '',
    email: user?.email ?? '',
    password: '',
    passwordConfirmation: '',
    siteId: user?.siteId ?? '',
  });
  const [formData, setFormData] = useState<UserFormData>(() => mapUserToForm(initialUser));
  const mode = location.state?.mode ?? 'edit';
  // Called when the form is submitted
  const handleDraftSubmit = () => {
    setStep("confirm");
  };

  // Called when the user confirms
  const handleFinalSubmit = async () => {
    if (!formData || !initialUser) return;

    const {
      password: _password,
      passwordConfirmation: _passwordConfirmation,
      ...payload
    } = formData;

    try {
      const { user: updatedUser, updatedSites } = await updateUser(id, payload);

      //update user in store
      editUser(id, updatedUser);

      //update any affected sites
      if (updatedSites?.length) {
        updatedSites.forEach(site => editSite(site.id, site));
      }

      toast({
        title: '成功',
        description: 'ユーザを編集しました',
      });

      navigate('/master/dashboard');
    } catch {
      toast({
        title: 'エラー',
        description: 'ユーザ編集に失敗しました',
        variant: 'destructive',
      });
    }
  };

  const handleDelete = async () => {
    if (!id) return;
    try {
      await deleteUser(id, initialUser?.siteId ? ({ id: initialUser.siteId } as Site) : undefined)
        .then((res) => {
          deleteUserStore(id); //update dashboard store
          if (res.site) {
            editSite(res.site.id, res.site);
          }
        })

      toast({
        title: '成功',
        description: 'ユーザを削除しました',
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
    setStep("form");
  };

  const handleFormBack = () => {
    navigate(-1);
  }

  if (!initialUser) {
    return <div>読み込み中...</div>;
  }

  return (
    <>
      {step === "form" && (
        <UserForm
          mode={mode}
          defaultValues={formData}
          originalData={mapUserToForm(initialUser)}
          onChange={setFormData}
          onNext={handleDraftSubmit}
          onBack={handleFormBack}
          onDelete={handleDelete}
          schema={userSchema}
          siteOptions={sitesQuery.data?.sites ?? []}
          isSiteOptionsLoading={sitesQuery.isLoading}
          siteOptionsError={sitesQuery.isError ? '拠点を取得できませんでした' : null}
        />
      )}

      {step === "confirm" && (
        <UserConfirmation
          data={formData}
          originalData={mapUserToForm(initialUser)}
          onBack={handleConfirmBack}
          onSubmit={handleFinalSubmit}
        />
      )}

    </>
  );
}
