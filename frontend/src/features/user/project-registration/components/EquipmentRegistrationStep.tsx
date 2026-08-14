import { forwardRef, useEffect, useImperativeHandle } from 'react';
import { useForm } from 'react-hook-form';
import { ApiError } from '@/api/_shared/errors';
import { validateSiteProjectEquipment } from '@/api/site';
import { FormCard } from '@/components/forms/form-layout';
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
};

type EquipmentRegistrationStepProps = {
  siteId: string;
  initialEquipment?: SolarEquipment;
  onAddSite: (draft: SolarPowerFormData) => void;
};

export const EquipmentRegistrationStep = forwardRef<
  EquipmentRegistrationStepRef,
  EquipmentRegistrationStepProps
>(({ siteId, initialEquipment, onAddSite }, ref) => {
  const sitesQuery = useSitesQuery();
  const form = useForm<SolarPowerFormData>({
    defaultValues:
      initialEquipment?.solarData ?? { ...SOLAR_FORM_DEFAULT_VALUES, siteId },
  });
  const sites = sitesQuery.data?.sites ?? EMPTY_SITE_OPTIONS;
  const siteOptionsError = sitesQuery.isError
    ? sitesQuery.error instanceof Error
      ? sitesQuery.error.message
      : '拠点情報を取得できませんでした。'
    : null;

  useEffect(() => {
    if (initialEquipment) {
      form.reset(initialEquipment.solarData);
    }
  }, [form, initialEquipment]);

  useEffect(() => {
    if (sites.length === 0) return;

    const selectedSiteId = form.getValues('siteId');
    if (sites.some((site) => site.siteId === selectedSiteId)) return;

    const routeSite = sites.find((site) => site.siteId === siteId);
    form.setValue('siteId', routeSite?.siteId ?? sites[0].siteId);
  }, [form, siteId, sites]);

  useImperativeHandle(ref, () => ({
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
        <FormCard>
          <SolarPowerEquipmentForm
            form={form}
            sites={sites}
            isSiteOptionsLoading={sitesQuery.isLoading}
            siteOptionsError={siteOptionsError}
            onAddSite={onAddSite}
          />
        </FormCard>
      </Form>
    </div>
  );
});

EquipmentRegistrationStep.displayName = 'EquipmentRegistrationStep';
