import React, { useCallback, useEffect, useMemo, useState } from 'react';
import { useForm, useWatch, type FieldPath } from 'react-hook-form';
import { useLocation, useNavigate } from 'react-router-dom';
import type { Project } from '@/api/projects';
import {
  mapAdminProjectPayload,
  mapAdminProjectPlanApplicationPayload,
  mapAdminProjectUpdatePayload,
  mapPlanPagePayload,
  mapPlanPageValues,
} from '@/api/admin/projectPayloadMapper';
import {
  createAdminProject,
  getProjectPlanAutofillByProject,
  getProjectPlanAutofillDefaults,
  getProjectPlanForm,
  getProjectPlanProgress,
  saveProjectPlanForm,
  updateAdminProject,
  uploadProjectPlanFormFile,
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
import {
  FIRST_PLAN_STEP,
  LAST_IMPLEMENTED_STEP,
  getPlanPageForSlug,
  getPlanPageForStep,
  type ProjectPlanPage,
} from './forms/projectPlanPages';
import type { ProjectPlanConditionalField } from '@/api/admin/types';
import { steps } from './forms/projectSteps';

type LocationState = {
  project?: Project;
  /**
   * Page to open on arrival. Omitted means "resume": the wizard picks the
   * first unfinished page itself.
   */
  planSlug?: string;
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
  const [isLoadingPlanPage, setIsLoadingPlanPage] = useState(false);
  const [submitError, setSubmitError] = useState<string | null>(null);
  const [createdProjectId, setCreatedProjectId] = useState<string | null>(null);
  const [savedProjectBaseSnapshot, setSavedProjectBaseSnapshot] = useState<
    string | null
  >(null);
  // Steps already saved on the server, so a returning operator can see how far
  // they got. Populated from the plan's progress endpoint.
  const [completedSteps, setCompletedSteps] = useState<number[]>([]);
  // Fields the backend fills from SQL; rendered read-only so edits are not
  // silently overwritten on the next read.
  const [readOnlyFields, setReadOnlyFields] = useState<string[]>([]);
  // 有/無 rules from the backend: a field only applies when its toggle matches.
  const [conditionalFields, setConditionalFields] = useState<
    ProjectPlanConditionalField[]
  >([]);
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
    if (!state?.project) return;

    // Annotated so the literal fields keep the form's narrow types; a bare
    // object literal widens them to string and no longer fits reset().
    const projectBase: NonNullable<
      AdminProjectRegisterFormInputValues['projectBase']
    > = {
      projectName: state.project.projectName,
      projectNumber: state.project.id,
      projectStatus: getProjectStatusValue(state.project.projectStatus),
      methodCategory: 'EN-S',
      methodNumber: '002',
      methodName: '太陽光発電設備の導入',
      methodMainEmission: 'CO2削減',
      methodCombiFeasibility: false,
      methodologyVersion: 'Ver.2.0',
    };
    form.reset({ projectBase });

    // Arriving with a project means resuming it, not starting a new one.
    // Without this the wizard treats the id as unset and 次へ creates a
    // duplicate project instead of updating this one.
    if (state.project.id) {
      // This base information came from the server, so it already counts as
      // saved. Without the snapshot, leaving step 1 would push an update built
      // from the placeholder values above.
      setSavedProjectBaseSnapshot(toSnapshot(projectBase));
      void resumePlan(String(state.project.id), state.planSlug);
    }
    // resumePlan is stable for a given project; re-running on every render
    // would refetch progress in a loop.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [state?.project, state?.planSlug, form]);

  // Only the steps the plan API actually serves; the rest of `steps` has no
  // backend page yet.
  const visibleSteps = steps.slice(0, LAST_IMPLEMENTED_STEP);
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
        .map((planForm) => getPlanPageForSlug(planForm.formSlug)?.step)
        .filter((planStep): planStep is number => planStep !== undefined);
      setCompletedSteps(done);
      setPlanProgress({
        completed: progress.completedForms ?? 0,
        total: progress.totalForms ?? 0,
      });
      return progress;
    } catch (e) {
      console.warn('Failed to load project plan progress', e);
      return undefined;
    }
  };

  /**
   * Reopens an existing project's plan.
   *
   * Seeding `createdProjectId` is what stops 次へ from creating a second
   * project: saveProjectBase branches on it, so without it a resume would
   * silently duplicate the record. It is also what lets the page-loading
   * effect below fetch anything at all.
   *
   * Where it lands: the caller's page if they named one, otherwise the first
   * unfinished page. A finished plan has neither, so it opens on the first
   * plan page and the operator picks from the dropdown — which is the point of
   * 計画書更新.
   */
  const resumePlan = async (projectId: string, requestedSlug?: string) => {
    setCreatedProjectId(projectId);

    const progress = await refreshProgress(projectId);
    const slug = requestedSlug ?? progress?.nextIncompleteSlug ?? undefined;
    const target = slug ? getPlanPageForSlug(slug)?.step : undefined;

    setStep(target ?? FIRST_PLAN_STEP);
  };

  /**
   * The application page keeps a bespoke mapper: it normalises booleans and
   * reconciles several legacy field aliases. Every other page goes through the
   * generic mapper driven by `PROJECT_PLAN_PAGES`.
   */
  const buildPagePayload = (
    page: ProjectPlanPage,
    values: AdminProjectRegisterFormInputValues,
  ): Record<string, unknown> =>
    page.slug === 'application'
      ? mapAdminProjectPlanApplicationPayload(values)
      : mapPlanPagePayload(
          values as Record<string, unknown>,
          page.formKey,
          page.renames,
        );

  /**
   * Reads one page back from the server.
   *
   * Returns what the caller should apply rather than applying it itself, so a
   * response arriving after the operator has already moved on can be dropped.
   */
  const fetchPlanPage = async (projectId: string, page: ProjectPlanPage) => {
    const response = await getProjectPlanForm(projectId, page.slug);
    return {
      values: mapPlanPageValues(response.data, page.renames),
      readOnlyFields: response.readOnlyFields ?? [],
      conditionalFields: response.conditionalFields ?? [],
    };
  };

  /**
   * Loads the current page every time the operator moves.
   *
   * This is what makes the wizard usable rather than just navigable. Without
   * it a page opens carrying the *previous* page's read-only and 有/無 rules,
   * the fields the backend fills from PostgreSQL arrive empty, and anything
   * already saved comes back blank — so 次へ is rejected and the operator
   * cannot get past the page at all.
   */
  useEffect(() => {
    if (!createdProjectId) return;

    // Step 1 is the SQL project record, not a plan page. It has no server-side
    // rules, and keeping the previous page's would disable the wrong inputs.
    const page = getPlanPageForStep(step);
    if (!page) {
      setReadOnlyFields([]);
      setConditionalFields([]);
      return;
    }

    let cancelled = false;
    setIsLoadingPlanPage(true);

    fetchPlanPage(createdProjectId, page)
      .then((loaded) => {
        if (cancelled) return;

        // Merge rather than replace: autofill has already put master-data
        // values into this page, and a record saved before a field existed
        // would otherwise blank it out on load.
        if (Object.keys(loaded.values).length > 0) {
          const key =
            page.formKey as FieldPath<AdminProjectRegisterFormInputValues>;
          const current = form.getValues(key) as Record<string, unknown> | undefined;
          form.setValue(
            key,
            { ...(current ?? {}), ...loaded.values } as never,
            { shouldDirty: false },
          );
        }
        setReadOnlyFields(loaded.readOnlyFields);
        setConditionalFields(loaded.conditionalFields);
      })
      .catch((e) => {
        // Best-effort: a failure must not trap the operator on the page.
        console.warn('Failed to load project plan page', page.slug, e);
        if (cancelled) return;
        setReadOnlyFields([]);
        setConditionalFields([]);
      })
      .finally(() => {
        if (!cancelled) setIsLoadingPlanPage(false);
      });

    return () => {
      cancelled = true;
    };
    // `form` is stable for the life of the component; re-running on it would
    // refetch the page on every keystroke.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [createdProjectId, step]);

  /**
   * Stages one page. The server stores it only if the whole page validates, so
   * a rejection means nothing was persisted and the operator stays put.
   */
  const savePlanPage = async (
    projectId: string,
    targetStep: number,
    values: AdminProjectRegisterFormInputValues,
  ): Promise<boolean> => {
    // Steps with no plan page (step 1) are saved elsewhere; nothing to stage.
    const page = getPlanPageForStep(targetStep);
    if (!page) return true;

    try {
      await saveProjectPlanForm(projectId, page.slug, buildPagePayload(page, values));
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
    // Step 1 is the SQL project record, not a page of the plan. Saving it is
    // what yields the project id everything else is keyed by.
    if (step === 1) {
      const projectId =
        createdProjectId && isProjectBaseSaved
          ? createdProjectId
          : await saveProjectBase();

      if (!projectId) return;

      await loadProjectApplication(projectId);
      return;
    }

    if (!createdProjectId) return;

    // Pages are atomic: only move on once this one is actually stored, so the
    // operator can never advance past a page the server rejected.
    const saved = await savePlanPage(createdProjectId, step, form.getValues());
    if (!saved) return;

    if (step < maxStep) {
      setStep(step + 1);
      return;
    }

    setConfirmationStep(1);
    setMode('confirm');
  };

  /**
   * Uploads a file for whichever page is open.
   *
   * The key comes back pointing at temporary storage and is promoted when the
   * page saves, so an upload on a page that is never completed costs nothing.
   */
  const uploadPlanFile = useCallback(
    async (fieldName: string, file: File) => {
      const page = getPlanPageForStep(step);
      if (!createdProjectId || !page) {
        throw new Error('先にプロジェクトを保存してください。');
      }
      // Config names are namespaced and a few differ from the backend's.
      const bare = fieldName.split('.').pop() ?? fieldName;
      return uploadProjectPlanFormFile(
        createdProjectId,
        page.slug,
        page.renames?.[bare] ?? bare,
        file,
      );
    },
    [createdProjectId, step],
  );

  const handleConfirm = async () => {
    await onSubmit(form.getValues());
  };

  const goBack = () => setStep((currentStep) => Math.max(currentStep - 1, 1));

  const jumpToStep = async (target: number | string) => {
    const targetStep = Number(target);

    // Leaving step 1 has to persist the project first — every plan page is
    // keyed by its id. Once that is done the operator goes to the page they
    // actually asked for, not to step 2: being able to open any page directly
    // is what makes a finished plan freely editable.
    if (targetStep > 1 && step === 1) {
      const projectId =
        createdProjectId && isProjectBaseSaved
          ? createdProjectId
          : await saveProjectBase();
      if (!projectId) return;
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

      // Every page, including the last, was already stored on the way here —
      // goNext will not advance past a page the server rejected. So this is a
      // confirmation step, not another write; just re-read where things stand.
      await refreshProgress(createdProjectId);

      toast({
        title: '保存しました',
        description: 'プロジェクト計画書を保存しました。',
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
              readOnlyFields={readOnlyFields}
              conditionalFields={conditionalFields}
              onUploadFile={uploadPlanFile}
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
                  disabled={
                    isSubmitting || isLoadingApplication || isLoadingPlanPage
                  }
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
                  isSavingProjectBase ||
                  isLoadingApplication ||
                  isLoadingPlanPage ||
                  isSubmitting
                }
              >
                {isSavingProjectBase
                  ? '保存中...'
                  : isLoadingApplication || isLoadingPlanPage
                    ? '読み込み中...'
                    : step === maxStep
                      ? '確認へ'
                      : '次へ'}
              </Button>
            </FormActions>
          </FormSectionStack>
        </FormCard>
      </FormPageContent>
    </FormStandalonePage>
  );
};
