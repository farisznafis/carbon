import { useNavigate, useParams } from 'react-router-dom';
import { LoadingPanel } from '@/components/feedback/loading';
import {
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
import { useAdminOperatorProfileQuery } from '@/hooks/admin/useAdminOperatorsQuery';
import { paths } from '@/routes/paths';
import { getOperatorPermissionLabel } from './operatorPermissions';

/** Renders the read-only operator profile loaded from the admin-specific endpoint. */
export default function AdminOperatorProfilePage() {
  const { userId } = useParams();
  const navigate = useNavigate();
  const profileQuery = useAdminOperatorProfileQuery(userId);
  const operator = profileQuery.data;
  /** Returns from the read-only profile to the operator management list. */
  const handleBack = () => navigate(paths.adminOperatorList);

  return (
    <FormStandalonePage>
      <PageBackButton
        onClick={handleBack}
        label=""
        className="absolute left-4 top-10 h-12 w-12 rounded-full p-0 sm:left-10 lg:left-16"
      />
      <FormPageContent>
        <div>
          <FormPageTitle as="h1">運用者プロファイル</FormPageTitle>
          <p className="mt-2 text-sm leading-6 text-muted-foreground">
            運用者のアカウント情報と付与されている権限を確認できます。
          </p>
        </div>

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
              <FormSection number={1} title="個人情報">
                <FormGrid>
                  <ConfirmationItem label="姓" value={operator.lastName || '-'} />
                  <ConfirmationItem label="名" value={operator.firstName || '-'} />
                  <ConfirmationItem label="姓（カナ）" value={operator.lastNameKana || '-'} />
                  <ConfirmationItem label="名（カナ）" value={operator.firstNameKana || '-'} />
                  <ConfirmationItem label="電話番号" value={operator.phoneNumber || '-'} />
                </FormGrid>
              </FormSection>

              <FormSection number={2} title="アカウント情報">
                <FormGrid>
                  <ConfirmationItem label="ユーザネーム" value={operator.username || '-'} />
                  <ConfirmationItem label="メールアドレス" value={operator.email || '-'} />
                  <ConfirmationItem label="ロール" value="運用者" />
                </FormGrid>
              </FormSection>

              <FormSection number={3} title="権限">
                {operator.permissions.length === 0 ? (
                  <p className="text-sm text-muted-foreground">権限なし</p>
                ) : (
                  <ul className="grid gap-3 sm:grid-cols-2">
                    {operator.permissions.map((permission) => (
                      <li
                        key={permission}
                        className="rounded-lg border border-border bg-background px-4 py-3 text-sm text-foreground"
                      >
                        {getOperatorPermissionLabel(permission)}
                      </li>
                    ))}
                  </ul>
                )}
              </FormSection>
            </FormSectionStack>
          </FormCard>
        )}
      </FormPageContent>
    </FormStandalonePage>
  );
}
