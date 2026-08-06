import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import type { AgreementFormValues } from './agreementsConfig';
import type { ContractStepFormValues } from './ContractStep';
import type { FinalTermsValues } from './FinalTermsStep';
import type { SolarEquipment } from './types';

type ProjectRegistrationReviewStepProps = {
  equipments: SolarEquipment[];
  methodologyNumber: string;
  agreementValues: Partial<AgreementFormValues>;
  contractValues: Partial<ContractStepFormValues>;
  finalTermsValues: Partial<FinalTermsValues>;
};

const displayText = (value?: string | null) => value?.trim() || '-';

const displayBoolean = (value?: boolean | null) => {
  if (value === true) return 'はい';
  if (value === false) return 'いいえ';
  return '-';
};

const displaySelection = (
  value: string | undefined,
  labels: Record<string, string>,
) => labels[value ?? ''] ?? '-';

function ReviewField({ label, value }: { label: string; value: string }) {
  return (
    <div className="min-w-0">
      <p className="text-small text-foreground">{label}</p>
      <p className="mt-1 break-words text-foreground">{value}</p>
    </div>
  );
}

export function ProjectRegistrationReviewStep({
  equipments,
  methodologyNumber,
  agreementValues,
  contractValues,
  finalTermsValues,
}: ProjectRegistrationReviewStepProps) {
  return (
    <div className="space-y-6">
      <div>
        <h2 className="text-2xl font-bold">申請内容の確認</h2>
        <p className="mt-2 text-foreground">
          入力内容をご確認の上、送信してください。
        </p>
      </div>

      <Card>
        <CardHeader>
          <CardTitle>設備情報</CardTitle>
        </CardHeader>
        <CardContent className="space-y-6">
          {equipments.map((equipment, index) => (
            <section
              key={equipment.id}
              className="space-y-4 border-b border-border pb-6 last:border-b-0 last:pb-0"
            >
              <h3 className="font-semibold">設備 {index + 1}</h3>
              <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
                <ReviewField
                  label="方法論番号"
                  value={displayText(methodologyNumber)}
                />
                <ReviewField
                  label="稼働開始日"
                  value={displayText(equipment.solarData.equipmentOpDate)}
                />
                <ReviewField
                  label="公称最大出力"
                  value={displayText(equipment.solarData.nominalMaxOutput)}
                />
                <ReviewField
                  label="PCSメーカー"
                  value={displayText(equipment.solarData.pcsManufacturer)}
                />
                <ReviewField
                  label="PCS型式"
                  value={displayText(equipment.solarData.pcsModel)}
                />
                <ReviewField
                  label="PCS導入台数"
                  value={displayText(equipment.solarData.powerCondUnitCount)}
                />
                <ReviewField
                  label="機器固有番号"
                  value={displayText(equipment.solarData.deviceNo)}
                />
                <ReviewField
                  label="夜間待機電力"
                  value={displayText(equipment.solarData.powerCondStandby)}
                />
                <ReviewField
                  label="蓄電池"
                  value={displaySelection(equipment.solarData.hasBattery, {
                    yes: 'あり',
                    no: 'なし',
                  })}
                />
                <ReviewField
                  label="設備の総投資額"
                  value={displayText(equipment.solarData.totalInvestment)}
                />
                <ReviewField
                  label="補助金"
                  value={displaySelection(equipment.solarData.hasSubsidy, {
                    yes: 'あり',
                    no: 'なし',
                  })}
                />
                {equipment.solarData.hasBattery === 'yes' && (
                  <>
                    <ReviewField
                      label="蓄電池メーカー"
                      value={displayText(equipment.solarData.batteryMaker)}
                    />
                    <ReviewField
                      label="蓄電池型式"
                      value={displayText(equipment.solarData.batteryModel)}
                    />
                    <ReviewField
                      label="蓄電池容量"
                      value={displayText(equipment.solarData.batteryCapacity)}
                    />
                  </>
                )}
                {equipment.solarData.hasSubsidy === 'yes' && (
                  <>
                    <ReviewField
                      label="補助金名称"
                      value={displayText(equipment.solarData.subsidyName)}
                    />
                    <ReviewField
                      label="補助金額"
                      value={displayText(equipment.solarData.subsidyAmount)}
                    />
                  </>
                )}
                <ReviewField
                  label="電力購入単価"
                  value={displayText(equipment.solarData.preProjectUnitPrice)}
                />
              </div>
              <ReviewField
                label="アップロード済みファイル"
                value={
                  equipment.files.map((file) => file.fileName).join('、') || '-'
                }
              />
            </section>
          ))}
        </CardContent>
      </Card>

      <Card>
        <CardHeader>
          <CardTitle>同意事項</CardTitle>
        </CardHeader>
        <CardContent className="grid gap-4 sm:grid-cols-2">
          <ReviewField
            label="登録情報の利用"
            value={displayBoolean(agreementValues.useRegisteredInfo)}
          />
          <ReviewField
            label="追加情報の提供"
            value={displayBoolean(agreementValues.provideAdditionalInfo)}
          />
          <ReviewField
            label="環境価値の移転"
            value={displayBoolean(agreementValues.transferEnvironmentalValue)}
          />
          <ReviewField
            label="他制度への未登録"
            value={displayBoolean(agreementValues.notRegisteredElsewhere)}
          />
          <ReviewField
            label="経団連参加状況"
            value={displaySelection(
              agreementValues.keidanrenParticipation,
              {
                participating: '参加',
                notParticipating: '不参加',
              },
            )}
          />
          <ReviewField
            label="特定排出者"
            value={displaySelection(agreementValues.specifiedEmitterStatus, {
              isSpecifiedEmitter: '該当',
              notSpecifiedEmitter: '非該当',
            })}
          />
          <ReviewField
            label="定期報告対象"
            value={displaySelection(
              agreementValues.energyRationalizationCompliance,
              {
                applicable: '該当',
                notApplicable: '非該当',
              },
            )}
          />
          {agreementValues.specifiedEmitterStatus ===
            'isSpecifiedEmitter' && (
            <>
              <ReviewField
                label="特定排出者コード"
                value={displayText(agreementValues.specifiedEmitterCode)}
              />
              <ReviewField
                label="調整後排出量への加算"
                value={displayBoolean(
                  agreementValues.emissionAdditionAgreement,
                )}
              />
            </>
          )}
          {agreementValues.energyRationalizationCompliance ===
            'applicable' && (
            <>
              <ReviewField
                label="定期報告種別"
                value={displaySelection(agreementValues.energyLawType, {
                  type1: '一種',
                  type2: '二種',
                })}
              />
              <ReviewField
                label="特定事業者番号"
                value={displayText(
                  agreementValues.energyLawBusinessNumber,
                )}
              />
              <ReviewField
                label="指定工場番号"
                value={displayText(agreementValues.energyLawFactoryNumber)}
              />
            </>
          )}
        </CardContent>
      </Card>

      <Card>
        <CardHeader>
          <CardTitle>事業者情報</CardTitle>
        </CardHeader>
        <CardContent className="grid gap-4 sm:grid-cols-2">
          <ReviewField
            label="事業者名"
            value={displayText(contractValues.businessName)}
          />
          <ReviewField
            label="代表者名"
            value={displayText(contractValues.representativeName)}
          />
          <ReviewField
            label="代表者役職"
            value={displayText(contractValues.representativePosition)}
          />
          <ReviewField
            label="持続可能性への同意"
            value={displayBoolean(contractValues.contractAgreed)}
          />
        </CardContent>
      </Card>

      <Card>
        <CardHeader>
          <CardTitle>入会規約</CardTitle>
        </CardHeader>
        <CardContent>
          <ReviewField
            label="規約への同意"
            value={displayBoolean(finalTermsValues.finalTermsAgreed)}
          />
        </CardContent>
      </Card>
    </div>
  );
}
