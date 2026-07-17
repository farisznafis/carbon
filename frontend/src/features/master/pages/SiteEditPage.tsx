import { useEffect, useMemo, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import type { Site } from '@/api/site';
import type { UserList } from '@/api/users';
import { deleteSite, updateSite, validateUpdateSite } from '@/api/site';
import { SiteForm, SiteFormData } from '@/features/master/forms/SiteForm';
import { SiteConfirmation } from './SiteConfirmation';
import { toast } from '@/hooks/use-toast';
import { logger } from '@/lib/logger';
import { useDashboardStore } from '@/stores/dashboardStore';
import { useUsersQuery } from '@/hooks/users/useUsersQuery';

const resolveSiteAssigneeId = (site: Site, users: UserList[] = []) => {
  if (site.picId) return site.picId;

  const siteAssignee = users.find(
    (user) =>
      user.role !== 'admin' &&
      (user.siteId === site.siteId || user.role === 'master') &&
      user.firstName === site.picFirstName &&
      user.lastName === site.picLastName,
  );

  return siteAssignee?.id ?? '';
};

const mapSiteToFormData = (
  site: Site,
  users?: UserList[],
): SiteFormData => ({
  siteName: site.siteName,
  siteNameKana: '',
  postalCode: site.postalCode,
  prefectureName: site.prefectureName,
  cityName: site.cityName,
  townName: '',
  blockName: site.blockName,
  addressDetail: site.addressDetail ?? site.buildingName ?? '',
  picFirstName: site.picFirstName,
  picFirstNameKana: '',
  picLastName: site.picLastName,
  picLastNameKana: '',
  picEmail: site.picEmail,
  picPhoneNumber: site.picPhoneNumber ?? '',
  picPosition: site.picPosition,
  userId: resolveSiteAssigneeId(site, users),
});

function SiteEdit() {
  const navigate = useNavigate();
  const [step, setStep] = useState<'form' | 'confirm'>('form');

  const { siteId } = useParams<{ siteId: string }>();
  const sites = useDashboardStore((state) => state.sites) ?? [];
  const initialSite = sites.find((site) => site.siteId === siteId);
  const editSite = useDashboardStore((state) => state.editSite);
  const deleteSiteStore = useDashboardStore((state) => state.deleteSiteStore);
  const setUsers = useDashboardStore((state) => state.setUsers);
  const storedUsers = useDashboardStore((state) => state.users);
  const { joinableSites } = useDashboardStore();
  const usersQuery = useUsersQuery();
  const users = useMemo(
    () => (storedUsers.length > 0 ? storedUsers : usersQuery.data?.users ?? []),
    [storedUsers, usersQuery.data?.users],
  );
  const initialFormData = useMemo(
    () => (initialSite ? mapSiteToFormData(initialSite, users) : undefined),
    [initialSite, users],
  );

  const [formData, setFormData] = useState<SiteFormData | undefined>(
    undefined,
  );

  useEffect(() => {
    if (initialFormData) {
      setFormData((current) => {
        if (!current) return initialFormData;

        if (!current.userId && initialFormData.userId) {
          return {
            ...current,
            userId: initialFormData.userId,
          };
        }

        return current;
      });
    }
  }, [initialFormData]);

  useEffect(() => {
    if (usersQuery.data?.users) {
      setUsers(usersQuery.data.users);
    }
  }, [setUsers, usersQuery.data?.users]);

  const selectableUsers = useMemo(
    () =>
      users.filter(
        (user) =>
          user.role !== 'admin' &&
          (user.role === 'master' ||
            user.siteId === siteId ||
            user.id === formData?.userId),
      ),
    [users, siteId, formData?.userId],
  );

  if (!siteId) {
    return <div>読み込み中...</div>;
  }

  if (!initialSite) {
    return <div>読み込み中...</div>;
  }

  const handleFormSubmit = async (values: SiteFormData) => {
    if (!siteId) return;

    const picuserId = values.userId ? Number(values.userId) : null;

    try {
      await validateUpdateSite(siteId, values, picuserId);
      setFormData(values);
      setStep('confirm');
    } catch (error) {
      logger.error('Site update validation error:', error);
      toast({
        title: 'エラー',
        description:
          error instanceof Error ? error.message : '拠点更新内容の検証に失敗しました',
        variant: 'destructive',
      });
      throw error;
    }
  };

  const handleFinalSubmit = async () => {
    if (!siteId || !formData) return;

    const picuserId = formData.userId ? Number(formData.userId) : null;
    const selectedUser = formData.userId
      ? users.find((user) => user.id === formData.userId)
      : null;

    try {
      const result = await updateSite(siteId, formData, picuserId);

      const updatedSite: Site = {
        ...initialSite,
        siteName: formData.siteName,
        postalCode: formData.postalCode,
        prefectureName: formData.prefectureName,
        cityName: formData.cityName,
        blockName: formData.blockName,
        addressDetail: formData.addressDetail,
        buildingName: formData.addressDetail,
        picId: formData.userId ?? initialSite.picId,
        picFirstName: selectedUser?.firstName ?? initialSite.picFirstName,
        picLastName: selectedUser?.lastName ?? initialSite.picLastName,
        picEmail: selectedUser?.email ?? initialSite.picEmail,
        picPosition: selectedUser?.position ?? initialSite.picPosition,
        picPhoneNumber: selectedUser?.phoneNumber ?? initialSite.picPhoneNumber,
        address:
          result.site?.address ??
          [
            formData.prefectureName,
            formData.cityName,
            formData.blockName,
            formData.addressDetail,
          ]
            .filter(Boolean)
            .join(''),
      };

      editSite(siteId, updatedSite);

      toast({
        title: '更新完了',
        description: '拠点を更新しました',
      });

      navigate('/master/dashboard');
    } catch (error) {
      logger.error('SiteEdit handleSubmit error:', error);
      toast({
        title: 'エラー',
        description:
          error instanceof Error ? error.message : '拠点更新に失敗しました',
        variant: 'destructive',
      });
    }
  };

  const handleBack = () => {
    setStep('form');
  };

  const handleDelete = async () => {
    if (!siteId) return;

    if (!joinableSites().some((site) => site.siteId === siteId)) {
      toast({
        title: 'エラー',
        description:
          'この拠点には削除できない利用データがあるため、削除できません。',
        variant: 'destructive',
      });
      return;
    }

    try {
      await deleteSite(siteId);
      deleteSiteStore(siteId);

      toast({
        title: '削除完了',
        description: '拠点を削除しました',
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

  return (
    <>
      {step === 'form' && (
        <SiteForm
          mode="edit"
          originalData={initialFormData}
          onChange={setFormData}
          onNext={handleFormSubmit}
          defaultValues={formData || initialFormData}
          onDelete={handleDelete}
          userOptions={selectableUsers}
          isUserOptionsLoading={usersQuery.isLoading}
          userOptionsError={
            usersQuery.isError ? '責任者候補を取得できませんでした' : null
          }
          userOptionsHelperText="この拠点に紐づいている社員のみ表示されます。"
        />
      )}

      {step === 'confirm' && (
        <SiteConfirmation
          mode="edit"
          data={formData}
          originalData={initialFormData}
          onBack={handleBack}
          onSubmit={handleFinalSubmit}
        />
      )}
    </>
  );
}

export default SiteEdit;
