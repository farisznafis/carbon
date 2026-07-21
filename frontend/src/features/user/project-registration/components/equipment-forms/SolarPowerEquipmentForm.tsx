import type { UseFormReturn } from 'react-hook-form';
import { Upload } from 'lucide-react';
import {
  FormGrid,
  FormSection,
  FormSectionStack,
} from '@/components/forms/form-layout';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { RadioGroup, RadioGroupItem } from '@/components/ui/radio-group';
import {
  FormControl,
  FormField,
  FormItem,
  FormLabel,
  FormMessage,
} from '@/components/ui/form';
import type { SolarPowerFormData } from '@/lib/equipmentValidations';

type SolarPowerEquipmentFormProps = Readonly<{
  form: UseFormReturn<SolarPowerFormData>;
}>;

type FileUploadControlProps = Readonly<{
  id: string;
  value?: File;
  onChange: (file?: File) => void;
}>;

function FileUploadControl({
  id,
  value,
  onChange,
}: FileUploadControlProps) {
  return (
    <div className="space-y-2">
      <label
        htmlFor={id}
        className="flex w-fit cursor-pointer items-center rounded-md bg-secondary px-3 py-1 text-sm text-secondary-foreground shadow-app-sm hover:bg-secondary/80"
      >
        <Upload className="mr-2 h-4 w-4" />
        ファイルを選択
      </label>
      <input
        id={id}
        type="file"
        className="hidden"
        onChange={(event) => onChange(event.target.files?.[0])}
      />
      {value && (
        <span className="text-sm text-muted-foreground">{value.name}</span>
      )}
    </div>
  );
}

type YesNoRadioProps = Readonly<{
  idPrefix: string;
  value: '' | 'yes' | 'no';
  onValueChange: (value: 'yes' | 'no') => void;
}>;

function YesNoRadio({ idPrefix, value, onValueChange }: YesNoRadioProps) {
  return (
    <RadioGroup
      value={value}
      onValueChange={(nextValue) =>
        onValueChange(nextValue as 'yes' | 'no')
      }
    >
      <div className="flex items-center space-x-2">
        <RadioGroupItem value="yes" id={`${idPrefix}-yes`} />
        <Label htmlFor={`${idPrefix}-yes`} className="cursor-pointer font-normal">
          有
        </Label>
      </div>
      <div className="flex items-center space-x-2">
        <RadioGroupItem value="no" id={`${idPrefix}-no`} />
        <Label htmlFor={`${idPrefix}-no`} className="cursor-pointer font-normal">
          無
        </Label>
      </div>
    </RadioGroup>
  );
}

export function SolarPowerEquipmentForm({
  form,
}: SolarPowerEquipmentFormProps) {
  const hasBattery = form.watch('hasBattery') === 'yes';
  const hasAdditionalEquipment = form.watch('additionalEquipment') === 'yes';
  const hasSubsidy = form.watch('hasSubsidy') === 'yes';

  return (
    <FormSectionStack>
      <FormSection number={1} title="太陽光発電設備の情報">
        <FormField
          control={form.control}
          name="equipmentOpDate"
          render={({ field }) => (
            <FormItem>
              <FormLabel>稼働開始日（電力受給開始日）</FormLabel>
              <FormControl>
                <Input type="date" className="max-w-sm" {...field} />
              </FormControl>
              <FormMessage />
            </FormItem>
          )}
        />
        <FormField
          control={form.control}
          name="operationStartDateFile"
          render={({ field }) => (
            <FormItem>
              <FormLabel>確認資料：太陽光発電電力受給契約確認書</FormLabel>
              <FormControl>
                <FileUploadControl
                  id="equipment-op-date-file"
                  value={field.value}
                  onChange={field.onChange}
                />
              </FormControl>
              <FormMessage />
            </FormItem>
          )}
        />
      </FormSection>

      <FormSection number={2} title="太陽光パネルの情報">
        <FormField
          control={form.control}
          name="nominalMaxOutput"
          render={({ field }) => (
            <FormItem>
              <FormLabel>公称最大出力 (kW)</FormLabel>
              <FormControl>
                <Input
                  type="number"
                  min="0"
                  step="any"
                  className="max-w-sm"
                  {...field}
                />
              </FormControl>
              <FormMessage />
            </FormItem>
          )}
        />
        <FormField
          control={form.control}
          name="nominalMaxOutputFile"
          render={({ field }) => (
            <FormItem>
              <FormLabel>確認資料：仕様書・カタログ</FormLabel>
              <FormControl>
                <FileUploadControl
                  id="nominal-max-output-file"
                  value={field.value}
                  onChange={field.onChange}
                />
              </FormControl>
              <FormMessage />
            </FormItem>
          )}
        />
      </FormSection>

      <FormSection number={3} title="パワーコンディショナーの情報">
        <FormGrid>
          <FormField
            control={form.control}
            name="pcsManufacturer"
            render={({ field }) => (
              <FormItem>
                <FormLabel>メーカー</FormLabel>
                <FormControl>
                  <Input {...field} />
                </FormControl>
                <FormMessage />
              </FormItem>
            )}
          />
          <FormField
            control={form.control}
            name="pcsModel"
            render={({ field }) => (
              <FormItem>
                <FormLabel>型式</FormLabel>
                <FormControl>
                  <Input {...field} />
                </FormControl>
                <FormMessage />
              </FormItem>
            )}
          />
          <FormField
            control={form.control}
            name="powerCondUnitCount"
            render={({ field }) => (
              <FormItem>
                <FormLabel>導入台数 (台)</FormLabel>
                <FormControl>
                  <Input type="number" min="0" step="1" {...field} />
                </FormControl>
                <FormMessage />
              </FormItem>
            )}
          />
          <FormField
            control={form.control}
            name="deviceNo"
            render={({ field }) => (
              <FormItem>
                <FormLabel>機器固有番号（製造番号）</FormLabel>
                <FormControl>
                  <Input {...field} />
                </FormControl>
                <FormMessage />
              </FormItem>
            )}
          />
          <FormField
            control={form.control}
            name="powerCondStandby"
            render={({ field }) => (
              <FormItem>
                <FormLabel>夜間待機電力（1台あたり） (W)</FormLabel>
                <FormControl>
                  <Input type="number" min="0" step="any" {...field} />
                </FormControl>
                <FormMessage />
              </FormItem>
            )}
          />
        </FormGrid>
        <FormField
          control={form.control}
          name="pcsFile"
          render={({ field }) => (
            <FormItem>
              <FormLabel>確認資料：仕様書・カタログ</FormLabel>
              <FormControl>
                <FileUploadControl
                  id="pcs-file"
                  value={field.value}
                  onChange={field.onChange}
                />
              </FormControl>
              <FormMessage />
            </FormItem>
          )}
        />
      </FormSection>

      <FormSection number={4} title="蓄電池の情報">
        <FormField
          control={form.control}
          name="hasBattery"
          render={({ field }) => (
            <FormItem>
              <FormLabel>蓄電池設置の有無</FormLabel>
              <FormControl>
                <YesNoRadio
                  idPrefix="battery"
                  value={field.value}
                  onValueChange={field.onChange}
                />
              </FormControl>
              <FormMessage />
            </FormItem>
          )}
        />
        {hasBattery && (
          <div className="space-y-4">
            <FormGrid>
              <FormField
                control={form.control}
                name="batteryMaker"
                render={({ field }) => (
                  <FormItem>
                    <FormLabel>メーカー</FormLabel>
                    <FormControl>
                      <Input {...field} />
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
                    <FormLabel>型式</FormLabel>
                    <FormControl>
                      <Input {...field} />
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
                      <Input type="number" min="0" step="any" {...field} />
                    </FormControl>
                    <FormMessage />
                  </FormItem>
                )}
              />
            </FormGrid>
            <FormField
              control={form.control}
              name="batteryFile"
              render={({ field }) => (
                <FormItem>
                  <FormLabel>確認資料：仕様書・カタログ</FormLabel>
                  <FormControl>
                    <FileUploadControl
                      id="battery-file"
                      value={field.value}
                      onChange={field.onChange}
                    />
                  </FormControl>
                  <FormMessage />
                </FormItem>
              )}
            />
          </div>
        )}
      </FormSection>

      <FormSection number={5} title="追加設備の情報">
        <FormField
          control={form.control}
          name="additionalEquipment"
          render={({ field }) => (
            <FormItem>
              <FormLabel>追加設備の有無</FormLabel>
              <FormControl>
                <YesNoRadio
                  idPrefix="additional-equipment"
                  value={field.value}
                  onValueChange={field.onChange}
                />
              </FormControl>
              <FormMessage />
            </FormItem>
          )}
        />
        {hasAdditionalEquipment && (
          <FormGrid>
            <FormField
              control={form.control}
              name="additionalEquipmentDate"
              render={({ field }) => (
                <FormItem>
                  <FormLabel>導入日</FormLabel>
                  <FormControl>
                    <Input type="date" {...field} />
                  </FormControl>
                  <FormMessage />
                </FormItem>
              )}
            />
            <FormField
              control={form.control}
              name="additionalEquipmentType"
              render={({ field }) => (
                <FormItem>
                  <FormLabel>設備種別</FormLabel>
                  <FormControl>
                    <Input {...field} />
                  </FormControl>
                  <FormMessage />
                </FormItem>
              )}
            />
            <FormField
              control={form.control}
              name="additionalUnitCount"
              render={({ field }) => (
                <FormItem>
                  <FormLabel>導入台数 (台)</FormLabel>
                  <FormControl>
                    <Input type="number" min="0" step="1" {...field} />
                  </FormControl>
                  <FormMessage />
                </FormItem>
              )}
            />
            <FormField
              control={form.control}
              name="additionalDeviceUniqueNo"
              render={({ field }) => (
                <FormItem>
                  <FormLabel>機器固有番号</FormLabel>
                  <FormControl>
                    <Input {...field} />
                  </FormControl>
                  <FormMessage />
                </FormItem>
              )}
            />
            <FormField
              control={form.control}
              name="ictEquipmentPowerConsumption"
              render={({ field }) => (
                <FormItem>
                  <FormLabel>ICT設備消費電力 (W)</FormLabel>
                  <FormControl>
                    <Input type="number" min="0" step="any" {...field} />
                  </FormControl>
                  <FormMessage />
                </FormItem>
              )}
            />
          </FormGrid>
        )}
      </FormSection>

      <FormSection number={6} title="追加性評価に必要な情報">
        <FormField
          control={form.control}
          name="totalInvestment"
          render={({ field }) => (
            <FormItem>
              <FormLabel>設備の総投資額 (円)</FormLabel>
              <FormControl>
                <Input type="number" min="0" step="1" {...field} />
              </FormControl>
              <FormMessage />
            </FormItem>
          )}
        />
        <FormField
          control={form.control}
          name="totalInvestmentFile"
          render={({ field }) => (
            <FormItem>
              <FormLabel>
                確認資料：工事請負契約書、請求書、領収書等
              </FormLabel>
              <FormControl>
                <FileUploadControl
                  id="total-investment-file"
                  value={field.value}
                  onChange={field.onChange}
                />
              </FormControl>
              <FormMessage />
            </FormItem>
          )}
        />

        <FormField
          control={form.control}
          name="hasSubsidy"
          render={({ field }) => (
            <FormItem>
              <FormLabel>補助金受給の有無</FormLabel>
              <FormControl>
                <YesNoRadio
                  idPrefix="subsidy"
                  value={field.value}
                  onValueChange={field.onChange}
                />
              </FormControl>
              <FormMessage />
            </FormItem>
          )}
        />
        {hasSubsidy && (
          <div className="space-y-4">
            <FormGrid>
              <FormField
                control={form.control}
                name="subsidyName"
                render={({ field }) => (
                  <FormItem>
                    <FormLabel>補助金名称</FormLabel>
                    <FormControl>
                      <Input {...field} />
                    </FormControl>
                    <FormMessage />
                  </FormItem>
                )}
              />
              <FormField
                control={form.control}
                name="subsidyAmount"
                render={({ field }) => (
                  <FormItem>
                    <FormLabel>補助金額 (円)</FormLabel>
                    <FormControl>
                      <Input type="number" min="0" step="any" {...field} />
                    </FormControl>
                    <FormMessage />
                  </FormItem>
                )}
              />
            </FormGrid>
            <FormField
              control={form.control}
              name="subsidyNameFile"
              render={({ field }) => (
                <FormItem>
                  <FormLabel>確認資料：補助金交付決定通知書</FormLabel>
                  <FormControl>
                    <FileUploadControl
                      id="subsidy-name-file"
                      value={field.value}
                      onChange={field.onChange}
                    />
                  </FormControl>
                  <FormMessage />
                </FormItem>
              )}
            />
          </div>
        )}

        <FormField
          control={form.control}
          name="preProjectUnitPrice"
          render={({ field }) => (
            <FormItem>
              <FormLabel>
                プロジェクト実施前の電力購入単価 (円/kWh)
              </FormLabel>
              <FormControl>
                <Input type="number" min="0" step="any" {...field} />
              </FormControl>
              <FormMessage />
            </FormItem>
          )}
        />
        <FormField
          control={form.control}
          name="preProjectUnitPriceFile"
          render={({ field }) => (
            <FormItem>
              <FormLabel>確認資料：電力会社からの請求明細書等</FormLabel>
              <FormControl>
                <FileUploadControl
                  id="pre-project-unit-price-file"
                  value={field.value}
                  onChange={field.onChange}
                />
              </FormControl>
              <FormMessage />
            </FormItem>
          )}
        />
      </FormSection>
    </FormSectionStack>
  );
}
