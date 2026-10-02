import { useEffect, useMemo, useState } from 'react';
import { useForm, type UseFormReturn } from 'react-hook-form';
import {
  Link,
  useLocation,
  useNavigate,
  useParams,
  useSearchParams,
} from 'react-router-dom';
import {
  saveProjectPlanForm,
  getAdminProjectDetail,
  getProjectPlanForm,
  updateAdminProject,
  type AdminProjectDetail,
  type ProjectPlanFormResponse,
  type UpdateAdminProjectPayload,
} from '@/api/admin';
import { mapAdminProjectPlanApplicationPayload } from '@/api/admin/projectPayloadMapper';
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
import { PageDropdown } from '@/components/forms/shared/PageDropdown';
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
import { Form } from '@/components/ui/form';
import { useAuth } from '@/contexts/AuthContext';
import { toast } from '@/hooks/use-toast';
import { logger } from '@/lib/logger';
import { paths } from '@/routes/paths';
import { MultiStepForm } from '@/features/admin/project-registration/forms/ProjectMultiPageForm';
import type { AdminProjectRegisterFormInputValues } from '@/features/admin/project-registration/forms/projectFormTypes';
import { steps } from '@/features/admin/project-registration/forms/projectSteps';
import { stepsForApplicationCategory } from '@/features/admin/project-registration/forms/applicationCategory';
import {
  adminProjectEditBasicFields,
  adminProjectEditMethodologyFields,
  adminProjectEditPicFields,
  adminProjectEditRepresentativeFields,
  type AdminProjectEditField,
} from './projectEditFields';

type LocationState = {
  project?: Project;
};

const visibleSteps = stepsForApplicationCategory(steps.slice(0, 2));
const maxStep = visibleSteps.length;

const textFallback = (value: unknown) => (value == null ? '' : String(value));

const numberFallback = (value: unknown) => {
  if (value == null || value === '') return 0;

  const numberValue = typeof value === 'number' ? value : Number(value);
  return Number.isFinite(numberValue) ? numberValue : 0;
};

const firstPresent = (...values: unknown[]) =>
  values.find((value) => value != null && value !== '');

const isRecord = (value: unknown): value is Record<string, unknown> =>
  Boolean(value) && typeof value === 'object' && !Array.isArray(value);

const resolveApplicationData = (
  response: ProjectPlanFormResponse,
): AdminProjectRegisterFormInputValues['projectApplication'] => {
  // The plan endpoint returns the page under `data`; the older `application`
  // and `projectApplication` shapes it used to fall back on were never sent.
  const value = response.data ?? {};

  return (isRecord(value) ? value : {}) as AdminProjectRegisterFormInputValues['projectApplication'];
};

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

const mapEditValuesToProjectBase = (
  values: UpdateAdminProjectPayload,
): AdminProjectRegisterFormInputValues['projectBase'] =>
  ({
    projectName: values.projectName,
    projectStatus: textFallback((values as { projectStatus?: unknown }).projectStatus),
    regionalCategory: values.regionalCategory,
    baselineEmissions: values.baselineEmissions,
    postProjectEmission: values.postProjectEmission,
    repFirstName: values.repFirstName,
    repFirstNameKana: values.repFirstNameKana,
    repLastName: values.repLastName,
    repLastNameKana: values.repLastNameKana,
    repEmail: values.repEmail,
    repPhoneNumber: values.repPhoneNumber,
    repPosition: values.repPosition,
    picFirstName: values.picFirstName,
    picFirstNameKana: values.picFirstNameKana,
    picLastName: values.picLastName,
    picLastNameKana: values.picLastNameKana,
    picEmail: values.picEmail,
    picPhoneNumber: values.picPhoneNumber,
    picPosition: values.picPosition,
    picDataManagement: values.dataManagementPic,
    methodologyVersion: values.methodologyVersion,
    methodCategory: values.methodCategory,
    methodNumber: values.methodNumber,
    methodName: values.methodName,
    methodMainEmission: values.methodMainEmission,
    methodCombiFeasibility: values.methodCombiFeasibility,
    methodDetails: values.methodDetails,
  }) as AdminProjectRegisterFormInputValues['projectBase'];

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

export default function AdminProjectEditPage() {
  const { projectId } = useParams();
  const navigate = useNavigate();
  const location = useLocation();
  const [searchParams, setSearchParams] = useSearchParams();
  const { user } = useAuth();
  const [loading, setLoading] = useState(true);
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);
  // Bumped by 再試行 to rerun the load effect after a failed fetch.
  const [reloadKey, setReloadKey] = useState(0);
  const [step, setStep] = useState(() => {
    const page = Number(searchParams.get('page'));
    return page >= 1 && page <= maxStep ? page : 1;
  });

  const processedBy = user?.id ? String(user.id) : 'admin';
  const stateProject = (location.state as LocationState | null)?.project;

  const defaultValues = useMemo(
    () => mapListProjectToValues(stateProject, processedBy),
    [processedBy, stateProject],
  );

  const form = useForm<UpdateAdminProjectPayload>({
    defaultValues,
  });

  const applicationForm = useForm<AdminProjectRegisterFormInputValues>({
    defaultValues: {},
  });

  useEffect(() => {
    const page = Number(searchParams.get('page'));
    const nextStep = page >= 1 && page <= maxStep ? page : 1;
    setStep(nextStep);
  }, [searchParams]);

  useEffect(() => {
    form.reset(defaultValues);
    applicationForm.setValue('projectBase', mapEditValuesToProjectBase(defaultValues), {
      shouldDirty: false,
    });
  }, [applicationForm, defaultValues, form]);

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
        const projectBaseValues = mapDetailToValues(detail, processedBy);
        form.reset(projectBaseValues);
        applicationForm.reset({
          projectBase: mapEditValuesToProjectBase(projectBaseValues),
        });

        try {
          const applicationResponse =
            await getProjectPlanForm(projectId, 'application');

          if (!isMounted) return;
          applicationForm.reset({
            projectBase: mapEditValuesToProjectBase(projectBaseValues),
            projectApplication: resolveApplicationData(applicationResponse),
          });
        } catch (applicationError) {
          logger.error(
            'Error loading admin project application for edit:',
            applicationError,
          );
          toast({
            title: 'プロジェクト申請情報を取得できませんでした',
            description:
              applicationError instanceof Error
                ? applicationError.message
                : undefined,
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
  }, [applicationForm, form, processedBy, projectId, reloadKey]);

  const jumpToStep = (target: number | string) => {
    const targetStep = Number(target);
    const nextStep = Math.min(Math.max(targetStep, 1), maxStep);

    setStep(nextStep);
    setSearchParams({ page: String(nextStep) });
  };

  const goBack = () => jumpToStep(step - 1);

  const goNext = () => jumpToStep(step + 1);

  const handleProjectBaseSubmit = form.handleSubmit(async (values) => {
    if (!projectId) return;

    try {
      setSubmitting(true);
      await updateAdminProject(projectId, {
        ...values,
        processedBy,
        baselineEmissions: numberFallback(values.baselineEmissions),
        postProjectEmission: numberFallback(values.postProjectEmission),
        methodCombiFeasibility: Boolean(values.methodCombiFeasibility),
      });

      toast({
        title: '更新しました',
        description: 'プロジェクト情報を更新しました。',
        variant: 'success',
      });
      navigate(paths.adminProjectDetail(projectId));
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
    } finally {
      setSubmitting(false);
    }
  });

  const handleProjectApplicationSubmit = applicationForm.handleSubmit(
    async (values) => {
      if (!projectId) return;

      try {
        setSubmitting(true);
        const mergedValues: AdminProjectRegisterFormInputValues = {
          ...values,
          projectBase: mapEditValuesToProjectBase(form.getValues()),
        };

        await saveProjectPlanForm(
          projectId,
          'application',
          mapAdminProjectPlanApplicationPayload(mergedValues),
        );

        toast({
          title: '更新しました',
          description: 'プロジェクト申請情報を更新しました。',
          variant: 'success',
        });
        navigate(paths.adminProjectDetail(projectId));
      } catch (e) {
        logger.error('Error updating admin project application:', e);
        toast({
          title: '更新に失敗しました',
          description:
            e instanceof Error
              ? e.message
              : 'プロジェクト申請情報を更新できませんでした。',
          variant: 'destructive',
        });
      } finally {
        setSubmitting(false);
      }
    },
  );

  const handleSubmit =
    step === 1 ? handleProjectBaseSubmit : handleProjectApplicationSubmit;

  const detailPath = projectId
    ? paths.adminProjectDetail(projectId)
    : paths.adminProjectList;
  const projectName = form.getValues('projectName');

  return (
    <FormStandalonePage>
      <PageBackButton
        onClick={() => navigate(detailPath)}
        label=''
        aria-label='プロジェクト詳細に戻る'
        className={floatingPageBackButtonClassName}
      />
      <FormPageContent>
        <Breadcrumb>
          <BreadcrumbList className='flex-nowrap'>
            <BreadcrumbItem>
              <BreadcrumbLink asChild>
                <Link to={paths.adminProjectList}>プロジェクト管理</Link>
              </BreadcrumbLink>
            </BreadcrumbItem>
            <BreadcrumbSeparator />
            <BreadcrumbItem className='min-w-0'>
              <BreadcrumbLink asChild>
                <Link
                  to={detailPath}
                  className='block max-w-40 truncate sm:max-w-72'
                >
                  {!loading && !error && projectName ? projectName : 'プロジェクト詳細'}
                </Link>
              </BreadcrumbLink>
            </BreadcrumbItem>
            <BreadcrumbSeparator />
            <BreadcrumbItem>
              <BreadcrumbPage>編集</BreadcrumbPage>
            </BreadcrumbItem>
          </BreadcrumbList>
        </Breadcrumb>

        <div>
          <FormPageTitle as='h1'>プロジェクト情報の編集</FormPageTitle>
          <p className='mt-2 text-sm leading-6 text-muted-foreground sm:text-base'>
            プロジェクトの基本情報と申請情報を編集します。
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
            <Button
              type='button'
              variant='outline'
              size='md'
              className='mt-5'
              onClick={() => setReloadKey((key) => key + 1)}
            >
              再試行
            </Button>
          </FormCard>
        ) : (
          <>
            <PageDropdown
              steps={visibleSteps}
              value={step}
              onChange={jumpToStep}
            />

            <Form {...form}>
              <form onSubmit={handleSubmit} noValidate>
                <FormCard>
                  <FormSectionStack spacing='relaxed'>
                    {step === 1 ? (
                      projectBaseSections.map((section, index) => (
                        <ProjectBaseSection
                          key={section.title}
                          number={index + 1}
                          title={section.title}
                          fields={section.fields}
                          form={form}
                        />
                      ))
                    ) : (
                      <MultiStepForm
                        form={applicationForm}
                        step={2}
                        stepsOverride={visibleSteps}
                      />
                    )}

                    <FormActions
                      disabled={submitting}
                      pendingActionId={submitting ? 'update' : null}
                      actions={[
                        ...(step > 1
                          ? [
                              {
                                id: 'back',
                                label: '戻る',
                                variant: 'outline' as const,
                                onClick: goBack,
                              },
                            ]
                          : []),
                        {
                          id: 'update',
                          label: '更新',
                          pendingLabel: '更新中...',
                          type: 'submit',
                        },
                        ...(step < maxStep
                          ? [
                              {
                                id: 'next',
                                label: '次へ',
                                variant: 'outline' as const,
                                onClick: goNext,
                              },
                            ]
                          : []),
                      ]}
                    />
                  </FormSectionStack>
                </FormCard>
              </form>
            </Form>
          </>
        )}
      </FormPageContent>
    </FormStandalonePage>
  );
}
