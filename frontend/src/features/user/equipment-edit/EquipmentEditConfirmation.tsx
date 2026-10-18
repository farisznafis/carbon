import { ConfirmationItem } from '@/components/feedback/Confirmation/ConfirmationItem';
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
import { Button } from '@/components/ui/button';
import type {
  SiteProjectEquipmentFile,
  SiteProjectEquipmentFileType,
} from '@/api/site';
import type { SolarPowerFormData } from '@/lib/equipmentValidations';
import { getEquipmentFileDisplayName } from './equipmentEditMapper';

type EquipmentEditConfirmationProps = {
  current: SolarPowerFormData;
  original: SolarPowerFormData;
  existingFiles: SiteProjectEquipmentFile[];
  isSubmitting: boolean;
  onBack: () => void;
  onConfirm: () => void;
};

function displayValue(value: string) {
  return value || '-';
}

function displayYesNo(value: SolarPowerFormData['hasBattery']) {
  if (value === 'yes') return 'あり';
  if (value === 'no') return 'なし';
  return '-';
}

function existingFileValue(
  files: SiteProjectEquipmentFile[],
  fileType: SiteProjectEquipmentFileType,
) {
  const file = files.find((candidate) => candidate.fileType === fileType);
  return file ? getEquipmentFileDisplayName(file) : '-';
}

function FileConfirmationItem({
  label,
  selectedFile,
  existingFiles,
  fileType,
}: {
  label: string;
  selectedFile?: File | null;
  existingFiles: SiteProjectEquipmentFile[];
  fileType: SiteProjectEquipmentFileType;
}) {
  const originalValue = existingFileValue(existingFiles, fileType);
  return (
    <ConfirmationItem
      label={label}
      value={selectedFile?.name ?? originalValue}
      originalValue={originalValue}
    />
  );
}

export function EquipmentEditConfirmation({
  current,
  original,
  existingFiles,
  isSubmitting,
  onBack,
  onConfirm,
}: EquipmentEditConfirmationProps) {
  return (
    <FormStandalonePage>
      <FormPageContent className="animate-slide-up">
        <div className="space-y-3">
          <FormPageTitle>設備変更の確認</FormPageTitle>
          <p className="text-small text-foreground">
            変更内容をご確認ください。
          </p>
        </div>

        <FormCard>
          <FormSectionStack spacing="relaxed">
            <FormSection number={1} title="太陽光発電設備の情報">
              <FormGrid>
                <ConfirmationItem
                  label="変更日"
                  value={displayValue(current.changeDate ?? '')}
                />
                <FileConfirmationItem
                  label="電力受給契約確認書"
                  selectedFile={current.operationStartDateFile}
                  existingFiles={existingFiles}
                  fileType="solar_contract"
                />
                <ConfirmationItem
                  label="公称最大出力 (kW)"
                  value={displayValue(current.nominalMaxOutput)}
                  originalValue={displayValue(original.nominalMaxOutput)}
                />
                <FileConfirmationItem
                  label="パネル仕様書・カタログ"
                  selectedFile={current.nominalMaxOutputFile}
                  existingFiles={existingFiles}
                  fileType="panel_catalog"
                />
              </FormGrid>
            </FormSection>

            <FormSection number={2} title="パワーコンディショナーの情報">
              <FormGrid>
                <ConfirmationItem
                  label="メーカー"
                  value={displayValue(current.pcsManufacturer)}
                  originalValue={displayValue(original.pcsManufacturer)}
                />
                <ConfirmationItem
                  label="型式"
                  value={displayValue(current.pcsModel)}
                  originalValue={displayValue(original.pcsModel)}
                />
                <ConfirmationItem
                  label="機器固有番号（製造番号）"
                  value={displayValue(current.deviceNo)}
                  originalValue={displayValue(original.deviceNo)}
                />
                <ConfirmationItem
                  label="夜間待機電力 (W)"
                  value={displayValue(current.powerCondStandby)}
                  originalValue={displayValue(original.powerCondStandby)}
                />
                <FileConfirmationItem
                  label="PCS仕様書・カタログ"
                  selectedFile={current.pcsFile}
                  existingFiles={existingFiles}
                  fileType="pcs_spec"
                />
              </FormGrid>
            </FormSection>

            <FormSection number={3} title="蓄電池の情報">
              <FormGrid>
                <ConfirmationItem
                  label="蓄電池設置の有無"
                  value={displayYesNo(current.hasBattery)}
                  originalValue={displayYesNo(original.hasBattery)}
                />
                {current.hasBattery === 'yes' && (
                  <>
                    <ConfirmationItem
                      label="メーカー"
                      value={displayValue(current.batteryMaker)}
                      originalValue={displayValue(original.batteryMaker)}
                    />
                    <ConfirmationItem
                      label="型式"
                      value={displayValue(current.batteryModel)}
                      originalValue={displayValue(original.batteryModel)}
                    />
                    <ConfirmationItem
                      label="容量 (kWh)"
                      value={displayValue(current.batteryCapacity)}
                      originalValue={displayValue(original.batteryCapacity)}
                    />
                    <FileConfirmationItem
                      label="蓄電池仕様書・カタログ"
                      selectedFile={current.batteryFile}
                      existingFiles={existingFiles}
                      fileType="battery_catalog"
                    />
                  </>
                )}
              </FormGrid>
            </FormSection>

            <FormSection number={4} title="追加性評価に必要な情報">
              <FormGrid>
                <ConfirmationItem
                  label="設備の総投資額 (円)"
                  value={displayValue(current.totalInvestment)}
                  originalValue={displayValue(original.totalInvestment)}
                />
                <FileConfirmationItem
                  label="投資額確認資料"
                  selectedFile={current.totalInvestmentFile}
                  existingFiles={existingFiles}
                  fileType="construction_invoice"
                />
                <ConfirmationItem
                  label="補助金受給の有無"
                  value={displayYesNo(current.hasSubsidy)}
                  originalValue={displayYesNo(original.hasSubsidy)}
                />
                {current.hasSubsidy === 'yes' && (
                  <>
                    <ConfirmationItem
                      label="補助金名称"
                      value={displayValue(current.subsidyName)}
                      originalValue={displayValue(original.subsidyName)}
                    />
                    <ConfirmationItem
                      label="補助金額 (円)"
                      value={displayValue(current.subsidyAmount)}
                      originalValue={displayValue(original.subsidyAmount)}
                    />
                    <FileConfirmationItem
                      label="補助金交付決定通知書"
                      selectedFile={current.subsidyNameFile}
                      existingFiles={existingFiles}
                      fileType="subsidy_notice"
                    />
                  </>
                )}
                <ConfirmationItem
                  label="プロジェクト実施前の電力購入単価 (円/kWh)"
                  value={displayValue(current.preProjectUnitPrice)}
                  originalValue={displayValue(original.preProjectUnitPrice)}
                />
                <FileConfirmationItem
                  label="電力購入単価確認資料"
                  selectedFile={current.preProjectUnitPriceFile}
                  existingFiles={existingFiles}
                  fileType="electricity_bill"
                />
              </FormGrid>
            </FormSection>

            <FormActions
              pendingActionId={isSubmitting ? 'confirm' : null}
              actions={[
                {
                  id: 'back',
                  label: '戻る',
                  variant: 'outline',
                  onClick: onBack,
                },
                {
                  id: 'confirm',
                  label: '変更する',
                  pendingLabel: '変更中...',
                  onClick: onConfirm,
                },
              ]}
            />
          </FormSectionStack>
        </FormCard>
      </FormPageContent>
    </FormStandalonePage>
  );
}
