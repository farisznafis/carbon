import { useEffect, useMemo, useState } from 'react';
import { useQueryClient } from '@tanstack/react-query';
import {
  Link,
  useLocation,
  useNavigate,
  useParams,
} from 'react-router-dom';
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
import { UserForm } from '@/features/master/forms/UserForm';
import { useAdminPermissions } from '@/features/admin/permissions/useAdminPermissions';
import {
  adminOperatorsQueryKey,
  useAdminOperatorsQuery,
  useAdminOperatorProfileQuery,
} from '@/hooks/admin/useAdminOperatorsQuery';
import { toast } from '@/hooks/use-toast';
import { paths } from '@/routes/paths';
import {
  OPERATOR_PERMISSION_SECTION_ID,
  OperatorPermissionSection,
} from './OperatorPermissionSection';
import {
  getOperatorFieldError,
  mapOperatorToFormValues,
  mapOperatorUpdatePayload,
} from './operatorFormModel';
import {
  isAdminOperatorRole,
  resolveOperatorRole,
} from './operatorRoles';

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
  const location = useLocation();
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const permissions = useAdminPermissions();
  const isEditingOwnAccount = Boolean(
    userId && permissions.operatorId && userId === permissions.operatorId,
  );
  const profileQuery = useAdminOperatorProfileQuery(userId);
  const operatorsQuery = useAdminOperatorsQuery(!isEditingOwnAccount, 'caller');
  const operator = profileQuery.data;
  const operatorList = operatorsQuery.data?.users ?? [];
  const effectiveRole = resolveOperatorRole(
    operator,
    operatorList,
  );
  const canEditPermissions = Boolean(
    operator &&
    operatorsQuery.data &&
    !isEditingOwnAccount &&
    !isAdminOperatorRole(effectiveRole),
  );
  const [permissionDraft, setPermissionDraft] = useState<string[] | null>(null);
  const [permissionError, setPermissionError] = useState<string | null>(null);

  // Clears an old edit draft when React Router reuses this page for another operator ID.
  useEffect(() => {
    setPermissionDraft(null);
    setPermissionError(null);
  }, [operator?.id]);

  // Moves keyboard and visual focus to 権限 when opened from the profile empty state.
  useEffect(() => {
    if (
      !operator ||
      !canEditPermissions ||
      location.hash !== `#${OPERATOR_PERMISSION_SECTION_ID}`
    ) {
      return;
    }

    const permissionSection = document.getElementById(
      OPERATOR_PERMISSION_SECTION_ID,
    );
    permissionSection?.focus({ preventScroll: true });
    permissionSection?.scrollIntoView({ block: 'start' });
  }, [canEditPermissions, location.hash, operator]);

  const formValues = useMemo(
    () => (operator ? mapOperatorToFormValues(operator) : null),
    [operator],
  );
  const selectedPermissions = canEditPermissions
    ? permissionDraft ?? operator?.permissions ?? []
    : [];
  const hasPermissionChanges = operator && canEditPermissions
    ? havePermissionChanges(selectedPermissions, operator.permissions)
    : false;

  /** Returns to the operator profile without saving changes. */
  const handleBack = () => {
    navigate(userId ? paths.adminOperatorProfile(userId) : paths.adminOperatorList);
  };

  /** Stores permission edits and clears any backend permission error. */
  const handlePermissionsChange = (permissions: string[]) => {
    if (!canEditPermissions) return;
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
        mapOperatorUpdatePayload(
          values,
          canEditPermissions ? selectedPermissions : undefined,
        ),
      );
      await queryClient.invalidateQueries({ queryKey: adminOperatorsQueryKey });
      toast({ description: '運用者情報を更新しました。' });
      navigate(paths.adminOperatorProfile(userId), { replace: true });
    } catch (error) {
      const backendPermissionError = canEditPermissions
        ? getOperatorFieldError(error, 'permissionCodes') ??
          getOperatorFieldError(error, 'permissions')
        : null;
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
      <PageBackButton
        onClick={handleBack}
        label=""
        aria-label="運用者プロファイルに戻る"
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
          <FormPageTitle as="h2">運用者情報の編集</FormPageTitle>
          <p className="mt-2 text-sm leading-6 text-muted-foreground sm:text-base">
            {canEditPermissions
              ? '運用者のプロフィールと権限情報を編集します。'
              : isEditingOwnAccount
                ? '自身のプロフィール情報を編集します。'
                : '管理者のプロフィール情報を編集します。'}
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
            hideSiteSection
            hidePositionField
            hideUsernameField
            hasExternalChanges={hasPermissionChanges}
            additionalSections={canEditPermissions ? (
              <OperatorPermissionSection
                selectedPermissions={selectedPermissions}
                onChange={handlePermissionsChange}
                error={permissionError}
                sectionNumber={3}
                sectionId={OPERATOR_PERMISSION_SECTION_ID}
              />
            ) : undefined}
            submitLabel="更新する"
            embedded
          />
        )}
      </FormPageContent>
    </FormStandalonePage>
  );
}
