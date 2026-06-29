import { useEffect, useMemo, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import type { Site } from '@/api/site';
import { deleteSite, updateSite } from '@/api/site';
import { SiteForm, SiteFormData } from '@/features/master/forms/SiteForm';
import { SiteConfirmation } from './SiteConfirmation';
import { toast } from '@/hooks/use-toast';
import { logger } from '@/lib/logger';
import { useDashboardStore } from '@/stores/dashboardStore';
import { useUsersQuery } from '@/hooks/users/useUsersQuery';

const mapSiteToFormData = (site: Site): SiteFormData => ({
  siteName: site.siteName,
  siteNameKana: '',
  postalCode: site.postalCode,
  prefectureName: site.prefectureName,
  cityName: site.cityName,
  townName: '',
  blockName: site.blockName,
  addressDetail: site.addressDetail ?? '',
  picFirstName: site.picFirstName,
  picFirstNameKana: '',
  picLastName: site.picLastName,
  picLastNameKana: '',
  picEmail: site.picEmail,
  picPhoneNumber: site.picPhoneNumber ?? '',
  picPosition: site.picPosition,
  userId: site.picId ?? '',
});

function SiteEdit() {
  const navigate = useNavigate();
  const [step, setStep] = useState<'form' | 'confirm'>('form');

  const { id } = useParams<{ id: string }>();
  const sites = useDashboardStore((state) => state.sites) ?? [];
  const initialSite = sites.find((site) => site.id === id);
  const initialFormData = initialSite ? mapSiteToFormData(initialSite) : undefined;

  const [formData, setFormData] = useState<SiteFormData | undefined>(
    undefined,
  );

  useEffect(() => {
    if (initialSite) {
      setFormData(mapSiteToFormData(initialSite));
    }
  }, [initialSite]);

  const editSite = useDashboardStore((state) => state.editSite);
  const deleteSiteStore = useDashboardStore((state) => state.deleteSiteStore);
  const setUsers = useDashboardStore((state) => state.setUsers);
  const { joinableSites } = useDashboardStore();
  const usersQuery = useUsersQuery();
  const users = useMemo(
    () => usersQuery.data?.users ?? [],
    [usersQuery.data?.users],
  );

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
            user.siteId === id ||
            user.id === formData?.userId),
      ),
    [users, id, formData?.userId],
  );

  if (!id) {
    return <div>読み込み中...</div>;
  }

  if (!initialSite) {
    return <div>読み込み中...</div>;
  }

  const handleFormSubmit = () => {
    setStep('confirm');
  };

  const handleFinalSubmit = async () => {
    if (!id || !formData) return;

    const picuserId = formData.userId ? Number(formData.userId) : null;
    const selectedUser = formData.userId
      ? users.find((user) => user.id === formData.userId)
      : null;

    try {
      const result = await updateSite(id, formData, picuserId);

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

      editSite(id, updatedSite);

      toast({
        title: '更新完了',
        description: '拠点を更新しました',
      });

      navigate('/master/dashboard');
    } catch (error) {
      logger.error('SiteEdit handleSubmit error:', error);
      toast({
        title: 'エラー',
        description: '拠点更新に失敗しました',
        variant: 'destructive',
      });
    }
  };

  const handleBack = () => {
    setStep('form');
  };

  const handleDelete = async () => {
    if (!id) return;

    if (!joinableSites().some((site) => site.id === id)) {
      toast({
        title: 'エラー',
        description:
          'この拠点には削除できない利用データがあるため、削除できません。',
        variant: 'destructive',
      });
      return;
    }

    try {
      await deleteSite(id);
      deleteSiteStore(id);

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
            usersQuery.isError ? '担当者候補を取得できませんでした' : null
          }
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
