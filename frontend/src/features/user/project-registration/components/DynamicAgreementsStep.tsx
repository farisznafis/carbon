import {
  forwardRef,
  type ReactNode,
  useEffect,
  useImperativeHandle,
  useRef,
} from 'react';
import { useForm } from 'react-hook-form';
import { REGEXP_ONLY_DIGITS } from 'input-otp';
import { Card, CardContent } from '@/components/ui/card';
import { Checkbox } from '@/components/ui/checkbox';
import {
  Form,
  FormControl,
  FormField,
  FormItem,
  FormLabel,
  FormMessage,
} from '@/components/ui/form';
import {
  InputOTP,
  InputOTPGroup,
  InputOTPSlot,
} from '@/components/ui/input-otp';
import { RadioGroup, RadioGroupItem } from '@/components/ui/radio-group';
import {
  agreementsConfig,
  type AgreementFormValues,
} from './agreementsConfig';

type AgreementsFormValues = AgreementFormValues;

const defaultAgreementValues: AgreementFormValues = {
  useRegisteredInfo: null,
  provideAdditionalInfo: null,
  transferEnvironmentalValue: null,
  notRegisteredElsewhere: null,
  keidanrenParticipation: '',
  specifiedEmitterStatus: '',
  energyRationalizationCompliance: '',
  emissionAdditionAgreement: null,
  specifiedEmitterCode: '',
  energyLawType: '',
  energyLawBusinessNumber: '',
  energyLawFactoryNumber: '',
};

const agreementLabels = Object.fromEntries(
  agreementsConfig.map((field) => [field.id, field.label]),
) as Record<keyof AgreementFormValues, string>;

const agreementOptions = Object.fromEntries(
  agreementsConfig.map((field) => [field.id, field.options ?? []]),
) as Record<
  keyof AgreementFormValues,
  Array<{ value: string; label: string }>
>;

type NumericCodeInputProps = {
  value: string;
  onChange: (value: string) => void;
  length: number;
  label: string;
};

function NumericCodeInput({
  value,
  onChange,
  length,
  label,
}: NumericCodeInputProps) {
  const separatorIndex = 3;

  return (
    <InputOTP
      value={value}
      onChange={onChange}
      maxLength={length}
      pattern={REGEXP_ONLY_DIGITS}
      aria-label={label}
      containerClassName="flex-wrap gap-2"
    >
      <InputOTPGroup>
        {Array.from({ length: separatorIndex }, (_, index) => (
          <InputOTPSlot
            key={index}
            index={index}
            className="h-9 w-8 sm:w-9"
          />
        ))}
      </InputOTPGroup>
      <span aria-hidden="true" className="text-muted-foreground">
        -
      </span>
      <InputOTPGroup>
        {Array.from(
          { length: length - separatorIndex },
          (_, index) => index + separatorIndex,
        ).map((index) => (
          <InputOTPSlot
            key={index}
            index={index}
            className="h-9 w-8 sm:w-9"
          />
        ))}
      </InputOTPGroup>
    </InputOTP>
  );
}

function RequiredFieldLabel({
  children,
  htmlFor,
}: {
  children: ReactNode;
  htmlFor?: string;
}) {
  return (
    <FormLabel
      htmlFor={htmlFor}
      className="flex cursor-pointer items-start gap-1 text-small font-medium leading-5 text-foreground"
    >
      <span className="min-w-0">{children}</span>
      <span aria-hidden="true" className="shrink-0 text-destructive">
        *
      </span>
      <span className="sr-only">必須</span>
    </FormLabel>
  );
}

function AgreementCard({ children }: { children: ReactNode }) {
  return (
    <Card className="rounded-xl border-border shadow-sm">
      <CardContent className="p-5 sm:p-6">{children}</CardContent>
    </Card>
  );
}

function RadioChoice({
  id,
  value,
  label,
}: {
  id: string;
  value: string;
  label: string;
}) {
  return (
    <div className="flex items-center gap-2">
      <RadioGroupItem id={id} value={value} className="size-4" />
      <FormLabel
        htmlFor={id}
        className="cursor-pointer text-body text-foreground"
      >
        {label}
      </FormLabel>
    </div>
  );
}

type DynamicAgreementsStepProps = {
  formValues: Partial<AgreementFormValues>;
  onValueChange: (values: Partial<AgreementFormValues>) => void;
  errors?: Record<string, string>;
};

export type DynamicAgreementsStepRef = {
  getValues: () => AgreementFormValues;
};

export const DynamicAgreementsStep = forwardRef<
  DynamicAgreementsStepRef,
  DynamicAgreementsStepProps
>(({ formValues, onValueChange, errors = {} }, ref) => {
  const form = useForm<AgreementsFormValues>({
    mode: 'onChange',
    defaultValues: {
      ...defaultAgreementValues,
      ...formValues,
    },
  });

  const watchedValues = form.watch();
  const previousValuesRef = useRef(watchedValues);
  const serverErrorFieldsRef = useRef<Array<keyof AgreementsFormValues>>([]);
  const specifiedEmitterStatus = form.watch('specifiedEmitterStatus');
  const energyRationalizationCompliance = form.watch(
    'energyRationalizationCompliance',
  );

  useImperativeHandle(
    ref,
    () => ({
      getValues: () => form.getValues(),
    }),
    [form],
  );

  useEffect(() => {
    form.reset({
      ...defaultAgreementValues,
      ...formValues,
    });
  }, [form, formValues]);

  useEffect(() => {
    serverErrorFieldsRef.current.forEach((fieldName) => {
      form.clearErrors(fieldName);
    });

    const serverErrorFields = Object.entries(errors).flatMap(
      ([fieldName, message]) => {
        if (!(fieldName in form.getValues()) || !message) return [];

        const agreementFieldName = fieldName as keyof AgreementsFormValues;
        form.setError(agreementFieldName, {
          type: 'server',
          message,
        });
        return [agreementFieldName];
      },
    );
    serverErrorFieldsRef.current = serverErrorFields;
  }, [errors, form]);

  useEffect(() => {
    if (
      JSON.stringify(previousValuesRef.current) !==
      JSON.stringify(watchedValues)
    ) {
      previousValuesRef.current = watchedValues;
      onValueChange(watchedValues);
    }
  }, [onValueChange, watchedValues]);

  const checkboxAgreements = [
    'useRegisteredInfo',
    'provideAdditionalInfo',
    'transferEnvironmentalValue',
    'notRegisteredElsewhere',
  ] as const;

  return (
    <div className="space-y-4">
      <p className="text-body text-muted-foreground">
        以下の各項目をご確認のうえ、該当する内容にご同意いただける場合はチェックを入れてください。すべての必須項目に同意いただいた場合のみ、次のステップへ進むことができます。
      </p>

      <Form {...form}>
        <Card className="rounded-xl border-border shadow-sm">
          <CardContent className="space-y-4 p-4 sm:space-y-6 sm:p-6">
            {checkboxAgreements.map((fieldName) => (
              <AgreementCard key={fieldName}>
                <FormField
                  control={form.control}
                  name={fieldName}
                  render={({ field }) => (
                    <FormItem className="flex items-start gap-3 space-y-0">
                      <FormControl>
                        <Checkbox
                          id={fieldName}
                          checked={field.value === true}
                          onCheckedChange={(checked) =>
                            field.onChange(checked === true ? true : null)
                          }
                          className="mt-0.5 size-4 shrink-0 rounded border-input"
                        />
                      </FormControl>
                      <div className="min-w-0 flex-1 space-y-1">
                        <RequiredFieldLabel htmlFor={fieldName}>
                          {agreementLabels[fieldName]}
                        </RequiredFieldLabel>
                        <FormMessage />
                      </div>
                    </FormItem>
                  )}
                />
              </AgreementCard>
            ))}

            <AgreementCard>
              <FormField
                control={form.control}
                name="keidanrenParticipation"
                render={({ field }) => (
                  <FormItem className="space-y-3">
                    <RequiredFieldLabel>
                      {agreementLabels.keidanrenParticipation}
                    </RequiredFieldLabel>
                    <FormControl>
                      <RadioGroup
                        value={field.value}
                        onValueChange={field.onChange}
                        className="gap-2"
                      >
                        {agreementOptions.keidanrenParticipation.map(
                          (option) => (
                            <RadioChoice
                              key={option.value}
                              id={`keidanren-${option.value}`}
                              value={option.value}
                              label={option.label}
                            />
                          ),
                        )}
                      </RadioGroup>
                    </FormControl>
                    <FormMessage />
                  </FormItem>
                )}
              />
            </AgreementCard>

            <AgreementCard>
              <FormField
                control={form.control}
                name="specifiedEmitterStatus"
                render={({ field }) => (
                  <FormItem className="space-y-3">
                    <RequiredFieldLabel>
                      {agreementLabels.specifiedEmitterStatus}
                    </RequiredFieldLabel>
                    <FormControl>
                      <RadioGroup
                        value={field.value}
                        onValueChange={field.onChange}
                        className="gap-3"
                      >
                        <RadioChoice
                          id="specified-emitter"
                          value="isSpecifiedEmitter"
                          label="特定排出者に該当します"
                        />

                        {specifiedEmitterStatus === 'isSpecifiedEmitter' && (
                          <div className="ml-6 space-y-5 border-l border-border pl-5">
                            <FormField
                              control={form.control}
                              name="specifiedEmitterCode"
                              render={({ field: codeField }) => (
                                <FormItem className="space-y-2">
                                  <RequiredFieldLabel>
                                    {agreementLabels.specifiedEmitterCode}
                                  </RequiredFieldLabel>
                                  <FormControl>
                                    <NumericCodeInput
                                      value={codeField.value || ''}
                                      onChange={codeField.onChange}
                                      length={9}
                                      label="特定排出者コード"
                                    />
                                  </FormControl>
                                  <p className="text-small text-muted-foreground">
                                    不明な場合はこちらから検索してください：
                                    <a
                                      href="https://policies.env.go.jp/earth/ghg-santeikohyo/search.html"
                                      target="_blank"
                                      rel="noopener noreferrer"
                                      className="break-all text-primary hover:underline"
                                    >
                                      https://policies.env.go.jp/earth/ghg-santeikohyo/search.html
                                    </a>
                                  </p>
                                  <FormMessage />
                                </FormItem>
                              )}
                            />

                            <FormField
                              control={form.control}
                              name="emissionAdditionAgreement"
                              render={({ field: agreementField }) => (
                                <FormItem className="flex items-start gap-3 space-y-0">
                                  <FormControl>
                                    <Checkbox
                                      id="emissionAdditionAgreement"
                                      checked={agreementField.value === true}
                                      onCheckedChange={(checked) =>
                                        agreementField.onChange(
                                          checked === true,
                                        )
                                      }
                                      className="mt-0.5 size-4 shrink-0 rounded border-input"
                                    />
                                  </FormControl>
                                  <div className="min-w-0 flex-1 space-y-1">
                                    <RequiredFieldLabel htmlFor="emissionAdditionAgreement">
                                      {
                                        agreementLabels.emissionAdditionAgreement
                                      }
                                    </RequiredFieldLabel>
                                    <FormMessage />
                                  </div>
                                </FormItem>
                              )}
                            />
                          </div>
                        )}

                        <RadioChoice
                          id="not-specified-emitter"
                          value="notSpecifiedEmitter"
                          label="特定排出者ではありません"
                        />
                      </RadioGroup>
                    </FormControl>
                    <FormMessage />
                  </FormItem>
                )}
              />
            </AgreementCard>

            <AgreementCard>
              <FormField
                control={form.control}
                name="energyRationalizationCompliance"
                render={({ field }) => (
                  <FormItem className="space-y-3">
                    <RequiredFieldLabel>
                      {agreementLabels.energyRationalizationCompliance}
                    </RequiredFieldLabel>
                    <FormControl>
                      <RadioGroup
                        value={field.value}
                        onValueChange={field.onChange}
                        className="gap-3"
                      >
                        <RadioChoice
                          id="energy-applicable"
                          value="applicable"
                          label="対象者に該当します"
                        />

                        {energyRationalizationCompliance === 'applicable' && (
                          <div className="ml-6 space-y-5 border-l border-border pl-5">
                            <FormField
                              control={form.control}
                              name="energyLawType"
                              render={({ field: typeField }) => (
                                <FormItem className="space-y-2">
                                  <RequiredFieldLabel>
                                    {agreementLabels.energyLawType}
                                  </RequiredFieldLabel>
                                  <FormControl>
                                    <RadioGroup
                                      value={typeField.value}
                                      onValueChange={typeField.onChange}
                                      className="gap-2"
                                    >
                                      {agreementOptions.energyLawType.map(
                                        (option) => (
                                          <RadioChoice
                                            key={option.value}
                                            id={`energy-law-${option.value}`}
                                            value={option.value}
                                            label={option.label}
                                          />
                                        ),
                                      )}
                                    </RadioGroup>
                                  </FormControl>
                                  <FormMessage />
                                </FormItem>
                              )}
                            />

                            <FormField
                              control={form.control}
                              name="energyLawBusinessNumber"
                              render={({ field: numberField }) => (
                                <FormItem className="space-y-2">
                                  <RequiredFieldLabel>
                                    {agreementLabels.energyLawBusinessNumber}
                                  </RequiredFieldLabel>
                                  <FormControl>
                                    <NumericCodeInput
                                      value={numberField.value || ''}
                                      onChange={numberField.onChange}
                                      length={7}
                                      label="省エネ法特定事業者番号"
                                    />
                                  </FormControl>
                                  <FormMessage />
                                </FormItem>
                              )}
                            />

                            <FormField
                              control={form.control}
                              name="energyLawFactoryNumber"
                              render={({ field: factoryField }) => (
                                <FormItem className="space-y-2">
                                  <RequiredFieldLabel>
                                    {agreementLabels.energyLawFactoryNumber}
                                  </RequiredFieldLabel>
                                  <FormControl>
                                    <NumericCodeInput
                                      value={factoryField.value || ''}
                                      onChange={factoryField.onChange}
                                      length={7}
                                      label="省エネ法指定工場番号"
                                    />
                                  </FormControl>
                                  <FormMessage />
                                </FormItem>
                              )}
                            />

                            <p className="text-small text-muted-foreground">
                              不明な場合はこちらから検索してください：
                              <a
                                href="https://www.enecho.meti.go.jp/category/saving_and_new/saving/004/"
                                target="_blank"
                                rel="noopener noreferrer"
                                className="break-all text-primary hover:underline"
                              >
                                https://www.enecho.meti.go.jp/category/saving_and_new/saving/004/
                              </a>
                            </p>
                          </div>
                        )}

                        <RadioChoice
                          id="energy-not-applicable"
                          value="notApplicable"
                          label="対象者に該当しません"
                        />
                      </RadioGroup>
                    </FormControl>
                    <FormMessage />
                  </FormItem>
                )}
              />
            </AgreementCard>
          </CardContent>
        </Card>
      </Form>
    </div>
  );
});

DynamicAgreementsStep.displayName = 'DynamicAgreementsStep';
