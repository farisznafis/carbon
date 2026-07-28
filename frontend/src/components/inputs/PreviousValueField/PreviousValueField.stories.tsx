import React from "react";
import { Meta, StoryFn } from "@storybook/react";
import { PreviousValueField } from "./PreviousValueField";
import { useForm } from "react-hook-form";
import { Form } from "@/components/ui/form";


export default {
  title: "Forms/PreviousValueField",
  component: PreviousValueField,
} as Meta<typeof PreviousValueField>;

/**
 * Stateful wrapper to mimic real form behavior
 */
const StatefulTemplate: StoryFn<
  React.ComponentProps<typeof PreviousValueField>
> = (args) => {
  const form = useForm({
    defaultValues: {
      field: args.value ?? "",
    },
  });

  const value = form.watch("field");

  return (
    <Form {...form}>
      <form className="max-w-md mx-auto">
        <PreviousValueField
          {...args}
          value={value}
          onChange={(v) => form.setValue("field", v)}
        />
      </form>
    </Form>
  );
};


/**
 * Default usage
 * - Empty input
 * - Previous value visible
 * - Copy button available
 */
export const Default = StatefulTemplate.bind({});
Default.args = {
  label: "電力使用量（kWh）",
  previousValue: "1180",
  unit: "kWh",
};

/**
 * No previous value
 * - Copy UI should not appear
 */
export const NoPreviousValue = StatefulTemplate.bind({});
NoPreviousValue.args = {
  label: "水使用量（m³）",
};

/**
 * Already copied
 * - Input matches previous value
 * - Button shows confirmation state
 */
export const CopiedState = StatefulTemplate.bind({});
CopiedState.args = {
  label: "ガス使用量（m³）",
  previousValue: "250",
  unit: "m³",
  value: "250",
};

/**
 * Editable after copy
 * - User can modify input
 * - Button becomes clickable again
 */
export const EditAfterCopy = StatefulTemplate.bind({});
EditAfterCopy.args = {
  label: "燃料使用量（L）",
  previousValue: "500",
  unit: "L",
};

/**
 * Large numeric value
 * - Tests overflow and spacing
 */
export const LargeValue = StatefulTemplate.bind({});
LargeValue.args = {
  label: "年間電力使用量（kWh）",
  previousValue: "1234567890",
  unit: "kWh",
};

/**
 * No unit provided
 */
export const NoUnit = StatefulTemplate.bind({});
NoUnit.args = {
  label: "訪問回数",
  previousValue: "5",
};

/**
 * Empty string previous value
 * - Defensive edge case
 */
export const EmptyPreviousValue = StatefulTemplate.bind({});
EmptyPreviousValue.args = {
  label: "その他",
  previousValue: "",
};
