import type { AdminCompanyListItem } from '@/api/admin/pendingCompanies';
import { PageBackButton } from '@/components/navigation/PageBackButton';
import { Button } from '@/components/ui/button';
import { cn } from '@/lib/utils';
import {
  BriefcaseBusiness,
  Building2,
  MapPin,
  UserRound,
} from 'lucide-react';
import type { ReactNode } from 'react';

type PendingCompanyApplicationDetailProps = {
  item: AdminCompanyListItem;
  reviewMode: boolean;
  onBack: () => void;
  onApprove: (item: AdminCompanyListItem) => void;
  onReject: (item: AdminCompanyListItem) => void;
  onSuspend: (item: AdminCompanyListItem) => void;
  onActivate: (item: AdminCompanyListItem) => void;
  accountActionDisabled?: boolean;
};

type DetailField = {
  label: string;
  value?: string | number | null;
  className?: string;
  valueClassName?: string;
};

function displayValue(value?: string | number | null) {
  if (value === null || value === undefined || value === '') return '-';
  return String(value);
}

function joinName(
  lastName?: string | null,
  firstName?: string | null,
): string | undefined {
  return [lastName, firstName].filter(Boolean).join(' ').trim() || undefined;
}

function DetailFieldItem({
  label,
  value,
  className,
  valueClassName,
}: DetailField) {
  return (
    <div className={cn('min-w-0 space-y-1', className)}>
      <p className="text-small font-bold text-muted-foreground">{label}</p>
      <p
        className={cn(
          'break-words text-body font-bold text-foreground',
          valueClassName,
        )}
      >
        {displayValue(value)}
      </p>
    </div>
  );
}

function DetailSection({
  title,
  icon,
  fields,
}: {
  title: string;
  icon: ReactNode;
  fields: DetailField[];
}) {
  return (
    <section className="space-y-6">
      <div className="flex items-center gap-3">
        <span className="flex h-11 w-11 items-center justify-center rounded-lg bg-primary text-primary-foreground">
          {icon}
        </span>
        <h2 className="text-foreground">{title}</h2>
      </div>
      <div className="grid grid-cols-1 gap-x-10 gap-y-6 md:grid-cols-2">
        {fields.map((field) => (
          <DetailFieldItem
            key={`${title}-${field.label}`}
            label={field.label}
            value={field.value}
            className={field.className}
            valueClassName={field.valueClassName}
          />
        ))}
      </div>
    </section>
  );
}

function formatRequestedProjects(item: AdminCompanyListItem) {
  return item.requestedMethodologies.length > 0
    ? item.requestedMethodologies.join('、')
    : '-';
}

export function PendingCompanyApplicationDetail({
  item,
  reviewMode,
  onBack,
  onApprove,
  onReject,
  onSuspend,
  onActivate,
  accountActionDisabled = false,
}: PendingCompanyApplicationDetailProps) {
  const legalFields: DetailField[] = [
    { label: '法人名', value: item.nameJA },
    {
      label: '氏名（カナ）',
      value: joinName(
        item.representativeLastNameKana,
        item.representativeFirstNameKana,
      ),
    },
    { label: '法人番号', value: item.corpNumber },
    { label: '会社名（英語）', value: item.nameEN },
    { label: '法人電話番号', value: item.phoneNumber },
  ];

  const addressFields: DetailField[] = [
    { label: '郵便番号', value: item.postalCode },
    { label: '都道府県', value: item.prefecture },
    { label: '市区町村', value: item.municipality },
    { label: '番地', value: item.blocknum },
    { label: '建物名', value: item.buildingName },
  ];

  const representativeFields: DetailField[] = [
    {
      label: '氏名',
      value: joinName(item.representativeLastName, item.representativeFirstName),
    },
    {
      label: '氏名（カナ）',
      value: joinName(
        item.representativeLastNameKana,
        item.representativeFirstNameKana,
      ),
    },
    { label: '役職', value: item.representativePosition },
    { label: 'メールアドレス', value: item.email },
    { label: '電話番号', value: item.representativePhoneNumber },
    { label: 'FAX番号', value: item.fax },
  ];

  const projectFields: DetailField[] = [
    {
      label: 'プロジェクト名',
      value: formatRequestedProjects(item),
      className: 'md:col-span-2',
      valueClassName: 'overflow-x-auto whitespace-nowrap',
    },
    {
      label: '活動情報',
      value: item.activityInformation,
      className: 'md:col-span-2',
    },
  ];

  return (
    <main className="relative min-h-screen bg-background px-4 py-10 sm:px-6">
      <PageBackButton
        onClick={onBack}
        label=""
        className="absolute left-4 top-10 h-12 w-12 rounded-full p-0 sm:left-10 lg:left-16"
        aria-label="戻る"
      />
      <div className="mx-auto w-full max-w-[1120px] space-y-10">
        <div className="space-y-10">
          <h1 className="text-foreground">申請情報の詳細</h1>

          <div className="rounded-xl border border-border bg-card p-8 shadow-app-sm md:p-12">
            <div className="grid grid-cols-1 gap-x-16 gap-y-12 lg:grid-cols-2">
              <DetailSection
                title="法人情報"
                icon={<Building2 className="h-6 w-6" />}
                fields={legalFields}
              />
              <DetailSection
                title="所在地"
                icon={<MapPin className="h-6 w-6" />}
                fields={addressFields}
              />
              <DetailSection
                title="責任者情報"
                icon={<UserRound className="h-6 w-6" />}
                fields={representativeFields}
              />
              <DetailSection
                title="参加希望プロジェクト"
                icon={<BriefcaseBusiness className="h-6 w-6" />}
                fields={projectFields}
              />
            </div>

            {reviewMode && (
              <div className="mt-14 flex justify-end gap-3">
                <Button
                  type="button"
                  variant="destructive"
                  size="md"
                  onClick={() => onReject(item)}
                >
                  却下
                </Button>
                <Button type="button" size="md" onClick={() => onApprove(item)}>
                  承認
                </Button>
              </div>
            )}
            {!reviewMode && item.status === 'ACTIVE' && (
              <div className="mt-14 flex justify-end">
                <Button
                  type="button"
                  variant="destructive"
                  size="md"
                  onClick={() => onSuspend(item)}
                  disabled={accountActionDisabled}
                  title={
                    accountActionDisabled
                      ? '管理者自身が所属する企業は停止できません。'
                      : undefined
                  }
                >
                  アカウント停止
                </Button>
              </div>
            )}
            {!reviewMode && item.status === 'SUSPENDED' && (
              <div className="mt-14 flex justify-end">
                <Button
                  type="button"
                  size="md"
                  onClick={() => onActivate(item)}
                >
                  アカウント有効化
                </Button>
              </div>
            )}
          </div>
        </div>
      </div>
    </main>
  );
}
