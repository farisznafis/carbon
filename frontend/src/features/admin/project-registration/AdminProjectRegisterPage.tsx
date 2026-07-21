import React, { useEffect, useMemo, useState } from 'react';
import { useForm, useWatch, type FieldPath } from 'react-hook-form';
import { useLocation, useNavigate } from 'react-router-dom';
import type { Project } from '@/api/projects';
import {
  mapAdminProjectPayload,
  mapAdminProjectPlanApplicationPayload,
} from '@/api/admin/projectPayloadMapper';
import {
  createAdminProject,
  createAdminProjectPlanApplication,
  getProjectPlanAutofillByProject,
  getProjectPlanAutofillDefaults,
} from '@/api/admin/projects';
import { ConfirmationItem } from '@/components/feedback/Confirmation/ConfirmationItem';
import { ConfirmationLayout } from '@/components/feedback/Confirmation/ConfirmationLayout';
import { PageDropdown } from '@/components/forms/shared/PageDropdown';
import { FormPageWrapper } from '@/components/layout/FormPageWrapper/FormPageWrapper';
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
      if (!createdProjectId || !isProjectBaseSaved) {
        showErrorToast(
          '保存が必要です',
          '次へ進む前にプロジェクト基本情報を保存してください。',
        );
        return;
      }

      await loadProjectApplication(createdProjectId);
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

      await createAdminProjectPlanApplication(
        createdProjectId,
        mapAdminProjectPlanApplicationPayload(values),
      );
      toast({
        title: '保存しました',
        description: 'プロジェクト申請情報を保存しました。',
      });
    } catch (e) {
      const message = applyBackendValidationErrors(form, e);
      setSubmitError(message);
      showErrorToast(
        '保存に失敗しました',
        message ??
          (e instanceof Error
            ? e.message
            : '申請情報を保存できませんでした。'),
      );
    } finally {
      setIsSubmitting(false);
    }
  };

  const onSave = async () => {
    setIsSavingProjectBase(true);
    setSubmitError(null);
    form.clearErrors();

    try {
      const createdBy = String(user?.id ?? '');
      const projectPayload = mapAdminProjectPayload(form.getValues(), createdBy);
      const createdProjects = await createAdminProject(projectPayload);
      const projectId = createdProjects?.[0]?.id;

      if (projectId === undefined || projectId === null || projectId === '') {
        throw new Error('Project ID was not returned');
      }

      setCreatedProjectId(String(projectId));
      setSavedProjectBaseSnapshot(projectBaseSnapshot);
      toast({
        title: '保存しました',
        description: 'プロジェクト基本情報を保存しました。',
      });
    } catch (e) {
      setCreatedProjectId(null);
      setSavedProjectBaseSnapshot(null);
      const message = applyBackendValidationErrors(form, e);
      setSubmitError(message);
      showErrorToast(
        '保存に失敗しました',
        message ??
          (e instanceof Error
            ? e.message
            : 'プロジェクト基本情報を保存できませんでした。'),
      );
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
        {submitError && <p className='text-sm text-red-600'>{submitError}</p>}
      </ConfirmationLayout>
    );
  }

  return (
    <FormPageWrapper
      pageName='プロジェクト登録申請'
      title=''
      onBack={() => navigate(paths.adminDashboard)}
      backText='戻る'
      contentMaxWidth='max-w-3xl'
    >
      <div className='mb-4 flex items-center justify-between gap-3'>
        <PageDropdown
          steps={visibleSteps}
          value={step}
          onChange={jumpToStep}
        />
        {step === 1 && <TemporaryAdminProjectAutofillButton form={form} />}
        {step === 2 && (
          <TemporaryAdminProjectApplicationAutofillButton form={form} />
        )}
      </div>

      <MultiStepForm
        form={form}
        step={step}
        stepsOverride={visibleSteps}
      />

      <div className='mt-6 flex justify-between gap-2'>
        {step === 1 && (
          <Button
            type='button'
            variant='outline'
            onClick={onSave}
            disabled={isSavingProjectBase}
          >
            {isSavingProjectBase ? '保存中...' : '保存'}
          </Button>
        )}
        {step > 1 && (
          <Button
            type='button'
            variant='outline'
            onClick={goBack}
          >
            戻る
          </Button>
        )}
        <Button
          type='button'
          onClick={goNext}
          disabled={
            isSavingProjectBase ||
            isLoadingApplication ||
            (step === 1 && !isProjectBaseSaved)
          }
        >
          {step === maxStep
            ? '確認へ'
            : isLoadingApplication
              ? '読み込み中...'
              : '次へ'}
        </Button>
      </div>
      {submitError && <p className='text-sm text-red-600'>{submitError}</p>}
    </FormPageWrapper>
  );
};
