import type { SiteProjectEquipmentFileType } from '@/api/site';
import { ConfirmationItem } from '@/components/feedback/Confirmation/ConfirmationItem';
import {
  FormActions,
  FormCard,
  FormGrid,
  FormSection,
  FormSectionStack,
} from '@/components/forms/form-layout';
import { Button } from '@/components/ui/button';
import type { AgreementFormValues } from './agreementsConfig';
import type { ContractStepFormValues } from './ContractStep';
import type { FinalTermsValues } from './FinalTermsStep';
import type { SolarEquipment } from './types';

type ProjectRegistrationReviewStepProps = {
  projectName: string;
  siteName: string;
  equipments: SolarEquipment[];
  methodologyNumber: string;
  methodologyVersion: string;
  agreementValues: Partial<AgreementFormValues>;
  contractValues: Partial<ContractStepFormValues>;
  finalTermsValues: Partial<FinalTermsValues>;
  isSubmitting: boolean;
  onBack: () => void;
  onSubmit: () => void;
};

// Converts optional text into the shared confirmation placeholder.
function displayText(value?: string | null): string {
  return value?.trim() || '-';
}

// Converts an optional boolean into a readable Japanese value.
function displayBoolean(value?: boolean | null): string {
  if (value === true) return 'はい';
  if (value === false) return 'いいえ';
  return '-';
}

// Resolves a stored select value to its user-facing label.
function displaySelection(
  value: string | undefined,
  labels: Record<string, string>,
): string {
  return labels[value ?? ''] ?? '-';
}

// Combines methodology number and version without an empty separator.
function displayMethodologyReference(
  methodologyNumber: string,
  methodologyVersion: string,
): string {
  return [methodologyNumber, methodologyVersion].filter(Boolean).join(' / ') || '-';
}

// Shows a local file selected before submit or an uploaded file name.
function displayEquipmentFile(
  equipment: SolarEquipment,
  fileType: SiteProjectEquipmentFileType,
  localFile?: File,
): string {
  if (localFile instanceof File) return localFile.name;

  return equipment.files.find((file) => file.fileType === fileType)?.fileName ?? '-';
}

// Renders the complete read-only review before participation is submitted.
export function ProjectRegistrationReviewStep({
  projectName,
  siteName,
  equipments,
  methodologyNumber,
  methodologyVersion,
  agreementValues,
  contractValues,
  finalTermsValues,
  isSubmitting,
  onBack,
  onSubmit,
}: ProjectRegistrationReviewStepProps) {
  return (
    <div className="space-y-6">
      <header className="space-y-2">
        <h2 className="text-foreground">申請内容の確認</h2>
        <p className="text-small text-foreground">
          入力内容をご確認の上、申請してください。
        </p>
      </header>

      <FormCard>
        <FormSectionStack spacing="relaxed">
          <FormSection number={1} title="申請対象">
            <FormGrid>
              <ConfirmationItem label="プロジェクト名" value={displayText(projectName)} />
              <ConfirmationItem label="対象拠点" value={displayText(siteName)} />
              <ConfirmationItem
                label="方法論番号・バージョン"
                value={displayMethodologyReference(
                  methodologyNumber,
                  methodologyVersion,
                )}
              />
            </FormGrid>
          </FormSection>

          <FormSection number={2} title="太陽光発電設備">
            <div className="space-y-8">
              {equipments.map((equipment, index) => (
                <section key={equipment.id} className="space-y-5">
                  {equipments.length > 1 && (
                    <h4 className="font-semibold text-foreground">
                      設備 {index + 1}
                    </h4>
                  )}

                  <FormGrid>
                    <ConfirmationItem
                      label="稼働開始日"
                      value={displayText(equipment.solarData.equipmentOpDate)}
                    />
                    <ConfirmationItem
                      label="確認資料：太陽光発電電力受給契約確認書"
                      value={displayEquipmentFile(
                        equipment,
                        'solar_contract',
                        equipment.solarData.operationStartDateFile,
                      )}
                    />
                    <ConfirmationItem
                      label="公称最大出力 (kW)"
                      value={displayText(equipment.solarData.nominalMaxOutput)}
                    />
                    <ConfirmationItem
                      label="確認資料：パネル仕様書・カタログ"
                      value={displayEquipmentFile(
                        equipment,
                        'panel_catalog',
                        equipment.solarData.nominalMaxOutputFile,
                      )}
                    />
                    <ConfirmationItem
                      label="PCSメーカー"
                      value={displayText(equipment.solarData.pcsManufacturer)}
                    />
                    <ConfirmationItem
                      label="PCS型式"
                      value={displayText(equipment.solarData.pcsModel)}
                    />
                    <ConfirmationItem
                      label="機器固有番号"
                      value={displayText(equipment.solarData.deviceNo)}
                    />
                    <ConfirmationItem
                      label="夜間待機電力 (W)"
                      value={displayText(equipment.solarData.powerCondStandby)}
                    />
                    <ConfirmationItem
                      label="確認資料：PCS仕様書・カタログ"
                      value={displayEquipmentFile(
                        equipment,
                        'pcs_spec',
                        equipment.solarData.pcsFile,
                      )}
                    />
                    <ConfirmationItem
                      label="蓄電池の有無"
                      value={displaySelection(equipment.solarData.hasBattery, {
                        yes: 'あり',
                        no: 'なし',
                      })}
                    />
                    {equipment.solarData.hasBattery === 'yes' && (
                      <>
                        <ConfirmationItem
                          label="蓄電池メーカー"
                          value={displayText(equipment.solarData.batteryMaker)}
                        />
                        <ConfirmationItem
                          label="蓄電池型式"
                          value={displayText(equipment.solarData.batteryModel)}
                        />
                        <ConfirmationItem
                          label="蓄電池容量 (kWh)"
                          value={displayText(equipment.solarData.batteryCapacity)}
                        />
                        <ConfirmationItem
                          label="確認資料：蓄電池仕様書・カタログ"
                          value={displayEquipmentFile(
                            equipment,
                            'battery_catalog',
                            equipment.solarData.batteryFile,
                          )}
                        />
                      </>
                    )}
                    <ConfirmationItem
                      label="設備の総投資額 (円)"
                      value={displayText(equipment.solarData.totalInvestment)}
                    />
                    <ConfirmationItem
                      label="確認資料：工事請負契約書、請求書、領収書等"
                      value={displayEquipmentFile(
                        equipment,
                        'construction_invoice',
                        equipment.solarData.totalInvestmentFile,
                      )}
                    />
                    <ConfirmationItem
                      label="補助金受給の有無"
                      value={displaySelection(equipment.solarData.hasSubsidy, {
                        yes: 'あり',
                        no: 'なし',
                      })}
                    />
                    {equipment.solarData.hasSubsidy === 'yes' && (
                      <>
                        <ConfirmationItem
                          label="補助金名称"
                          value={displayText(equipment.solarData.subsidyName)}
                        />
                        <ConfirmationItem
                          label="補助金額 (円)"
                          value={displayText(equipment.solarData.subsidyAmount)}
                        />
                        <ConfirmationItem
                          label="確認資料：補助金交付決定通知書"
                          value={displayEquipmentFile(
                            equipment,
                            'subsidy_notice',
                            equipment.solarData.subsidyNameFile,
                          )}
                        />
                      </>
                    )}
                    <ConfirmationItem
                      label="プロジェクト実施前の電力購入単価 (円/kWh)"
                      value={displayText(equipment.solarData.preProjectUnitPrice)}
                    />
                    <ConfirmationItem
                      label="確認資料：電力会社からの請求明細書等"
                      value={displayEquipmentFile(
                        equipment,
                        'electricity_bill',
                        equipment.solarData.preProjectUnitPriceFile,
                      )}
                    />
                  </FormGrid>
                </section>
              ))}
            </div>
          </FormSection>

          <FormSection number={3} title="同意事項">
            <FormGrid>
              <ConfirmationItem label="登録情報の利用" value={displayBoolean(agreementValues.useRegisteredInfo)} />
              <ConfirmationItem label="追加情報の提供" value={displayBoolean(agreementValues.provideAdditionalInfo)} />
              <ConfirmationItem label="環境価値の移転" value={displayBoolean(agreementValues.transferEnvironmentalValue)} />
              <ConfirmationItem label="他制度への未登録" value={displayBoolean(agreementValues.notRegisteredElsewhere)} />
              <ConfirmationItem
                label="経団連参加状況"
                value={displaySelection(agreementValues.keidanrenParticipation, {
                  participating: '参加しています',
                  notParticipating: '参加していません',
                })}
              />
              <ConfirmationItem
                label="特定排出者"
                value={displaySelection(agreementValues.specifiedEmitterStatus, {
                  isSpecifiedEmitter: '該当します',
                  notSpecifiedEmitter: '該当しません',
                })}
              />
              {agreementValues.specifiedEmitterStatus === 'isSpecifiedEmitter' && (
                <>
                  <ConfirmationItem label="特定排出者コード" value={displayText(agreementValues.specifiedEmitterCode)} />
                  <ConfirmationItem label="調整後排出量への加算" value={displayBoolean(agreementValues.emissionAdditionAgreement)} />
                </>
              )}
              <ConfirmationItem
                label="定期報告対象"
                value={displaySelection(
                  agreementValues.energyRationalizationCompliance,
                  { applicable: '該当します', notApplicable: '該当しません' },
                )}
              />
              {agreementValues.energyRationalizationCompliance === 'applicable' && (
                <>
                  <ConfirmationItem
                    label="定期報告種別"
                    value={displaySelection(agreementValues.energyLawType, {
                      type1: '一種',
                      type2: '二種',
                    })}
                  />
                  <ConfirmationItem label="省エネ法特定事業者番号" value={displayText(agreementValues.energyLawBusinessNumber)} />
                  <ConfirmationItem label="省エネ法指定工場番号" value={displayText(agreementValues.energyLawFactoryNumber)} />
                </>
              )}
            </FormGrid>
          </FormSection>

          <FormSection number={4} title="持続可能性">
            <FormGrid>
              <ConfirmationItem label="事業者名" value={displayText(contractValues.businessName)} />
              <ConfirmationItem label="代表者名" value={displayText(contractValues.representativeName)} />
              <ConfirmationItem label="代表者役職" value={displayText(contractValues.representativePosition)} />
              <ConfirmationItem label="持続可能性への同意" value={displayBoolean(contractValues.contractAgreed)} />
            </FormGrid>
          </FormSection>

          <FormSection number={5} title="入会規約">
            <ConfirmationItem label="規約への同意" value={displayBoolean(finalTermsValues.finalTermsAgreed)} />
          </FormSection>

          <FormActions
            pendingActionId={isSubmitting ? 'submit' : null}
            actions={[
              {
                id: 'back',
                label: '戻る',
                variant: 'outline',
                onClick: onBack,
              },
              {
                id: 'submit',
                label: '申請する',
                pendingLabel: '送信中...',
                onClick: onSubmit,
              },
            ]}
          />
        </FormSectionStack>
      </FormCard>
    </div>
  );
}
