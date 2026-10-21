import { useNavigate, useParams } from 'react-router-dom';
import type { AdminOperator } from '@/api/admin/operators';
import { isForbiddenApiError } from '@/api/_shared/errors';
import { ProfileDetailField } from '@/components/data-display/ProfileDetailField';
import { LoadingPanel } from '@/components/feedback/loading';
import {
  FormCard,
  FormPageContent,
  FormPageTitle,
  FormSection,
  FormSectionStack,
  FormStandalonePage,
} from '@/components/forms/form-layout';
import { usePageBreadcrumb } from '@/components/navigation/pageBreadcrumbContext';
import { useAdminOperatorProfileQuery } from '@/hooks/admin/useAdminOperatorsQuery';
import { paths } from '@/routes/paths';
import { useAdminPermissions } from '@/features/admin/permissions/useAdminPermissions';
import { PermissionDeniedNotice } from '@/features/admin/permissions/PermissionDeniedNotice';
import { OperatorPermissionGroups } from './OperatorPermissionGroups';
import {
  getOperatorRoleLabel,
  isAdminOperatorRole,
} from './operatorRoles';
import { ActionButton } from '@/components/actions';

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

/** Renders the read-only operator profile loaded from the admin-specific endpoint. */
export default function AdminOperatorProfilePage() {
  const { userId } = useParams();
  const navigate = useNavigate();
  const permissions = useAdminPermissions();
  const profileQuery = useAdminOperatorProfileQuery(userId);
  const operator = profileQuery.data;
  const effectiveRole = operator?.role ?? '';

  usePageBreadcrumb([
    { label: '運用者', to: paths.adminOperatorList },
    { label: operator ? formatOperatorName(operator) : '運用者のプロファイル' },
  ]);

  /** Opens the edit page for the operator currently displayed. */
  const handleEdit = () => {
    if (!operator) return;

    /** Navigates to the selected operator's edit form. */
    const openEditPage = () => navigate(paths.adminOperatorEdit(operator.id));

    if (operator.id === permissions.operatorId) {
      openEditPage();
      return;
    }

    permissions.guardAction('OPERATOR_USER_UPDATE', openEditPage);
  };

  return (
    <FormStandalonePage>
      <FormPageContent>

        <header className="flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
          <div className="min-w-0">
            <FormPageTitle as="h2">運用者のプロファイル</FormPageTitle>
            <p className="mt-2 text-sm leading-6 text-muted-foreground sm:text-base">
              運用者のプロフィールと権限情報を確認します。
            </p>
          </div>
          {operator ? (
            <ActionButton kind="edit" onClick={handleEdit}>
              編集
            </ActionButton>
          ) : null}
        </header>

        {profileQuery.isLoading ? (
          <LoadingPanel message="プロファイルを読み込んでいます" />
        ) : isForbiddenApiError(profileQuery.error) && userId !== permissions.operatorId ? (
          <PermissionDeniedNotice permission="OPERATOR_USER_READ" />
        ) : profileQuery.isError || !operator ? (
          <FormCard className="text-center">
            <p className="text-sm text-foreground">
              運用者のプロファイルを表示できませんでした。
            </p>
            <ActionButton
              kind="retry"
              className="mt-5"
              onClick={() => profileQuery.refetch()}
            />
          </FormCard>
        ) : (
          <FormCard>
            <FormSectionStack spacing="relaxed">
              <FormSection number={1} title="基本情報">
                <dl className="grid gap-x-10 gap-y-6 md:grid-cols-2">
                  <ProfileDetailField
                    label="氏名"
                    value={formatOperatorName(operator)}
                  />
                  <ProfileDetailField
                    label="氏名（カナ）"
                    value={formatOperatorKanaName(operator)}
                  />
                  <ProfileDetailField
                    label="電話番号"
                    value={operator.phoneNumber || '-'}
                  />
                </dl>
              </FormSection>

              <FormSection number={2} title="アカウント情報">
                <dl className="grid gap-x-10 gap-y-6 md:grid-cols-2">
                  <ProfileDetailField
                    label="ユーザネーム"
                    value={operator.username || '-'}
                  />
                  <ProfileDetailField
                    label="メールアドレス"
                    value={operator.email || '-'}
                  />
                  <ProfileDetailField
                    label="ロール"
                    value={getOperatorRoleLabel(effectiveRole)}
                  />
                </dl>
              </FormSection>

              <FormSection
                number={3}
                title="権限設定"
                meta={(
                  <span className="text-sm font-medium text-primary-strong">
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
