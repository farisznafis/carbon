import { useEffect, useState, type FormEvent } from 'react';
import {
  useLocation,
  useNavigate,
  useParams,
  useSearchParams,
} from 'react-router-dom';
import { useForm } from 'react-hook-form';
import { ApiError } from '@/api/_shared/errors';
import { validateSiteProjectEquipment } from '@/api/site';
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
import type {
  Equipment,
  EquipmentFormType,
} from '@/features/user/project-registration/components/types';
import {
  mapSolarFormFilesForValidation,
  mapSolarFormToEquipmentValidationItem,
} from '@/features/user/project-registration/equipmentValidationMapper';
import {
  getMissingEquipmentRequiredFieldErrors,
  mapEquipmentValidationFieldErrors,
  SOLAR_FORM_DEFAULT_VALUES,
} from '@/features/user/project-registration/solarEquipmentFormModel';
import { getSolarEquipmentDraft } from '@/features/user/project-registration/equipmentConfirmationState';
import {
  isFromMasterDashboard,
  masterDashboardProjectMembershipState,
} from '@/features/user/project-registration/projectMembershipNavigation';
import { useToast } from '@/hooks/use-toast';
import { useSitesQuery } from '@/hooks/sites/useSitesQuery';
import {
  type OtherEquipmentFormData,
  type SolarPowerFormData,
} from '@/lib/equipmentValidations';
import { paths } from '@/routes/paths';

const OTHER_FORM_DEFAULT_VALUES: OtherEquipmentFormData = {
  oldEquipmentName: '',
  oldEquipmentId: '',
  oldEquipmentType: '',
  newEquipmentName: '',
  newEquipmentId: '',
  newEquipmentType: '',
};

const EMPTY_SITE_OPTIONS: never[] = [];

type EquipmentSubmissionMode = 'draft' | 'create';

function getInternalReturnPath(state: unknown): string | null {
  if (!state || typeof state !== 'object' || !('returnTo' in state)) {
    return null;
  }

  const returnTo = (state as { returnTo?: unknown }).returnTo;
  return typeof returnTo === 'string' && returnTo.startsWith('/')
    ? returnTo
    : null;
}

/** Reads whether confirmation should create equipment immediately or keep a membership draft. */
function getEquipmentSubmissionMode(state: unknown): EquipmentSubmissionMode {
  if (!state || typeof state !== 'object') return 'draft';

  return (state as { submissionMode?: unknown }).submissionMode === 'create'
    ? 'create'
    : 'draft';
}

function getStringValue(
  source: Record<string, unknown>,
  key: string,
  legacyKey?: string,
) {
  const value = source[key] ?? (legacyKey ? source[legacyKey] : undefined);
  return typeof value === 'string' ? value : '';
}

function getYesNoValue(
  source: Record<string, unknown>,
  key: string,
): '' | 'yes' | 'no' {
  const value = source[key];
  return value === 'yes' || value === 'no' ? value : '';
}

function normalizeStoredSolarData(
  value: unknown,
  fallbackSiteId: string,
): SolarPowerFormData {
  if (!value || typeof value !== 'object') {
    return { ...SOLAR_FORM_DEFAULT_VALUES, siteId: fallbackSiteId };
  }
  const source = value as Record<string, unknown>;

  return {
    ...SOLAR_FORM_DEFAULT_VALUES,
    siteId: getStringValue(source, 'siteId') || fallbackSiteId,
    equipmentOpDate: getStringValue(source, 'equipmentOpDate'),
    nominalMaxOutput: getStringValue(source, 'nominalMaxOutput'),
    pcsManufacturer: getStringValue(
      source,
      'pcsManufacturer',
      'pcManufacturer',
    ),
    pcsModel: getStringValue(source, 'pcsModel', 'pcModel'),
    powerCondUnitCount: getStringValue(
      source,
      'powerCondUnitCount',
      'pcQuantity',
    ),
    deviceNo: getStringValue(source, 'deviceNo', 'pcSerialNumber'),
    powerCondStandby: getStringValue(
      source,
      'powerCondStandby',
      'pcStandbyPower',
    ),
    hasBattery: getYesNoValue(source, 'hasBattery'),
    batteryMaker: getStringValue(
      source,
      'batteryMaker',
      'batteryManufacturer',
    ),
    batteryModel: getStringValue(source, 'batteryModel'),
    batteryCapacity: getStringValue(source, 'batteryCapacity'),
    totalInvestment: getStringValue(source, 'totalInvestment'),
    hasSubsidy: getYesNoValue(source, 'hasSubsidy'),
    subsidyName: getStringValue(source, 'subsidyName'),
    subsidyAmount: getStringValue(source, 'subsidyAmount'),
    preProjectUnitPrice: getStringValue(
      source,
      'preProjectUnitPrice',
      'prePurchaseUnitPrice',
    ),
  };
}

export default function EquipmentFormPage() {
  const navigate = useNavigate();
  const location = useLocation();
  const { siteId = '', projectId = '' } = useParams<{
    siteId: string;
    projectId: string;
  }>();
  const [searchParams] = useSearchParams();
  const equipmentId = searchParams.get('equipmentId');
  const returnPath = getInternalReturnPath(location.state);
  const submissionMode = getEquipmentSubmissionMode(location.state);
  const enteredFromMasterDashboard = isFromMasterDashboard(location.state);
  const equipmentDraft = getSolarEquipmentDraft(location.state);
  const { toast } = useToast();
  const sitesQuery = useSitesQuery();
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [formType] = useState<EquipmentFormType>(() =>
    searchParams.get('type') === 'other' ? 'other' : 'solar',
  );

  const solarForm = useForm<SolarPowerFormData>({
    defaultValues: equipmentDraft ?? { ...SOLAR_FORM_DEFAULT_VALUES, siteId },
  });

  const otherForm = useForm<OtherEquipmentFormData>({
    defaultValues: OTHER_FORM_DEFAULT_VALUES,
  });

  useEffect(() => {
    if (equipmentDraft) return;
    if (!equipmentId) return;

    const storedEquipment = localStorage.getItem(
      `project_${projectId}_equipment`,
    );
    if (!storedEquipment) return;

    try {
      const equipmentList = JSON.parse(storedEquipment) as Equipment[];
      const equipment = equipmentList.find((item) => item.id === equipmentId);
      if (!equipment) return;

      if (equipment.formType === 'solar') {
        solarForm.reset(normalizeStoredSolarData(equipment.solarData, siteId));
      } else {
        otherForm.reset({
          oldEquipmentName: equipment.oldEquipmentName,
          oldEquipmentId: equipment.oldEquipmentId,
          oldEquipmentType: equipment.oldEquipmentType,
          newEquipmentName: equipment.newEquipmentName,
          newEquipmentId: equipment.newEquipmentId,
          newEquipmentType: equipment.newEquipmentType,
        });
      }
    } catch {
      toast({
        title: '設備情報を読み込めませんでした',
        variant: 'destructive',
      });
    }
  }, [equipmentDraft, equipmentId, otherForm, projectId, siteId, solarForm, toast]);

  const sites = sitesQuery.data?.sites ?? EMPTY_SITE_OPTIONS;
  const siteOptionsError = sitesQuery.isError
    ? sitesQuery.error instanceof Error
      ? sitesQuery.error.message
      : '拠点情報を取得できませんでした。'
    : null;

  useEffect(() => {
    if (sites.length === 0) return;

    const selectedSiteId = solarForm.getValues('siteId');
    if (sites.some((site) => site.siteId === selectedSiteId)) return;

    const routeSite = sites.find((site) => site.siteId === siteId);
    solarForm.setValue('siteId', routeSite?.siteId ?? sites[0].siteId);
  }, [siteId, sites, solarForm]);

  const saveEquipmentLocally = (equipmentData: Equipment) => {
    const storageKey = `project_${projectId}_equipment`;
    const storedEquipment = localStorage.getItem(storageKey);
    let equipmentList: Equipment[] = storedEquipment
      ? (JSON.parse(storedEquipment) as Equipment[])
      : [];

    equipmentList = equipmentId
      ? equipmentList.map((item) =>
          item.id === equipmentId ? equipmentData : item,
        )
      : [...equipmentList, equipmentData];

    localStorage.setItem(storageKey, JSON.stringify(equipmentList));
  };

  const navigateToProjectRegister = () => {
    navigate(paths.projectRegister(siteId, projectId), {
      state: enteredFromMasterDashboard
        ? masterDashboardProjectMembershipState
        : undefined,
    });
  };

  const submitSolarEquipment = async (data: SolarPowerFormData) => {
    if (isSubmitting) return;
    solarForm.clearErrors();
    setIsSubmitting(true);

    try {
      const tempId = equipmentId ?? `equipment-${Date.now()}`;
      const validationFiles = mapSolarFormFilesForValidation(data);
      const payload = mapSolarFormToEquipmentValidationItem(
        data,
        tempId,
        data.siteId,
        validationFiles,
      );
      const missingFieldErrors =
        getMissingEquipmentRequiredFieldErrors(data);

      await validateSiteProjectEquipment(payload);

      if (missingFieldErrors.length > 0) {
        missingFieldErrors.forEach(({ fieldName, message }) => {
          solarForm.setError(fieldName, { type: 'required', message });
        });
        throw new Error('必須項目を入力してください。');
      }

      navigate(paths.projectEquipmentConfirmation(siteId, projectId), {
        state: {
          equipmentId: tempId,
          isEdit: Boolean(equipmentId),
          formData: data,
          returnTo: returnPath ?? undefined,
          submissionMode,
          fromMasterDashboard: enteredFromMasterDashboard,
        },
      });
    } catch (error) {
      getMissingEquipmentRequiredFieldErrors(data).forEach(
        ({ fieldName, message }) => {
          solarForm.setError(fieldName, { type: 'required', message });
        },
      );

      if (
        error instanceof ApiError &&
          error.payload?.errors &&
          typeof error.payload.errors === 'object'
        ) {
          mapEquipmentValidationFieldErrors(
            error.payload.errors,
            data,
          ).forEach(({ fieldName, message }) => {
            solarForm.setError(fieldName, { type: 'server', message });
          });
        }

      toast({
        title: '設備情報を確認できませんでした',
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

  /** Resubmits current values without letting stale backend field errors block the request. */
  const handleSolarFormSubmit = (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    void submitSolarEquipment(solarForm.getValues());
  };

  const submitOtherEquipment = (data: OtherEquipmentFormData) => {
    const tempId = equipmentId ?? `equipment-${Date.now()}`;
    saveEquipmentLocally({
      id: tempId,
      formType: 'other',
      ...data,
    });
    navigateToProjectRegister();
  };

  /** Keeps cancellation in the Master tab when no explicit return path exists. */
  const handleCancel = () => {
    const membershipReturnPath = enteredFromMasterDashboard
      ? paths.masterDashboardMembership
      : paths.projectMembership(siteId);
    navigate(returnPath ?? membershipReturnPath, {
      state: enteredFromMasterDashboard
        ? masterDashboardProjectMembershipState
        : undefined,
    });
  };

  const pageTitle = equipmentId ? '設備編集' : '設備登録';
  const equipmentTypeLabel =
    formType === 'solar' ? '太陽光発電設備' : 'その他設備';

  return (
    <FormStandalonePage>
      <PageBackButton
        onClick={handleCancel}
        label=""
        className="absolute left-4 top-10 h-12 w-12 rounded-full p-0 sm:left-10 lg:left-16"
      />

      <FormPageContent>
        <FormPageTitle>{pageTitle}</FormPageTitle>
        <p className="text-sm text-foreground">
          設備種別：{equipmentTypeLabel}
        </p>

        {formType === 'solar' ? (
          <Form {...solarForm}>
            <form
              onSubmit={handleSolarFormSubmit}
              noValidate
            >
              <FormCard>
                <SolarPowerEquipmentForm
                  form={solarForm}
                  sites={sites}
                  isSiteOptionsLoading={sitesQuery.isLoading}
                  siteOptionsError={siteOptionsError}
                />
                <FormActions
                  className="pt-6"
                  pendingActionId={isSubmitting ? 'confirm' : null}
                  actions={[
                    {
                      id: 'cancel',
                      label: 'キャンセル',
                      variant: 'outline',
                      onClick: handleCancel,
                    },
                    {
                      id: 'confirm',
                      label: '確認',
                      pendingLabel: '確認中...',
                      type: 'submit',
                    },
                  ]}
                />
              </FormCard>
            </form>
          </Form>
        ) : (
          <Form {...otherForm}>
            <form
              onSubmit={otherForm.handleSubmit(submitOtherEquipment)}
              noValidate
            >
              <FormCard>
                <FormActions
                  className="pt-6"
                  actions={[
                    {
                      id: 'cancel',
                      label: 'キャンセル',
                      variant: 'outline',
                      onClick: handleCancel,
                    },
                    {
                      id: 'confirm',
                      label: '確認',
                      type: 'submit',
                    },
                  ]}
                />
              </FormCard>
            </form>
          </Form>
        )}
      </FormPageContent>
    </FormStandalonePage>
  );
}
