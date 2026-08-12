import React, { useEffect, useMemo, useState } from 'react';
import { useForm, useWatch, type FieldPath } from 'react-hook-form';
import { useLocation, useNavigate } from 'react-router-dom';
import type { Project } from '@/api/projects';
import {
  mapAdminProjectPayload,
  mapAdminProjectPlanApplicationPayload,
  mapAdminProjectUpdatePayload,
} from '@/api/admin/projectPayloadMapper';
import {
  createAdminProject,
  getProjectPlanAutofillByProject,
  getProjectPlanAutofillDefaults,
  getProjectPlanProgress,
  saveProjectPlanForm,
  updateAdminProject,
} from '@/api/admin/projects';
import { ConfirmationItem } from '@/components/feedback/Confirmation/ConfirmationItem';
import { ConfirmationLayout } from '@/components/feedback/Confirmation/ConfirmationLayout';
import { PageDropdown } from '@/components/forms/shared/PageDropdown';
import {
  FormActions,
  FormCard,
  FormPageContent,
  FormPageTitle,
  FormSectionStack,
  FormStandalonePage,
} from '@/components/forms/form-layout';
import { PageBackButton } from '@/components/navigation/PageBackButton';
import { Button } from '@/components/ui/button';
import { useAuth } from '@/contexts/AuthContext';
import { toast } from '@/hooks/use-toast';
import { paths } from '@/routes/paths';
import { MultiStepForm } from './forms/ProjectMultiPageForm';
import {
  TemporaryAdminProjectApplicationAutofillButton,
  TemporaryAdminProjectAutofillButton,
} from './forms/TemporaryAdminProjectAutofillButton';
import { applyBackendValidationErrors } from './forms/backendValidationErrors';
import type { AdminProjectRegisterFormInputValues } from './forms/projectFormTypes';
import { ProjectStatusSchema } from './forms/projectBaseSchema';
import { getFormSlugForStep, getStepForFormSlug } from './forms/projectPlanFormSlugs';
import { steps } from './forms/projectSteps';

type LocationState = {
  project?: Project;
};

export interface AdminProjectRegisterPage {
  id: string;
  label: string;
  content: React.ReactNode;
}

export interface AdminProjectRegisterProps {
  pages: AdminProjectRegisterPage[];
  initialPageId?: string;
  onPageChange?: (pageId: string) => void;
}

const getProjectStatusValue = (status: string) => {
  const result = ProjectStatusSchema.safeParse(status);
  return result.success ? result.data : ProjectStatusSchema.options[0];
};

const toSnapshot = (value: unknown) => JSON.stringify(value ?? {});

const getAutofillData = (response: unknown): Record<string, unknown> => {
  if (!response || typeof response !== 'object') {
    return {};
  }

  const responseRecord = response as {
    data?: unknown;
  };
  const value = responseRecord.data;

  if (!value || typeof value !== 'object' || Array.isArray(value)) {
    return {};
  }

  return value as Record<string, unknown>;
};

const getConfirmationDisplayValue = (
  value: unknown,
  options?: { label: string; value: unknown }[],
) => {
  if (value === null || value === undefined || value === '') {
    return '-';
  }

  const selectedOption = options?.find((option) => Object.is(option.value, value));
  return selectedOption?.label ?? String(value);
};

export const AdminProjectRegister: React.FC = () => {
  const { user } = useAuth();
  const navigate = useNavigate();
  const [mode, setMode] = useState<'form' | 'confirm'>('form');
  const [step, setStep] = useState(1);
  const [confirmationStep, setConfirmationStep] = useState(1);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [isSavingProjectBase, setIsSavingProjectBase] = useState(false);
  const [isLoadingApplication, setIsLoadingApplication] = useState(false);
  const [submitError, setSubmitError] = useState<string | null>(null);
  const [createdProjectId, setCreatedProjectId] = useState<string | null>(null);
  const [savedProjectBaseSnapshot, setSavedProjectBaseSnapshot] = useState<
    string | null
  >(null);
  // Steps already saved on the server, so a returning operator can see how far
  // they got. Populated from the plan's progress endpoint.
  const [completedSteps, setCompletedSteps] = useState<number[]>([]);
  const [planProgress, setPlanProgress] = useState<{
    completed: number;
    total: number;
  } | null>(null);
  const { state } = useLocation() as { state?: LocationState };

  const form = useForm<AdminProjectRegisterFormInputValues>({
    mode: 'onChange',
    defaultValues: {},
  });

  useEffect(() => {
    if (state?.project) {
      form.reset({
        projectBase: {
          projectName: state.project.projectName,
          projectNumber: state.project.id,
          projectStatus: getProjectStatusValue(state.project.projectStatus),
          methodCategory: 'EN-S',
          methodNumber: '002',
          methodName: '太陽光発電設備の導入',
          methodMainEmission: 'CO2削減',
          methodCombiFeasibility: false,
          methodologyVersion: 'Ver.2.0',
        },
      });
    }
  }, [state?.project, form]);

  const visibleSteps = steps.slice(0, 2);
  const maxStep = visibleSteps.length;
  const projectBaseValues = useWatch({
    control: form.control,
    name: 'projectBase',
  });
  const projectBaseSnapshot = useMemo(
    () => toSnapshot(projectBaseValues),
    [projectBaseValues],
  );
  const isProjectBaseSaved =
    !!createdProjectId && savedProjectBaseSnapshot === projectBaseSnapshot;

  const showErrorToast = (title: string, description?: string | null) => {
    toast({
      title,
      description: description || undefined,
      variant: 'destructive',
    });
  };

  /**
   * Refreshes which pages are already stored. Best-effort: progress is a
   * convenience, so a failure here must not block filling the form.
   */
  const refreshProgress = async (projectId: string) => {
    try {
      const progress = await getProjectPlanProgress(projectId);
      const done = (progress.forms ?? [])
        .filter((planForm) => planForm.status === 'COMPLETE')
        .map((planForm) => getStepForFormSlug(planForm.formSlug))
        .filter((planStep): planStep is number => planStep !== undefined);
      setCompletedSteps(done);
      setPlanProgress({
        completed: progress.completedForms ?? 0,
        total: progress.totalForms ?? 0,
      });
    } catch (e) {
      console.warn('Failed to load project plan progress', e);
    }
  };

  const APPLICATION_STEP = 2;

  /**
   * Stages the プロジェクト申請 page. The server stores it only if the whole page
   * validates, so a rejection means nothing was persisted and the operator
   * stays where they are.
   *
   * Each page needs its own payload mapper; when steps 3+ are added to
   * `visibleSteps`, give them a mapper and save them the same way.
   */
  const saveApplicationPage = async (
    projectId: string,
    values: AdminProjectRegisterFormInputValues,
  ): Promise<boolean> => {
    const slug = getFormSlugForStep(APPLICATION_STEP);
    if (!slug) return true;

    try {
      await saveProjectPlanForm(
        projectId,
        slug,
        mapAdminProjectPlanApplicationPayload(values),
      );
      await refreshProgress(projectId);
      return true;
    } catch (e) {
      const message = applyBackendValidationErrors(form, e);
      setSubmitError(message);
      showErrorToast(
        '保存に失敗しました',
        message ??
          (e instanceof Error ? e.message : 'この画面の内容を保存できませんでした。'),
      );
      return false;
    }
  };

  const loadProjectApplication = async (projectId: string) => {
    setIsLoadingApplication(true);
    setSubmitError(null);

    try {
      const [defaultsResponse, projectResponse] = await Promise.all([
        getProjectPlanAutofillDefaults(),
        getProjectPlanAutofillByProject(projectId),
      ]);
      const projectApplication = {
        ...getAutofillData(defaultsResponse),
        ...getAutofillData(projectResponse),
      } as AdminProjectRegisterFormInputValues['projectApplication'];

      if (Object.keys(projectApplication).length > 0) {
        form.setValue('projectApplication', projectApplication, {
          shouldDirty: false,
        });
      }

      // Show which pages are already stored, so a returning operator can tell
      // how far they got before continuing.
      await refreshProgress(projectId);

      setStep(2);
    } catch (e) {
      const message =
        e instanceof Error
          ? e.message
          : '申請情報を取得できませんでした。';
      setSubmitError(message);
      showErrorToast('申請情報を取得できませんでした', message);
    } finally {
      setIsLoadingApplication(false);
    }
  };

  const goNext = async () => {
    if (step < maxStep) {
      const projectId =
        createdProjectId && isProjectBaseSaved
          ? createdProjectId
          : await saveProjectBase();

      if (!projectId) return;

      await loadProjectApplication(projectId);
      return;
    }

    setConfirmationStep(1);
    setMode('confirm');
  };

  const handleConfirm = async () => {
    await onSubmit(form.getValues());
  };

  const goBack = () => setStep((currentStep) => Math.max(currentStep - 1, 1));

  const jumpToStep = async (target: number | string) => {
    const targetStep = Number(target);

    if (targetStep > 1 && step === 1) {
      await goNext();
      return;
    }

    setStep(() => Math.min(Math.max(targetStep, 1), maxStep));
  };

  const jumpToConfirmationStep = async (target: number | string) => {
    const targetStep = Number(target);
    setConfirmationStep(() => Math.min(Math.max(targetStep, 1), maxStep));
  };

  const backToForm = () => {
    setStep(confirmationStep);
    setMode('form');
  };

  const onSubmit = async (values: AdminProjectRegisterFormInputValues) => {
    setIsSubmitting(true);
    setSubmitError(null);
    form.clearErrors();

    try {
      if (!createdProjectId) {
        throw new Error('Project ID is required to submit project application');
      }

      // saveApplicationPage surfaces its own errors, so only report success here.
      const saved = await saveApplicationPage(createdProjectId, values);
      if (!saved) {
        return;
      }

      toast({
        title: '保存しました',
        description: 'プロジェクト申請情報を保存しました。',
        variant: 'success',
      });
    } finally {
      setIsSubmitting(false);
    }
  };

  const saveProjectBase = async (): Promise<string | null> => {
    if (isSavingProjectBase) return null;

    setIsSavingProjectBase(true);
    setSubmitError(null);
    form.clearErrors();

    try {
      const createdBy = String(user?.id ?? '');
      let projectId = createdProjectId;

      if (projectId) {
        await updateAdminProject(
          projectId,
          mapAdminProjectUpdatePayload(form.getValues(), createdBy),
        );
      } else {
        const projectPayload = mapAdminProjectPayload(
          form.getValues(),
          createdBy,
        );
        const createdProjects = await createAdminProject(projectPayload);
        const returnedProjectId = createdProjects?.[0]?.id;

        if (
          returnedProjectId === undefined ||
          returnedProjectId === null ||
          returnedProjectId === ''
        ) {
          throw new Error('Project ID was not returned');
        }

        projectId = String(returnedProjectId);
      }

      setCreatedProjectId(projectId);
      setSavedProjectBaseSnapshot(projectBaseSnapshot);
      toast({
        title: '保存しました',
        description: 'プロジェクト基本情報を保存しました。',
        variant: 'success',
      });
      return projectId;
    } catch (e) {
      if (!createdProjectId) {
        setCreatedProjectId(null);
        setSavedProjectBaseSnapshot(null);
      }
      const message = applyBackendValidationErrors(form, e);
      setSubmitError(message);
      showErrorToast(
        '保存に失敗しました',
        message ??
          (e instanceof Error
            ? e.message
            : 'プロジェクト基本情報を保存できませんでした。'),
      );
      return null;
    } finally {
      setIsSavingProjectBase(false);
    }
  };

  if (mode === 'confirm') {
    return (
      <ConfirmationLayout
        pageName='プロジェクト作成の確認'
        title='入力内容'
        description='以下の内容をご確認ください。'
        onBack={backToForm}
        backText='戻る'
        onConfirm={handleConfirm}
        isSubmitting={isSubmitting}
      >
        <PageDropdown
          steps={visibleSteps}
          value={confirmationStep}
          onChange={jumpToConfirmationStep}
        />
        {visibleSteps
          .find((visibleStep) => visibleStep.id === confirmationStep)
          ?.fields.map((field) => {
            const value = form.getValues(
              field.name as FieldPath<AdminProjectRegisterFormInputValues>,
            );
            const display = getConfirmationDisplayValue(value, field.options);

            return (
              <ConfirmationItem
                key={String(field.name)}
                label={field.label}
                value={display}
              />
            );
          })}
        {submitError && <p className='text-sm text-destructive'>{submitError}</p>}
      </ConfirmationLayout>
    );
  }

  return (
    <FormStandalonePage>
      <PageBackButton
        onClick={() => navigate(paths.adminDashboard)}
        label=''
        className='absolute left-4 top-10 h-12 w-12 rounded-full p-0 sm:left-10 lg:left-16'
      />

      <FormPageContent>
        <FormPageTitle align='center'>プロジェクト登録申請</FormPageTitle>

        <div className='flex items-center justify-between gap-3'>
          <PageDropdown
            steps={visibleSteps}
            value={step}
            onChange={jumpToStep}
            completedIds={completedSteps}
          />
          {step === 1 && <TemporaryAdminProjectAutofillButton form={form} />}
          {step === 2 && (
            <TemporaryAdminProjectApplicationAutofillButton form={form} />
          )}
        </div>

        {planProgress && planProgress.total > 0 && (
          <p className='text-sm text-muted-foreground'>
            入力済み: {planProgress.completed} / {planProgress.total} ページ
          </p>
        )}

        <FormCard>
          <FormSectionStack spacing='relaxed'>
            <MultiStepForm
              form={form}
              step={step}
              stepsOverride={visibleSteps}
            />

            {submitError && (
              <p className='text-sm text-destructive'>{submitError}</p>
            )}

            <FormActions
              className={
                step > 1
                  ? 'grid grid-cols-1 gap-3 sm:grid-cols-2'
                  : 'grid grid-cols-1 gap-3'
              }
            >
              {step > 1 && (
                <Button
                  type='button'
                  variant='outline'
                  size='md'
                  className='w-full'
                  onClick={goBack}
                  disabled={isSubmitting || isLoadingApplication}
                >
                  戻る
                </Button>
              )}
              <Button
                type='button'
                size='md'
                className='w-full'
                onClick={goNext}
                disabled={
                  isSavingProjectBase || isLoadingApplication || isSubmitting
                }
              >
                {step === maxStep
                  ? '確認へ'
                  : isSavingProjectBase
                    ? '保存中...'
                    : isLoadingApplication
                      ? '読み込み中...'
                      : '次へ'}
              </Button>
            </FormActions>
          </FormSectionStack>
        </FormCard>
      </FormPageContent>
    </FormStandalonePage>
  );
};
