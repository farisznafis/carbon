export type YesNoValue = '' | 'yes' | 'no';

export type SolarPowerFormData = {
  siteId: string;
  equipmentOpDate: string;
  operationStartDateFile?: File;
  nominalMaxOutput: string;
  nominalMaxOutputFile?: File;
  pcsManufacturer: string;
  pcsModel: string;
  powerCondUnitCount: string;
  deviceNo: string;
  powerCondStandby: string;
  pcsFile?: File;
  hasBattery: YesNoValue;
  batteryMaker: string;
  batteryModel: string;
  batteryCapacity: string;
  batteryFile?: File;
  totalInvestment: string;
  totalInvestmentFile?: File;
  hasSubsidy: YesNoValue;
  subsidyName: string;
  subsidyNameFile?: File;
  subsidyAmount: string;
  preProjectUnitPrice: string;
  preProjectUnitPriceFile?: File;
};

export type OtherEquipmentFormData = {
  oldEquipmentName: string;
  oldEquipmentId: string;
  oldEquipmentType: string;
  newEquipmentName: string;
  newEquipmentId: string;
  newEquipmentType: string;
};
