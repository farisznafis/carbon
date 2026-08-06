import type { ReactNode } from 'react';
import type { ParticipationReviewDetail } from '@/api/admin';
import { FormCard } from '@/components/forms/form-layout';
import { formatAdminCompanyDateTime } from '@/features/admin/company-detail/companyDetailDisplay';
import {
  displayParticipationBoolean,
  displayParticipationValue,
  getParticipationReviewStage,
  formatFileType,
} from './participationReviewDisplay';
import { ParticipationReviewFileActions } from './ParticipationReviewFileActions';

function DetailField({
  label,
  children,
  fullWidth,
}: {
  label: string;
  children: ReactNode;
  fullWidth?: boolean;
}) {
  return (
    <div className={fullWidth ? 'space-y-1.5 md:col-span-2' : 'space-y-1.5'}>
      <dt className="text-small font-bold text-foreground">{label}</dt>
      <dd className="break-words text-body text-foreground">{children}</dd>
    </div>
  );
}

function DetailCard({
  title,
  children,
}: {
  title: string;
  children: ReactNode;
}) {
  return (
    <FormCard>
      <section className="space-y-6">
        <h2 className="text-xl font-bold text-foreground">{title}</h2>
        {children}
      </section>
    </FormCard>
  );
}

function DetailGrid({ children }: { children: ReactNode }) {
  return <dl className="grid grid-cols-1 gap-4 md:grid-cols-2">{children}</dl>;
}

export function ParticipationReviewSections({
  review,
}: {
  review: ParticipationReviewDetail;
}) {
  const { equipment, confirmationItems, sustainabilityCommitment } = review;

  return (
    <>
      <DetailCard title="申請情報">
        <DetailGrid>
          <DetailField label="入会届ID">{review.participationId}</DetailField>
          <DetailField label="レビュー段階">
            {getParticipationReviewStage(review)}
          </DetailField>
          <DetailField label="プロジェクト名">
            {displayParticipationValue(review.projectName)}
          </DetailField>
          <DetailField label="会社名">
            {displayParticipationValue(review.companyName)}
          </DetailField>
          <DetailField label="拠点名">
            {displayParticipationValue(review.siteName)}
          </DetailField>
          <DetailField label="申請日時">
            {formatAdminCompanyDateTime(review.submittedAt)}
          </DetailField>
        </DetailGrid>
      </DetailCard>

      <DetailCard title="設備情報">
        <DetailGrid>
          <DetailField label="稼働開始日">
            {displayParticipationValue(equipment.equipmentOpDate)}
          </DetailField>
          <DetailField label="公称最大出力（kW）">
            {displayParticipationValue(equipment.nominalMaxOutput)}
          </DetailField>
          <DetailField label="PCSメーカー">
            {displayParticipationValue(equipment.pcsManufacturer)}
          </DetailField>
          <DetailField label="PCS型式">
            {displayParticipationValue(equipment.pcsModel)}
          </DetailField>
          <DetailField label="PCS導入台数">
            {displayParticipationValue(equipment.pcsUnitCount)}
          </DetailField>
          <DetailField label="機器固有番号">
            {displayParticipationValue(equipment.deviceNo)}
          </DetailField>
          <DetailField label="夜間待機電力（W）">
            {displayParticipationValue(equipment.pcsStandby)}
          </DetailField>
          <DetailField label="蓄電池の有無">
            {displayParticipationBoolean(equipment.hasBattery)}
          </DetailField>
          {equipment.hasBattery === true && (
            <>
              <DetailField label="蓄電池メーカー">
                {displayParticipationValue(equipment.batteryMaker)}
              </DetailField>
              <DetailField label="蓄電池型式">
                {displayParticipationValue(equipment.batteryModel)}
              </DetailField>
              <DetailField label="蓄電池容量（kWh）">
                {displayParticipationValue(equipment.batteryCapacity)}
              </DetailField>
            </>
          )}
          <DetailField label="設備の総投資額（円）">
            {displayParticipationValue(equipment.totalInvestment)}
          </DetailField>
          <DetailField label="補助金受給の有無">
            {displayParticipationBoolean(equipment.hasSubsidy)}
          </DetailField>
          {equipment.hasSubsidy === true && (
            <>
              <DetailField label="補助金名称">
                {displayParticipationValue(equipment.subsidyName)}
              </DetailField>
              <DetailField label="補助金額（円）">
                {displayParticipationValue(equipment.subsidyAmount)}
              </DetailField>
            </>
          )}
          <DetailField label="プロジェクト実施前の電力購入単価（円/kWh）">
            {displayParticipationValue(equipment.preProjectUnitPrice)}
          </DetailField>
        </DetailGrid>
      </DetailCard>

      <DetailCard title="確認資料">
        <div className="space-y-6">
          <div className="space-y-2">
            <h3 className="text-body font-bold text-foreground">
              証憑ドキュメント
            </h3>
            {equipment.evidenceDocuments.length ? (
              <ul className="space-y-2">
                {equipment.evidenceDocuments.map((document, index) => (
                  <li
                    key={`${document}-${index}`}
                    className="rounded-lg border border-border bg-muted/20 px-4 py-3 text-small"
                  >
                    {document}
                  </li>
                ))}
              </ul>
            ) : (
              <p className="text-small text-foreground">―</p>
            )}
          </div>

          <div className="space-y-2">
            <h3 className="text-body font-bold text-foreground">
              アップロードファイル
            </h3>
            {equipment.files.length ? (
              <ul className="space-y-2">
                {equipment.files.map((file, index) => (
                  <li
                    key={file.fileId || file.s3Key || String(index)}
                    className="rounded-lg border border-border bg-muted/20 px-4 py-3"
                  >
                    <div className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
                      <div className="min-w-0">
                        <p className="text-small font-bold text-foreground">
                          {formatFileType(file.fileType)}
                        </p>
                        {/* <p className="mt-1 break-all text-xs text-foreground">
                          {displayParticipationValue(file.s3Key || file.fileId)}
                        </p> */}
                      </div>
                      <ParticipationReviewFileActions file={file} />
                    </div>
                  </li>
                ))}
              </ul>
            ) : (
              <p className="text-small text-foreground">―</p>
            )}
          </div>
        </div>
      </DetailCard>

      <DetailCard title="確認・同意事項">
        <DetailGrid>
          <DetailField label="登録情報の利用">
            {displayParticipationBoolean(confirmationItems.isAgreeDataUse)}
          </DetailField>
          <DetailField label="追加情報の提供">
            {displayParticipationBoolean(confirmationItems.isAgreeExtraData)}
          </DetailField>
          <DetailField label="環境価値の移転">
            {displayParticipationBoolean(confirmationItems.isAgreeTransfer)}
          </DetailField>
          <DetailField label="他制度への未登録">
            {displayParticipationBoolean(confirmationItems.isNotRegistered)}
          </DetailField>
          <DetailField label="経団連への参加">
            {displayParticipationBoolean(
              confirmationItems.isKeidanrenParticipation,
            )}
          </DetailField>
          <DetailField label="温室効果ガス排出者">
            {displayParticipationBoolean(confirmationItems.isEmitter)}
          </DetailField>
          <DetailField label="排出者コード">
            {displayParticipationValue(confirmationItems.emitterCode)}
          </DetailField>
          <DetailField label="調整後排出量への追加">
            {displayParticipationBoolean(
              confirmationItems.isAdjustedEmissionsAdded,
            )}
          </DetailField>
          <DetailField label="定期報告の対象">
            {displayParticipationBoolean(
              confirmationItems.isSubjectToPeriodicReport,
            )}
          </DetailField>
          <DetailField label="定期報告の第一種">
            {displayParticipationBoolean(
              confirmationItems.isPeriodicReportTypeOne,
            )}
          </DetailField>
          <DetailField label="エネルギー事業者コード">
            {displayParticipationValue(
              confirmationItems.energyBusinessCode,
            )}
          </DetailField>
          <DetailField label="工場コード">
            {displayParticipationValue(confirmationItems.factoryCode)}
          </DetailField>
        </DetailGrid>
      </DetailCard>

      <DetailCard title="サステナビリティに関する誓約">
        <DetailGrid>
          <DetailField label="会社名">
            {displayParticipationValue(sustainabilityCommitment.companyName)}
          </DetailField>
          <DetailField label="代表者名">
            {displayParticipationValue(sustainabilityCommitment.repName)}
          </DetailField>
          <DetailField label="代表者役職">
            {displayParticipationValue(sustainabilityCommitment.repRole)}
          </DetailField>
          <DetailField label="誓約への同意">
            {displayParticipationBoolean(
              sustainabilityCommitment.hasConsent,
            )}
          </DetailField>
          <DetailField label="規約への同意">
            {displayParticipationBoolean(
              sustainabilityCommitment.isAgreeTerms,
            )}
          </DetailField>
        </DetailGrid>
      </DetailCard>

      <DetailCard title="レビュー担当者">
        <DetailGrid>
          <DetailField label="一次確認者">
            {displayParticipationValue(
              review.review.primaryApplReviewerName,
            )}
          </DetailField>
          <DetailField label="二次確認者">
            {displayParticipationValue(
              review.review.secondaryApplReviewerName,
            )}
          </DetailField>
        </DetailGrid>
      </DetailCard>
    </>
  );
}
