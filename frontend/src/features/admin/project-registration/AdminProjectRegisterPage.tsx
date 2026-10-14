import React, { useState } from 'react';
import { FormProvider, useForm } from 'react-hook-form';
import { useNavigate } from 'react-router-dom';
import {
  mapAdminProjectPayload,
  mapAdminProjectUpdatePayload,
} from '@/api/admin/projectPayloadMapper';
import { uploadProjectFile } from '@/api/admin/projectFiles';
import {
  createAdminProject,
  saveProjectPlanForm,
  updateAdminProject,
} from '@/api/admin/projects';
import type { FieldConfig } from '@/components/forms/shared/GenericFormFields/GenericFormFields';
import {
  FormActions,
  FormCard,
  FormPageContent,
  FormPageTitle,
  FormSection,
  FormSectionStack,
  FormStandalonePage,
} from '@/components/forms/form-layout';
import { usePageBreadcrumb } from '@/components/navigation/pageBreadcrumbContext';
import { useAuth } from '@/contexts/AuthContext';
import { PlanFilePicker } from '@/features/admin/project-plan/PlanFilePicker';
import { PlanSummarySection } from '@/features/admin/project-plan/PlanSummarySection';
import {
  PLAN_SUMMARY_SLUG,
  isPlanSummaryEmpty,
  planSummaryProblem,
  toPlanPagePayload,
  type PlanSummaryFormValues,
} from '@/features/admin/project-plan/planSummary';
import { toast } from '@/hooks/use-toast';
import { paths } from '@/routes/paths';
import { DynamicFormRenderer } from './forms/DynamicFormRenderer';
import { TemporaryAdminProjectAutofillButton } from './forms/TemporaryAdminProjectAutofillButton';
import { applyBackendValidationErrors } from './forms/backendValidationErrors';
import {
  emissionFields,
  membersFields,
  methodologyFields,
  operatorInfo,
  picFields,
  projectFields,
  representativeFields,
} from './forms/projectBaseFields';
import type { AdminProjectRegisterFormInputValues } from './forms/projectFormTypes';

type ProjectFields = FieldConfig<AdminProjectRegisterFormInputValues>[];

/** The project record, grouped the way the operator thinks about it. */
const projectRecordSections: ReadonlyArray<{ title: string; fields: ProjectFields }> = [
  { title: '基本情報', fields: [...projectFields, ...operatorInfo] as ProjectFields },
  { title: '方法論', fields: methodologyFields as ProjectFields },
  {
    title: '代表者・責任者',
    fields: [...representativeFields, ...picFields] as ProjectFields,
  },
  {
    title: '会員数（実績・合計）・排出量',
    fields: [...membersFields, ...emissionFields] as ProjectFields,
  },
];

const errorMessage = (error: unknown, fallback: string) =>
  error instanceof Error && error.message ? error.message : fallback;

/**
 * プロジェクト作成, on one page.
 *
 * The 計画書 is written in the official Excel 様式 and attached here as a file;
 * the system does not ask for its content again. The only plan values typed
 * are the ones the system itself uses — 認証対象期間 and the planned member
 * counts per 年度 — and the rest of the page is the project record.
 *
 * Saving does three things in order: the project record, the file, the plan
 * values. The file and the plan values are stored against the project's id,
 * which does not exist until the record is saved. If a later step fails the
 * project is already there, so the page says which part is missing and 保存
 * picks up from that part instead of creating the project again.
 */
export const AdminProjectRegister: React.FC = () => {
  const { user } = useAuth();
  const navigate = useNavigate();

  usePageBreadcrumb([
    { label: 'プロジェクト', to: paths.adminProjectList },
    { label: 'プロジェクト作成' },
  ]);

  const form = useForm<AdminProjectRegisterFormInputValues>({
    mode: 'onChange',
    defaultValues: {},
  });
  const planForm = useForm<PlanSummaryFormValues>({ defaultValues: {} });

  const [planFile, setPlanFile] = useState<File | null>(null);
  const [uploadedFileName, setUploadedFileName] = useState<string | null>(null);
  const [createdProjectId, setCreatedProjectId] = useState<string | null>(null);
  const [isSaving, setIsSaving] = useState(false);
  const [submitError, setSubmitError] = useState<string | null>(null);
  const [planError, setPlanError] = useState<string | null>(null);

  const fail = (title: string, message: string) => {
    setSubmitError(message);
    toast({ title, description: message, variant: 'destructive' });
  };

  /** Creates the project, or updates it when an earlier attempt already created it. */
  const saveProjectRecord = async (): Promise<string> => {
    const createdBy = String(user?.id ?? '');

    if (createdProjectId) {
      await updateAdminProject(
        createdProjectId,
        mapAdminProjectUpdatePayload(form.getValues(), createdBy),
      );
      return createdProjectId;
    }

    const created = await createAdminProject(mapAdminProjectPayload(form.getValues(), createdBy));
    const id = created?.[0]?.id;
    if (id === undefined || id === null || id === '') {
      throw new Error('Project ID was not returned');
    }
    setCreatedProjectId(String(id));
    return String(id);
  };

  const handleSave = async () => {
    if (isSaving) return;

    setSubmitError(null);
    setPlanError(null);
    form.clearErrors();

    const planValues = planForm.getValues();
    const planProblem = planSummaryProblem(planValues);
    if (planProblem) {
      setPlanError(planProblem);
      fail('入力内容をご確認ください', planProblem);
      return;
    }

    setIsSaving(true);
    try {
      let projectId: string;
      try {
        projectId = await saveProjectRecord();
      } catch (error) {
        const message = applyBackendValidationErrors(form, error);
        fail(
          '保存に失敗しました',
          message ?? errorMessage(error, 'プロジェクトを保存できませんでした。'),
        );
        return;
      }

      if (planFile) {
        try {
          await uploadProjectFile(projectId, 'PROJECT_PLAN', planFile);
          setUploadedFileName(planFile.name);
          setPlanFile(null);
        } catch (error) {
          fail(
            '計画書ファイルをアップロードできませんでした',
            `プロジェクトは保存されましたが、計画書ファイルをアップロードできませんでした。${errorMessage(error, '')}「保存」を押すと、もう一度アップロードします。`,
          );
          return;
        }
      }

      if (!isPlanSummaryEmpty(planValues)) {
        try {
          await saveProjectPlanForm(projectId, PLAN_SUMMARY_SLUG, toPlanPagePayload(planValues));
        } catch (error) {
          const message = errorMessage(error, '');
          setPlanError(message || '認証対象期間・会員数計画を保存できませんでした。');
          fail(
            '認証対象期間・会員数計画を保存できませんでした',
            `プロジェクトは保存されましたが、認証対象期間・会員数計画を保存できませんでした。${message}「保存」を押すと、もう一度保存します。`,
          );
          return;
        }
      }

      toast({
        title: '保存しました',
        description: 'プロジェクトを作成しました。',
        variant: 'success',
      });
      navigate(paths.adminProjectDetail(projectId));
    } finally {
      setIsSaving(false);
    }
  };

  return (
    <FormStandalonePage>
      <FormPageContent>
        <FormPageTitle align='center'>プロジェクト作成</FormPageTitle>

        <div className='flex justify-end'>
          <TemporaryAdminProjectAutofillButton form={form} />
        </div>

        <FormCard>
          <FormSectionStack spacing='relaxed'>
            <FormSection
              number={1}
              title='計画書ファイル'
              description='Excelで作成したプロジェクト計画書をアップロードします。保存時に送信されます。'
            >
              <PlanFilePicker file={planFile} onChange={setPlanFile} disabled={isSaving} />
              {uploadedFileName ? (
                <p role='status' className='text-sm text-muted-foreground'>
                  アップロード済み: {uploadedFileName}
                </p>
              ) : null}
            </FormSection>

            <FormSection
              number={2}
              title='認証対象期間・会員数計画'
              description='計画書に記載した認証対象期間と、年度ごとの会員数（計画値）を入力します。'
            >
              <PlanSummarySection form={planForm} disabled={isSaving} error={planError} />
            </FormSection>

            <FormProvider {...form}>
              {projectRecordSections.map((section, index) => (
                <FormSection key={section.title} number={index + 3} title={section.title}>
                  <DynamicFormRenderer<AdminProjectRegisterFormInputValues>
                    fields={section.fields}
                  />
                </FormSection>
              ))}
            </FormProvider>

            {submitError ? (
              <p role='alert' className='text-sm text-destructive'>
                {submitError}
              </p>
            ) : null}

            <FormActions
              actions={[
                {
                  id: 'save',
                  label: isSaving ? '保存中...' : '保存',
                  onClick: () => void handleSave(),
                  disabled: isSaving,
                },
              ]}
            />
          </FormSectionStack>
        </FormCard>
      </FormPageContent>
    </FormStandalonePage>
  );
};
