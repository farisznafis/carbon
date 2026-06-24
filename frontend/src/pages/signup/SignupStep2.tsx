import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
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
import { Textarea } from '@/components/ui/textarea';
import { useSignup } from '@/contexts/SignupContext';
import {
  defaultMethodologyValues,
  MethodologyFormData,
  methodologyFormSchema,
  participationMethodologies,
} from './signupStep2Model';
import { RequiredLabel } from '@/components/forms/shared/RequiredLabel';

export const SignupStep2 = () => {
  const { signupData, updateSignupData, setCurrentStep } = useSignup();

  const form = useForm<MethodologyFormData>({
    // resolver: zodResolver(methodologyFormSchema),
    mode: 'onChange',
    defaultValues: {
      methodology:
        signupData.methodology.length > 0
          ? signupData.methodology
          : defaultMethodologyValues.methodology,
      activityInformation:
        signupData.activityInformation ||
        defaultMethodologyValues.activityInformation,
    },
  });

  const handleNext = (data: MethodologyFormData) => {
    const activityInformation = data.activityInformation.trim();

    updateSignupData({
      methodology: data.methodology,
      activityInformation,
    });

    setCurrentStep(3);
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
          name="activityInformation"
          render={({ field }) => (
            <FormItem>
              <FormLabel>
                現在の活動情報
                <span className="text-red-500 text-xs ml-1"></span>
              </FormLabel>
              <p className="text-sm text-muted-foreground">
                ※排出量削減活動の概要をご記入ください。
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
