import type { UseFormReturn } from 'react-hook-form';
import type { Site } from '@/api/site';
import { RequiredLabel } from '@/components/forms/shared';
import { FileSelectButton } from '@/components/inputs/FileSelectButton/FileSelectButton';
import {
  FormGrid,
  FormSection,
  FormSectionStack,
} from '@/components/forms/form-layout';
import { Input } from '@/components/ui/input';
import {
  FormControl,
  FormField,
  FormItem,
  FormMessage,
} from '@/components/ui/form';
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '@/components/ui/select';
import type { SolarPowerFormData } from '@/lib/equipmentValidations';

const EQUIPMENT_FILE_FORMATS = '(.pdf, .csv, .jpg, .jpeg, .png)';

type SolarPowerEquipmentFormProps = Readonly<{
  form: UseFormReturn<SolarPowerFormData>;
  sites: Site[];
  isSiteOptionsLoading: boolean;
  siteOptionsError: string | null;
}>;

type FileUploadControlProps = Readonly<{
  id: string;
  label: string;
  value?: File;
  onChange: (file?: File) => void;
}>;

function FileUploadControl({
  id,
  label,
  value,
  onChange,
}: FileUploadControlProps) {
  return (
    <FileSelectButton
      id={id}
      label={label}
      variant="design-system-pro"
      supportedFormats={EQUIPMENT_FILE_FORMATS}
      required
      value={value ?? null}
      onChange={(file) => onChange(file ?? undefined)}
    />
  );
}

type YesNoSelectProps = Readonly<{
  value: '' | 'yes' | 'no';
  onValueChange: (value: 'yes' | 'no') => void;
}>;

function YesNoSelect({ value, onValueChange }: YesNoSelectProps) {
  return (
    <Select
      value={value}
      onValueChange={(nextValue) =>
        onValueChange(nextValue as 'yes' | 'no')
      }
    >
      <SelectTrigger>
        <SelectValue placeholder="選択してください" />
      </SelectTrigger>
      <SelectContent>
        <SelectItem value="yes">あり</SelectItem>
        <SelectItem value="no">なし</SelectItem>
      </SelectContent>
    </Select>
  );
}

export function SolarPowerEquipmentForm({
  form,
  sites,
  isSiteOptionsLoading,
  siteOptionsError,
}: SolarPowerEquipmentFormProps) {
  const hasBattery = form.watch('hasBattery') === 'yes';
  const hasSubsidy = form.watch('hasSubsidy') === 'yes';

  return (
    <FormSectionStack spacing="relaxed">
      <FormField
        control={form.control}
        name="siteId"
        render={({ field }) => (
          <FormItem>
            <RequiredLabel>対象拠点</RequiredLabel>
            <Select
              value={field.value}
              onValueChange={field.onChange}
              disabled={
                isSiteOptionsLoading ||
                Boolean(siteOptionsError) ||
                sites.length === 0
              }
            >
              <FormControl>
                <SelectTrigger>
                  <SelectValue placeholder="拠点を選択してください" />
                </SelectTrigger>
              </FormControl>
              <SelectContent>
                {sites.map((site) => (
                  <SelectItem key={site.siteId} value={site.siteId}>
                    {site.siteName}
                  </SelectItem>
                ))}
              </SelectContent>
            </Select>
            {siteOptionsError && (
              <p className="text-sm text-destructive">{siteOptionsError}</p>
            )}
            <FormMessage />
          </FormItem>
        )}
      />

      <FormSection number={1} title="太陽光発電設備の情報">
        <FormField
          control={form.control}
          name="equipmentOpDate"
          render={({ field }) => (
            <FormItem>
              <RequiredLabel>稼働開始日（電力受給開始日）</RequiredLabel>
              <FormControl>
                <Input type="date" {...field} />
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
              <FormControl>
                <FileUploadControl
                  id="equipment-op-date-file"
                  label="確認資料：太陽光発電電力受給契約確認書"
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
              <RequiredLabel>公称最大出力 (kW)</RequiredLabel>
              <FormControl>
                <Input
                  type="number"
                  min="0"
                  step="any"
                  placeholder="数値を入力"
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
              <FormControl>
                <FileUploadControl
                  id="nominal-max-output-file"
                  label="確認資料：仕様書・カタログ"
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
        <FormGrid columns={1}>
          <FormField
            control={form.control}
            name="pcsManufacturer"
            render={({ field }) => (
              <FormItem>
                <RequiredLabel>メーカー</RequiredLabel>
                <FormControl>
                  <Input placeholder="メーカー名を入力" {...field} />
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
                <RequiredLabel>型式</RequiredLabel>
                <FormControl>
                  <Input placeholder="型式を入力" {...field} />
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
                <RequiredLabel>導入台数 (台)</RequiredLabel>
                <FormControl>
                  <Input
                    type="number"
                    min="0"
                    step="1"
                    placeholder="数値を入力"
                    {...field}
                  />
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
                <RequiredLabel>機器固有番号（製造番号）</RequiredLabel>
                <FormControl>
                  <Input placeholder="機器固有番号を入力" {...field} />
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
                <RequiredLabel>
                  夜間待機電力（1台あたり） (W)
                </RequiredLabel>
                <FormControl>
                  <Input
                    type="number"
                    min="0"
                    step="any"
                    placeholder="数値を入力"
                    {...field}
                  />
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
              <FormControl>
                <FileUploadControl
                  id="pcs-file"
                  label="確認資料：仕様書・カタログ"
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
              <RequiredLabel>蓄電池設置の有無</RequiredLabel>
              <FormControl>
                <YesNoSelect
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
            <FormGrid columns={1}>
              <FormField
                control={form.control}
                name="batteryMaker"
                render={({ field }) => (
                  <FormItem>
                    <RequiredLabel>メーカー</RequiredLabel>
                    <FormControl>
                      <Input placeholder="メーカー名を入力" {...field} />
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
                    <RequiredLabel>型式</RequiredLabel>
                    <FormControl>
                      <Input placeholder="型式を入力" {...field} />
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
                    <RequiredLabel>容量 (kWh)</RequiredLabel>
                    <FormControl>
                      <Input
                        type="number"
                        min="0"
                        step="any"
                        placeholder="数値を入力"
                        {...field}
                      />
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
                  <FormControl>
                    <FileUploadControl
                      id="battery-file"
                      label="確認資料：仕様書・カタログ"
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

      <FormSection number={5} title="追加性評価に必要な情報">
        <FormField
          control={form.control}
          name="totalInvestment"
          render={({ field }) => (
            <FormItem>
              <RequiredLabel>設備の総投資額 (円)</RequiredLabel>
              <FormControl>
                <Input
                  type="number"
                  min="0"
                  step="1"
                  placeholder="数値を入力"
                  {...field}
                />
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
              <FormControl>
                <FileUploadControl
                  id="total-investment-file"
                  label="確認資料：工事請負契約書、請求書、領収書等"
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
              <RequiredLabel>補助金受給の有無</RequiredLabel>
              <FormControl>
                <YesNoSelect
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
            <FormGrid columns={1}>
              <FormField
                control={form.control}
                name="subsidyName"
                render={({ field }) => (
                  <FormItem>
                    <RequiredLabel>補助金名称</RequiredLabel>
                    <FormControl>
                      <Input placeholder="補助金名称を入力" {...field} />
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
                    <RequiredLabel>補助金額 (円)</RequiredLabel>
                    <FormControl>
                      <Input
                        type="number"
                        min="0"
                        step="any"
                        placeholder="数値を入力"
                        {...field}
                      />
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
                  <FormControl>
                    <FileUploadControl
                      id="subsidy-name-file"
                      label="確認資料：補助金交付決定通知書"
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
              <RequiredLabel>
                プロジェクト実施前の電力購入単価 (円/kWh)
              </RequiredLabel>
              <FormControl>
                <Input
                  type="number"
                  min="0"
                  step="any"
                  placeholder="数値を入力"
                  {...field}
                />
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
              <FormControl>
                <FileUploadControl
                  id="pre-project-unit-price-file"
                  label="確認資料：電力会社からの請求明細書等"
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
