import type { ReactNode } from 'react';
import { Link, useNavigate, useParams } from 'react-router-dom';
import type { AdminOperator } from '@/api/admin/operators';
import { LoadingPanel } from '@/components/feedback/loading';
import {
  FormCard,
  FormPageContent,
  FormPageTitle,
  FormSection,
  FormSectionStack,
  FormStandalonePage,
} from '@/components/forms/form-layout';
import {
  PageBackButton,
  floatingPageBackButtonClassName,
} from '@/components/navigation/PageBackButton';
import {
  Breadcrumb,
  BreadcrumbItem,
  BreadcrumbLink,
  BreadcrumbList,
  BreadcrumbPage,
  BreadcrumbSeparator,
} from '@/components/ui/breadcrumb';
import { Button } from '@/components/ui/button';
import {
  useAdminOperatorsQuery,
  useAdminOperatorProfileQuery,
} from '@/hooks/admin/useAdminOperatorsQuery';
import { paths } from '@/routes/paths';
import { OperatorPermissionGroups } from './OperatorPermissionGroups';
import {
  getOperatorRoleLabel,
  isAdminOperatorRole,
  resolveOperatorRole,
} from './operatorRoles';

/** Formats a Japanese operator name in family-name-first order. */
function formatOperatorName(operator: AdminOperator): string {
  return [operator.lastName, operator.firstName].filter(Boolean).join(' ') || '-';
}

/** Formats the operator kana name in family-name-first order. */
function formatOperatorKanaName(operator: AdminOperator): string {
  return [operator.lastNameKana, operator.firstNameKana]
    .filter(Boolean)
    .join(' ') || '-';
}

type OperatorProfileFieldProps = {
  label: string;
  value: ReactNode;
};

/** Displays one read-only operator label and value within a numbered section. */
function OperatorProfileField({
  label,
  value,
}: Readonly<OperatorProfileFieldProps>) {
  return (
    <div className="min-w-0 space-y-1">
      <dt className="text-sm font-medium text-muted-foreground">{label}</dt>
      <dd className="break-words font-medium text-foreground">{value}</dd>
    </div>
  );
}

/** Renders the read-only operator profile loaded from the admin-specific endpoint. */
export default function AdminOperatorProfilePage() {
  const { userId } = useParams();
  const navigate = useNavigate();
  const profileQuery = useAdminOperatorProfileQuery(userId);
  const operatorsQuery = useAdminOperatorsQuery(true, 'caller');
  const operator = profileQuery.data;
  const effectiveRole = resolveOperatorRole(
    operator,
    operatorsQuery.data?.users ?? [],
  );

  /** Returns to the operator list from the profile page. */
  const handleBack = () => navigate(paths.adminOperatorList);

  /** Opens the edit page for the operator currently displayed. */
  const handleEdit = () => {
    if (operator) navigate(paths.adminOperatorEdit(operator.id));
  };

  return (
    <FormStandalonePage>
      <PageBackButton
        onClick={handleBack}
        label=""
        aria-label="運用者一覧に戻る"
        className={floatingPageBackButtonClassName}
      />
      <FormPageContent>
        <Breadcrumb>
          <BreadcrumbList className="flex-nowrap">
            <BreadcrumbItem>
              <BreadcrumbLink asChild>
                <Link to={paths.adminOperatorList}>運用者管理</Link>
              </BreadcrumbLink>
            </BreadcrumbItem>
            <BreadcrumbSeparator />
            <BreadcrumbItem className="min-w-0">
              <BreadcrumbPage className="block truncate">
                {operator ? formatOperatorName(operator) : '運用者のプロファイル'}
              </BreadcrumbPage>
            </BreadcrumbItem>
          </BreadcrumbList>
        </Breadcrumb>

        <header className="flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
          <div className="min-w-0">
            <FormPageTitle as="h1">運用者のプロファイル</FormPageTitle>
            <p className="mt-2 text-sm leading-6 text-muted-foreground sm:text-base">
              運用者のプロフィールと権限情報を確認します。
            </p>
          </div>
          {operator ? (
            <Button type="button" variant="outline" size="md" onClick={handleEdit}>
              編集
            </Button>
          ) : null}
        </header>

        {profileQuery.isLoading ? (
          <LoadingPanel message="プロファイルを読み込んでいます" />
        ) : profileQuery.isError || !operator ? (
          <FormCard className="text-center">
            <p className="text-sm text-foreground">
              運用者のプロファイルを表示できませんでした。
            </p>
            <Button
              type="button"
              variant="outline"
              size="md"
              className="mt-5"
              onClick={() => profileQuery.refetch()}
            >
              再試行
            </Button>
          </FormCard>
        ) : (
          <FormCard>
            <FormSectionStack spacing="relaxed">
              <FormSection number={1} title="基本情報">
                <dl className="grid gap-x-10 gap-y-6 md:grid-cols-2">
                  <OperatorProfileField
                    label="氏名"
                    value={formatOperatorName(operator)}
                  />
                  <OperatorProfileField
                    label="氏名（カナ）"
                    value={formatOperatorKanaName(operator)}
                  />
                  <OperatorProfileField
                    label="電話番号"
                    value={operator.phoneNumber || '-'}
                  />
                </dl>
              </FormSection>

              <FormSection number={2} title="アカウント情報">
                <dl className="grid gap-x-10 gap-y-6 md:grid-cols-2">
                  <OperatorProfileField
                    label="ユーザネーム"
                    value={operator.username || '-'}
                  />
                  <OperatorProfileField
                    label="メールアドレス"
                    value={operator.email || '-'}
                  />
                  <OperatorProfileField
                    label="ロール"
                    value={getOperatorRoleLabel(effectiveRole)}
                  />
                </dl>
              </FormSection>

              <FormSection
                number={3}
                title="権限設定"
                meta={(
                  <span className="text-sm font-medium text-primary">
                    {operator.permissions.length}機能
                  </span>
                )}
                description="利用権限は実行できる操作を表します。"
              >
                <OperatorPermissionGroups
                  permissions={operator.permissions}
                  configureHref={
                    isAdminOperatorRole(effectiveRole)
                      ? undefined
                      : paths.adminOperatorPermissionEdit(operator.id)
                  }
                />
              </FormSection>
            </FormSectionStack>
          </FormCard>
        )}
      </FormPageContent>
    </FormStandalonePage>
  );
}
