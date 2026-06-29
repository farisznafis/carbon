import { useForm, type UseFormReturn } from 'react-hook-form';
import { Button } from '@/components/ui/button';
import { Card } from '@/components/ui/card';
import { Checkbox } from '@/components/ui/checkbox';
import {
  Form,
  FormControl,
  FormField,
  FormItem,
  FormLabel,
  FormMessage,
} from '@/components/ui/form';
import { RadioGroup, RadioGroupItem } from '@/components/ui/radio-group';
import { Textarea } from '@/components/ui/textarea';
import { useSignup } from '@/contexts/SignupContext';
import { validateSignupMethodology } from '@/api/company';
import { ApiError } from '@/api/_shared/errors';
import { useToast } from '@/hooks/use-toast';
import {
  activityHasOptions,
  defaultMethodologyValues,
  type MethodologyFormData,
  methodologyFormSchema,
  participationMethodologies,
} from './signupStep2Model';
import { RequiredLabel } from '@/components/forms/shared/RequiredLabel';
import { logger } from '@/lib/logger';

export const SignupStep2 = () => {
  const { signupData, updateSignupData, setCurrentStep } = useSignup();
  const { toast } = useToast();

  const form = useForm<MethodologyFormData>({
    mode: 'onChange',
    defaultValues: {
      methodology:
        signupData.methodology.length > 0
          ? signupData.methodology
          : defaultMethodologyValues.methodology,
      activityHas:
        signupData.activityHas || defaultMethodologyValues.activityHas,
      activityInformation:
        signupData.activityHas === 'no'
          ? ''
          : signupData.activityInformation === '-'
            ? ''
            : signupData.activityInformation ||
        defaultMethodologyValues.activityInformation,
    },
  });

  const activityHas = form.watch('activityHas');

  function toMessage(value: unknown): string {
    if (typeof value === 'string') {
      return value;
    }

    if (Array.isArray(value)) {
      return value.map(toMessage).filter(Boolean).join('\n');
    }

    if (value && typeof value === 'object' && 'message' in value) {
      return toMessage((value as { message?: unknown }).message);
    }

    return '';
  }

  function applyBackendValidationErrors(
    formInstance: UseFormReturn<MethodologyFormData>,
    error: unknown,
  ): string | null {
    if (!(error instanceof ApiError)) {
      return error instanceof Error ? error.message : null;
    }

    const errors = error.payload?.errors;
    let applied = false;

    if (errors && typeof errors === 'object' && !Array.isArray(errors)) {
      Object.entries(errors).forEach(([field, value]) => {
        const message = toMessage(value);

        if (!message || !(field in formInstance.getValues())) {
          return;
        }

        formInstance.setError(field as keyof MethodologyFormData, {
          type: 'server',
          message,
        });
        applied = true;
      });
    }

    if (applied) {
      return null;
    }

    return error.payload?.message || error.message;
  }

  const handleNext = async (data: MethodologyFormData) => {
    form.clearErrors();

    try {
      await validateSignupMethodology({
        methodology: data.methodology,
        activityInformation:
          data.activityHas === 'no' ? '-' : data.activityInformation.trim(),
      });

      updateSignupData({
        methodology: data.methodology,
        activityHas: data.activityHas,
        activityInformation:
          data.activityHas === 'no' ? '-' : data.activityInformation.trim(),
      });

      setCurrentStep(3);
    } catch (error) {
      const message = applyBackendValidationErrors(form, error);

      toast({
        title: '入力内容を確認してください',
        description: message || '入力内容に誤りがあります。',
        variant: 'destructive',
      });
      logger.error('Signup step 2 validation failed:', error);
    }
  };

  return (
    <Form {...form}>
      <form onSubmit={form.handleSubmit(handleNext)} className="space-y-6">
        <FormField
          control={form.control}
          name="methodology"
          render={({ field }) => (
            <FormItem>
              <div className="space-y-4">
                <RequiredLabel>参加したいプロジェクト</RequiredLabel>
                <Card className="overflow-hidden rounded-md">
                  {participationMethodologies.map((methodology) => {
                    const checked = field.value.includes(methodology.value);

                    return (
                      <label
                        key={methodology.value}
                        className="flex min-h-14 cursor-pointer items-center gap-3 border-b px-4 py-3 transition-colors hover:bg-muted/40 last:border-b-0"
                      >
                        <Checkbox
                          checked={checked}
                          onCheckedChange={(isChecked) => {
                            const nextValue = isChecked
                              ? [...field.value, methodology.value]
                              : field.value.filter(
                                  (value) => value !== methodology.value,
                                );

                            field.onChange(nextValue);
                          }}
                          aria-label={methodology.label}
                          className="h-4 w-4 shrink-0"
                        />
                        <span className="min-w-0 flex-1 text-sm font-medium leading-relaxed">
                          {methodology.label}
                        </span>
                      </label>
                    );
                  })}
                </Card>
                <FormMessage />
              </div>
            </FormItem>
          )}
        />

        <FormField
          control={form.control}
          name="activityHas"
          render={({ field }) => (
            <FormItem>
              <RequiredLabel>現在の活動情報</RequiredLabel>
              <FormControl>
                <RadioGroup
                  value={field.value}
                  onValueChange={(value) => {
                    field.onChange(value);
                    if (value === 'no' || form.getValues('activityInformation') === '-') {
                      form.setValue('activityInformation', '');
                    }
                  }}
                  className="flex gap-6"
                >
                  {activityHasOptions.map((option) => (
                    <div key={option.value} className="flex items-center gap-2">
                      <RadioGroupItem
                        value={option.value}
                        id={`activity-${option.value}`}
                      />
                      <FormLabel
                        htmlFor={`activity-${option.value}`}
                        className="text-sm font-normal cursor-pointer"
                      >
                        {option.label}
                      </FormLabel>
                    </div>
                  ))}
                </RadioGroup>
              </FormControl>
              <FormMessage />
            </FormItem>
          )}
        />

        {activityHas === 'yes' && (
          <FormField
            control={form.control}
            name="activityInformation"
            render={({ field }) => (
              <FormItem>
                <p className="text-sm text-muted-foreground">
                  {/* ※排出量削減活動の概要をご記入ください。 */}
                </p>
                <FormControl>
                  <Textarea
                    rows={4}
                    placeholder="例）工場内の老朽化した蛍光灯をLED照明へ順次更新中"
                    className="min-h-24 resize-y"
                    {...field}
                  />
                </FormControl>
                <FormMessage />
              </FormItem>
            )}
          />
        )}

        {activityHas === 'no' && (
          <div className="pointer-events-none">
            <Textarea
              rows={4}
              className="min-h-24 resize-y border-border bg-[#B1B5BB]/20 disabled:border-border disabled:bg-[#B1B5BB]/20 disabled:opacity-100"
              disabled
              value=""
            />
          </div>
        )}

        <div className="flex gap-4 pt-4">
          <Button
            type="button"
            variant="outline"
            onClick={() => setCurrentStep(1)}
            className="flex-1"
          >
            戻る
          </Button>
          <Button type="submit" className="flex-1">
            次へ
          </Button>
        </div>
      </form>
    </Form>
  );
};
