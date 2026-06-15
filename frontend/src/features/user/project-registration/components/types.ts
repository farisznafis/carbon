export type EquipmentFormType = "solar" | "other";

// export interface Equipment { //solar panel
//   id: string;
//   equipmentOpDate: string;
//   nominalMaxOutput: string; 
//   manufacturer: string;
//   modelNo: string;
//   powerCondUnitCount: string;
//   subsidyName?: string;
//   verificationFile?: File;
//   companyId: string;
//   formType?: string;
//   solarData?: SolarPowerData;
// }

export interface Equipment {
  id: string;
  formType: EquipmentFormType; //maybe remove if methodology included
  //shared variables
  equipmentOpDate: string;
  nominalMaxOutput: string; 
  manufacturer: string;
  modelNo: string;
  unitCount: string;
  totalInvestment: string;
  totalInvestmentFile?: File | null;
  receivedSubsidy: string;
  subsidyName: string;
  subsidyNameFile?: File | null;
  subsidyAmount: string;
  prePurchaseUnitPrice: string;
  prePurchaseUnitPriceFile?: File | null;
  // For "other" equipment type
  oldEquipmentName?: string;
  oldEquipmentId?: string;
  oldEquipmentType?: string;
  newEquipmentName?: string;
  newEquipmentId?: string;
  newEquipmentType?: string;
  // For "solar" equipment type
  solarData?: SolarPowerData;
  verificationFile?: File;
  methodology?: string; 
}

export interface SolarPowerData extends Equipment{
  powerCondUnitCount?: string;
  equipmentOpDateFile?: File | null;
  nominalMaxOutputFile?: File | null;
  pcSerialNumber: string;
  pcStandbyPower: string;
  pcFile?: File | null;
  hasBattery: string;
  batteryFile?: File | null;
  batteryManufacturer: string;
  batteryModel: string;
  batteryCapacity: string;
  
}

export interface Agreements {
  dataAccuracy: boolean;
  compliance: boolean;
  reporting: boolean;
  inspection: boolean;
}