import type { ReactNode } from 'react';
import { FormProvider, type UseFormReturn } from 'react-hook-form';
import type { FieldConfig } from '@/components/forms/shared/GenericFormFields/GenericFormFields';
import {
  FormActions,
  FormCard,
  FormPageContent,
  FormPageTitle,
  FormSection,
  FormSectionStack,
  FormStandalonePage,
} from '@/components/forms/form-layout';
import { DynamicFormRenderer } from './DynamicFormRenderer';
import {
  emissionFields,
  methodologyFields,
  operatorInfo,
  picFields,
  projectFields,
  representativeFields,
} from './projectBaseFields';
import type { AdminProjectRegisterFormInputValues } from './projectFormTypes';

type ProjectRecordValues = AdminProjectRegisterFormInputValues;
type ProjectFields = FieldConfig<ProjectRecordValues>[];

/** The project record, grouped the way the operator thinks about it. */
const projectRecordSections: ReadonlyArray<{ title: string; fields: ProjectFields }> = [
  { title: '基本情報', fields: [...projectFields, ...operatorInfo] as ProjectFields },
  { title: '方法論', fields: methodologyFields as ProjectFields },
  {
    title: '代表者・責任者',
    fields: [...representativeFields, ...picFields] as ProjectFields,
  },
  { title: '排出量', fields: emissionFields as ProjectFields },
];

/**
 * The page around the project record: title, and whatever sits above the card.
 *
 * プロジェクト作成 and プロジェクト更新 are the same record, so they are the same
 * page; only the title and the button differ. Keep it that way — the two had
 * drifted into different sections, field sets and layouts once already.
 */
export function ProjectRecordPage({
  title,
  toolbar,
  children,
}: Readonly<{ title: string; toolbar?: ReactNode; children: ReactNode }>) {
  return (
    <FormStandalonePage>
      <FormPageContent>
        <FormPageTitle align='center'>{title}</FormPageTitle>
        {toolbar ? <div className='flex justify-end'>{toolbar}</div> : null}
        {children}
      </FormPageContent>
    </FormStandalonePage>
  );
}

/**
 * The project record's numbered sections, the 計画書 note, and one button.
 *
 * `lockedFields` are shown but cannot be changed — on 更新, the ones the update
 * API does not take (番号, 運営会社) or that have their own action (ステータス).
 */
export function ProjectRecordForm({
  form,
  lockedFields = [],
  planFileNote,
  submitLabel,
  submittingLabel,
  submitting,
  submitError,
  onSubmit,
}: Readonly<{
  form: UseFormReturn<ProjectRecordValues>;
  lockedFields?: readonly string[];
  planFileNote: string;
  submitLabel: string;
  submittingLabel: string;
  submitting: boolean;
  submitError: string | null;
  onSubmit: () => void;
}>) {
  const lock = (fields: ProjectFields): ProjectFields =>
    fields.map((field) =>
      lockedFields.includes(field.name as string) ? { ...field, disabled: true } : field,
    );

  return (
    <FormCard>
      <FormSectionStack spacing='relaxed'>
        <FormProvider {...form}>
          {projectRecordSections.map((section, index) => (
            <FormSection key={section.title} number={index + 1} title={section.title}>
              <DynamicFormRenderer<ProjectRecordValues> fields={lock(section.fields)} />
            </FormSection>
          ))}
        </FormProvider>

        <FormSection number={projectRecordSections.length + 1} title='計画書ファイル'>
          <p className='rounded-md border border-dashed border-border px-4 py-6 text-center text-sm text-muted-foreground'>
            {planFileNote}
          </p>
        </FormSection>

        {submitError ? (
          <p role='alert' className='text-sm text-destructive-strong'>
            {submitError}
          </p>
        ) : null}

        <FormActions
          disabled={submitting}
          pendingActionId={submitting ? 'submit' : null}
          actions={[
            {
              id: 'submit',
              label: submitLabel,
              pendingLabel: submittingLabel,
              onClick: onSubmit,
            },
          ]}
        />
      </FormSectionStack>
    </FormCard>
  );
}
