import { useNavigate, useParams } from 'react-router-dom';
import { useEffect, useState } from 'react';
import { SiteForm, SiteFormData } from '@/features/master/forms/SiteForm';
import { SiteConfirmation } from './SiteConfirmation';
import { toast } from '@/hooks/use-toast';
import { useDashboardStore } from '@/stores/dashboardStore';
import { updateSite, deleteSite } from '@/api/site';
import type { Site } from '@/api/site';
import { logger } from '@/lib/logger';

function SiteEdit() {
  const navigate = useNavigate();
  const [step, setStep] = useState<'form' | 'confirm'>('form');

  const { id } = useParams<{ id: string }>();
  const sites = useDashboardStore((state) => state.sites) ?? [];
  const initialSite = sites.find((site) => site.id === id);

  const [formData, setFormData] = useState<SiteFormData | undefined>(undefined);

  useEffect(() => {
    if (initialSite) {
      setFormData(initialSite);
    }
  }, [initialSite]);

  const editSite = useDashboardStore((state) => state.editSite);
  const deleteSiteStore = useDashboardStore((state) => state.deleteSiteStore);
  const { joinableSites } = useDashboardStore();

  if (!id) {
    return <div>読み込み中...</div>;
  }

  if (!initialSite) {
    return <div>読み込み中...</div>;
  }

  const handleFormSubmit = () => {
    // if (!formData) return;
    setStep('confirm');
  };

  const handleFinalSubmit = async () => {
    if (!id || !formData) return;

    try {
      const result = await updateSite(id, formData);

      const updatedSite: Site = {
        ...initialSite,
        postalCode: formData.postalCode,
        prefectureName: formData.prefectureName,
        cityName: formData.cityName,
        blockName: formData.blockName,
        addressDetail: formData.addressDetail,
        address:
          result.site?.address ??
          [
            formData.prefectureName,
            formData.cityName,
            formData.townName,
            formData.blockName,
            formData.addressDetail,
          ]
            .filter(Boolean)
            .join(''),
      };

      editSite(id, updatedSite);

      toast({
        title: '成功',
        description: '拠点を編集しました',
      });

      navigate('/master/dashboard');
    } catch (error) {
      logger.error('SiteEdit handleSubmit error:', error);
      toast({
        title: 'エラー',
        description: '拠点編集に失敗しました',
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
        description: 'この拠点には割り当てられたユーザが存在するため、削除できません。',
        variant: 'destructive',
      });
      return;
    }

    try {
      await deleteSite(id);
      deleteSiteStore(id);

      toast({
        title: '成功',
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
          originalData={initialSite}
          onChange={setFormData}
          onNext={handleFormSubmit}
          defaultValues={formData || initialSite}
          onDelete={handleDelete}
        />
      )}

      {step === 'confirm' && (
        <SiteConfirmation
          mode="edit"
          data={formData}
          originalData={initialSite}
          onBack={handleBack}
          onSubmit={handleFinalSubmit}
        />
      )}
    </>
  );
}

export default SiteEdit;
