import { useState } from 'react';
import { useQueryClient } from '@tanstack/react-query';
import { useNavigate } from 'react-router-dom';
import { ApiError } from '@/api/_shared/errors';
import { createAdminOperator } from '@/api/admin/operators';
import type { UserFormData } from '@/api/users';
import {
  FormPageContent,
  FormPageTitle,
  FormStandalonePage,
} from '@/components/forms/form-layout';
import {
  PageBackButton,
  floatingPageBackButtonClassName,
} from '@/components/navigation/PageBackButton';
import { UserForm } from '@/features/master/forms/UserForm';
import { adminOperatorsQueryKey } from '@/hooks/admin/useAdminOperatorsQuery';
import { toast } from '@/hooks/use-toast';
import { paths } from '@/routes/paths';
import { OperatorPermissionSection } from './OperatorPermissionSection';
import {
  EMPTY_OPERATOR_VALUES,
  getOperatorFieldError,
  mapOperatorCreatePayload,
} from './operatorFormModel';

/** Renders the admin-specific operator registration flow using shared user fields. */
export default function AdminOperatorRegisterPage() {
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const [selectedPermissions, setSelectedPermissions] = useState<string[]>([]);
  const [permissionError, setPermissionError] = useState<string | null>(null);

  /** Returns to the operator list without submitting the registration form. */
  const handleBack = () => navigate(paths.adminOperatorList);

  /** Updates the operator permission selection and clears its validation error. */
  const handlePermissionsChange = (permissions: string[]) => {
    setSelectedPermissions(permissions);
    if (permissions.length > 0) setPermissionError(null);
  };

  /** Creates the operator with an optional permission selection. */
  const handleSubmit = async (values: UserFormData) => {
    setPermissionError(null);

    try {
      await createAdminOperator(
        mapOperatorCreatePayload(values, selectedPermissions),
      );
      await queryClient.invalidateQueries({ queryKey: adminOperatorsQueryKey });
      toast({ description: '運用者を登録しました。' });
      navigate(paths.adminOperatorList, { replace: true });
    } catch (error) {
      const backendPermissionError =
        getOperatorFieldError(error, 'permissionCodes') ??
        getOperatorFieldError(error, 'permissions');
      if (backendPermissionError) {
        setPermissionError(backendPermissionError);
      } else if (!(error instanceof ApiError && error.payload?.errors)) {
        toast({
          title: 'エラー',
          description:
            error instanceof Error
              ? error.message
              : '運用者を登録できませんでした。',
          variant: 'destructive',
        });
      }
      throw error;
    }
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
        <div>
          <FormPageTitle as="h1">運用者を追加</FormPageTitle>
          <p className="mt-2 text-sm leading-6 text-muted-foreground">
            運用業務を担当するユーザーのアカウントと権限を登録します。
          </p>
        </div>
        <UserForm
          mode="create"
          onChange={() => undefined}
          onNext={handleSubmit}
          onBack={handleBack}
          defaultValues={EMPTY_OPERATOR_VALUES}
          originalData={EMPTY_OPERATOR_VALUES}
          hideSiteSection
          hidePositionField
          additionalSections={
            <OperatorPermissionSection
              selectedPermissions={selectedPermissions}
              onChange={handlePermissionsChange}
              error={permissionError}
              sectionNumber={3}
            />
          }
          submitLabel="登録する"
          embedded
        />
      </FormPageContent>
    </FormStandalonePage>
  );
}
