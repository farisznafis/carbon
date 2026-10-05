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
import {
  getSiteEquipmentList,
  type SiteProjectEquipment,
} from '@/api/site';
import { logger } from '@/lib/logger';
import { buildMonitoringPayload } from './monitoringPayload';
import type { FileUploadProgressItem } from '@/components/feedback/loading';

type MonitoringFormData = typeof formDefinitions[ProjectType]['emptyData'];
type RouteState = {
  projectId?: string;
  siteId?: string;
};

type EvidenceUploadKey =
  | 'elpv_evidence'
  | 'elpvr_evidence'
  | 'elpj_battery_evidence'
  | 'elpj_control_evidence';

type EvidenceUploadJob = {
  id: EvidenceUploadKey;
  fileName: string;
  upload: (onProgress: (progress: number | null) => void) => Promise<string>;
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

  const fieldNameMap: Record<string, string> = {
    equipment_id: 'equipmentId',
    date_recorded: 'dateRecorded',
    elpv_value: 'ELpv_value',
    elpvr_value: 'ELpvr_value',
    elpj_battery_value: 'ELPJ_battery_value',
    elpj_control_value: 'ELPJ_control_value',
    elpj_accuracy: 'ELPJ_accuracy',
    elpj_control_equipment_spec_files: 'elpj_control_evidence',
  };

  return Object.entries(errors as Record<string, unknown>).reduce<
    Record<string, string>
  >((fieldErrors, [field, value]) => {
    const message = toErrorMessage(value);

    if (message) {
      fieldErrors[fieldNameMap[field] ?? field] = message;
    }

    return fieldErrors;
  }, {});
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
  const [uploadItems, setUploadItems] = useState<FileUploadProgressItem[]>([]);
  const [submitError, setSubmitError] = useState<string | null>(null);
  const [serverFieldErrors, setServerFieldErrors] = useState<Record<string, string>>({});
  const [equipmentList, setEquipmentList] = useState<SiteProjectEquipment[]>([]);
  const [isEquipmentLoading, setIsEquipmentLoading] = useState(false);
  const [equipmentLoadError, setEquipmentLoadError] = useState<string | null>(null);
  const [equipmentReloadKey, setEquipmentReloadKey] = useState(0);
  const originalDataRef = useRef<MonitoringFormData | null>(null);
  const uploadJobsRef = useRef<EvidenceUploadJob[]>([]);
  const uploadedEvidenceUrlsRef = useRef<Partial<Record<EvidenceUploadKey, string>>>({});

  const routeState = (location.state || {}) as RouteState;
  const equipmentSiteId = String(
    routeState.siteId ??
      new URLSearchParams(location.search).get('siteId') ??
      asRecord(formData).siteId ??
      '',
  );

  useEffect(() => {
    if (!definition) return;

    setFormData(definition.emptyData);
    originalDataRef.current = definition.emptyData;
  }, [definition]);

  useEffect(() => {
    if (!equipmentSiteId || projectType !== 'solar') {
      setEquipmentList([]);
      setEquipmentLoadError(
        equipmentSiteId ? null : '設備一覧を取得する拠点を特定できませんでした。',
      );
      return;
    }

    let isActive = true;
    setIsEquipmentLoading(true);
    setEquipmentLoadError(null);

    void getSiteEquipmentList(equipmentSiteId)
      .then((equipments) => {
        if (isActive) setEquipmentList(equipments);
      })
      .catch((error: unknown) => {
        if (!isActive) return;
        setEquipmentList([]);
        setEquipmentLoadError(
          error instanceof Error
            ? error.message
            : '設備一覧の取得に失敗しました。',
        );
      })
      .finally(() => {
        if (isActive) setIsEquipmentLoading(false);
      });

    return () => {
      isActive = false;
    };
  }, [equipmentReloadKey, equipmentSiteId, projectType]);

  if (!definition || !projectType) {
    return (
      <div className='flex min-h-screen items-center justify-center'>
        <p className='text-foreground'>
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
      projectIdValue: String(
        state.projectId ??
          search.get('projectId') ??
          currentData.projectId ??
          '',
      ),
      siteIdValue:
        state.siteId ?? search.get('siteId') ?? currentData.siteId ?? '',
    };
  };

  const handleValidateAndNext = async (values: MonitoringFormData) => {
    const { projectIdValue, siteIdValue } = getRouteValues(values);
    const payload = buildMonitoringPayload(asRecord(values), {
      projectType,
      projectId: projectIdValue,
      siteId: siteIdValue,
      hasBattery: asRecord(values).equipmentHasBattery === true,
    });

    try {
      setServerFieldErrors({});
      setSubmitError(null);
      await validateMonitoringData(payload);
      uploadJobsRef.current = [];
      uploadedEvidenceUrlsRef.current = {};
      setUploadItems([]);
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

      const message =
        error instanceof Error
          ? error.message
          : '入力内容の確認に失敗しました。';
      setSubmitError(message);
      toast({
        title: '入力内容を確認できませんでした',
        description: message,
        variant: 'destructive',
      });
    }
  };

  /** Builds stable upload jobs for the evidence files selected in the form. */
  const createEvidenceUploadJobs = () => {
    const currentData = asRecord(formData);
    const { projectIdValue, siteIdValue } = getRouteValues(formData);
    const descriptionValue = String(currentData.description ?? '');
    const siteIdString = String(siteIdValue);
    const jobs: EvidenceUploadJob[] = [];

    const addUpload = (
      id: EvidenceUploadKey,
      value: unknown,
      upload: (file: File, onProgress: (progress: number | null) => void) => Promise<string>,
    ) => {
      if (!(value instanceof File)) return;

      jobs.push({
        id,
        fileName: value.name,
        upload: (onProgress) => upload(value, onProgress),
      });
    };

    addUpload('elpv_evidence', currentData.elpv_evidence, (file, onProgress) =>
      uploadElpvFile(file, projectIdValue, siteIdString, descriptionValue, { onProgress }),
    );
    addUpload('elpvr_evidence', currentData.elpvr_evidence, (file, onProgress) =>
      uploadElpvrFile(file, projectIdValue, siteIdString, descriptionValue, { onProgress }),
    );
    if (currentData.equipmentHasBattery === true) {
      addUpload(
        'elpj_battery_evidence',
        currentData.elpj_battery_evidence,
        (file, onProgress) =>
          uploadElpjBatteryFile(
            file,
            projectIdValue,
            siteIdString,
            descriptionValue,
            { onProgress },
          ),
      );
    }
    addUpload(
      'elpj_control_evidence',
      currentData.elpj_control_evidence,
      (file, onProgress) =>
        uploadElpjControlFile(file, projectIdValue, siteIdString, descriptionValue, { onProgress }),
    );

    return jobs;
  };

  /** Applies a partial state update to one file progress row. */
  const updateUploadItem = (
    itemId: EvidenceUploadKey,
    update: Partial<FileUploadProgressItem>,
  ) => {
    setUploadItems((items) =>
      items.map((item) => (item.id === itemId ? { ...item, ...update } : item)),
    );
  };

  /** Uploads one evidence file and preserves successful URLs for later retries. */
  const runEvidenceUpload = async (job: EvidenceUploadJob) => {
    updateUploadItem(job.id, { status: 'uploading', progress: null, errorMessage: undefined });

    try {
      const uploadedUrl = await job.upload((progress) => {
        updateUploadItem(job.id, { status: 'uploading', progress });
      });
      uploadedEvidenceUrlsRef.current[job.id] = uploadedUrl;
      updateUploadItem(job.id, { status: 'success', progress: 100 });
      return true;
    } catch (error) {
      logger.error(`${job.id} upload failed`, error);
      updateUploadItem(job.id, {
        status: 'error',
        progress: null,
        errorMessage:
          error instanceof Error && error.message
            ? error.message
            : 'アップロードに失敗しました。',
      });
      return false;
    }
  };

  /** Submits monitoring data after every required evidence file is available. */
  const submitMonitoringRecord = async () => {
    const currentData = asRecord(formData);
    const { projectIdValue, siteIdValue } = getRouteValues(formData);
    const payload = buildMonitoringPayload(currentData, {
      projectType,
      projectId: projectIdValue,
      siteId: siteIdValue,
      hasBattery: currentData.equipmentHasBattery === true,
      evidenceUrls: uploadedEvidenceUrlsRef.current,
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

      setSubmitError('データの送信に失敗しました。しばらくしてから再度お試しください。');
      setIsSubmitting(false);
    }
  };

  /** Starts pending uploads and submits without repeating completed files. */
  const handleConfirm = async () => {
    const { projectIdValue, siteIdValue } = getRouteValues(formData);
    const jobs = uploadJobsRef.current.length > 0
      ? uploadJobsRef.current
      : createEvidenceUploadJobs();

    if (jobs.length > 0 && (!projectIdValue || !siteIdValue)) {
      setSubmitError('アップロード対象のプロジェクトまたは拠点を確認してください。');
      return;
    }

    if (uploadJobsRef.current.length === 0) {
      uploadJobsRef.current = jobs;
      setUploadItems(
        jobs.map((job) => ({
          id: job.id,
          fileName: job.fileName,
          status: 'queued',
          progress: null,
        })),
      );
    }

    setSubmitError(null);
    setServerFieldErrors({});
    setIsSubmitting(true);

    const pendingJobs = jobs.filter(
      (job) => !uploadedEvidenceUrlsRef.current[job.id],
    );
    const uploadResults = await Promise.all(
      pendingJobs.map((job) => runEvidenceUpload(job)),
    );

    if (uploadResults.some((succeeded) => !succeeded)) {
      setSubmitError('一部のファイルをアップロードできませんでした。失敗したファイルを再試行してください。');
      setIsSubmitting(false);
      return;
    }

    await submitMonitoringRecord();
  };

  /** Retries only the selected file and resumes submission after all files succeed. */
  const handleRetryUpload = async (itemId: string) => {
    const job = uploadJobsRef.current.find((candidate) => candidate.id === itemId);
    if (!job || isSubmitting) return;

    setSubmitError(null);
    setIsSubmitting(true);
    const succeeded = await runEvidenceUpload(job);
    const allUploadsComplete = uploadJobsRef.current.every(
      (candidate) => Boolean(uploadedEvidenceUrlsRef.current[candidate.id]),
    );

    if (succeeded && allUploadsComplete) {
      await submitMonitoringRecord();
      return;
    }

    setIsSubmitting(false);
  };

  return step === 'form' ? (
    <Form
      mode='create'
      value={formData}
      onChange={setFormData}
      serverErrors={serverFieldErrors}
      equipmentList={equipmentList}
      isEquipmentLoading={isEquipmentLoading}
      equipmentLoadError={equipmentLoadError}
      onEquipmentRetry={() => setEquipmentReloadKey((key) => key + 1)}
      onNext={handleValidateAndNext}
      onBack={() => navigate(-1)}
    />
  ) : (
    <MonitoringConfirmationLayout
      title='モニタリングデータ'
      description={submitError || (isSubmitting ? '登録処理を進めています' : '以下の内容をご確認ください。')}
      items={getConfirmationItems(
        formData,
        originalDataRef.current ?? undefined,
      )}
      onBack={() => setStep('form')}
      onConfirm={handleConfirm}
      isSubmitting={isSubmitting}
      uploadItems={uploadItems}
      onRetryUpload={(itemId) => void handleRetryUpload(itemId)}
    />
  );
};

export default MonitoringRegister;
