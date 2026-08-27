import { useEffect, useMemo, useState } from 'react';
import { useQueryClient } from '@tanstack/react-query';
import { Link, useNavigate, useParams } from 'react-router-dom';
import { ApiError } from '@/api/_shared/errors';
import { updateAdminOperator } from '@/api/admin/operators';
import type { UserFormData } from '@/api/users';
import { LoadingPanel } from '@/components/feedback/loading';
import {
  FormCard,
  FormPageContent,
  FormPageTitle,
  FormStandalonePage,
} from '@/components/forms/form-layout';
import {
  Breadcrumb,
  BreadcrumbItem,
  BreadcrumbLink,
  BreadcrumbList,
  BreadcrumbPage,
  BreadcrumbSeparator,
} from '@/components/ui/breadcrumb';
import { Button } from '@/components/ui/button';
import { UserForm } from '@/features/master/forms/UserForm';
import {
  adminOperatorsQueryKey,
  useAdminOperatorProfileQuery,
} from '@/hooks/admin/useAdminOperatorsQuery';
import { useSitesQuery } from '@/hooks/sites/useSitesQuery';
import { toast } from '@/hooks/use-toast';
import { paths } from '@/routes/paths';
import { OperatorPermissionSection } from './OperatorPermissionSection';
import {
  getOperatorFieldError,
  mapOperatorToFormValues,
  mapOperatorUpdatePayload,
} from './operatorFormModel';

/** Compares permission selections independent of API ordering. */
function havePermissionChanges(
  currentPermissions: readonly string[],
  originalPermissions: readonly string[],
): boolean {
  return JSON.stringify([...currentPermissions].sort()) !==
    JSON.stringify([...originalPermissions].sort());
}

/** Formats the breadcrumb label in Japanese family-name-first order. */
function formatOperatorName(lastName: string, firstName: string): string {
  return [lastName, firstName].filter(Boolean).join(' ') || '運用者';
}

/** Renders the operator edit flow backed by PATCH /admin/companies/operator-user/{userId}. */
export default function AdminOperatorEditPage() {
  const { userId } = useParams();
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const profileQuery = useAdminOperatorProfileQuery(userId);
  const sitesQuery = useSitesQuery();
  const operator = profileQuery.data;
  const [permissionDraft, setPermissionDraft] = useState<string[] | null>(null);
  const [permissionError, setPermissionError] = useState<string | null>(null);

  // Clears an old edit draft when React Router reuses this page for another operator ID.
  useEffect(() => {
    setPermissionDraft(null);
    setPermissionError(null);
  }, [operator?.id]);

  const formValues = useMemo(
    () => (operator ? mapOperatorToFormValues(operator) : null),
    [operator],
  );
  const selectedPermissions = permissionDraft ?? operator?.permissions ?? [];
  const hasPermissionChanges = operator
    ? havePermissionChanges(selectedPermissions, operator.permissions)
    : false;

  /** Returns to the operator profile without saving changes. */
  const handleBack = () => {
    navigate(userId ? paths.adminOperatorProfile(userId) : paths.adminOperatorList);
  };

  /** Stores permission edits and clears any backend permission error. */
  const handlePermissionsChange = (permissions: string[]) => {
    setPermissionDraft(permissions);
    setPermissionError(null);
  };

  /** Updates the operator profile and invalidates list/detail caches. */
  const handleSubmit = async (values: UserFormData) => {
    if (!userId) return;

    setPermissionError(null);

    try {
      await updateAdminOperator(
        userId,
        mapOperatorUpdatePayload(values, selectedPermissions),
      );
      await queryClient.invalidateQueries({ queryKey: adminOperatorsQueryKey });
      toast({ description: '運用者情報を更新しました。' });
      navigate(paths.adminOperatorProfile(userId), { replace: true });
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
              : '運用者情報を更新できませんでした。',
          variant: 'destructive',
        });
      }
      throw error;
    }
  };

  return (
    <FormStandalonePage>
      <FormPageContent>
        <Breadcrumb>
          <BreadcrumbList className="flex-nowrap">
            <BreadcrumbItem>
              <BreadcrumbLink asChild>
                <Link to={paths.adminOperatorList}>運用者管理</Link>
              </BreadcrumbLink>
            </BreadcrumbItem>
            {operator ? (
              <>
                <BreadcrumbSeparator />
                <BreadcrumbItem className="min-w-0">
                  <BreadcrumbLink asChild>
                    <Link
                      className="block max-w-48 truncate"
                      to={paths.adminOperatorProfile(operator.id)}
                    >
                      {formatOperatorName(operator.lastName, operator.firstName)}
                    </Link>
                  </BreadcrumbLink>
                </BreadcrumbItem>
              </>
            ) : null}
            <BreadcrumbSeparator />
            <BreadcrumbItem>
              <BreadcrumbPage>編集</BreadcrumbPage>
            </BreadcrumbItem>
          </BreadcrumbList>
        </Breadcrumb>

        <div>
          <FormPageTitle as="h1">運用者情報の編集</FormPageTitle>
          <p className="mt-2 text-sm leading-6 text-muted-foreground sm:text-base">
            運用者のプロフィールと権限情報を編集します。
          </p>
        </div>

        {profileQuery.isLoading ? (
          <LoadingPanel message="運用者情報を読み込んでいます" />
        ) : profileQuery.isError || !operator || !formValues ? (
          <FormCard className="text-center">
            <p className="text-sm text-foreground">
              運用者情報を表示できませんでした。
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
          <UserForm
            mode="edit"
            onChange={() => undefined}
            onNext={handleSubmit}
            onBack={handleBack}
            defaultValues={formValues}
            originalData={formValues}
            siteOptions={sitesQuery.data?.sites ?? []}
            isSiteOptionsLoading={sitesQuery.isLoading}
            siteOptionsError={
              sitesQuery.isError ? '担当拠点を取得できませんでした。' : null
            }
            hideUsernameField
            hasExternalChanges={hasPermissionChanges}
            additionalSections={
              <OperatorPermissionSection
                selectedPermissions={selectedPermissions}
                onChange={handlePermissionsChange}
                error={permissionError}
              />
            }
            submitLabel="更新する"
            embedded
          />
        )}
      </FormPageContent>
    </FormStandalonePage>
  );
}
