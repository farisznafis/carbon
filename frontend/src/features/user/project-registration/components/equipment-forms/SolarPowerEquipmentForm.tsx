import { Label } from "@/components/ui/label";
import { Input } from "@/components/ui/input";
import { UseFormReturn } from "react-hook-form";
import { SolarPowerFormData } from "@/lib/equipmentValidations";
import { FormField, FormItem, FormLabel, FormControl, FormMessage } from "@/components/ui/form";
import { Upload } from "lucide-react";
import { RadioGroup, RadioGroupItem } from "@/components/ui/radio-group";

interface SolarPowerEquipmentFormProps {
  form: UseFormReturn<SolarPowerFormData>;
}

export function SolarPowerEquipmentForm({ form }: Readonly<SolarPowerEquipmentFormProps>) {
  const hasBattery = form.watch("hasBattery");

  return (
    <div className="space-y-6">
      {/* 太陽光発電設備の情報 */}
      <div className="space-y-4">
        <h3 className="font-semibold text-lg">1. 太陽光発電設備の情報</h3>
        <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
          <FormField
            control={form.control}
            name="equipmentOpDate"
            render={({ field }) => (
              <FormItem>
                <FormLabel>稼働開始日</FormLabel>
                <FormControl>
                  <Input type="date" {...field} />
                </FormControl>
                <FormMessage />
              </FormItem>
            )}
          />
        </div>

        <FormField
          control={form.control}
          name="operationStartDateFile"
          render={({ field: { value, onChange, ...field } }) => (
            <FormItem>
              <FormLabel>確認資料：太陽光発電電力受給契約確認書</FormLabel>
              <FormControl>
                <div className="space-y-2">
                  <label
                    htmlFor="equipment-op-date-file"
                    className="cursor-pointer text-sm text-gray-800 bg-gray-200 hover:bg-gray-300 py-1 px-3 rounded-md shadow-sm flex items-center w-fit"
                  >
                    <Upload className="mr-2 h-4 w-4" />
                    ファイルを選択
                  </label>
                  <input
                    id="equipment-op-date-file"
                    type="file"
                    className="hidden"
                    onChange={(e) => {
                      const file = e.target.files?.[0];
                      onChange(file);
                    }}
                    {...field}
                  />
                  {value && (
                    <span className="text-sm text-gray-700">{value.name}</span>
                  )}
                </div>
              </FormControl>
              <FormMessage />
            </FormItem>
          )}
        />
      </div>

      {/* 太陽光パネルの情報 */}
      <div className="space-y-4">
        <h3 className="font-semibold text-lg">2. 太陽光パネルの情報</h3>
        <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
          <FormField
            control={form.control}
            name="nominalMaxOutput"
            render={({ field }) => (
              <FormItem>
                <FormLabel>公称最大出力 (kW)</FormLabel>
                <FormControl>
                  <Input type="number" placeholder="例：10.5" {...field} />
                </FormControl>
                <FormMessage />
              </FormItem>
            )}
          />
        </div>
        <FormField
          control={form.control}
          name="nominalMaxOutputFile"
          render={({ field: { value, onChange, ...field } }) => (
            <FormItem>
              <FormLabel>確認資料：仕様書・カタログ</FormLabel>
              <FormControl>
                <div className="space-y-2">
                  <label
                    htmlFor="nominal-max-output-file"
                    className="cursor-pointer text-sm text-gray-800 bg-gray-200 hover:bg-gray-300 py-1 px-3 rounded-md shadow-sm flex items-center w-fit"
                  >
                    <Upload className="mr-2 h-4 w-4" />
                    ファイルを選択
                  </label>
                  <input
                    id="nominal-max-output-file"
                    type="file"
                    className="hidden"
                    onChange={(e) => {
                      const file = e.target.files?.[0];
                      onChange(file);
                    }}
                    {...field}
                  />
                  {value && (
                    <span className="text-sm text-gray-700">{value.name}</span>
                  )}
                </div>
              </FormControl>
              <FormMessage />
            </FormItem>
          )}
        />
      </div>

      {/* パワーコンディショナー情報 */}
      <div className="space-y-4">
        <h3 className="font-semibold text-lg">3. パワーコンディショナー情報</h3>
        <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
          <FormField
            control={form.control}
            name="pcManufacturer"
            render={({ field }) => (
              <FormItem>
                <FormLabel>メーカー</FormLabel>
                <FormControl>
                  <Input placeholder="例：○○電機" {...field} />
                </FormControl>
                <FormMessage />
              </FormItem>
            )}
          />

          <FormField
            control={form.control}
            name="pcModel"
            render={({ field }) => (
              <FormItem>
                <FormLabel>型番</FormLabel>
                <FormControl>
                  <Input placeholder="例：ABC-123" {...field} />
                </FormControl>
                <FormMessage />
              </FormItem>
            )}
          />

          <FormField
            control={form.control}
            name="pcQuantity"
            render={({ field }) => (
              <FormItem>
                <FormLabel>台数</FormLabel>
                <FormControl>
                  <Input type="number" placeholder="例：2" {...field} />
                </FormControl>
                <FormMessage />
              </FormItem>
            )}
          />

          <FormField
            control={form.control}
            name="pcSerialNumber"
            render={({ field }) => (
              <FormItem>
                <FormLabel>機器固有番号（製造番号）</FormLabel>
                <FormControl>
                  <Input placeholder="例：SN123456" {...field} />
                </FormControl>
                <FormMessage />
              </FormItem>
            )}
          />

          <FormField
            control={form.control}
            name="pcStandbyPower"
            render={({ field }) => (
              <FormItem>
                <FormLabel>夜間待機電力（1台あたり） (W)</FormLabel>
                <FormControl>
                  <Input type="number" placeholder="例：50" {...field} />
                </FormControl>
                <FormMessage />
              </FormItem>
            )}
          />
        </div>

        <FormField
          control={form.control}
          name="pcFile"
          render={({ field: { value, onChange, ...field } }) => (
            <FormItem>
              <FormLabel>確認資料：仕様書</FormLabel>
              <FormControl>
                <div className="space-y-2">
                  <label
                    htmlFor="pc-file"
                    className="cursor-pointer text-sm text-gray-800 bg-gray-200 hover:bg-gray-300 py-1 px-3 rounded-md shadow-sm flex items-center w-fit"
                  >
                    <Upload className="mr-2 h-4 w-4" />
                    ファイルを選択
                  </label>
                  <input
                    id="pc-file"
                    type="file"
                    className="hidden"
                    onChange={(e) => {
                      const file = e.target.files?.[0];
                      onChange(file);
                    }}
                    {...field}
                  />
                  {value && (
                    <span className="text-sm text-gray-700">{value.name}</span>
                  )}
                </div>
              </FormControl>
              <FormMessage />
            </FormItem>
          )}
        />
      </div>

      {/* 蓄電池情報 */}
      <div className="space-y-4">
        <h3 className="font-semibold text-lg">4. 蓄電池情報</h3>

        <FormField
          control={form.control}
          name="hasBattery"
          render={({ field }) => (
            <FormItem>
              <FormLabel>蓄電池の有無</FormLabel>
              <FormControl>
                <RadioGroup value={field.value} onValueChange={field.onChange}>
                  <div className="flex items-center space-x-2">
                    <RadioGroupItem value="yes" id="battery-yes" />
                    <Label htmlFor="battery-yes" className="font-normal cursor-pointer">
                      あり
                    </Label>
                  </div>
                  <div className="flex items-center space-x-2">
                    <RadioGroupItem value="no" id="battery-no" />
                    <Label htmlFor="battery-no" className="font-normal cursor-pointer">
                      なし
                    </Label>
                  </div>
                </RadioGroup>
              </FormControl>
              <FormMessage />
            </FormItem>
          )}
        />

        {hasBattery === "yes" && (
          <div className="space-y-4">
            <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
              <FormField
                control={form.control}
                name="batteryManufacturer"
                render={({ field }) => (
                  <FormItem>
                    <FormLabel>メーカー</FormLabel>
                    <FormControl>
                      <Input placeholder="例：△△バッテリー" {...field} />
                    </FormControl>
                    <FormMessage />
                  </FormItem>
                )}
              />

              <FormField
                control={form.control}
                name="batteryModel"
                render={({ field }) => (
                  <FormItem>
                    <FormLabel>型番</FormLabel>
                    <FormControl>
                      <Input placeholder="例：BAT-456" {...field} />
                    </FormControl>
                    <FormMessage />
                  </FormItem>
                )}
              />

              <FormField
                control={form.control}
                name="batteryCapacity"
                render={({ field }) => (
                  <FormItem>
                    <FormLabel>容量 (kWh)</FormLabel>
                    <FormControl>
                      <Input type="number" placeholder="例：5.0" {...field} />
                    </FormControl>
                    <FormMessage />
                  </FormItem>
                )}
              />
            </div>

            <FormField
              control={form.control}
              name="batteryFile"
              render={({ field: { value, onChange, ...field } }) => (
                <FormItem>
                  <FormLabel>確認資料：仕様書・カタログ</FormLabel>
                  <FormControl>
                    <div className="space-y-2">
                      <label
                        htmlFor="battery-file"
                        className="cursor-pointer text-sm text-gray-800 bg-gray-200 hover:bg-gray-300 py-1 px-3 rounded-md shadow-sm flex items-center w-fit"
                      >
                        <Upload className="mr-2 h-4 w-4" />
                        ファイルを選択
                      </label>
                      <input
                        id="battery-file"
                        type="file"
                        className="hidden"
                        onChange={(e) => {
                          const file = e.target.files?.[0];
                          onChange(file);
                        }}
                        {...field}
                      />
                      {value && (
                        <span className="text-sm text-gray-700">{value.name}</span>
                      )}
                    </div>
                  </FormControl>
                  <FormMessage />
                </FormItem>
              )}
            />
          </div>
        )}
      </div>

      {/* 追加性評価に必要な情報 */}
      <div className="space-y-4">
        <h3 className="font-semibold text-lg">5. 追加性評価に必要な情報</h3>

        <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
          <FormField
            control={form.control}
            name="totalInvestment"
            render={({ field }) => (
              <FormItem className="md:col-span-2">
                <FormLabel>設備の総投資額 (円)</FormLabel>
                <FormControl>
                  <Input type="number" placeholder="例：1000000" {...field} />
                </FormControl>
                <FormMessage />
              </FormItem>
            )}
          />
        </div>
            
        <FormField
          control={form.control}
          name="totalInvestmentFile"
          render={({ field: { value, onChange, ...field } }) => (
            <FormItem>
              <FormLabel>確認資料：工事請負契約書、請求書、領収書等</FormLabel>
              <FormControl>
                <div className="space-y-2">
                  <label
                    htmlFor="total-investment-file"
                    className="cursor-pointer text-sm text-gray-800 bg-gray-200 hover:bg-gray-300 py-1 px-3 rounded-md shadow-sm flex items-center w-fit"
                  >
                    <Upload className="mr-2 h-4 w-4" />
                    ファイルを選択
                  </label>
                  <input
                    id="total-investment-file"
                    type="file"
                    className="hidden"
                    onChange={(e) => {
                      const file = e.target.files?.[0];
                      onChange(file);
                    }}
                    {...field}
                  />
                  {value && (
                    <span className="text-sm text-gray-700">{value.name}</span>
                  )}
                </div>
              </FormControl>
              <FormMessage />
            </FormItem>
          )}
        />

        {/* 蓄電池情報 */}
        <div>
        <FormField
          control={form.control}
          name="hasSubsidy"
          render={({ field }) => (
            <FormItem>
              <FormLabel>補助金の有無</FormLabel>
              <FormControl>
                <RadioGroup value={field.value} onValueChange={field.onChange}>
                  <div className="flex items-center space-x-2">
                    <RadioGroupItem value="yes" id="subsidy-yes" />
                    <Label htmlFor="subsidy-yes" className="font-normal cursor-pointer">
                      あり
                    </Label>
                  </div>
                  <div className="flex items-center space-x-2">
                    <RadioGroupItem value="no" id="subsidy-no" />
                    <Label htmlFor="subsidy-no" className="font-normal cursor-pointer">
                      なし
                    </Label>
                  </div>
                </RadioGroup>
              </FormControl>
              <FormMessage />
            </FormItem>
          )}
        />
        </div>

        <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
          <FormField
            control={form.control}
            name="receivedSubsidy"
            render={({ field }) => (
              <FormItem>
                <FormLabel>補助金受領額 (円)</FormLabel>
                <FormControl>
                  <Input type="number" placeholder="例：300000" {...field} />
                </FormControl>
                <FormMessage />
              </FormItem>
            )}
          />
        </div>

        <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
          <FormField
            control={form.control}
            name="subsidyName"
            render={({ field }) => (
              <FormItem className="md:col-span-2">
                <FormLabel>補助金名称</FormLabel>
                <FormControl>
                  <Input placeholder="例：再生可能エネルギー導入補助金" {...field} />
                </FormControl>
                <FormMessage />
              </FormItem>
            )}
          />
        </div>

        <FormField
          control={form.control}
          name="subsidyNameFile"
          render={({ field: { value, onChange, ...field } }) => (
            <FormItem>
              <FormLabel>確認資料：補助金交付決定通知書</FormLabel>
              <FormControl>
                <div className="space-y-2">
                  <label
                    htmlFor="subsidy-name-file"
                    className="cursor-pointer text-sm text-gray-800 bg-gray-200 hover:bg-gray-300 py-1 px-3 rounded-md shadow-sm flex items-center w-fit"
                  >
                    <Upload className="mr-2 h-4 w-4" />
                    ファイルを選択
                  </label>
                  <input
                    id="subsidy-name-file"
                    type="file"
                    className="hidden"
                    onChange={(e) => {
                      const file = e.target.files?.[0];
                      onChange(file);
                    }}
                    {...field}
                  />
                  {value && (
                    <span className="text-sm text-gray-700">{value.name}</span>
                  )}
                </div>
              </FormControl>
              <FormMessage />
            </FormItem>
          )}
        />

        <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
          <FormField
            control={form.control}
            name="subsidyAmount"
            render={({ field }) => (
              <FormItem>
                <FormLabel>補助金額 (円)</FormLabel>
                <FormControl>
                  <Input type="number" placeholder="例：300000" {...field} />
                </FormControl>
                <FormMessage />
              </FormItem>
            )}
          />
        </div>

        <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
          <FormField
            control={form.control}
            name="prePurchaseUnitPrice"
            render={({ field }) => (
              <FormItem className="md:col-span-2">
                <FormLabel>プロジェクト実施前の電力購入単価 (円/kWh)</FormLabel>
                <FormControl>
                  <Input type="number" placeholder="例：42" {...field} />
                </FormControl>
                <FormMessage />
              </FormItem>
            )}
          />
        </div>

        <FormField
          control={form.control}
          name="prePurchaseUnitPriceFile"
          render={({ field: { value, onChange, ...field } }) => (
            <FormItem>
              <FormLabel>確認資料：電力会社からの請求明細書等</FormLabel>
              <FormControl>
                <div className="space-y-2">
                  <label
                    htmlFor="pre-purchase-unit-price-file"
                    className="cursor-pointer text-sm text-gray-800 bg-gray-200 hover:bg-gray-300 py-1 px-3 rounded-md shadow-sm flex items-center w-fit"
                  >
                    <Upload className="mr-2 h-4 w-4" />
                    ファイルを選択
                  </label>
                  <input
                    id="pre-purchase-unit-price-file"
                    type="file"
                    className="hidden"
                    onChange={(e) => {
                      const file = e.target.files?.[0];
                      onChange(file);
                    }}
                    {...field}
                  />
                  {value && (
                    <span className="text-sm text-gray-700">{value.name}</span>
                  )}
                </div>
              </FormControl>
              <FormMessage />
            </FormItem>
          )}
        />
      </div>
    </div>
  );
}
