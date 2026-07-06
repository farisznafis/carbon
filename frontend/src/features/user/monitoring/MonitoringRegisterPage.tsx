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
  validateMonitoringData,
  uploadElpjBatteryFile,
  uploadElpjControlFile,
  uploadElpvFile,
  uploadElpvrFile,
} from '@/api/monitoring';
import { ApiError } from '@/api/_shared/errors';
import { useToast } from '@/hooks/use-toast';
import type { MonitoringData } from '@/api/monitoring';
import { logger } from '@/lib/logger';

type MonitoringFormData = typeof formDefinitions[ProjectType]['emptyData'];
type RouteState = {
  projectId?: string | number;
  siteId?: string | number;
};

type MonitoringPayloadContext = {
  projectType: string;
  projectIdValue: unknown;
  siteIdValue: unknown;
  evidenceUrls?: Partial<Record<keyof MonitoringData, string | null>>;
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

function fileValueToString(value: unknown): string {
  if (value instanceof File) return value.name;
  return typeof value === 'string' ? value : '';
}

function buildMonitoringPayload(
  data: MonitoringFormData,
  context: MonitoringPayloadContext,
): MonitoringData {
  const currentData = asRecord(data);
  const evidenceUrls = context.evidenceUrls ?? {};

  return {
    projectType: context.projectType,
    projectId: Number(context.projectIdValue) || 0,
    siteId: Number(context.siteIdValue) || null,
    dateRecorded: String(currentData.dateRecorded ?? new Date().toISOString()),
    ELpv_value: Number(currentData.ELpv_value) || 0,
    ELpvr_value: Number(currentData.ELpvr_value) || 0,
    changedBattery: Boolean(currentData.changedBattery),
    ELPJ_battery_value: currentData.changedBattery
      ? Number(currentData.ELPJ_battery_value) || 0
      : null,
    ELPJ_control_value: Number(currentData.ELPJ_control_value) || 0,
    ELPJ_accuracy: Number(currentData.ELPJ_accuracy) || 0,
    ELpv_evidence:
      evidenceUrls.ELpv_evidence ?? fileValueToString(currentData.ELpv_evidence),
    ELpvr_evidence:
      evidenceUrls.ELpvr_evidence ?? fileValueToString(currentData.ELpvr_evidence),
    ELPJ_battery_evidence:
      evidenceUrls.ELPJ_battery_evidence ??
      fileValueToString(currentData.ELPJ_battery_evidence),
    ELPJ_control_equipment_spec_files:
      evidenceUrls.ELPJ_control_equipment_spec_files ??
      fileValueToString(currentData.ELPJ_control_equipment_spec_files),
  };
}

const MonitoringRegister = () => {
  const navigate = useNavigate();
  const location = useLocation();
  const { projectType } = useParams<{ projectType: string }>();
  const { toast } = useToast();
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

  const getRouteValues = (data: MonitoringFormData) => {
    const state = (location.state || {}) as RouteState;
    const search = new URLSearchParams(location.search);
    const currentData = asRecord(data);

    return {
      projectIdValue:
        state.projectId ?? search.get('projectId') ?? currentData.projectId ?? '',
      siteIdValue:
        state.siteId ?? search.get('siteId') ?? currentData.siteId ?? '',
    };
  };

  const handleValidateAndNext = async (values: MonitoringFormData) => {
    const { projectIdValue, siteIdValue } = getRouteValues(values);
    const payload = buildMonitoringPayload(values, {
      projectType,
      projectIdValue,
      siteIdValue,
    });

    try {
      setServerFieldErrors({});
      setSubmitError(null);
      await validateMonitoringData(payload);
      setFormData(values);
      setStep('confirm');
    } catch (error) {
      logger.error('Monitoring validation error:', error);

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
        toast({
          title: message || '入力内容に誤りがあります。',
          description,
          variant: 'destructive',
        });
        return;
      }

      setSubmitError(
        error instanceof Error
          ? error.message
          : '入力内容の確認に失敗しました。',
      );
    }
  };

  const handleConfirm = async () => {
    const currentData = asRecord(formData);
    const { projectIdValue, siteIdValue } = getRouteValues(formData);
    const descriptionValue = String(currentData.description ?? '');

    const uploadTasks: Array<{
      key: string;
      upload: () => Promise<string | null>;
    }> = [];

    const addUpload = (
      key: string,
      value: unknown,
      upload: (file: File) => Promise<string>,
    ) => {
      if (!(value instanceof File)) return;

      uploadTasks.push({
        key,
        upload: () =>
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
      });
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

    if (uploadTasks.length > 0 && (!projectIdValue || !siteIdValue)) {
      setSubmitError(
        'アップロードには projectId と siteId が必要です。プロジェクトまたはサイトを選択してください。',
      );
      return;
    }

    setSubmitError(null);
    setServerFieldErrors({});
    setUploadProgress({ done: 0, total: uploadTasks.length });
    setIsSubmitting(true);

    const uploadResults = uploadTasks.length > 0
      ? await Promise.all(uploadTasks.map((task) => task.upload()))
      : [];
    const uploadedMap: Record<string, string | null> = {};
    uploadResults.forEach((result, index) => {
      uploadedMap[uploadTasks[index].key] = result;
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

    const payload = buildMonitoringPayload(formData, {
      projectType,
      projectIdValue,
      siteIdValue,
      evidenceUrls: uploadedMap,
    });

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
      onNext={handleValidateAndNext}
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
