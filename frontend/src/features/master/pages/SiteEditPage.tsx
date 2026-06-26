import { useEffect, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import type { Site } from '@/api/site';
import { deleteSite, updateSite } from '@/api/site';
import { SiteForm, SiteFormData } from '@/features/master/forms/SiteForm';
import { SiteConfirmation } from './SiteConfirmation';
import { toast } from '@/hooks/use-toast';
import { logger } from '@/lib/logger';
import { useDashboardStore } from '@/stores/dashboardStore';

function SiteEdit() {
  const navigate = useNavigate();
  const [step, setStep] = useState<'form' | 'confirm'>('form');

  const { id } = useParams<{ id: string }>();
  const sites = useDashboardStore((state) => state.sites) ?? [];
  const initialSite = sites.find((site) => site.id === id);

  const [formData, setFormData] = useState<SiteFormData | undefined>(
    undefined,
  );

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
