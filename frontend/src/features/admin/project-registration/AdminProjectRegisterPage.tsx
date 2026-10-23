import React, { useState } from 'react';
import { useForm } from 'react-hook-form';
import { useNavigate } from 'react-router-dom';
import { mapAdminProjectPayload } from '@/api/admin/projectPayloadMapper';
import { createAdminProject } from '@/api/admin/projects';
import { usePageBreadcrumb } from '@/components/navigation/pageBreadcrumbContext';
import { useAuth } from '@/contexts/AuthContext';
import { toast } from '@/hooks/use-toast';
import { paths } from '@/routes/paths';
import { ProjectRecordForm, ProjectRecordPage } from './forms/ProjectRecordForm';
import { TemporaryAdminProjectAutofillButton } from './forms/TemporaryAdminProjectAutofillButton';
import { applyBackendValidationErrors } from './forms/backendValidationErrors';
import type { AdminProjectRegisterFormInputValues } from './forms/projectFormTypes';

/** The project's 書類 tab, with the upload dialog open for the 計画書. */
const planUploadPath = (projectId: string) =>
  `${paths.adminProjectDetail(projectId)}?tab=documents&upload=PROJECT_PLAN`;

const errorMessage = (error: unknown, fallback: string) =>
  error instanceof Error && error.message ? error.message : fallback;

/**
 * プロジェクト作成, on one page.
 *
 * What is typed here is the project record. The 計画書 itself is written in the
 * official Excel 様式 — 認証対象期間 and the member counts included — and the
 * system does not ask for its content again. A file needs a project to belong
 * to, so it is uploaded after 保存, in the project's 書類 tab, which opens next.
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

  const [isSaving, setIsSaving] = useState(false);
  const [submitError, setSubmitError] = useState<string | null>(null);

  const handleSave = async () => {
    if (isSaving) return;

    setSubmitError(null);
    form.clearErrors();

    setIsSaving(true);
    try {
      const created = await createAdminProject(
        mapAdminProjectPayload(form.getValues(), String(user?.id ?? '')),
      );
      const id = created?.[0]?.id;
      if (id === undefined || id === null || id === '') {
        throw new Error('Project ID was not returned');
      }

      toast({
        title: 'プロジェクトを作成しました',
        description: '続けて計画書ファイルをアップロードできます。',
        variant: 'success',
      });
      navigate(planUploadPath(String(id)));
    } catch (error) {
      const message =
        applyBackendValidationErrors(form, error) ??
        errorMessage(error, 'プロジェクトを保存できませんでした。');
      setSubmitError(message);
      toast({ title: '保存に失敗しました', description: message, variant: 'destructive' });
    } finally {
      setIsSaving(false);
    }
  };

  return (
    <ProjectRecordPage
      title='プロジェクト作成'
      // toolbar={<TemporaryAdminProjectAutofillButton form={form} />}
    >
      <ProjectRecordForm
        form={form}
        planFileNote='計画書ファイル（Excel）は、プロジェクトを保存した後、プロジェクト詳細の「書類」タブでアップロードします。「保存」を押すと、アップロード画面が開きます。'
        submitLabel='保存'
        submittingLabel='保存中...'
        submitting={isSaving}
        submitError={submitError}
        onSubmit={() => void handleSave()}
      />
    </ProjectRecordPage>
  );
};
