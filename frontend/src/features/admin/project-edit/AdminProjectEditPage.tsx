import { useEffect, useMemo, useState } from 'react';
import { useForm, type PathValue, type UseFormReturn } from 'react-hook-form';
import {
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
import { PageDropdown } from '@/components/forms/shared/PageDropdown';
import { FormPageWrapper } from '@/components/layout/FormPageWrapper/FormPageWrapper';
import { PageBackButton } from '@/components/navigation/PageBackButton';
import { Button } from '@/components/ui/button';
import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from '@/components/ui/card';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '@/components/ui/select';
import { Textarea } from '@/components/ui/textarea';
import { useAuth } from '@/contexts/AuthContext';
import { toast } from '@/hooks/use-toast';
import { logger } from '@/lib/logger';
import { paths } from '@/routes/paths';
import { MultiStepForm } from '@/features/admin/project-registration/forms/ProjectMultiPageForm';
import type { AdminProjectRegisterFormInputValues } from '@/features/admin/project-registration/forms/projectFormTypes';
import { steps } from '@/features/admin/project-registration/forms/projectSteps';
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

const visibleSteps = steps.slice(0, 2);
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

function FieldGrid({
  fields,
  form,
}: {
  fields: AdminProjectEditField[];
  form: UseFormReturn<UpdateAdminProjectPayload>;
}) {
  return (
    <div className='grid grid-cols-1 gap-4 md:grid-cols-2'>
      {fields.map((field) => {
        const fieldName = field.name;
        const isFullWidthField = fieldName === 'methodDetails';

        return (
          <div
            key={fieldName}
            className={`space-y-2 ${isFullWidthField ? 'md:col-span-2' : ''}`}
          >
            <Label htmlFor={fieldName}>{field.label}</Label>
            {field.type === 'select' ? (
              <Select
                value={String(form.watch(fieldName) ?? '')}
                onValueChange={(value) => {
                  const selected = field.options?.find(
                    (option) => String(option.value) === value,
                  );

                  form.setValue(
                    fieldName,
                    selected?.value as PathValue<
                      UpdateAdminProjectPayload,
                      typeof fieldName
                    >,
                    { shouldDirty: true },
                  );
                }}
              >
                <SelectTrigger id={fieldName}>
                  <SelectValue placeholder={field.placeholder} />
                </SelectTrigger>
                <SelectContent>
                  {field.options?.map((option) => (
                    <SelectItem
                      key={String(option.value)}
                      value={String(option.value)}
                    >
                      {option.label}
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
            ) : field.type === 'textarea' ? (
              <Textarea
                id={fieldName}
                rows={5}
                placeholder={field.placeholder}
                {...form.register(fieldName)}
              />
            ) : (
              <Input
                id={fieldName}
                type={field.type === 'number' ? 'number' : field.type ?? 'text'}
                step={field.type === 'number' ? 'any' : undefined}
                placeholder={field.placeholder}
                {...form.register(fieldName, {
                  valueAsNumber: field.type === 'number',
                })}
              />
            )}
          </div>
        );
      })}
    </div>
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
  }, [applicationForm, form, processedBy, projectId]);

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

  if (loading) {
    return (
      <div className='flex min-h-screen items-center justify-center'>
        読み込み中...
      </div>
    );
  }

  if (error) {
    return (
      <div className='flex min-h-screen items-center justify-center p-6'>
        <Card className='w-full max-w-md'>
          <CardHeader>
            <CardTitle>プロジェクト情報を取得できませんでした</CardTitle>
            <CardDescription>{error}</CardDescription>
          </CardHeader>
          <CardContent>
            <PageBackButton onClick={() => navigate(-1)} />
          </CardContent>
        </Card>
      </div>
    );
  }

  return (
    <FormPageWrapper
      pageName='プロジェクト情報更新'
      title=''
      onBack={() => navigate(-1)}
      // backText='戻る'
      contentMaxWidth='max-w-3xl'
      wrapInCard={false}
    >
      <form onSubmit={handleSubmit} className='space-y-6'>
        <div className='mb-4'>
          <PageDropdown
            steps={visibleSteps}
            value={step}
            onChange={jumpToStep}
          />
        </div>

        {step === 1 ? (
          <>
        <Card>
          <CardHeader>
            <CardTitle>基本情報</CardTitle>
          </CardHeader>
          <CardContent>
            <FieldGrid fields={adminProjectEditBasicFields} form={form} />
          </CardContent>
        </Card>

        <Card>
          <CardHeader>
            <CardTitle>代表者情報</CardTitle>
          </CardHeader>
          <CardContent>
            <FieldGrid
              fields={adminProjectEditRepresentativeFields}
              form={form}
            />
          </CardContent>
        </Card>

        <Card>
          <CardHeader>
            <CardTitle>責任者情報</CardTitle>
          </CardHeader>
          <CardContent>
            <FieldGrid fields={adminProjectEditPicFields} form={form} />
          </CardContent>
        </Card>

        <Card>
          <CardHeader>
            <CardTitle>方法論情報</CardTitle>
          </CardHeader>
          <CardContent>
            <FieldGrid fields={adminProjectEditMethodologyFields} form={form} />
          </CardContent>
        </Card>
          </>
        ) : (
          <Card>
            <CardContent className='pt-6'>
              <MultiStepForm
                form={applicationForm}
                step={2}
                stepsOverride={visibleSteps}
              />
            </CardContent>
          </Card>
        )}

        <div className='flex items-center justify-between gap-3'>
          <div>
            {step > 1 && (
              <Button
                type='button'
                variant='outline'
                onClick={goBack}
              >
                戻る
              </Button>
            )}
          </div>

          <div className='flex items-center gap-3'>
            <Button type='submit' disabled={submitting}>
              {submitting ? '更新中...' : '更新'}
            </Button>

            {step < maxStep && (
              <Button
                type='button'
                onClick={goNext}
              >
                次へ
              </Button>
            )}
          </div>
        </div>

      </form>
    </FormPageWrapper>
  );
}
