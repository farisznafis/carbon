import { useEffect, forwardRef, useImperativeHandle } from 'react';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import { Card, CardContent } from '@/components/ui/card';
import { Checkbox } from '@/components/ui/checkbox';
import { RadioGroup, RadioGroupItem } from '@/components/ui/radio-group';
import {
  Form,
  FormControl,
  FormField,
  FormItem,
  FormLabel,
  FormMessage,
} from '@/components/ui/form';
import { type AgreementFormValues } from './agreementsConfig';
import { agreementsSchema } from '@/lib/validations'; 
import { InteractiveBlocksInput } from '@/components/inputs/InteractiveBlocksInput';

type AgreementsFormValues = z.infer<typeof agreementsSchema>;

interface DynamicAgreementsStepProps {
  formValues: Partial<AgreementFormValues>;
  onValueChange: (values: Partial<AgreementFormValues>) => void;
  errors?: Record<string, string>;
}

export interface DynamicAgreementsStepRef {
  triggerValidation: () => Promise<boolean>;
}

export const DynamicAgreementsStep = forwardRef<DynamicAgreementsStepRef, DynamicAgreementsStepProps>(({
  formValues,
  onValueChange,
}, ref) => {
  const form = useForm<AgreementsFormValues>({
    // resolver: zodResolver(agreementsSchema),
    mode: "onChange",
    defaultValues: {
      useRegisteredInfo: false,
      provideAdditionalInfo: false,
      transferEnvironmentalValue: false,
      notRegisteredElsewhere: false,
      keidanrenParticipation: '',
      specifiedEmitterStatus: '',
      energyRationalizationCompliance: '',
      emissionAdditionAgreement: false,
      specifiedEmitterCode: '',
      energyLawType: '',
      energyLawBusinessNumber: '',
      energyLawFactoryNumber: '',
    },
    ...(formValues as Partial<AgreementsFormValues>),
  });

  const watchedValues = form.watch();
  const specifiedEmitterStatus = form.watch('specifiedEmitterStatus');
  const energyRationalizationCompliance = form.watch('energyRationalizationCompliance');

  // Expose validation method to parent
  useImperativeHandle(ref, () => ({
    triggerValidation: async () => {
      return await form.trigger();
    },
  }), [form]);

  // Reset form with saved values when component mounts or formValues changes
  useEffect(() => {
    form.reset(formValues as AgreementsFormValues);
  }, [form, formValues]);

  // Sync form values with parent component
  useEffect(() => {
    onValueChange(watchedValues);
  }, [watchedValues, onValueChange]);

  return (
    <div className="space-y-6">
      <div>
        <h2 className="text-2xl font-bold mb-2">同意事項</h2>
        <p className="text-muted-foreground">
          以下の項目をご確認の上、同意してください
        </p>
      </div>

      <Form {...form}>
        <div className="space-y-4">
          {/* Basic agreements */}
          <Card>
            <CardContent className="pt-6 space-y-4">
              <FormField
                control={form.control}
                name="useRegisteredInfo"
                render={({ field }) => (
                  <FormItem className="flex flex-row items-start space-x-3 space-y-0">
                    <FormControl>
                      <Checkbox checked={field.value} onCheckedChange={field.onChange} 
                      className='w-5 h-5 rounded-none border-gray-300'/>
                    </FormControl>
                    <div className="flex-1 leading-none">
                      <FormLabel className="text-sm font-semibold cursor-pointer leading-relaxed">
                        本入会届に記載された情報を使用することに同意します。
                        <span className="text-destructive ml-1">*</span>
                      </FormLabel>
                      <FormMessage />
                    </div>
                  </FormItem>
                )}
              />
            </CardContent>
          </Card>

          <Card>
            <CardContent className="pt-6 space-y-4">
              <FormField
                control={form.control}
                name="provideAdditionalInfo"
                render={({ field }) => (
                  <FormItem className="flex flex-row items-start space-x-3 space-y-0">
                    <FormControl>
                      <Checkbox checked={field.value} onCheckedChange={field.onChange} 
                      className='w-5 h-5 rounded-none border-gray-300'/>
                    </FormControl>
                    <div className="flex-1 leading-none">
                      <FormLabel className="text-sm font-semibold cursor-pointer leading-relaxed">
                        本入会届に記載された以外の情報を提供することに同意します。
                        <span className="text-destructive ml-1">*</span>
                      </FormLabel>
                      <FormMessage />
                    </div>
                  </FormItem>
                )}
              />
            </CardContent>
          </Card>

          <Card>
              <CardContent className="pt-6 space-y-4">
              <FormField
                control={form.control}
                name="transferEnvironmentalValue"
                render={({ field }) => (
                  <FormItem className="flex flex-row items-start space-x-3 space-y-0">
                    <FormControl>
                      <Checkbox checked={field.value} onCheckedChange={field.onChange} 
                      className='w-5 h-5 rounded-none border-gray-300'/>
                    </FormControl>
                    <div className="flex-1 leading-none">
                      <FormLabel className="text-sm font-semibold cursor-pointer leading-relaxed">
                        環境価値を譲渡し、削減主張できなくなることに同意します。
                        <span className="text-destructive ml-1">*</span>
                      </FormLabel>
                      <FormMessage />
                    </div>
                  </FormItem>
                )}
              />
            </CardContent>
          </Card>

          <Card>
            <CardContent className="pt-6 space-y-4">    
              <FormField
                control={form.control}
                name="notRegisteredElsewhere"
                render={({ field }) => (
                  <FormItem className="flex flex-row items-start space-x-3 space-y-0">
                    <FormControl>
                      <Checkbox checked={field.value} onCheckedChange={field.onChange} 
                      className='w-5 h-5 rounded-none border-gray-300'/>
                    </FormControl>
                    <div className="flex-1 leading-none">
                      <FormLabel className="text-sm font-semibold cursor-pointer leading-relaxed">
                        他制度・プロジェクトに登録していないことに同意します。
                        <span className="text-destructive ml-1">*</span>
                      </FormLabel>
                      <FormMessage />
                    </div>
                  </FormItem>
                )}
              />
            </CardContent>
          </Card>

          {/* Keidanren participation */}
          <Card>
            <CardContent className="pt-6">
              <FormField
                control={form.control}
                name="keidanrenParticipation"
                render={({ field }) => (
                  <FormItem className="space-y-3">
                    <FormLabel>
                      日本経済団体連合会における経団連カーボンニュートラル行動計画への参加については以下の通りです。
                      <span className="text-destructive ml-1">*</span>
                    </FormLabel>
                    <FormControl>
                      <RadioGroup value={field.value} onValueChange={field.onChange}>
                        <div className="flex items-center space-x-2">
                          <RadioGroupItem value="participating" id="participating" />
                          <FormLabel htmlFor="participating" className="text-sm font-normal cursor-pointer">
                            参加しています
                          </FormLabel>
                        </div>
                        <div className="flex items-center space-x-2">
                          <RadioGroupItem value="notParticipating" id="notParticipating" />
                          <FormLabel htmlFor="notParticipating" className="text-sm font-normal cursor-pointer">
                            参加していません
                          </FormLabel>
                        </div>
                      </RadioGroup>
                    </FormControl>
                    <FormMessage />
                  </FormItem>
                )}
              />
            </CardContent>
          </Card>

          {/* Specified emitter status */}
          <Card>
            <CardContent className="pt-6 space-y-4">
              <FormField
                control={form.control}
                name="specifiedEmitterStatus"
                render={({ field }) => (
                  <FormItem className="space-y-3">
                    <FormLabel>
                      地球温暖化対策の推進に関する法律に基づく算定・報告・公表制度における特定排出者の区分について同意します。
                      <span className="text-destructive ml-1">*</span>
                    </FormLabel>
                    <FormControl>
                      <RadioGroup value={field.value} onValueChange={field.onChange}>
                        <div className="flex items-center space-x-2">
                          <RadioGroupItem value="isSpecifiedEmitter" id="isSpecifiedEmitter" />
                          <FormLabel htmlFor="isSpecifiedEmitter" className="text-sm font-normal cursor-pointer">
                            特定排出者に該当します
                          </FormLabel>
                        </div>
                        <div className="flex items-center space-x-2">
                          <RadioGroupItem value="notSpecifiedEmitter" id="notSpecifiedEmitter" />
                          <FormLabel htmlFor="notSpecifiedEmitter" className="text-sm font-normal cursor-pointer">
                            特定排出者ではありません
                          </FormLabel>
                        </div>
                      </RadioGroup>
                    </FormControl>
                    <FormMessage />
                  </FormItem>
                )}
              />
              
              {/* Conditional fields when specified emitter */}
              {specifiedEmitterStatus === 'isSpecifiedEmitter' && (
                <div className="ml-6 pt-4 border-t space-y-4">
                  <FormField
                    control={form.control}
                    name="emissionAdditionAgreement"
                    render={({ field }) => (
                      <FormItem className="flex flex-row items-start space-x-3 space-y-0">
                        <FormControl>
                          <Checkbox checked={field.value} onCheckedChange={field.onChange} 
                          className='w-5 h-5 rounded-none border-gray-300'/>
                        </FormControl>
                        <div className="flex-1 leading-none">
                          <FormLabel className="text-sm font-normal cursor-pointer leading-relaxed">
                            特定排出者に該当する場合、プログラム型運営・管理者へ譲渡した環境価値 (Ｊ－クレジット) に相当する排出量を、当該年度の調整後排出量の計算において加算することに同意します。
                            <span className="text-destructive ml-1">*</span>
                          </FormLabel>
                          <FormMessage />
                        </div>
                      </FormItem>
                    )}
                  />
                  
                  <FormField
                    control={form.control}
                    name="specifiedEmitterCode"
                    render={({ field }) => (
                      <FormItem>
                        <FormLabel>
                          特定排出者コード (9ケタの値)
                          <span className="text-destructive ml-1">*</span>
                        </FormLabel>
                        <FormControl>
                          <InteractiveBlocksInput
                            value={field.value || ""}
                            onChange={field.onChange}
                            length={9}          // change this to 5, 8, etc.
                          />
                        </FormControl>
                        <p className="text-xs">
                          不明な場合はこちらから検索してください：
                          <a
                            href="https://policies.env.go.jp/earth/ghg-santeikohyo/search.html"
                            target="_blank"
                            rel="noopener noreferrer"
                            className="text-blue-600 hover:underline"
                          >
                            https://policies.env.go.jp/earth/ghg-santeikohyo/search.html
                          </a>
                        </p>
                        <FormMessage />
                      </FormItem>
                    )}
                  />
                </div>
              )}
            </CardContent>
          </Card>

          {/* Energy rationalization compliance */}
          <Card>
            <CardContent className="pt-6 space-y-4">
              <FormField
                control={form.control}
                name="energyRationalizationCompliance"
                render={({ field }) => (
                  <FormItem className="space-y-3">
                    <FormLabel>
                      エネルギーの使用の合理化に関する法律に基づく定期報告における
                      <span className="text-destructive ml-1">*</span>
                    </FormLabel>
                    <FormControl>
                      <RadioGroup value={field.value} onValueChange={field.onChange}>
                        <div className="flex items-center space-x-2">
                          <RadioGroupItem value="applicable" id="applicable" />
                          <FormLabel htmlFor="applicable" className="text-sm font-normal cursor-pointer">
                            対象者に該当します
                          </FormLabel>
                        </div>
                        <div className="flex items-center space-x-2">
                          <RadioGroupItem value="notApplicable" id="notApplicable" />
                          <FormLabel htmlFor="notApplicable" className="text-sm font-normal cursor-pointer">
                            対象者に該当しません
                          </FormLabel>
                        </div>
                      </RadioGroup>
                    </FormControl>
                    <FormMessage />
                  </FormItem>
                )}
              />
              
              {/* Conditional fields when energy law applies */}
              {energyRationalizationCompliance === 'applicable' && (
                <div className="ml-6 pt-4 border-t space-y-4">
                  <FormField
                    control={form.control}
                    name="energyLawType"
                    render={({ field }) => (
                      <FormItem className="space-y-3">
                        <FormLabel>
                          種別
                          <span className="text-destructive ml-1">*</span>
                        </FormLabel>
                        <FormControl>
                          <RadioGroup value={field.value} onValueChange={field.onChange}>
                            <div className="flex items-center space-x-2">
                              <RadioGroupItem value="type1" id="type1" />
                              <FormLabel htmlFor="type1" className="text-sm font-normal cursor-pointer">
                                一種
                              </FormLabel>
                            </div>
                            <div className="flex items-center space-x-2">
                              <RadioGroupItem value="type2" id="type2" />
                              <FormLabel htmlFor="type2" className="text-sm font-normal cursor-pointer">
                                二種
                              </FormLabel>
                            </div>
                          </RadioGroup>
                        </FormControl>
                        <FormMessage />
                      </FormItem>
                    )}
                  />
                  
                  <FormField
                    control={form.control}
                    name="energyLawBusinessNumber"
                    render={({ field }) => (
                      <FormItem>
                        <FormLabel>
                          省エネ法特定事業者番号又は特定連鎖化事業者番号 (7ケタの値)
                          <span className="text-destructive ml-1">*</span>
                        </FormLabel>
                        <FormControl>
                          <FormControl>
                            <InteractiveBlocksInput
                              value={field.value || ""}
                              onChange={field.onChange}
                              length={7}         
                            />
                        </FormControl>
                        </FormControl>
                        <FormMessage />
                      </FormItem>
                    )}
                  />
                  
                  <FormField
                    control={form.control}
                    name="energyLawFactoryNumber"
                    render={({ field }) => (
                      <FormItem>
                        <FormLabel>
                          省エネ法指定工場番号 (7ケタの値)
                          <span className="text-destructive ml-1">*</span>
                        </FormLabel>
                        <FormControl>
                          <FormControl>
                            <InteractiveBlocksInput
                              value={field.value || ""}
                              onChange={field.onChange}
                              length={7}          // change this to 5, 8, etc.
                            />
                        </FormControl>
                        </FormControl>
                        <FormMessage />
                      </FormItem>
                    )}
                  />
                </div>
              )}
            </CardContent>
          </Card>
        </div>
      </Form>
    </div>
  );
});

DynamicAgreementsStep.displayName = 'DynamicAgreementsStep';

// Validation function for the entire form - maintains compatibility with ProjectRegister
const validateAgreementsForm = (
  formValues: Partial<AgreementFormValues>
): { isValid: boolean; errors: Record<string, string> } => {
  try {
    agreementsSchema.parse(formValues);
    return { isValid: true, errors: {} };
  } catch (error) {
    if (error instanceof z.ZodError) {
      const errors: Record<string, string> = {};
      error.errors.forEach((err) => {
        if (err.path[0]) {
          errors[err.path[0] as string] = err.message;
        }
      });
      return { isValid: false, errors };
    }
    return { isValid: false, errors: {} };
  }
};
