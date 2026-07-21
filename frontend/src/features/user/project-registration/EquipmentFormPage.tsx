import { useEffect, useState } from 'react';
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
import { mapSolarFormToEquipmentValidationItem } from '@/features/user/project-registration/equipmentValidationMapper';
import {
  isFromMasterDashboard,
  masterDashboardProjectMembershipState,
} from '@/features/user/project-registration/projectMembershipNavigation';
import { useToast } from '@/hooks/use-toast';
import {
  type OtherEquipmentFormData,
  type SolarPowerFormData,
} from '@/lib/equipmentValidations';
import { paths } from '@/routes/paths';

const SOLAR_FORM_DEFAULT_VALUES: SolarPowerFormData = {
  equipmentOpDate: '',
  operationStartDateFile: undefined,
  nominalMaxOutput: '',
  nominalMaxOutputFile: undefined,
  pcsManufacturer: '',
  pcsModel: '',
  powerCondUnitCount: '',
  deviceNo: '',
  powerCondStandby: '',
  pcsFile: undefined,
  hasBattery: '',
  batteryMaker: '',
  batteryModel: '',
  batteryCapacity: '',
  batteryFile: undefined,
  additionalEquipment: '',
  ictEquipmentPowerConsumption: '',
  additionalEquipmentDate: '',
  additionalEquipmentType: '',
  additionalUnitCount: '',
  additionalDeviceUniqueNo: '',
  totalInvestment: '',
  totalInvestmentFile: undefined,
  hasSubsidy: '',
  subsidyName: '',
  subsidyNameFile: undefined,
  subsidyAmount: '',
  preProjectUnitPrice: '',
  preProjectUnitPriceFile: undefined,
};

const OTHER_FORM_DEFAULT_VALUES: OtherEquipmentFormData = {
  oldEquipmentName: '',
  oldEquipmentId: '',
  oldEquipmentType: '',
  newEquipmentName: '',
  newEquipmentId: '',
  newEquipmentType: '',
};

const SOLAR_FORM_FIELD_NAMES = new Set<keyof SolarPowerFormData>(
  Object.keys(SOLAR_FORM_DEFAULT_VALUES) as Array<keyof SolarPowerFormData>,
);
const solarApiFieldAliases: Record<string, keyof SolarPowerFormData> = {
  pcsUnitCount: 'powerCondUnitCount',
  pcsStandby: 'powerCondStandby',
};
const REQUIRED_FILE_MESSAGE = '確認資料をアップロードしてください。';

function getMissingFileFields(
  data: SolarPowerFormData,
): Array<keyof SolarPowerFormData> {
  const requiredFiles: Array<{
    fieldName: keyof SolarPowerFormData;
    file?: File;
  }> = [
    {
      fieldName: 'operationStartDateFile',
      file: data.operationStartDateFile,
    },
    {
      fieldName: 'nominalMaxOutputFile',
      file: data.nominalMaxOutputFile,
    },
    { fieldName: 'pcsFile', file: data.pcsFile },
    {
      fieldName: 'totalInvestmentFile',
      file: data.totalInvestmentFile,
    },
    {
      fieldName: 'preProjectUnitPriceFile',
      file: data.preProjectUnitPriceFile,
    },
    ...(data.hasBattery === 'yes'
      ? [
          {
            fieldName: 'batteryFile' as const,
            file: data.batteryFile,
          },
        ]
      : []),
    ...(data.hasSubsidy === 'yes'
      ? [
          {
            fieldName: 'subsidyNameFile' as const,
            file: data.subsidyNameFile,
          },
        ]
      : []),
  ];

  return requiredFiles
    .filter(({ file }) => !(file instanceof File))
    .map(({ fieldName }) => fieldName);
}

function getErrorMessage(value: unknown): string | null {
  if (typeof value === 'string') return value;
  if (Array.isArray(value)) {
    const messages = value.filter(
      (message): message is string => typeof message === 'string',
    );
    return messages.length ? messages.join('、') : null;
  }
  return null;
}

function getSolarFormFieldName(path: string): keyof SolarPowerFormData | null {
  const fieldName = path.replace(/\[\d+\]/g, '').split('.').at(-1);
  if (!fieldName) return null;

  if (fieldName in solarApiFieldAliases) {
    return solarApiFieldAliases[fieldName];
  }

  return SOLAR_FORM_FIELD_NAMES.has(fieldName as keyof SolarPowerFormData)
    ? (fieldName as keyof SolarPowerFormData)
    : null;
}

function getInternalReturnPath(state: unknown): string | null {
  if (!state || typeof state !== 'object' || !('returnTo' in state)) {
    return null;
  }

  const returnTo = (state as { returnTo?: unknown }).returnTo;
  return typeof returnTo === 'string' && returnTo.startsWith('/')
    ? returnTo
    : null;
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

function normalizeStoredSolarData(value: unknown): SolarPowerFormData {
  if (!value || typeof value !== 'object') return SOLAR_FORM_DEFAULT_VALUES;
  const source = value as Record<string, unknown>;

  return {
    ...SOLAR_FORM_DEFAULT_VALUES,
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
    additionalEquipment: getYesNoValue(source, 'additionalEquipment'),
    ictEquipmentPowerConsumption: getStringValue(
      source,
      'ictEquipmentPowerConsumption',
    ),
    additionalEquipmentDate: getStringValue(
      source,
      'additionalEquipmentDate',
    ),
    additionalEquipmentType: getStringValue(
      source,
      'additionalEquipmentType',
    ),
    additionalUnitCount: getStringValue(source, 'additionalUnitCount'),
    additionalDeviceUniqueNo: getStringValue(
      source,
      'additionalDeviceUniqueNo',
    ),
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
  const enteredFromMasterDashboard = isFromMasterDashboard(location.state);
  const { toast } = useToast();
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [formType] = useState<EquipmentFormType>(() =>
    searchParams.get('type') === 'other' ? 'other' : 'solar',
  );

  const solarForm = useForm<SolarPowerFormData>({
    defaultValues: SOLAR_FORM_DEFAULT_VALUES,
  });

  const otherForm = useForm<OtherEquipmentFormData>({
    defaultValues: OTHER_FORM_DEFAULT_VALUES,
  });

  useEffect(() => {
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
        solarForm.reset(normalizeStoredSolarData(equipment.solarData));
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
  }, [equipmentId, otherForm, projectId, solarForm, toast]);

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
    const missingFileFields = getMissingFileFields(data);
    missingFileFields.forEach((fieldName) => {
      solarForm.setError(fieldName, {
        type: 'required',
        message: REQUIRED_FILE_MESSAGE,
      });
    });
    setIsSubmitting(true);

    try {
      const tempId = equipmentId ?? `equipment-${Date.now()}`;
      const payload = mapSolarFormToEquipmentValidationItem(
        data,
        tempId,
        siteId,
        [],
      );

      await validateSiteProjectEquipment(payload);
      if (missingFileFields.length > 0) {
        toast({
          title: '確認資料が不足しています',
          description: REQUIRED_FILE_MESSAGE,
          variant: 'destructive',
        });
        return;
      }

      saveEquipmentLocally({
        id: tempId,
        formType: 'solar',
        solarData: data,
      });
      navigateToProjectRegister();
    } catch (error) {
      if (
        error instanceof ApiError &&
        error.payload?.errors &&
        typeof error.payload.errors === 'object'
      ) {
        Object.entries(error.payload.errors).forEach(([path, value]) => {
          const fieldName = getSolarFormFieldName(path);
          const message = getErrorMessage(value);
          if (fieldName && message) {
            solarForm.setError(fieldName, { type: 'server', message });
          }
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

  const submitOtherEquipment = (data: OtherEquipmentFormData) => {
    const tempId = equipmentId ?? `equipment-${Date.now()}`;
    saveEquipmentLocally({
      id: tempId,
      formType: 'other',
      ...data,
    });
    navigateToProjectRegister();
  };

  const handleCancel = () => {
    navigate(returnPath ?? paths.projectMembership(siteId), {
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
        <p className="text-sm text-muted-foreground">
          設備種別：{equipmentTypeLabel}
        </p>

        {formType === 'solar' ? (
          <Form {...solarForm}>
            <form
              onSubmit={solarForm.handleSubmit(submitSolarEquipment)}
              noValidate
            >
              <FormCard>
                <SolarPowerEquipmentForm form={solarForm} />
                <div className="flex justify-end gap-4 pt-6">
                  <Button
                    type="button"
                    variant="outline"
                    onClick={handleCancel}
                    disabled={isSubmitting}
                    size="md"
                  >
                    キャンセル
                  </Button>
                  <Button
                    type="submit"
                    disabled={isSubmitting}
                    size="md"
                  >
                    {isSubmitting ? '確認中...' : '確認'}
                  </Button>
                </div>
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
                <div className="flex justify-end gap-4 pt-6">
                  <Button
                    type="button"
                    variant="outline"
                    onClick={handleCancel}
                    size="md"
                  >
                    キャンセル
                  </Button>
                  <Button type="submit" size="md">
                    確認
                  </Button>
                </div>
              </FormCard>
            </form>
          </Form>
        )}
      </FormPageContent>
    </FormStandalonePage>
  );
}
