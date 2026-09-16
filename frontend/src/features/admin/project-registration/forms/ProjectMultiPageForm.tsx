"use client";
// Multi-step form component for admin project forms
import type { FieldValues, UseFormReturn } from "react-hook-form";
import type { FieldConfig } from "@/components/forms/shared/GenericFormFields/GenericFormFields";
import { FormProvider } from "react-hook-form";
import { DynamicFormRenderer } from "./DynamicFormRenderer";
import { PlanFileUploadProvider, type PlanFileUploader } from "./PlanFileUploadContext";
import { steps } from "./projectSteps";
import { logger } from "@/lib/logger";
import { FormSection } from '@/components/forms/form-layout';

/** Separates a table's name from a column inside its rows: `years[].startDate`. */
const ROW_MARKER = '[]';

export type MultiStepFormProps<TFormValues extends FieldValues> = {
  form: UseFormReturn<TFormValues>;
  step: number; // 1-based index
  stepsOverride?: ReadonlyArray<typeof steps[number]>;
  /**
   * Backend field names filled from SQL. Those inputs are disabled: the server
   * overwrites them on every read, so an edit here would be lost without the
   * operator noticing.
   *
   * A column of a repeating table is named row-relative — `years[].yearStartDate`
   * — because the rule holds for every row.
   */
  readOnlyFields?: ReadonlyArray<string>;
  /**
   * 有/無 rules from the backend. A dependent field only applies while its
   * toggle holds the given value, so it is hidden otherwise — the server
   * enforces the same rule via @RequiredIf. Row-relative names apply to every
   * row of their table.
   */
  conditionalFields?: ReadonlyArray<{
    field: string;
    when: string;
    is: boolean;
  }>;
  /**
   * Uploads a file for the current page. Absent means file inputs fall back to
   * holding the File itself, which no plan page can save.
   */
  onUploadFile?: PlanFileUploader;
};

export function MultiStepForm<TFormValues extends FieldValues>({
  form,
  step,
  stepsOverride,
  readOnlyFields,
  conditionalFields,
  onUploadFile,
}: Readonly<MultiStepFormProps<TFormValues>>) {
  const stepList = stepsOverride ?? steps;
  // Config names are namespaced ("projectOverview.corpNumber"); the backend
  // reports the bare field name.
  const lockedNames = new Set(readOnlyFields ?? []);
  const rulesByField = new Map(
    (conditionalFields ?? []).map((rule) => [rule.field, rule]),
  );

  /**
   * Config names are namespaced ("projectApplication.emitterCode") while the
   * backend reports bare names, so the toggle is looked up under the same
   * prefix as the field it governs.
   */
  /** The toggle governing a field, read from under the same namespace. */
  const toggleFor = (fieldName: string) => {
    const bare = fieldName.split('.').pop() ?? '';
    const rule = rulesByField.get(bare);
    if (!rule) return null;

    const prefix = fieldName.slice(0, fieldName.length - bare.length);
    return { rule, value: form.getValues(`${prefix}${rule.when}` as never) };
  };

  const applies = (fieldName: string) => {
    const toggle = toggleFor(fieldName);
    if (!toggle) return true;

    // An unanswered toggle must not hide the field. It starts undefined, and
    // hiding on that would leave the operator unable to reach a field the
    // server still asks for once they answer.
    if (typeof toggle.value !== 'boolean') return true;
    return toggle.value === toggle.rule.is;
  };

  /**
   * Whether the server would actually demand this field right now.
   *
   * A field under a 有/無 rule is required only while its toggle holds the
   * value the rule keys on. Shown with the toggle still unanswered, it carries
   * no requirement yet — and marking it with the required badge sends the
   * operator hunting for something the server is not asking for.
   */
  const isRequiredNow = (fieldName: string) => {
    const toggle = toggleFor(fieldName);
    return toggle ? toggle.value === toggle.rule.is : true;
  };

  /**
   * The same two rules, applied down a table's columns.
   *
   * A column's rules are keyed row-relative, so they cannot be resolved here —
   * whether a row's 詳細 is required depends on that row's own toggle. What
   * this can do is translate the rule into something the table renderer
   * evaluates per row:
   *
   * - visibility becomes `hideWhen` on the opposite value, which matches the
   *   page-level behaviour of leaving a field visible while its toggle is
   *   still unanswered;
   * - the required badge is dropped, because it sits in the header and would
   *   otherwise claim every row needs a column that only some rows do.
   */
  const applyToColumns = (
    field: FieldConfig<TFormValues>,
    bareTableName: string,
  ): FieldConfig<TFormValues> => {
    const columns = field.columns;
    if (!columns) return field;

    return {
      ...field,
      columns: columns.map((column) => {
        const key = `${bareTableName}${ROW_MARKER}.${column.name}`;
        let next = column;

        if (lockedNames.has(key)) {
          next = { ...next, disabled: true };
        }

        const rule = rulesByField.get(key);
        if (rule) {
          const toggle = rule.when.split(ROW_MARKER + '.').pop() ?? rule.when;
          next = {
            ...next,
            required: false,
            hideWhen: { field: toggle, value: !rule.is },
          };
        }

        return next;
      }),
    };
  };

  const current = stepList.find((s) => s.id === step);

  if (!current) {
    logger.error(`Step with ID ${step} not found`, {
      availableStepIds: stepList.map((s) => s.id),
    });
    return null;
  }

  return (
    <FormSection number={step} title={current.title}>
      <FormProvider {...form}>
        <PlanFileUploadProvider uploader={onUploadFile ?? null}>
        <DynamicFormRenderer<TFormValues>
          fields={
            (lockedNames.size === 0 && rulesByField.size === 0
              ? current.fields
              : current.fields
                  .filter((field) => applies(String(field.name)))
                  .map((field) => {
                    const name = String(field.name);
                    const bare = name.split('.').pop() ?? '';
                    let next = field as FieldConfig<TFormValues>;
                    if (lockedNames.has(bare)) {
                      next = { ...next, disabled: true };
                    }
                    if (!isRequiredNow(name)) {
                      next = { ...next, required: false };
                    }
                    if (next.type === 'table') {
                      next = applyToColumns(next, bare);
                    }
                    return next;
                  })) as FieldConfig<TFormValues>[]
          }
        />
        </PlanFileUploadProvider>
      </FormProvider>
    </FormSection>
  );
}
