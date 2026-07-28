import { useState } from 'react';
import { useLocation, useNavigate, useParams } from 'react-router-dom';
import {
  FormActions,
  FormCard,
  FormGrid,
  FormPageContent,
  FormPageTitle,
  FormSection,
  FormSectionStack,
  FormStandalonePage,
} from '@/components/forms/form-layout';
import { ConfirmationItem } from '@/components/feedback/Confirmation/ConfirmationItem';
import { PageBackButton } from '@/components/navigation/PageBackButton';
import { Button } from '@/components/ui/button';
import {
  uploadSiteProjectEquipmentFile,
  type SiteProjectEquipmentValidationFile,
} from '@/api/site';
import type { Equipment } from '@/features/user/project-registration/components/types';
import {
  getEquipmentConfirmationState,
  type EquipmentConfirmationState,
} from '@/features/user/project-registration/equipmentConfirmationState';
import { masterDashboardProjectMembershipState } from '@/features/user/project-registration/projectMembershipNavigation';
import { getSolarFormSelectedFiles } from '@/features/user/project-registration/equipmentValidationMapper';
import { useSitesQuery } from '@/hooks/sites/useSitesQuery';
import { useToast } from '@/hooks/use-toast';
import type { SolarPowerFormData } from '@/lib/equipmentValidations';
import { paths } from '@/routes/paths';

const displayValue = (value?: string | null) => value?.trim() || 'なし';

const displayYesNo = (value: SolarPowerFormData['hasBattery']) => {
  if (value === 'yes') return 'あり';
  if (value === 'no') return 'なし';
  return '未選択';
};

const fileName = (file?: File) => file?.name ?? 'なし';

function withoutLocalFiles(data: SolarPowerFormData): SolarPowerFormData {
  return {
    ...data,
    operationStartDateFile: undefined,
    nominalMaxOutputFile: undefined,
    pcsFile: undefined,
    batteryFile: undefined,
    totalInvestmentFile: undefined,
    subsidyNameFile: undefined,
    preProjectUnitPriceFile: undefined,
  };
}

function saveEquipment(
  projectId: string,
  confirmationState: EquipmentConfirmationState,
  files: SiteProjectEquipmentValidationFile[],
): Extract<Equipment, { formType: 'solar' }> {
  const storageKey = `project_${projectId}_equipment`;
  const pendingEquipment: Extract<Equipment, { formType: 'solar' }> = {
    id: confirmationState.equipmentId,
    formType: 'solar',
    solarData: confirmationState.formData,
    files,
  };
  const storedEquipmentData: Equipment = {
    ...pendingEquipment,
    solarData: withoutLocalFiles(confirmationState.formData),
  };
  localStorage.setItem(storageKey, JSON.stringify([storedEquipmentData]));
  return pendingEquipment;
}

export default function EquipmentConfirmationPage() {
  const navigate = useNavigate();
  const location = useLocation();
  const { siteId = '', projectId = '' } = useParams<{
    siteId: string;
    projectId: string;
  }>();
  const { toast } = useToast();
  const sitesQuery = useSitesQuery();
  const [isSubmitting, setIsSubmitting] = useState(false);
  const confirmationState = getEquipmentConfirmationState(location.state);
  const data = confirmationState?.formData;
  const selectedSite = sitesQuery.data?.sites.find(
    (site) => site.siteId === data?.siteId,
  );

  const navigateBack = () => {
    const equipmentIdQuery = confirmationState?.isEdit
      ? `?equipmentId=${encodeURIComponent(confirmationState.equipmentId)}`
      : '';
    navigate(`${paths.projectEquipment(siteId, projectId)}${equipmentIdQuery}`, {
      replace: true,
      state: {
        ...(confirmationState?.fromMasterDashboard
          ? masterDashboardProjectMembershipState
          : {}),
        returnTo: confirmationState?.returnTo,
        equipmentDraft: data,
      },
    });
  };

  const handleRegister = async () => {
    if (!confirmationState || !data || isSubmitting) return;
    setIsSubmitting(true);

    try {
      const uploadedFiles = await Promise.all(
        getSolarFormSelectedFiles(data).map(({ fileType, file }) =>
          uploadSiteProjectEquipmentFile(data.siteId, fileType, file),
        ),
      );
      const pendingEquipment = saveEquipment(
        projectId,
        confirmationState,
        uploadedFiles,
      );
      navigate(paths.projectRegister(siteId, projectId), {
        replace: true,
        state: {
          ...(confirmationState.fromMasterDashboard
            ? masterDashboardProjectMembershipState
            : {}),
          pendingEquipment,
        },
      });
    } catch (error) {
      toast({
        title: '設備情報を登録できませんでした',
        description:
          error instanceof Error
            ? error.message
            : '設備情報の保存に失敗しました。',
        variant: 'destructive',
      });
    } finally {
      setIsSubmitting(false);
    }
  };

  if (!data) {
    return (
      <FormStandalonePage>
        <FormPageContent>
          <FormPageTitle>設備登録の確認</FormPageTitle>
          <FormCard className="space-y-5 text-center">
            <p className="text-muted-foreground">確認する設備情報がありません。</p>
            <Button type="button" onClick={navigateBack}>
              設備入力へ戻る
            </Button>
          </FormCard>
        </FormPageContent>
      </FormStandalonePage>
    );
  }

  return (
    <FormStandalonePage>
      <PageBackButton
        onClick={navigateBack}
        label=""
        className="absolute left-4 top-10 h-12 w-12 rounded-full p-0 sm:left-10 lg:left-16"
      />

      <FormPageContent>
        <div className="space-y-3">
          <FormPageTitle>設備登録の確認</FormPageTitle>
          <p className="text-small text-muted-foreground">
            内容をご確認ください。
          </p>
        </div>

        <FormCard>
          <FormSectionStack spacing="relaxed">
            <FormSection number={1} title="対象拠点">
              <ConfirmationItem
                label="拠点"
                value={selectedSite?.siteName ?? data.siteId}
              />
            </FormSection>

            <FormSection number={2} title="太陽光発電設備の情報">
              <FormGrid>
                <ConfirmationItem label="稼働開始日" value={displayValue(data.equipmentOpDate)} />
                <ConfirmationItem
                  label="確認資料"
                  value={fileName(data.operationStartDateFile)}
                />
                <ConfirmationItem
                  label="公称最大出力 (kW)"
                  value={displayValue(data.nominalMaxOutput)}
                />
                <ConfirmationItem
                  label="パネル確認資料"
                  value={fileName(data.nominalMaxOutputFile)}
                />
              </FormGrid>
            </FormSection>

            <FormSection number={3} title="パワーコンディショナーの情報">
              <FormGrid>
                <ConfirmationItem label="メーカー" value={displayValue(data.pcsManufacturer)} />
                <ConfirmationItem label="型式" value={displayValue(data.pcsModel)} />
                <ConfirmationItem label="導入台数" value={displayValue(data.powerCondUnitCount)} />
                <ConfirmationItem label="機器固有番号" value={displayValue(data.deviceNo)} />
                <ConfirmationItem label="夜間待機電力 (W)" value={displayValue(data.powerCondStandby)} />
                <ConfirmationItem label="確認資料" value={fileName(data.pcsFile)} />
              </FormGrid>
            </FormSection>

            <FormSection number={4} title="蓄電池・追加設備の情報">
              <FormGrid>
                <ConfirmationItem label="蓄電池の有無" value={displayYesNo(data.hasBattery)} />
                {data.hasBattery === 'yes' && (
                  <>
                    <ConfirmationItem label="蓄電池メーカー" value={displayValue(data.batteryMaker)} />
                    <ConfirmationItem label="蓄電池型式" value={displayValue(data.batteryModel)} />
                    <ConfirmationItem label="容量 (kWh)" value={displayValue(data.batteryCapacity)} />
                    <ConfirmationItem label="蓄電池確認資料" value={fileName(data.batteryFile)} />
                  </>
                )}
                <ConfirmationItem
                  label="追加設備の有無"
                  value={displayYesNo(data.additionalEquipment)}
                />
                {data.additionalEquipment === 'yes' && (
                  <>
                    <ConfirmationItem label="導入日" value={displayValue(data.additionalEquipmentDate)} />
                    <ConfirmationItem label="設備種別" value={displayValue(data.additionalEquipmentType)} />
                    <ConfirmationItem label="導入台数" value={displayValue(data.additionalUnitCount)} />
                    <ConfirmationItem label="機器固有番号" value={displayValue(data.additionalDeviceUniqueNo)} />
                    <ConfirmationItem label="ICT設備消費電力 (W)" value={displayValue(data.ictEquipmentPowerConsumption)} />
                  </>
                )}
              </FormGrid>
            </FormSection>

            <FormSection number={5} title="追加性評価に必要な情報">
              <FormGrid>
                <ConfirmationItem label="設備の総投資額 (円)" value={displayValue(data.totalInvestment)} />
                <ConfirmationItem label="総投資額確認資料" value={fileName(data.totalInvestmentFile)} />
                <ConfirmationItem label="補助金受給の有無" value={displayYesNo(data.hasSubsidy)} />
                {data.hasSubsidy === 'yes' && (
                  <>
                    <ConfirmationItem label="補助金名称" value={displayValue(data.subsidyName)} />
                    <ConfirmationItem label="補助金額 (円)" value={displayValue(data.subsidyAmount)} />
                    <ConfirmationItem label="補助金確認資料" value={fileName(data.subsidyNameFile)} />
                  </>
                )}
                <ConfirmationItem
                  label="プロジェクト実施前の電力購入単価 (円/kWh)"
                  value={displayValue(data.preProjectUnitPrice)}
                />
                <ConfirmationItem
                  label="電力購入単価確認資料"
                  value={fileName(data.preProjectUnitPriceFile)}
                />
              </FormGrid>
            </FormSection>

            <FormActions className="grid grid-cols-1 gap-3 sm:grid-cols-2">
              <Button
                type="button"
                variant="outline"
                size="sm"
                className="w-full"
                onClick={navigateBack}
                disabled={isSubmitting}
              >
                戻る
              </Button>
              <Button
                type="button"
                size="sm"
                className="w-full"
                onClick={handleRegister}
                disabled={isSubmitting}
              >
                {isSubmitting ? '登録中...' : '登録'}
              </Button>
            </FormActions>
          </FormSectionStack>
        </FormCard>
      </FormPageContent>
    </FormStandalonePage>
  );
}
