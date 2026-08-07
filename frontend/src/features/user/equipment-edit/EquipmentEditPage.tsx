import { useCallback, useEffect, useMemo, useState } from 'react';
import { useForm, useWatch } from 'react-hook-form';
import { useNavigate, useParams } from 'react-router-dom';
import { ApiError } from '@/api/_shared/errors';
import {
  getSiteEquipmentById,
  updateSiteEquipment,
  uploadSiteProjectEquipmentFile,
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
import { Button } from '@/components/ui/button';
import { Form } from '@/components/ui/form';
import { SolarPowerEquipmentForm } from '@/features/user/project-registration/components/equipment-forms/SolarPowerEquipmentForm';
import { mapEquipmentValidationFieldErrors } from '@/features/user/project-registration/solarEquipmentFormModel';
import { getSolarFormSelectedFiles } from '@/features/user/project-registration/equipmentValidationMapper';
import { useToast } from '@/hooks/use-toast';
import type { SolarPowerFormData } from '@/lib/equipmentValidations';
import { paths } from '@/routes/paths';
import { EquipmentEditConfirmation } from './EquipmentEditConfirmation';
import {
  hasEquipmentEditChanges,
  mapEquipmentEditToUpdateRequest,
  mapEquipmentToEditForm,
  mergeEquipmentFilesForUpdate,
} from './equipmentEditMapper';

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

  const handleReview = (values: SolarPowerFormData) => {
    if (!originalValues || !hasEquipmentEditChanges(values, originalValues)) {
      return;
    }

    form.clearErrors();
    setConfirmedValues(values);
    setStep('confirmation');
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
      const files = mergeEquipmentFilesForUpdate(existingFiles, uploadedFiles);
      const payload = mapEquipmentEditToUpdateRequest(
        confirmedValues,
        originalValues,
        files,
      );
      await updateSiteEquipment(siteId, equipmentId, payload);
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
              <Button type="button" onClick={() => setReloadKey((key) => key + 1)}>
                再読み込み
              </Button>
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
          <p className="text-small text-foreground">
            設備ID：{equipmentId}
          </p>
        </div>

        <Form {...form}>
          <form onSubmit={form.handleSubmit(handleReview)} noValidate>
            <FormCard>
              <SolarPowerEquipmentForm
                form={form}
                mode="edit"
                existingFiles={existingFiles}
              />
              <FormActions className="grid grid-cols-1 gap-3 pt-8 sm:grid-cols-2">
                <Button
                  type="button"
                  variant="outline"
                  size="md"
                  className="w-full"
                  onClick={navigateBack}
                >
                  キャンセル
                </Button>
                <Button
                  type="submit"
                  size="md"
                  className="w-full"
                  disabled={!hasChanges}
                >
                  変更内容を確認
                </Button>
              </FormActions>
            </FormCard>
          </form>
        </Form>
      </FormPageContent>
    </FormStandalonePage>
  );
}
