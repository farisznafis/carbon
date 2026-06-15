import { z } from 'zod';

// Solar Power Equipment Schema
export const solarPowerSchema = z.object({
  equipmentOpDate: z.string().min(1, "運転開始日を入力してください"),
  operationStartDateFile: z.instanceof(File, { message: "確認資料をアップロードしてください" }),
  nominalMaxOutput: z.string().min(1, "公称最大出力を入力してください"),
  nominalMaxOutputFile: z.instanceof(File, { message: "確認資料をアップロードしてください" }),
  pcManufacturer: z.string().min(1, "メーカーを入力してください"),
  pcModel: z.string().min(1, "型番を入力してください"),
  pcQuantity: z.string().min(1, "台数を入力してください"),
  pcSerialNumber: z.string().min(1, "シリアル番号を入力してください"),
  pcStandbyPower: z.string().min(1, "待機時消費電力を入力してください"),
  pcFile: z.instanceof(File, { message: "確認資料をアップロードしてください" }),
  hasBattery: z.string().min(1, "蓄電池の有無を選択してください"),
  batteryManufacturer: z.string(),
  batteryModel: z.string(),
  batteryCapacity: z.string(),
  batteryFile: z.instanceof(File, { message: "確認資料をアップロードしてください" }).optional(),
  totalInvestment: z.string().min(1, "総投資額を入力してください"),
  totalInvestmentFile: z.instanceof(File, { message: "確認資料をアップロードしてください" }),
  hasSubsidy: z.string().min(1, "補助金の有無を選択してください"),
  receivedSubsidy: z.string().min(1, "補助金の受領有無を選択してください"),
  subsidyName: z.string().min(1, "補助金名称を入力してください"),
  subsidyNameFile: z.instanceof(File, { message: "確認資料をアップロードしてください" }),
  subsidyAmount: z.string().min(1, "補助金額を入力してください"),
  prePurchaseUnitPrice: z.string().min(1, "更新前買取単価を入力してください"),
  prePurchaseUnitPriceFile: z.instanceof(File, { message: "確認資料をアップロードしてください" }),
}).refine((data) => {
  if (data.hasBattery === "yes") {
    if (!data.batteryManufacturer) {
      return false;
    }
  }
  return true;
}, {
  message: "メーカーを入力してください",
  path: ["batteryManufacturer"],
}).refine((data) => {
  if (data.hasBattery === "yes") {
    if (!data.batteryModel) {
      return false;
    }
  }
  return true;
}, {
  message: "型番を入力してください",
  path: ["batteryModel"],
}).refine((data) => {
  if (data.hasBattery === "yes") {
    if (!data.batteryCapacity) {
      return false;
    }
  }
  return true;
}, {
  message: "容量を入力してください",
  path: ["batteryCapacity"],
}).refine((data) => {
  if (data.hasBattery === "yes" && !data.batteryFile) {
    return false;
  }
  return true;
}, {
  message: "確認資料をアップロードしてください",
  path: ["batteryFile"],
});

// Other Equipment Schema
export const otherEquipmentSchema = z.object({
  oldEquipmentName: z.string().min(1, "旧設備名を入力してください"),
  oldEquipmentId: z.string().min(1, "旧設備IDを入力してください"),
  oldEquipmentType: z.string().min(1, "旧設備タイプを入力してください"),
  newEquipmentName: z.string().min(1, "新設備名を入力してください"),
  newEquipmentId: z.string().min(1, "新設備IDを入力してください"),
  newEquipmentType: z.string().min(1, "新設備タイプを入力してください"),
});

export type SolarPowerFormData = z.infer<typeof solarPowerSchema>;
export type OtherEquipmentFormData = z.infer<typeof otherEquipmentSchema>;
