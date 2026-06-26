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
    return <div>隱ｭ縺ｿ霎ｼ縺ｿ荳ｭ...</div>;
  }

  if (!initialSite) {
    return <div>隱ｭ縺ｿ霎ｼ縺ｿ荳ｭ...</div>;
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
            formData.blockName,
            formData.addressDetail,
          ]
            .filter(Boolean)
            .join(''),
      };

      editSite(id, updatedSite);

      toast({
        title: '謌仙粥',
        description: '諡轤ｹ繧堤ｷｨ髮・＠縺ｾ縺励◆',
      });

      navigate('/master/dashboard');
    } catch (error) {
      logger.error('SiteEdit handleSubmit error:', error);
      toast({
        title: '繧ｨ繝ｩ繝ｼ',
        description: '諡轤ｹ邱ｨ髮・↓螟ｱ謨励＠縺ｾ縺励◆',
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
        title: '繧ｨ繝ｩ繝ｼ',
        description: 'この拠点には削除できない利用データがあるため、削除できません。',
        variant: 'destructive',
      });
      return;
    }

    try {
      await deleteSite(id);
      deleteSiteStore(id);

      toast({
        title: '謌仙粥',
        description: '諡轤ｹ繧貞炎髯､縺励∪縺励◆',
      });

      navigate('/master/dashboard');
    } catch (error) {
      logger.error(error);
      toast({
        title: '繧ｨ繝ｩ繝ｼ',
        description: '蜑企勁荳ｭ縺ｫ繧ｨ繝ｩ繝ｼ縺檎匱逕溘＠縺ｾ縺励◆',
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
