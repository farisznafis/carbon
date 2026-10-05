import { useCallback, useEffect, useMemo, useState } from 'react';
import { useForm, useWatch } from 'react-hook-form';
import { useNavigate, useParams } from 'react-router-dom';
import { ApiError } from '@/api/_shared/errors';
import {
  getSiteEquipmentById,
  changeSiteEquipment,
  uploadSiteProjectEquipmentFile,
  validateSiteProjectEquipment,
  type SiteProjectEquipmentFile,
} from '@/api/site';
import {
  FormActions,
  FormCard,
  FormPageContent,
  FormPageTitle,
  FormStandalonePage,
} from '@/components/forms/form-layout';
import { PageBackButton } from '@/components/navigation/PageBackButton';
import { Form } from '@/components/ui/form';
import { SolarPowerEquipmentForm } from '@/features/user/project-registration/components/equipment-forms/SolarPowerEquipmentForm';
import { mapEquipmentValidationFieldErrors } from '@/features/user/project-registration/solarEquipmentFormModel';
import {
  getSolarFormSelectedFiles,
  mapSolarFormFilesForValidation,
  mapSolarFormToEquipmentValidationItem,
} from '@/features/user/project-registration/equipmentValidationMapper';
import { useToast } from '@/hooks/use-toast';
import type { SolarPowerFormData } from '@/lib/equipmentValidations';
import { paths } from '@/routes/paths';
import { EquipmentEditConfirmation } from './EquipmentEditConfirmation';
import {
  hasEquipmentEditChanges,
  mapEquipmentEditToChangeRequest,
  mapEquipmentToEditForm,
  mergeEquipmentFilesForChange,
} from './equipmentEditMapper';
import { ActionButton } from '@/components/actions';

type EquipmentEditStep = 'form' | 'confirmation';

function parseEquipmentId(value: string | undefined) {
  if (!value || !/^\d+$/.test(value)) return null;

  const equipmentId = Number(value);
  return Number.isSafeInteger(equipmentId) && equipmentId > 0
    ? equipmentId
    : null;
}

export default function EquipmentEditPage() {
  const navigate = useNavigate();
  const { toast } = useToast();
  const {
    siteId = '',
    projectId = '',
    equipmentId: equipmentIdParam,
  } = useParams<{
    siteId: string;
    projectId: string;
    equipmentId: string;
  }>();
  const equipmentId = parseEquipmentId(equipmentIdParam);
  const [step, setStep] = useState<EquipmentEditStep>('form');
  const [originalValues, setOriginalValues] =
    useState<SolarPowerFormData | null>(null);
  const [confirmedValues, setConfirmedValues] =
    useState<SolarPowerFormData | null>(null);
  const [existingFiles, setExistingFiles] = useState<SiteProjectEquipmentFile[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [loadError, setLoadError] = useState<string | null>(null);
  const [reloadKey, setReloadKey] = useState(0);

  const form = useForm<SolarPowerFormData>();
  const currentValues = useWatch({ control: form.control });
  const returnPath = useMemo(
    () =>
      siteId && projectId
        ? `${paths.siteProjectDetail(siteId, projectId)}?${new URLSearchParams({ tab: 'equipment' })}`
        : paths.userDashboard,
    [projectId, siteId],
  );

  const navigateBack = useCallback(() => {
    navigate(returnPath);
  }, [navigate, returnPath]);

  useEffect(() => {
    if (!siteId || !projectId || equipmentId === null) {
      setLoadError('設備情報を特定できませんでした。');
      setIsLoading(false);
      return;
    }

    let isActive = true;
    setIsLoading(true);
    setLoadError(null);

    void getSiteEquipmentById(siteId, equipmentId)
      .then((equipment) => {
        if (!isActive) return;

        const values = mapEquipmentToEditForm(equipment, siteId);
        form.reset(values);
        setOriginalValues(values);
        setExistingFiles(Array.isArray(equipment.files) ? equipment.files : []);
      })
      .catch((error: unknown) => {
        if (!isActive) return;
        setLoadError(
          error instanceof Error
            ? error.message
            : '設備情報の取得に失敗しました。',
        );
      })
      .finally(() => {
        if (isActive) setIsLoading(false);
      });

    return () => {
      isActive = false;
    };
  }, [equipmentId, form, projectId, reloadKey, siteId]);

  const hasChanges = useMemo(
    () =>
      originalValues !== null &&
      hasEquipmentEditChanges(
        currentValues as SolarPowerFormData,
        originalValues,
      ),
    [currentValues, originalValues],
  );

  /** Validates the replacement snapshot through POST /site/project/equipment/validate before review. */
  const handleReview = async (values: SolarPowerFormData) => {
    if (!values.changeDate?.trim()) {
      form.setError('changeDate', {
        type: 'required',
        message: '変更日を入力してください。',
      });
      return;
    }

    if (
      !originalValues ||
      equipmentId === null ||
      !siteId ||
      !hasEquipmentEditChanges(values, originalValues)
    ) {
      return;
    }

    form.clearErrors();
    setIsSubmitting(true);

    const validationValues: SolarPowerFormData = {
      ...values,
      equipmentOpDate: values.changeDate,
    };

    try {
      const validationFiles = mergeEquipmentFilesForChange(
        existingFiles,
        mapSolarFormFilesForValidation(values),
      );
      await validateSiteProjectEquipment(
        mapSolarFormToEquipmentValidationItem(
          validationValues,
          `equipment-${equipmentId}`,
          siteId,
          validationFiles,
        ),
      );
      setConfirmedValues(values);
      setStep('confirmation');
    } catch (error) {
      const fieldErrors =
        error instanceof ApiError
          ? mapEquipmentValidationFieldErrors(
              error.payload?.errors,
              validationValues,
            )
          : [];

      fieldErrors.forEach(({ fieldName, message }) => {
        form.setError(fieldName === 'equipmentOpDate' ? 'changeDate' : fieldName, {
          type: 'server',
          message,
        });
      });
      toast({
        title: '変更内容を確認できませんでした',
        description:
          error instanceof Error
            ? error.message
            : '設備情報の検証に失敗しました。',
        variant: 'destructive',
      });
    } finally {
      setIsSubmitting(false);
    }
  };

  const handleConfirm = async () => {
    if (
      !siteId ||
      equipmentId === null ||
      !originalValues ||
      !confirmedValues ||
      isSubmitting
    ) {
      return;
    }

    setIsSubmitting(true);

    try {
      const uploadedFiles = await Promise.all(
        getSolarFormSelectedFiles(confirmedValues).map(({ fileType, file }) =>
          uploadSiteProjectEquipmentFile(siteId, fileType, file),
        ),
      );
      const files = mergeEquipmentFilesForChange(existingFiles, uploadedFiles);
      const payload = mapEquipmentEditToChangeRequest(
        confirmedValues,
        files,
      );
      await changeSiteEquipment(siteId, equipmentId, payload);
      toast({
        title: '変更完了',
        description: '設備情報を変更しました。',
      });
      navigate(returnPath, { replace: true });
    } catch (error) {
      const fieldErrors =
        error instanceof ApiError
          ? mapEquipmentValidationFieldErrors(
              error.payload?.errors,
              confirmedValues,
            )
          : [];

      if (fieldErrors.length > 0) {
        fieldErrors.forEach(({ fieldName, message }) => {
          form.setError(fieldName, { type: 'server', message });
        });
        setStep('form');
      }

      toast({
        title: '設備情報を変更できませんでした',
        description:
          error instanceof Error
            ? error.message
            : '設備情報の変更に失敗しました。',
        variant: 'destructive',
      });
    } finally {
      setIsSubmitting(false);
    }
  };

  if (isLoading) {
    return (
      <FormStandalonePage>
        <FormPageContent>
          <FormCard className="text-center text-foreground">
            設備情報を読み込んでいます...
          </FormCard>
        </FormPageContent>
      </FormStandalonePage>
    );
  }

  if (loadError || !originalValues) {
    return (
      <FormStandalonePage>
        <PageBackButton
          onClick={navigateBack}
          label=""
          className="absolute left-4 top-10 h-12 w-12 rounded-full p-0 sm:left-10 lg:left-16"
        />
        <FormPageContent>
          <FormPageTitle>設備変更</FormPageTitle>
          <FormCard className="space-y-5 text-center">
            <p role="alert" className="text-destructive">
              {loadError ?? '設備情報を取得できませんでした。'}
            </p>
            {equipmentId !== null && siteId && projectId && (
              <ActionButton kind="retry" onClick={() => setReloadKey((key) => key + 1)}>
                再読み込み
              </ActionButton>
            )}
          </FormCard>
        </FormPageContent>
      </FormStandalonePage>
    );
  }

  if (step === 'confirmation' && confirmedValues) {
    return (
      <EquipmentEditConfirmation
        current={confirmedValues}
        original={originalValues}
        existingFiles={existingFiles}
        isSubmitting={isSubmitting}
        onBack={() => setStep('form')}
        onConfirm={() => void handleConfirm()}
      />
    );
  }

  return (
    <FormStandalonePage>
      <PageBackButton
        onClick={navigateBack}
        label=""
        className="absolute left-4 top-10 h-12 w-12 rounded-full p-0 sm:left-10 lg:left-16"
      />

      <FormPageContent className="animate-slide-up">
        <div className="space-y-3">
          <FormPageTitle>設備変更</FormPageTitle>
          {/* <p className="text-small text-foreground">
            設備ID：{equipmentId}
          </p> */}
        </div>

        <Form {...form}>
          <form onSubmit={form.handleSubmit(handleReview)} noValidate>
            <FormCard>
              <SolarPowerEquipmentForm
                form={form}
                mode="edit"
                existingFiles={existingFiles}
              />
              <FormActions
                className="pt-8"
                pendingActionId={isSubmitting ? 'confirm' : null}
                actions={[
                  {
                    id: 'cancel',
                    label: 'キャンセル',
                    variant: 'outline',
                    onClick: navigateBack,
                  },
                  {
                    id: 'confirm',
                    label: '変更内容を確認',
                    pendingLabel: '確認中...',
                    type: 'submit',
                    disabled: !hasChanges,
                  },
                ]}
              />
            </FormCard>
          </form>
        </Form>
      </FormPageContent>
    </FormStandalonePage>
  );
}
