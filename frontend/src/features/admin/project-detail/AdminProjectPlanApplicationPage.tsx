import { useMemo, useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import { useNavigate, useParams } from 'react-router-dom';
import { getProjectPlanForm } from '@/api/admin/projects';
import { mapPlanPageValues } from '@/api/admin/projectPayloadMapper';
import type { FieldConfig } from '@/components/forms/shared/GenericFormFields/GenericFormFields';
import { PageSkeleton, SkeletonCard } from '@/components/feedback/loading';
import {
  FormActions,
  FormCard,
  FormSection,
} from '@/components/forms/form-layout';
import { PageDropdown } from '@/components/forms/shared/PageDropdown';
import { PageBackButton } from '@/components/navigation/PageBackButton';
import { Badge } from '@/components/ui/badge';
import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from '@/components/ui/card';
import {
  useAdminProjectDetailQuery,
  useAdminProjectPlanProgressQuery,
} from '@/hooks/admin/useAdminProjectDetailQuery';
import { paths } from '@/routes/paths';
import { mapDetailToProjectBase } from '@/features/admin/project-registration/forms/projectBaseFromDetail';
import {
  LAST_IMPLEMENTED_STEP,
  getPlanPageForSlug,
  getPlanPageForStep,
  rulesInFormNames,
} from '@/features/admin/project-registration/forms/projectPlanPages';
import { steps } from '@/features/admin/project-registration/forms/projectSteps';
import {
  adjacentStepId,
  clampStepId,
  stepsForApplicationCategory,
} from '@/features/admin/project-registration/forms/applicationCategory';
import { AdminProjectDetailLayout } from './AdminProjectDetailLayout';
import { PlanPageValues } from './plan-view/PlanPageValues';
import { formatProjectDetailDateTime } from './projectDetailDisplay';

/**
 * Every step the wizard serves — step 1 is the project record, 2+ are plan
 * pages — limited to what a プロジェクト登録 files, as in the wizard.
 */
const visibleSteps = stepsForApplicationCategory(steps.slice(0, LAST_IMPLEMENTED_STEP));
const maxStep = visibleSteps[visibleSteps.length - 1].id;

/** Step 1's configs are shared with the create form, whose labels are terse. */
const projectBaseLabelOverrides: Record<string, string> = {
  'projectBase.projectName': 'プロジェクト名称',
  'projectBase.projectStatus': 'ステータス',
  'projectBase.regionalCategory': '地域区分',
  'projectBase.projectNumber': 'プロジェクト番号',
  'projectBase.companyName': '会社名',
  'projectBase.repLastName': '代表者 姓',
  'projectBase.repFirstName': '代表者 名',
  'projectBase.repLastNameKana': '代表者 姓（カナ）',
  'projectBase.repFirstNameKana': '代表者 名（カナ）',
  'projectBase.repEmail': '代表者メールアドレス',
  'projectBase.repPhoneNumber': '代表者電話番号',
  'projectBase.repPosition': '代表者役職',
  'projectBase.picFirstName': '責任者 名',
  'projectBase.picLastName': '責任者 姓',
  'projectBase.picFirstNameKana': '責任者 名（カナ）',
  'projectBase.picLastNameKana': '責任者 姓（カナ）',
  'projectBase.picEmail': '責任者メールアドレス',
  'projectBase.picPhoneNumber': '責任者電話番号',
  'projectBase.picPosition': '担当役職',
  'projectBase.picDataManagement': 'データ管理責任者',
  'projectBase.newMembersTotal': '新規社員数（件）',
  'projectBase.withdrawnMembers': '退会社員数（件）',
  'projectBase.cumulativeMembers': '認証対象となる累計社員数（件）',
  'projectBase.membersTotal': '延べ社員数（件）',
  'projectBase.emissionReduction': '削減量',
  'projectBase.baselineEmissions': 'ベースライン排出量',
  'projectBase.postProjectEmission': 'プロジェクト後排出量',
  'projectBase.methodologyVersion': '方法論バージョン',
  'projectBase.methodCategory': '方法論区分',
  'projectBase.methodNumber': '方法論番号',
  'projectBase.methodName': '方法論名称',
  'projectBase.methodMainEmission': '方法論主排出源活動',
  'projectBase.methodCombiFeasibility': '方法論組み合わせ可否',
  'projectBase.methodDetails': '方法論説明',
  'projectBase.operatorCompanyId': '運営会社ID',
};

/**
 * プロジェクト計画書, read-only: every page the wizard fills, as it is stored.
 *
 * Pages are fetched one at a time as the viewer moves, the same way the wizard
 * loads them, and cached so 戻る/次へ does not refetch. Values come back under
 * backend field names and are renamed to the form's before display — without
 * that, every page with a rename showed its renamed fields as empty.
 */
export default function AdminProjectPlanApplicationPage() {
  const { projectId } = useParams();
  const navigate = useNavigate();
  const [step, setStep] = useState(1);

  const projectQuery = useAdminProjectDetailQuery(projectId);
  const progressQuery = useAdminProjectPlanProgressQuery(projectId);
  const project = projectQuery.data;

  const page = getPlanPageForStep(step);
  const pageQuery = useQuery({
    queryKey: ['admin', 'projects', projectId ?? '', 'plan-page', page?.slug ?? ''],
    queryFn: () => getProjectPlanForm(projectId as string, page?.slug ?? ''),
    enabled: Boolean(projectId && page),
    staleTime: 30_000,
    retry: false,
  });

  const progressBySlug = useMemo(
    () => new Map((progressQuery.data?.forms ?? []).map((form) => [form.formSlug, form])),
    [progressQuery.data],
  );
  const completedSteps = useMemo(
    () =>
      (progressQuery.data?.forms ?? [])
        .filter((form) => form.status === 'COMPLETE')
        .map((form) => getPlanPageForSlug(form.formSlug)?.step)
        .filter((planStep): planStep is number => planStep !== undefined),
    [progressQuery.data],
  );

  const currentStep = visibleSteps.find((item) => item.id === step) ?? visibleSteps[0];

  if (projectQuery.isLoading) {
    return (
      <PageSkeleton>
        <div className="mx-auto w-full max-w-admin space-y-5">
          <SkeletonCard className="min-h-24" />
          <SkeletonCard className="min-h-[40rem]" />
        </div>
      </PageSkeleton>
    );
  }

  if (!projectId || projectQuery.isError || !project) {
    return (
      <div className="flex min-h-screen items-center justify-center p-6">
        <Card className="w-full max-w-md">
          <CardHeader>
            <CardTitle>プロジェクトが見つかりません</CardTitle>
            <CardDescription>
              {projectQuery.error instanceof Error
                ? projectQuery.error.message
                : 'プロジェクト情報を取得できませんでした。'}
            </CardDescription>
          </CardHeader>
          <CardContent>
            <PageBackButton
              onClick={() => navigate(paths.adminProjectList)}
              aria-label="プロジェクト管理に戻る"
            />
          </CardContent>
        </Card>
      </div>
    );
  }

  const progress = page ? progressBySlug.get(page.slug) : undefined;
  const pageStatus = page ? (pageQuery.data?.status ?? progress?.status) : undefined;
  const isStored = pageStatus === 'COMPLETE';
  const updatedAt = pageQuery.data?.updatedAt ?? progress?.updatedAt;
  const updatedBy = pageQuery.data?.updatedBy ?? progress?.updatedBy;

  /**
   * 更新 opens the page being read. Step 1 is the project record, edited on its
   * own screen; a plan page opens in the wizard, which resumes this project
   * rather than creating one because it is handed the project. The layout has
   * already checked PROJECT_UPDATE.
   */
  const handleEdit = () => {
    if (!page) {
      navigate(`${paths.adminProjectEdit(projectId)}?page=1`);
      return;
    }
    navigate(paths.adminProjectRegister, {
      state: {
        project: {
          id: projectId,
          projectName: project.projectName,
          projectStatus: project.projectStatus,
        },
        planSlug: page.slug,
      },
    });
  };

  const renderPageBody = () => {
    const fields = currentStep.fields as ReadonlyArray<FieldConfig>;

    if (!page) {
      return (
        <PlanPageValues
          projectId={projectId}
          fields={fields}
          values={{ projectBase: mapDetailToProjectBase(project) }}
          labelOverrides={projectBaseLabelOverrides}
        />
      );
    }

    if (pageQuery.isLoading) {
      return (
        <div role="status" aria-label="計画書のページを読み込んでいます">
          <SkeletonCard className="min-h-48" />
        </div>
      );
    }

    if (pageQuery.isError) {
      return (
        <p role="alert" className="text-sm text-destructive">
          {pageQuery.error instanceof Error
            ? pageQuery.error.message
            : 'このページを取得できませんでした。'}
        </p>
      );
    }

    // Autofill fills a never-saved page from master data; showing that as if
    // the operator had entered it would misreport how far the plan has got.
    if (!isStored) {
      return (
        <p className="rounded-md border border-dashed border-border px-4 py-8 text-center text-sm text-muted-foreground">
          このページはまだ入力されていません。
        </p>
      );
    }

    return (
      <PlanPageValues
        projectId={projectId}
        fields={fields}
        values={{ [page.formKey]: mapPlanPageValues(pageQuery.data?.data, page.renames) }}
        conditionalFields={rulesInFormNames(
          pageQuery.data?.conditionalFields ?? [],
          page.renames,
        )}
      />
    );
  };

  const completed = progressQuery.data?.completedForms ?? 0;
  const total = progressQuery.data?.totalForms ?? 0;

  return (
    <AdminProjectDetailLayout
      projectId={projectId}
      projectName={project.projectName}
      title="プロジェクト計画書"
      description="計画書の入力内容と関連資料を確認します。"
      currentBreadcrumb="計画書"
      backTo={paths.adminProjectDetail(projectId)}
      backLabel="プロジェクト詳細に戻る"
      onEdit={handleEdit}
    >
      <div className="flex flex-col gap-3 sm:flex-row sm:items-end sm:justify-between">
        <PageDropdown
          steps={visibleSteps}
          value={step}
          onChange={(target) => setStep(clampStepId(visibleSteps, Number(target)))}
          completedIds={completedSteps}
          className="w-full sm:w-96"
        />
        {total > 0 ? (
          <p className="text-sm text-muted-foreground">
            入力済み: {completed} / {total} ページ
          </p>
        ) : null}
      </div>

      <FormCard>
        <FormSection
          title={currentStep.title}
          meta={
            page ? (
              <span className="flex flex-wrap items-center gap-2">
                <Badge variant={isStored ? 'default' : 'outline'}>
                  {isStored ? '入力済み' : '未入力'}
                </Badge>
                {progress?.optional ? <Badge variant="secondary">任意</Badge> : null}
              </span>
            ) : null
          }
          description={
            isStored && updatedAt
              ? `最終更新: ${formatProjectDetailDateTime(updatedAt)}${updatedBy ? `（${updatedBy}）` : ''}`
              : undefined
          }
        >
          {renderPageBody()}
        </FormSection>

        <FormActions
          className="pt-8"
          actions={[
            ...(step > 1
              ? [
                  {
                    id: 'back',
                    label: '戻る',
                    variant: 'outline' as const,
                    onClick: () => setStep((current) => adjacentStepId(visibleSteps, current, -1)),
                  },
                ]
              : []),
            ...(step < maxStep
              ? [
                  {
                    id: 'next',
                    label: '次へ',
                    onClick: () => setStep((current) => adjacentStepId(visibleSteps, current, 1)),
                  },
                ]
              : []),
          ]}
        />
      </FormCard>
    </AdminProjectDetailLayout>
  );
}
