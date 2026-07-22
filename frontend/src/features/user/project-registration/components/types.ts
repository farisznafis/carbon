import type {
  OtherEquipmentFormData,
  SolarPowerFormData,
} from '@/lib/equipmentValidations';
import type { SiteProjectEquipmentValidationFile } from '@/api/site';

export type EquipmentFormType = 'solar' | 'other';

export type SolarEquipment = {
  id: string;
  formType: 'solar';
  solarData: SolarPowerFormData;
  files: SiteProjectEquipmentValidationFile[];
};

export type OtherEquipment = OtherEquipmentFormData & {
  id: string;
  formType: 'other';
};

export type Equipment = SolarEquipment | OtherEquipment;

export interface Agreements {
  dataAccuracy: boolean;
  compliance: boolean;
  reporting: boolean;
  inspection: boolean;
}
