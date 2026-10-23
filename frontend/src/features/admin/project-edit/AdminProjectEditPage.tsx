import { useEffect, useState } from 'react';
import { useForm } from 'react-hook-form';
import { useNavigate, useParams } from 'react-router-dom';
import { getAdminProjectDetail, updateAdminProject } from '@/api/admin';
import { mapAdminProjectUpdatePayload } from '@/api/admin/projectPayloadMapper';
import { ActionButton } from '@/components/actions';
import { SkeletonCard } from '@/components/feedback/loading';
import { FormCard } from '@/components/forms/form-layout';
import { usePageBreadcrumb } from '@/components/navigation/pageBreadcrumbContext';
import { useAuth } from '@/contexts/AuthContext';
import { toast } from '@/hooks/use-toast';
import { logger } from '@/lib/logger';
import { paths } from '@/routes/paths';
import {
  ProjectRecordForm,
  ProjectRecordPage,
} from '@/features/admin/project-registration/forms/ProjectRecordForm';
import { applyBackendValidationErrors } from '@/features/admin/project-registration/forms/backendValidationErrors';
import { mapDetailToProjectBase } from '@/features/admin/project-registration/forms/projectBaseFromDetail';
import type { AdminProjectRegisterFormInputValues } from '@/features/admin/project-registration/forms/projectFormTypes';

/**
 * Shown, but not editable here: the update API does not take 番号 or 運営会社,
 * and ステータス has its own action with its own permission.
 */
const LOCKED_FIELDS = [
  'projectBase.projectNumber',
  'projectBase.projectStatus',
  'projectBase.operatorCompanyId',
] as const;

const errorMessage = (error: unknown, fallback: string) =>
  error instanceof Error && error.message ? error.message : fallback;

/**
 * プロジェクト更新 — the same page as プロジェクト作成, filled in.
 *
 * The 計画書 file is not here: files are uploaded in the project's 書類 tab,
 * the one place that manages them.
 */
export default function AdminProjectEditPage() {
  const { projectId } = useParams();
  const navigate = useNavigate();
  const { user } = useAuth();
  const [loading, setLoading] = useState(true);
  const [loadError, setLoadError] = useState<string | null>(null);
  const [isSaving, setIsSaving] = useState(false);
  const [submitError, setSubmitError] = useState<string | null>(null);
  // Bumped by 再試行 to rerun the load effect after a failed fetch.
  const [reloadKey, setReloadKey] = useState(0);

  const processedBy = user?.id || 'admin';

  const form = useForm<AdminProjectRegisterFormInputValues>({
    mode: 'onChange',
    defaultValues: {},
  });

  useEffect(() => {
    let isMounted = true;

    const loadProject = async () => {
      if (!projectId) {
        setLoadError('プロジェクトIDが見つかりません。');
        setLoading(false);
        return;
      }

      try {
        setLoading(true);
        setLoadError(null);
        const detail = await getAdminProjectDetail(projectId);

        if (!isMounted) return;
        form.reset({
          projectBase: mapDetailToProjectBase(detail),
        } as AdminProjectRegisterFormInputValues);
      } catch (e) {
        logger.error('Error loading admin project for edit:', e);
        if (!isMounted) return;
        setLoadError(errorMessage(e, 'プロジェクト情報を取得できませんでした。'));
      } finally {
        if (isMounted) setLoading(false);
      }
    };

    loadProject();

    return () => {
      isMounted = false;
    };
  }, [form, projectId, reloadKey]);

  const handleSave = async () => {
    if (!projectId || isSaving) return;

    setSubmitError(null);
    form.clearErrors();

    setIsSaving(true);
    try {
      await updateAdminProject(
        projectId,
        mapAdminProjectUpdatePayload(form.getValues(), processedBy),
      );

      toast({
        title: '更新しました',
        description: 'プロジェクト情報を更新しました。',
        variant: 'success',
      });
      navigate(paths.adminProjectDetail(projectId));
    } catch (error) {
      logger.error('Error updating admin project:', error);
      const message =
        applyBackendValidationErrors(form, error) ??
        errorMessage(error, 'プロジェクト情報を更新できませんでした。');
      // Field errors are listed by FormErrorSummary; a generic toast on top only confuses.
      if (Object.keys(form.formState.errors).length === 0) {
        setSubmitError(message);
        toast({ title: '更新に失敗しました', description: message, variant: 'destructive' });
      }
    } finally {
      setIsSaving(false);
    }
  };

  const detailPath = projectId
    ? paths.adminProjectDetail(projectId)
    : paths.adminProjectList;
  const projectName = form.getValues('projectBase.projectName');

  usePageBreadcrumb([
    { label: 'プロジェクト', to: paths.adminProjectList },
    {
      label: !loading && !loadError && projectName ? projectName : 'プロジェクト詳細',
      to: detailPath,
    },
    { label: 'プロジェクト更新' },
  ]);

  return (
    <ProjectRecordPage title='プロジェクト更新'>
      {loading ? (
        <SkeletonCard label='プロジェクト情報を読み込んでいます' className='min-h-96' />
      ) : loadError ? (
        <FormCard className='text-center'>
          <p className='text-sm text-foreground'>
            プロジェクト情報を表示できませんでした。
          </p>
          <p className='mt-1 text-sm text-muted-foreground'>{loadError}</p>
          <ActionButton
            kind='retry'
            className='mt-5'
            onClick={() => setReloadKey((key) => key + 1)}
          />
        </FormCard>
      ) : (
        <ProjectRecordForm
          form={form}
          lockedFields={LOCKED_FIELDS}
          planFileNote='計画書ファイル（Excel）は、プロジェクト詳細の「書類」タブで管理します。'
          submitLabel='更新'
          submittingLabel='更新中...'
          submitting={isSaving}
          submitError={submitError}
          onSubmit={() => void handleSave()}
        />
      )}
    </ProjectRecordPage>
  );
}
