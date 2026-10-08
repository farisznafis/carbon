import { useEffect, useMemo, useState } from 'react';
import { useForm, type UseFormReturn } from 'react-hook-form';
import { useLocation, useNavigate, useParams } from 'react-router-dom';
import {
  saveProjectPlanForm,
  getAdminProjectDetail,
  getProjectPlanForm,
  updateAdminProject,
  type AdminProjectDetail,
  type UpdateAdminProjectPayload,
} from '@/api/admin';
import type { Project } from '@/api/projects';
import { LoadingPanel } from '@/components/feedback/loading';
import {
  FormActions,
  FormCard,
  FormGrid,
  FormPageContent,
  FormPageTitle,
  FormSection,
  FormSectionStack,
  FormStandalonePage,
} from '@/components/forms/form-layout';
import { GenericFormFields } from '@/components/forms/shared/GenericFormFields/GenericFormFields';
import { usePageBreadcrumb } from '@/components/navigation/pageBreadcrumbContext';
import { Form } from '@/components/ui/form';
import { useAuth } from '@/contexts/AuthContext';
import { toast } from '@/hooks/use-toast';
import { logger } from '@/lib/logger';
import { paths } from '@/routes/paths';
import { PlanFileList } from '@/features/admin/project-plan/PlanFileList';
import { PlanSummarySection } from '@/features/admin/project-plan/PlanSummarySection';
import {
  PLAN_SUMMARY_SLUG,
  fromPlanPageData,
  isPlanSummaryEmpty,
  planSummaryProblem,
  toPlanPagePayload,
  type PlanSummaryFormValues,
} from '@/features/admin/project-plan/planSummary';
import {
  adminProjectEditBasicFields,
  adminProjectEditMethodologyFields,
  adminProjectEditPicFields,
  adminProjectEditRepresentativeFields,
  type AdminProjectEditField,
} from './projectEditFields';
import { ActionButton } from '@/components/actions';

type LocationState = {
  project?: Project;
};

const textFallback = (value: unknown) => (value == null ? '' : String(value));

const numberFallback = (value: unknown) => {
  if (value == null || value === '') return 0;

  const numberValue = typeof value === 'number' ? value : Number(value);
  return Number.isFinite(numberValue) ? numberValue : 0;
};

const firstPresent = (...values: unknown[]) =>
  values.find((value) => value != null && value !== '');

const createEmptyValues = (processedBy: string): UpdateAdminProjectPayload => ({
  processedBy,
  projectName: '',
  baselineEmissions: 0,
  postProjectEmission: 0,
  repFirstName: '',
  repFirstNameKana: '',
  repLastName: '',
  repLastNameKana: '',
  repEmail: '',
  repPhoneNumber: '',
  repPosition: '',
  picFirstName: '',
  picFirstNameKana: '',
  picLastName: '',
  picLastNameKana: '',
  picEmail: '',
  picPhoneNumber: '',
  picPosition: '',
  dataManagementPic: '',
  regionalCategory: '',
  methodologyVersion: '',
  methodCategory: '',
  methodNumber: '',
  methodName: '',
  methodMainEmission: '',
  methodCombiFeasibility: false,
  methodDetails: '',
});

const mapListProjectToValues = (
  project: Project | undefined,
  processedBy: string,
): UpdateAdminProjectPayload => ({
  ...createEmptyValues(processedBy),
  projectName: textFallback(project?.projectName),
  baselineEmissions: numberFallback(project?.baselineEmissions),
  postProjectEmission: numberFallback(project?.postProjectEmission),
  methodologyVersion: textFallback(project?.methodologyVersion),
  methodCategory: textFallback(project?.methodCategory),
  methodNumber: textFallback(project?.methodNumber),
  methodName: textFallback(project?.methodName),
  methodMainEmission: textFallback(project?.methodMainEmission),
  methodCombiFeasibility: Boolean(project?.methodCombiFeasibility),
});

const mapDetailToValues = (
  detail: AdminProjectDetail,
  processedBy: string,
): UpdateAdminProjectPayload => ({
  processedBy,
  projectName: textFallback(detail.projectName),
  baselineEmissions: numberFallback(
    firstPresent(detail.baselineEmissions, detail.baselineEmission),
  ),
  postProjectEmission: numberFallback(
    firstPresent(detail.postProjectEmission, detail.postProjectEmissions),
  ),
  repFirstName: textFallback(detail.repFirstName),
  repFirstNameKana: textFallback(detail.repFirstNameKana),
  repLastName: textFallback(detail.repLastName),
  repLastNameKana: textFallback(detail.repLastNameKana),
  repEmail: textFallback(detail.repEmail),
  repPhoneNumber: textFallback(detail.repPhoneNumber),
  repPosition: textFallback(detail.repPosition),
  picFirstName: textFallback(detail.picFirstName),
  picFirstNameKana: textFallback(detail.picFirstNameKana),
  picLastName: textFallback(detail.picLastName),
  picLastNameKana: textFallback(detail.picLastNameKana),
  picEmail: textFallback(detail.picEmail),
  picPhoneNumber: textFallback(detail.picPhoneNumber),
  picPosition: textFallback(detail.picPosition),
  dataManagementPic: textFallback(detail.dataManagementPic),
  regionalCategory: textFallback(detail.regionalCategory),
  methodologyVersion: textFallback(detail.methodologyVersion),
  methodCategory: textFallback(detail.methodologyCategory),
  methodNumber: textFallback(detail.methodologyNumber),
  methodName: textFallback(detail.methodologyName),
  methodMainEmission: textFallback(detail.methodologyMainEmission),
  methodCombiFeasibility: Boolean(detail.methodCombiFeasibility),
  methodDetails: textFallback(detail.methodDetails),
});

const projectBaseSections = [
  { title: '基本情報', fields: adminProjectEditBasicFields },
  { title: '代表者情報', fields: adminProjectEditRepresentativeFields },
  { title: '責任者情報', fields: adminProjectEditPicFields },
  { title: '方法論情報', fields: adminProjectEditMethodologyFields },
] as const;

/** Renders one numbered project-record section with the shared form field components. */
function ProjectBaseSection({
  number,
  title,
  fields,
  form,
}: Readonly<{
  number: number;
  title: string;
  fields: readonly AdminProjectEditField[];
  form: UseFormReturn<UpdateAdminProjectPayload>;
}>) {
  // Textareas span the full row, so they sit below the two-column grid.
  const gridFields = fields.filter((field) => field.type !== 'textarea');
  const fullWidthFields = fields.filter((field) => field.type === 'textarea');

  return (
    <FormSection number={number} title={title}>
      {gridFields.length > 0 && (
        <FormGrid>
          <GenericFormFields form={form} fields={gridFields} />
        </FormGrid>
      )}
      {fullWidthFields.length > 0 && (
        <GenericFormFields form={form} fields={fullWidthFields} />
      )}
    </FormSection>
  );
}

/**
 * プロジェクト情報の編集, on one page.
 *
 * The same sections as project creation: the 計画書 file, the plan values the
 * system keeps (認証対象期間 and the planned member counts per 年度), then the
 * project record. A file is uploaded as soon as it is chosen — the project
 * exists, so there is nothing to wait for — while 更新 saves the record and
 * then the plan values.
 */
export default function AdminProjectEditPage() {
  const { projectId } = useParams();
  const navigate = useNavigate();
  const location = useLocation();
  const { user } = useAuth();
  const [loading, setLoading] = useState(true);
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [planError, setPlanError] = useState<string | null>(null);
  // The unit stored with the plan page; sent back unchanged on save.
  const [planUnitEmission, setPlanUnitEmission] = useState<string | undefined>(undefined);
  // Bumped by 再試行 to rerun the load effect after a failed fetch.
  const [reloadKey, setReloadKey] = useState(0);

  const processedBy = user?.id ? String(user.id) : 'admin';
  const stateProject = (location.state as LocationState | null)?.project;

  const defaultValues = useMemo(
    () => mapListProjectToValues(stateProject, processedBy),
    [processedBy, stateProject],
  );

  const form = useForm<UpdateAdminProjectPayload>({
    defaultValues,
  });

  const planForm = useForm<PlanSummaryFormValues>({ defaultValues: {} });

  useEffect(() => {
    form.reset(defaultValues);
  }, [defaultValues, form]);

  useEffect(() => {
    let isMounted = true;

    const loadProject = async () => {
      if (!projectId) {
        setError('プロジェクトIDが見つかりません。');
        setLoading(false);
        return;
      }

      try {
        setLoading(true);
        setError(null);
        const detail = await getAdminProjectDetail(projectId);

        if (!isMounted) return;
        form.reset(mapDetailToValues(detail, processedBy));

        // The plan values are a separate record. Failing to read them must not
        // keep the operator from editing the project itself.
        try {
          const planResponse = await getProjectPlanForm(projectId, PLAN_SUMMARY_SLUG);

          if (!isMounted) return;
          const loaded = fromPlanPageData(planResponse.data);
          planForm.reset(loaded.values);
          setPlanUnitEmission(loaded.unitEmission);
        } catch (planLoadError) {
          logger.error('Error loading admin project plan values for edit:', planLoadError);
          toast({
            title: '認証対象期間・会員数計画を取得できませんでした',
            description: planLoadError instanceof Error ? planLoadError.message : undefined,
            variant: 'destructive',
          });
        }
      } catch (e) {
        logger.error('Error loading admin project for edit:', e);
        if (!isMounted) return;
        setError(
          e instanceof Error
            ? e.message
            : 'プロジェクト情報を取得できませんでした。',
        );
      } finally {
        if (isMounted) setLoading(false);
      }
    };

    loadProject();

    return () => {
      isMounted = false;
    };
  }, [form, planForm, processedBy, projectId, reloadKey]);

  const handleSubmit = form.handleSubmit(async (values) => {
    if (!projectId) return;

    setPlanError(null);
    const planValues = planForm.getValues();
    const planProblem = planSummaryProblem(planValues);
    if (planProblem) {
      setPlanError(planProblem);
      toast({
        title: '入力内容をご確認ください',
        description: planProblem,
        variant: 'destructive',
      });
      return;
    }

    try {
      setSubmitting(true);
      await updateAdminProject(projectId, {
        ...values,
        processedBy,
        baselineEmissions: numberFallback(values.baselineEmissions),
        postProjectEmission: numberFallback(values.postProjectEmission),
        methodCombiFeasibility: Boolean(values.methodCombiFeasibility),
      });
    } catch (e) {
      logger.error('Error updating admin project:', e);
      toast({
        title: '更新に失敗しました',
        description:
          e instanceof Error
            ? e.message
            : 'プロジェクト情報を更新できませんでした。',
        variant: 'destructive',
      });
      setSubmitting(false);
      return;
    }

    try {
      if (!isPlanSummaryEmpty(planValues)) {
        await saveProjectPlanForm(
          projectId,
          PLAN_SUMMARY_SLUG,
          toPlanPagePayload(planValues, planUnitEmission),
        );
      }

      toast({
        title: '更新しました',
        description: 'プロジェクト情報を更新しました。',
        variant: 'success',
      });
      navigate(paths.adminProjectDetail(projectId));
    } catch (e) {
      logger.error('Error updating admin project plan values:', e);
      const message =
        e instanceof Error && e.message
          ? e.message
          : '認証対象期間・会員数計画を更新できませんでした。';
      setPlanError(message);
      toast({
        title: '認証対象期間・会員数計画を更新できませんでした',
        description: `プロジェクト情報は更新されました。${message}`,
        variant: 'destructive',
      });
    } finally {
      setSubmitting(false);
    }
  });

  const detailPath = projectId
    ? paths.adminProjectDetail(projectId)
    : paths.adminProjectList;
  const projectName = form.getValues('projectName');

  usePageBreadcrumb([
    { label: 'プロジェクト管理', to: paths.adminProjectList },
    {
      label: !loading && !error && projectName ? projectName : 'プロジェクト詳細',
      to: detailPath,
    },
    { label: '編集' },
  ]);

  return (
    <FormStandalonePage>
      <FormPageContent>

        <div>
          <FormPageTitle as='h1'>プロジェクト情報の編集</FormPageTitle>
          <p className='mt-2 text-sm leading-6 text-muted-foreground sm:text-base'>
            計画書ファイル、認証対象期間・会員数計画、プロジェクトの基本情報を編集します。
          </p>
        </div>

        {loading ? (
          <LoadingPanel message='プロジェクト情報を読み込んでいます' />
        ) : error ? (
          <FormCard className='text-center'>
            <p className='text-sm text-foreground'>
              プロジェクト情報を表示できませんでした。
            </p>
            <p className='mt-1 text-sm text-muted-foreground'>{error}</p>
            <ActionButton
              kind='retry'
              className='mt-5'
              onClick={() => setReloadKey((key) => key + 1)}
            />
          </FormCard>
        ) : (
          <Form {...form}>
            <form onSubmit={handleSubmit} noValidate>
              <FormCard>
                <FormSectionStack spacing='relaxed'>
                  <FormSection
                    number={1}
                    title='計画書ファイル'
                    description='Excelで作成したプロジェクト計画書です。選択するとすぐにアップロードされ、以前のファイルは残ります。'
                  >
                    {projectId ? <PlanFileList projectId={projectId} canUpload /> : null}
                  </FormSection>

                  <FormSection
                    number={2}
                    title='認証対象期間・会員数計画'
                    description='計画書に記載した認証対象期間と、年度ごとの会員数（計画値）を入力します。'
                  >
                    <PlanSummarySection
                      form={planForm}
                      disabled={submitting}
                      error={planError}
                    />
                  </FormSection>

                  {projectBaseSections.map((section, index) => (
                    <ProjectBaseSection
                      key={section.title}
                      number={index + 3}
                      title={section.title}
                      fields={section.fields}
                      form={form}
                    />
                  ))}

                  <FormActions
                    disabled={submitting}
                    pendingActionId={submitting ? 'update' : null}
                    actions={[
                      {
                        id: 'update',
                        label: '更新',
                        pendingLabel: '更新中...',
                        type: 'submit',
                      },
                    ]}
                  />
                </FormSectionStack>
              </FormCard>
            </form>
          </Form>
        )}
      </FormPageContent>
    </FormStandalonePage>
  );
}
