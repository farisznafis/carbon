import type { AdminCompanyListItem } from '@/api/admin/pendingCompanies';
import { StatusBadge } from '@/components/data-display/StatusBadge';
import { usePageBreadcrumb } from '@/components/navigation/pageBreadcrumbContext';
import { Button } from '@/components/ui/button';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { CompanyStatusBadge } from '@/features/admin/company-detail/CompanyStatusBadge';
import { DefinitionRow } from '@/features/admin/company-detail/components/DefinitionRow';
import {
  formatAdminCompanyAddress,
  formatAdminCompanyDateTime,
} from '@/features/admin/company-detail/companyDetailDisplay';
import { BriefcaseBusiness, Building2, Check, UserRound, X } from 'lucide-react';

type PendingCompanyApplicationDetailProps = {
  item: AdminCompanyListItem;
  reviewMode: boolean;
  backLabel?: string;
  onBack: () => void;
  onApprove: (item: AdminCompanyListItem) => void;
  onReject: (item: AdminCompanyListItem) => void;
};

function joinName(
  lastName?: string | null,
  firstName?: string | null,
): string | undefined {
  return [lastName, firstName].filter(Boolean).join(' ').trim() || undefined;
}

/** Renders a submitted company application in the same layout as the company detail page. */
export function PendingCompanyApplicationDetail({
  item,
  reviewMode,
  backLabel = '企業一覧',
  onBack,
  onApprove,
  onReject,
}: PendingCompanyApplicationDetailProps) {
  const companyName = item.nameJA || `企業ID: ${item.companyId}`;
  usePageBreadcrumb([{ label: backLabel, onClick: onBack }, { label: companyName }]);
  const representativeName = joinName(
    item.representativeLastName,
    item.representativeFirstName,
  );
  const representativeNameKana = joinName(
    item.representativeLastNameKana,
    item.representativeFirstNameKana,
  );

  return (
    <main className="relative min-h-screen bg-background px-4 py-8 sm:px-6">
      <div className="mx-auto w-full max-w-7xl space-y-6">

        {/* ── Page Header ─────────────────────────────────────────────────── */}
        <div className="min-w-0 space-y-3">
          {/* Title row */}
          <div className="flex items-start justify-between gap-4">
            <div className="space-y-1.5 min-w-0">
              <h1 className="text-2xl font-bold tracking-tight text-foreground truncate">
                {companyName}
              </h1>

              {/* Inline metadata chips */}
              <div className="flex flex-wrap items-center gap-x-4 gap-y-1.5 pt-0.5">
                <span className="inline-flex items-center gap-1 text-sm text-foreground">
                  ステータス：
                  <CompanyStatusBadge status={item.status} />
                </span>

                {item.corpNumber && (
                  <span className="text-sm text-foreground">
                    法人番号：
                    <span className="font-medium text-foreground ml-1">
                      {item.corpNumber}
                    </span>
                  </span>
                )}

                {item.createdAt && (
                  <span className="text-sm text-foreground">
                    申請日時：
                    <span className="font-medium text-foreground ml-1">
                      {formatAdminCompanyDateTime(item.createdAt)}
                    </span>
                  </span>
                )}
              </div>
            </div>

            {reviewMode && (
              <div className="flex shrink-0 items-center gap-2">
                <Button
                  type="button"
                  variant="destructive-outline"
                  size="md"
                  onClick={() => onReject(item)}
                >
                  <X aria-hidden="true" />
                  却下
                </Button>
                <Button
                  type="button"
                  variant="success"
                  size="md"
                  onClick={() => onApprove(item)}
                >
                  <Check aria-hidden="true" />
                  承認
                </Button>
              </div>
            )}
          </div>
        </div>

        <div className="grid grid-cols-1 items-stretch gap-6 lg:grid-cols-2">
          {/* ── 企業情報 ─────────────────────────────────────────────────── */}
          <Card>
            <CardHeader className="pb-2">
              <div className="flex items-center gap-2">
                <Building2 className="h-5 w-5 text-muted-foreground" />
                <CardTitle className="text-lg font-bold">企業情報</CardTitle>
              </div>
            </CardHeader>
            <CardContent>
              <dl className="divide-y divide-border/50">
                <DefinitionRow label="企業名">{item.nameJA || '―'}</DefinitionRow>
                <DefinitionRow label="企業名（カナ）">{item.nameFurigana || '―'}</DefinitionRow>
                <DefinitionRow label="企業名（英語）">{item.nameEN || '―'}</DefinitionRow>
                <DefinitionRow label="法人番号">{item.corpNumber || '―'}</DefinitionRow>
                <DefinitionRow label="電話番号">{item.phoneNumber || '―'}</DefinitionRow>
                <DefinitionRow label="所在地">{formatAdminCompanyAddress(item)}</DefinitionRow>
              </dl>
            </CardContent>
          </Card>

          {/* ── 責任者情報 ───────────────────────────────────────────────── */}
          <Card>
            <CardHeader className="pb-2">
              <div className="flex items-center gap-2">
                <UserRound className="h-5 w-5 text-muted-foreground" />
                <CardTitle className="text-lg font-bold">責任者情報</CardTitle>
              </div>
            </CardHeader>
            <CardContent>
              <dl className="divide-y divide-border/50">
                <DefinitionRow label="氏名">{representativeName || '―'}</DefinitionRow>
                <DefinitionRow label="氏名（カナ）">{representativeNameKana || '―'}</DefinitionRow>
                <DefinitionRow label="役職">{item.representativePosition || '―'}</DefinitionRow>
                <DefinitionRow label="メールアドレス">{item.email || '―'}</DefinitionRow>
                <DefinitionRow label="電話番号">{item.representativePhoneNumber || '―'}</DefinitionRow>
                <DefinitionRow label="FAX番号">{item.fax || '―'}</DefinitionRow>
              </dl>
            </CardContent>
          </Card>
        </div>

        {/* ── 参加希望プロジェクト ─────────────────────────────────────────── */}
        <Card>
          <CardHeader className="pb-2">
            <div className="flex items-center gap-2">
              <BriefcaseBusiness className="h-5 w-5 text-muted-foreground" />
              <CardTitle className="text-lg font-bold">参加希望プロジェクト</CardTitle>
            </div>
          </CardHeader>
          <CardContent>
            <dl className="divide-y divide-border/50">
              <DefinitionRow label="プロジェクト名">
                {item.requestedMethodologies.length > 0 ? (
                  <div className="flex flex-wrap gap-1.5">
                    {item.requestedMethodologies.map((methodology) => (
                      <StatusBadge key={methodology}>{methodology}</StatusBadge>
                    ))}
                  </div>
                ) : (
                  '―'
                )}
              </DefinitionRow>
              <DefinitionRow label="活動情報">
                <span className="whitespace-pre-wrap">{item.activityInformation || '―'}</span>
              </DefinitionRow>
            </dl>
          </CardContent>
        </Card>
      </div>
    </main>
  );
}
