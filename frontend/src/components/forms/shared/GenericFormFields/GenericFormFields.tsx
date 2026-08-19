// GenericFormFields.tsx
import {
  FormField,
  FormItem,
  FormControl,
  FormMessage,
} from "@/components/ui/form";
import { RequiredLabel } from "@/components/forms/shared/RequiredLabel";
import { Input } from "@/components/ui/input";
import { Textarea } from "@/components/ui/textarea";
import { Checkbox } from "@/components/ui/checkbox";
import {
  Select,
  SelectTrigger,
  SelectValue,
  SelectContent,
  SelectItem,
} from "@/components/ui/select";
import type { UseFormReturn, FieldValues, Path, FieldPath } from "react-hook-form";

export type FieldType =
  | "text"
  | "textarea"
  | "checkbox"
  | "number"
  | "email"
  | "select"
  | "date"
  | "file";

export interface SelectOption<T = string> {
  label: string;
  value: T;
}

/**
 * Configuration for a single form field.
 *
 * NOTE ON FILE INPUTS:
 * - `type: "file"` will produce a File or File[] in form values
 * - File uploads are handled via FormData at submit time
 * - This component intentionally does NOT perform uploads
 */

export interface FieldConfig<TFormValues extends FieldValues = FieldValues> {
  name: Path<TFormValues>;
  label: string;
  type: FieldType;
  placeholder?: string;
  required?: boolean;
  /**
   * Locks the input outright, for a value the form does not own — a field the
   * server fills from its own data, say. `disabledWhen` covers the cases that
   * depend on another answer; this one does not depend on anything.
   */
  disabled?: boolean;
  options?: SelectOption<unknown>[]; // for select
  //for file
  accept?: string; // MIME types or file extensions (e.g. "image/*")
  multiple?: boolean;

  // show/hide logic
  showWhen?:
  | {
      field: Path<TFormValues>;
      value: string | string[] | number | boolean | Array<string | string[] |number | boolean>;
    }
  | Array<{
      field: Path<TFormValues>;
      value: string | string[] | number | boolean | Array<string | string[] |number | boolean>;
    }>;

  disabledWhen?:
    | {
        field: Path<TFormValues>;
        value: string | string[] | number | boolean | Array<string | string[] |number | boolean>; // Value(s) that make this field disabled
      }
    | Array<{
        field: Path<TFormValues>;
        value: string | string[] | number | boolean | Array<string | string[] |number | boolean>;
      }>;

  hideWhen?:
    | {
        field: Path<TFormValues>;
        value: string | string[] | number | boolean | Array<string | string[] |number | boolean>; // Value(s) that make this field hidden
      }
    | Array<{
        field: Path<TFormValues>;
        value: string | string[] | number | boolean | Array<string | string[] |number | boolean>;
      }>;
}

interface GenericFormFieldsProps<TFormValues extends FieldValues> {
  form: UseFormReturn<TFormValues>;
  fields: FieldConfig<TFormValues>[];
}

export function GenericFormFields<TFormValues extends FieldValues>({
  form,
  fields,
}: Readonly<GenericFormFieldsProps<TFormValues>>) {
  return (
    <>
      {fields.map((config) => (
        <FormField
          key={config.name as string}
          control={form.control}
          name={config.name}
          render={({ field }) => (
            <FormItem>
              <RequiredLabel required={config.required === true}>{config.label}</RequiredLabel>
              <FormControl>
                {config.type === "file" ? (
                  <Input
                    type="file"
                    accept={config.accept}
                    multiple={config.multiple}

                    /**
                    * File inputs MUST be uncontrolled.
                    * - We do NOT bind `value`
                    * - Browsers block programmatic control of file values for security
                    */
                    onChange={(e) => {
                      /**
                       * `e.target.files` returns a FileList or null
                       * FileList is array-like but NOT a real array
                       */
                      const files = e.target.files;

                      /**
                     * Normalize the value we pass to react-hook-form:
                     * - single file field → File | null
                     * - multiple file field → File[] | null
                     *
                     * This keeps form values predictable and easy to serialize
                     * later when building FormData for submission.
                     */
                      field.onChange(
                        config.multiple ? files : files?.[0] ?? null
                      );
                    }}
                  />
                ) : config.type === "checkbox" ? (
                  <Checkbox
                    checked={Boolean(field.value)}
                    onCheckedChange={(checked) => field.onChange(checked === true)}
                    disabled={field.disabled}
                    ref={field.ref}
                  />
                ) : config.type === "textarea" ? (
                  <Textarea
                    {...field}
                    value={field.value as string | undefined}
                    placeholder={config.placeholder}
                  />
                ) : config.type === "number" ? (
                  <Input
                    type="number"
                    value={
                      field.value === undefined || field.value === null
                        ? ""
                        : String(field.value)
                    }
                    onChange={(e) => {
                      const v = e.target.value;
                      field.onChange(v === "" ? undefined : Number(v));
                    }}
                    placeholder={config.placeholder}
                  />
                ) : config.type === "select" ? (
                  <Select
                    value={
                      field.value === null || field.value === undefined
                        ? ""
                        : String(field.value)
                    }
                    onValueChange={(value) => {
                      const selected = config.options?.find(
                        (opt) => String(opt.value) === value
                      );
                      field.onChange(selected?.value);
                    }}
                  >
                    <SelectTrigger>
                      <SelectValue placeholder={config.placeholder} />
                    </SelectTrigger>
                    <SelectContent>
                      {config.options?.map((opt) => (
                        <SelectItem
                          key={String(opt.value)}
                          value={String(opt.value)}
                        >
                          {opt.label}
                        </SelectItem>
                      ))}
                    </SelectContent>
                  </Select>
                ) : config.type === "date" ? (
                  <Input
                    type="date"
                    value={
                      field.value === undefined || field.value === null
                        ? ""
                        : (field.value as string)
                    }
                    onChange={(e) => field.onChange(e.target.value || undefined)}
                    placeholder={config.placeholder}
                  />
                ) : (
                  <Input
                    {...field}
                    type={config.type === "email" ? "email" : "text"}
                    value={
                      field.value as
                      | string
                      | number
                      | readonly string[]
                      | undefined
                    }
                    placeholder={config.placeholder}
                  />
                )}
              </FormControl>
              <FormMessage />
            </FormItem>
          )}
        />
      ))}
    </>
  );
}
