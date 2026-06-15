import { useNavigate } from 'react-router-dom';
import { useEffect, useState } from 'react';
import { toast } from '@/hooks/use-toast';
import { useDashboardStore } from '@/stores/dashboardStore';
import { getProfile, type ProfileViewModel } from '@/api/profile';
import { registerSite } from '@/api/site';
import { SiteForm, SiteFormData } from '@/features/master/forms/SiteForm';
import { SiteConfirmation } from './SiteConfirmation';
import { logger } from '@/lib/logger';

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
};

const mapProfileToSitePicValues = (profile: ProfileViewModel): Pick<
  SiteFormData,
  | 'picFirstName'
  | 'picFirstNameKana'
  | 'picLastName'
  | 'picLastNameKana'
  | 'picEmail'
  | 'picPhoneNumber'
  | 'picPosition'
> => ({
  picFirstName: profile.firstName,
  picFirstNameKana: profile.firstNameKana,
  picLastName: profile.lastName,
  picLastNameKana: profile.lastNameKana,
  picEmail: profile.email,
  picPhoneNumber: profile.phoneNumber,
  picPosition: profile.userPosition,
});

function SiteRegister() {
  const [step, setStep] = useState<'form' | 'confirm'>('form');
  const navigate = useNavigate();
  const addSite = useDashboardStore((state) => state.addSite);
  const [formData, setFormData] = useState<SiteFormData>(emptySiteValues);
  const [originalData, setOriginalData] = useState<SiteFormData>(emptySiteValues);
  const [isProfileLoading, setIsProfileLoading] = useState(true);

  useEffect(() => {
    let isMounted = true;

    const loadProfile = async () => {
      try {
        setIsProfileLoading(true);
        const profile = await getProfile();
        if (!isMounted) return;

        const picValues = mapProfileToSitePicValues(profile);
        const nextValues = {
          ...emptySiteValues,
          ...picValues,
        };

        setFormData(nextValues);
        setOriginalData(nextValues);
      } catch (error) {
        logger.error('Error loading profile for site register:', error);
        if (!isMounted) return;

        toast({
          title: 'エラー',
          description: '担当者情報の取得に失敗しました',
          variant: 'destructive',
        });
      } finally {
        if (isMounted) {
          setIsProfileLoading(false);
        }
      }
    };

    loadProfile();

    return () => {
      isMounted = false;
    };
  }, []);

  const handleDraftSubmit = () => {
    setStep('confirm');
  };

  const handleFinalSubmit = async () => {
    try {
      const result = await registerSite(formData);
      addSite(result.site);

      toast({
        title: '成功',
        description: '拠点を登録しました',
      });

      navigate('/master/dashboard');
    } catch (error) {
      logger.error('Error creating site:', error);
      toast({
        title: 'エラー',
        description: '拠点登録に失敗しました',
        variant: 'destructive',
      });
    }
  };

  const handleBack = () => {
    setStep('form');
  };

  return (
    <div>
      {step === 'form' && (
        <SiteForm
          mode='create'
          onChange={setFormData}
          onNext={handleDraftSubmit}
          defaultValues={formData}
          originalData={originalData}
          disablePicFields
          disableSubmit={isProfileLoading}
        />
      )}

      {step === 'confirm' && (
        <SiteConfirmation
          mode='create'
          data={formData}
          originalData={originalData}
          onBack={handleBack}
          onSubmit={handleFinalSubmit}
        />
      )}
    </div>
  );
}

export default SiteRegister;
