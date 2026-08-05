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
import { PageBackButton } from '@/components/navigation/PageBackButton';
import { Button } from '@/components/ui/button';
import type { SolarPowerFormData } from '@/lib/equipmentValidations';

type EquipmentEditConfirmationProps = {
  current: SolarPowerFormData;
  original: SolarPowerFormData;
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

export function EquipmentEditConfirmation({
  current,
  original,
  isSubmitting,
  onBack,
  onConfirm,
}: EquipmentEditConfirmationProps) {
  return (
    <FormStandalonePage>
      <PageBackButton
        onClick={onBack}
        label=""
        className="absolute left-4 top-10 h-12 w-12 rounded-full p-0 sm:left-10 lg:left-16"
      />

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
                  label="稼働開始日（電力受給開始日）"
                  value={displayValue(current.equipmentOpDate)}
                  originalValue={displayValue(original.equipmentOpDate)}
                />
                <ConfirmationItem
                  label="公称最大出力 (kW)"
                  value={displayValue(current.nominalMaxOutput)}
                  originalValue={displayValue(original.nominalMaxOutput)}
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
                  label="導入台数 (台)"
                  value={displayValue(current.powerCondUnitCount)}
                  originalValue={displayValue(original.powerCondUnitCount)}
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
                  </>
                )}
                <ConfirmationItem
                  label="プロジェクト実施前の電力購入単価 (円/kWh)"
                  value={displayValue(current.preProjectUnitPrice)}
                  originalValue={displayValue(original.preProjectUnitPrice)}
                />
              </FormGrid>
            </FormSection>

            <FormActions className="grid grid-cols-1 gap-3 sm:grid-cols-2">
              <Button
                type="button"
                variant="outline"
                size="md"
                className="w-full"
                onClick={onBack}
                disabled={isSubmitting}
              >
                戻る
              </Button>
              <Button
                type="button"
                size="md"
                className="w-full"
                onClick={onConfirm}
                disabled={isSubmitting}
              >
                {isSubmitting ? '変更中...' : '変更する'}
              </Button>
            </FormActions>
          </FormSectionStack>
        </FormCard>
      </FormPageContent>
    </FormStandalonePage>
  );
}
