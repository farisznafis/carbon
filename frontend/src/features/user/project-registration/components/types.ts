import type {
  OtherEquipmentFormData,
  SolarPowerFormData,
} from '@/lib/equipmentValidations';

export type EquipmentFormType = 'solar' | 'other';

export type SolarEquipment = {
  id: string;
  formType: 'solar';
  solarData: SolarPowerFormData;
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
