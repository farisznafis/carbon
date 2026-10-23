import { forwardRef, useEffect, useImperativeHandle, useRef } from 'react';
import { useForm } from 'react-hook-form';
import { ApiError } from '@/api/_shared/errors';
import { validateSiteProjectEquipment } from '@/api/site';
import { FormActions, FormCard } from '@/components/forms/form-layout';
import { FormErrorSummary } from '@/components/forms/FormErrorSummary';
import { Form } from '@/components/ui/form';
import { SolarPowerEquipmentForm } from '@/features/user/project-registration/components/equipment-forms/SolarPowerEquipmentForm';
import type { SolarEquipment } from '@/features/user/project-registration/components/types';
import {
  mapSolarFormFilesForValidation,
  mapSolarFormToEquipmentValidationItem,
} from '@/features/user/project-registration/equipmentValidationMapper';
import {
  getMissingEquipmentRequiredFieldErrors,
  mapEquipmentValidationFieldErrors,
  SOLAR_FORM_DEFAULT_VALUES,
} from '@/features/user/project-registration/solarEquipmentFormModel';
import { useSitesQuery } from '@/hooks/sites/useSitesQuery';
import type { SolarPowerFormData } from '@/lib/equipmentValidations';

const EMPTY_SITE_OPTIONS: never[] = [];

export type EquipmentRegistrationStepRef = {
  validate: () => Promise<SolarEquipment>;
  /** True when the last validate marked fields, so the page needs no extra toast. */
  hasFieldErrors: () => boolean;
};

type EquipmentRegistrationStepProps = {
  initialEquipment?: SolarEquipment;
  isSubmitting: boolean;
  onAddSite: (draft: SolarPowerFormData) => void;
  onBack: () => void;
  onNext: () => void;
  onValueChange: (draft: SolarPowerFormData) => void;
};

export const EquipmentRegistrationStep = forwardRef<
  EquipmentRegistrationStepRef,
  EquipmentRegistrationStepProps
>(({ initialEquipment, isSubmitting, onAddSite, onBack, onNext, onValueChange }, ref) => {
  const sitesQuery = useSitesQuery();
  const form = useForm<SolarPowerFormData>({
    defaultValues:
      initialEquipment?.solarData ?? { ...SOLAR_FORM_DEFAULT_VALUES },
  });
  const sites = sitesQuery.data?.sites ?? EMPTY_SITE_OPTIONS;
  const hasAppliedInitialEquipmentRef = useRef(false);
  const siteOptionsError = sitesQuery.isError
    ? sitesQuery.error instanceof Error
      ? sitesQuery.error.message
      : '拠点情報を取得できませんでした。'
    : null;

  useEffect(() => {
    if (initialEquipment && !hasAppliedInitialEquipmentRef.current) {
      hasAppliedInitialEquipmentRef.current = true;
      form.reset(initialEquipment.solarData);
    }
  }, [form, initialEquipment]);

  useEffect(() => {
    const subscription = form.watch((draft) => {
      onValueChange(draft as SolarPowerFormData);
    });

    return () => subscription.unsubscribe();
  }, [form, onValueChange]);

  useEffect(() => {
    const selectedSiteId = form.getValues('siteId');
    if (!selectedSiteId) return;
    if (sites.some((site) => site.siteId === selectedSiteId)) return;

    // Clear stale drafts without selecting a site on the user's behalf.
    // form.setValue('siteId', '');
  }, [form, sites]);

  useImperativeHandle(ref, () => ({
    hasFieldErrors: () => Object.keys(form.formState.errors).length > 0,
    validate: async () => {
      form.clearErrors();
      const formData = form.getValues();
      const tempId = initialEquipment?.id ?? `equipment-${Date.now()}`;
      const missingFieldErrors =
        getMissingEquipmentRequiredFieldErrors(formData);

      try {
        await validateSiteProjectEquipment(
          mapSolarFormToEquipmentValidationItem(
            formData,
            tempId,
            formData.siteId,
            mapSolarFormFilesForValidation(formData),
          ),
        );

        if (missingFieldErrors.length > 0) {
          missingFieldErrors.forEach(({ fieldName, message }) => {
            form.setError(fieldName, { type: 'required', message });
          });
          throw new Error('必須項目を入力してください。');
        }
      } catch (error) {
        missingFieldErrors.forEach(({ fieldName, message }) => {
          form.setError(fieldName, { type: 'required', message });
        });

        if (
          error instanceof ApiError &&
          error.payload?.errors &&
          typeof error.payload.errors === 'object'
        ) {
          mapEquipmentValidationFieldErrors(
            error.payload.errors,
            formData,
          ).forEach(({ fieldName, message }) => {
            form.setError(fieldName, { type: 'server', message });
          });
        }
        throw error;
      }

      return {
        id: tempId,
        formType: 'solar',
        solarData: formData,
        files: initialEquipment?.files ?? [],
      };
    },
  }));

  return (
    <div className="space-y-4">
      <div>
        <h2 className="text-foreground">設備登録</h2>
        <p className="text-small mt-2 text-foreground">
          プロジェクトに登録する設備情報と確認資料を入力してください。
        </p>
      </div>
      <Form {...form}>
        <FormCard className="space-y-8">
          <SolarPowerEquipmentForm
            form={form}
            sites={sites}
            isSiteOptionsLoading={sitesQuery.isLoading}
            siteOptionsError={siteOptionsError}
            onAddSite={onAddSite}
          />
          <FormErrorSummary />
          <FormActions
            pendingActionId={isSubmitting ? 'next' : null}
            actions={[
              {
                id: 'back',
                label: '戻る',
                variant: 'outline',
                onClick: onBack,
              },
              {
                id: 'next',
                label: '次へ',
                pendingLabel: '確認中...',
                onClick: onNext,
              },
            ]}
          />
        </FormCard>
      </Form>
    </div>
  );
});

EquipmentRegistrationStep.displayName = 'EquipmentRegistrationStep';
