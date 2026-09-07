export type YesNoValue = '' | 'yes' | 'no';

export type SolarPowerFormData = {
  siteId: string;
  /** Used only by the history-preserving equipment change flow. */
  changeDate?: string;
  equipmentOpDate: string;
  operationStartDateFile?: File | null;
  nominalMaxOutput: string;
  nominalMaxOutputFile?: File | null;
  pcsManufacturer: string;
  pcsModel: string;
  deviceNo: string;
  powerCondStandby: string;
  pcsFile?: File | null;
  hasBattery: YesNoValue;
  batteryMaker: string;
  batteryModel: string;
  batteryCapacity: string;
  batteryFile?: File | null;
  totalInvestment: string;
  totalInvestmentFile?: File | null;
  hasSubsidy: YesNoValue;
  subsidyName: string;
  subsidyNameFile?: File | null;
  subsidyAmount: string;
  preProjectUnitPrice: string;
  preProjectUnitPriceFile?: File | null;
};

export type OtherEquipmentFormData = {
  oldEquipmentName: string;
  oldEquipmentId: string;
  oldEquipmentType: string;
  newEquipmentName: string;
  newEquipmentId: string;
  newEquipmentType: string;
};
