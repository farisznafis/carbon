import { useEffect, useRef, useState } from 'react';
import { useLocation, useNavigate, useParams } from 'react-router-dom';
import {
  MonitoringConfirmationLayout,
} from '@/features/user/monitoring/forms/MonitoringConfirmation';
import {
  ProjectType,
  formDefinitions,
} from '@/features/user/monitoring/forms/formDefinitions';
import {
  submitMonitoringData,
  uploadElpjBatteryFile,
  uploadElpjControlFile,
  uploadElpvFile,
  uploadElpvrFile,
} from '@/api/monitoring';
import { ApiError } from '@/api/_shared/errors';
import { useAuth } from '@/contexts/AuthContext';
import { useToast } from '@/hooks/use-toast';
import type { MonitoringData } from '@/api/monitoring';
import { logger } from '@/lib/logger';

type MonitoringFormData = typeof formDefinitions[ProjectType]['emptyData'];
type RouteState = {
  projectId?: string | number;
  siteId?: string | number;
  userId?: string | number;
};

function isProjectType(value: string | undefined): value is ProjectType {
  return Boolean(value && value in formDefinitions);
}

function asRecord(data: MonitoringFormData): Record<string, unknown> {
  return data as Record<string, unknown>;
}

function toErrorMessage(value: unknown): string {
  if (Array.isArray(value)) {
    return value.map(toErrorMessage).filter(Boolean).join('\n');
  }

  if (value && typeof value === 'object' && 'message' in value) {
    return toErrorMessage((value as { message: unknown }).message);
  }

  return typeof value === 'string' ? value : '';
}

function toServerFieldErrors(errors: unknown): Record<string, string> {
  if (!errors || typeof errors !== 'object' || Array.isArray(errors)) {
    return {};
  }

  return Object.entries(errors as Record<string, unknown>).reduce<
    Record<string, string>
  >((fieldErrors, [field, value]) => {
    const message = toErrorMessage(value);

    if (message) {
      fieldErrors[field] = message;
    }

    return fieldErrors;
  }, {});
}

const MonitoringRegister = () => {
  const navigate = useNavigate();
  const location = useLocation();
  const { projectType } = useParams<{ projectType: string }>();
  const { toast } = useToast();
  const { user } = useAuth();
  const definition = isProjectType(projectType)
    ? formDefinitions[projectType]
    : null;

  const [step, setStep] = useState<'form' | 'confirm'>('form');
  const [formData, setFormData] = useState<MonitoringFormData>(
    definition?.emptyData ?? formDefinitions.solar.emptyData,
  );
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [uploadProgress, setUploadProgress] = useState({ done: 0, total: 0 });
  const [submitError, setSubmitError] = useState<string | null>(null);
  const [serverFieldErrors, setServerFieldErrors] = useState<Record<string, string>>({});
  const originalDataRef = useRef<MonitoringFormData | null>(null);

  useEffect(() => {
    if (!definition) return;

    setFormData(definition.emptyData);
    originalDataRef.current = definition.emptyData;
  }, [definition]);

  if (!definition || !projectType) {
    return (
      <div className='flex min-h-screen items-center justify-center'>
        <p className='text-muted-foreground'>
          未対応のプロジェクト種別です
        </p>
      </div>
    );
  }

  const {
    Form,
    getConfirmationItems,
  } = definition;

  const handleConfirm = async () => {
    const state = (location.state || {}) as RouteState;
    const search = new URLSearchParams(location.search);
    const currentData = asRecord(formData);

    const projectIdValue =
      state.projectId ?? search.get('projectId') ?? currentData.projectId ?? '';
    const siteIdValue =
      state.siteId ?? search.get('siteId') ?? currentData.siteId ?? '';
    const descriptionValue = String(currentData.description ?? '');

    const uploadKeys: string[] = [];
    const uploadPromises: Array<Promise<string | null>> = [];

    const addUpload = (
      key: string,
      value: unknown,
      upload: (file: File) => Promise<string>,
    ) => {
      if (!(value instanceof File)) return;

      uploadKeys.push(key);
      uploadPromises.push(
        upload(value)
          .then((url) => {
            setUploadProgress((progress) => ({
              ...progress,
              done: progress.done + 1,
            }));
            return url;
          })
          .catch((error: unknown) => {
            logger.error(`${key} upload failed`, error);
            setUploadProgress((progress) => ({
              ...progress,
              done: progress.done + 1,
            }));
            return null;
          }),
      );
    };

    // Ensure projectId/siteId are passed as strings to satisfy upload API typings
    const projectIdStr = String(projectIdValue);
    const siteIdStr = String(siteIdValue);

    addUpload('ELpv_evidence', currentData.ELpv_evidence, (file) =>
      uploadElpvFile(file, projectIdStr, siteIdStr, descriptionValue),
    );
    addUpload('ELpvr_evidence', currentData.ELpvr_evidence, (file) =>
      uploadElpvrFile(file, projectIdStr, siteIdStr, descriptionValue),
    );
    addUpload('ELPJ_battery_evidence', currentData.ELPJ_battery_evidence, (file) =>
      uploadElpjBatteryFile(file, projectIdStr, siteIdStr, descriptionValue),
    );
    addUpload(
      'ELPJ_control_equipment_spec_files',
      currentData.ELPJ_control_equipment_spec_files,
      (file) =>
        uploadElpjControlFile(file, projectIdStr, siteIdStr, descriptionValue),
    );

    if (uploadPromises.length > 0 && (!projectIdValue || !siteIdValue)) {
      setSubmitError(
        'アップロードには projectId と siteId が必要です。プロジェクトまたはサイトを選択してください。',
      );
      return;
    }

    setSubmitError(null);
    setServerFieldErrors({});
    setUploadProgress({ done: 0, total: uploadPromises.length });
    setIsSubmitting(true);

    const uploadResults = uploadPromises.length > 0
      ? await Promise.all(uploadPromises)
      : [];
    const uploadedMap: Record<string, string | null> = {};
    uploadResults.forEach((result, index) => {
      uploadedMap[uploadKeys[index]] = result;
    });

    const anyUploadFailed = Object.values(uploadedMap).some(
      (value) => value == null,
    );

    if (anyUploadFailed) {
      setSubmitError(
        'ファイルのアップロードに失敗しました。再度お試しください。',
      );
      setIsSubmitting(false);
      return;
    }

    const payload: MonitoringData = {
      projectType,
      projectId: Number(state.projectId ?? currentData.projectId ?? 0),
      siteId: Number(state.siteId ?? currentData.siteId ?? 0) || null,
      userId: Number(state.userId ?? user?.id ?? 0) || null,
      dateRecorded: String(currentData.dateRecorded ?? new Date().toISOString()),
      ELpv_value: Number(currentData.ELpv_value) || 0,
      ELpvr_value: Number(currentData.ELpvr_value) || 0,
      changedBattery: Boolean(currentData.changedBattery),
      ELPJ_battery_value: currentData.changedBattery
        ? Number(currentData.ELPJ_battery_value) || 0
        : null,
      ELPJ_control_value: Number(currentData.ELPJ_control_value) || 0,
      ELPJ_accuracy: Number(currentData.ELPJ_accuracy) || 0,
      ELpv_evidence: uploadedMap.ELpv_evidence ?? '',
      ELpvr_evidence: uploadedMap.ELpvr_evidence ?? '',
      ELPJ_battery_evidence: uploadedMap.ELPJ_battery_evidence ?? '',
      ELPJ_control_equipment_spec_files:
        uploadedMap.ELPJ_control_equipment_spec_files ?? '',
    };

    try {
      await submitMonitoringData(payload);
      toast({
        title: 'データの送信に成功しました',
        description: 'モニタリングデータが正常に登録されました。',
        variant: 'default',
      });
      setIsSubmitting(false);
      navigate(-1);
    } catch (error) {
      logger.error('Monitoring submit error:', error);

      const fieldErrors =
        error instanceof ApiError
          ? toServerFieldErrors(error.payload?.errors)
          : {};

      if (Object.keys(fieldErrors).length > 0) {
        const message =
          error instanceof ApiError
            ? error.payload?.message || error.message
            : '入力内容に誤りがあります。';
        const description = Object.values(fieldErrors).slice(0, 3).join('\n');

        setServerFieldErrors(fieldErrors);
        setSubmitError(null);
        setStep('form');
        toast({
          title: message || '入力内容に誤りがあります。',
          description,
          variant: 'destructive',
        });
        setIsSubmitting(false);
        return;
      }

      setSubmitError(
        'データの送信に失敗しました。しばらくしてから再度お試しください。',
      );
      setIsSubmitting(false);
    }
  };

  return step === 'form' ? (
    <Form
      mode='create'
      value={formData}
      onChange={setFormData}
      serverErrors={serverFieldErrors}
      onNext={() => {
        setServerFieldErrors({});
        setSubmitError(null);
        setStep('confirm');
      }}
      onBack={() => navigate(-1)}
    />
  ) : (
    <MonitoringConfirmationLayout
      title='モニタリングデータ'
      description={
        submitError
          ? submitError
          : isSubmitting
            ? `ファイルをアップロード中 (${uploadProgress.done}/${uploadProgress.total})`
            : '以下の内容をご確認ください。'
      }
      items={getConfirmationItems(
        formData,
        originalDataRef.current ?? undefined,
      )}
      onBack={() => setStep('form')}
      onConfirm={handleConfirm}
      isSubmitting={isSubmitting}
    />
  );
};

export default MonitoringRegister;
