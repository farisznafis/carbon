import type { Meta, StoryObj } from "@storybook/react";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import * as z from "zod";
import { Form } from "@/components/ui/form";
import { Button } from "@/components/ui/button";
import { GenericFormFields, FieldConfig } from "./GenericFormFields";

const schema = z.object({
  title: z.string().min(1),
  description: z.string().optional(),
  published: z.boolean().default(false),
});

type FormValues = z.infer<typeof schema>;

const fields: FieldConfig<FormValues>[] = [
  { name: "title", label: "Title", type: "text", placeholder: "Enter title" },
  {
    name: "description",
    label: "Description",
    type: "textarea",
    placeholder: "Enter description",
  },
  { name: "published", label: "Published", type: "checkbox" },
];

const meta: Meta<typeof GenericFormFields> = {
  title: "Forms/GenericFormFields",
  component: GenericFormFields,
};

export default meta;

type Story = StoryObj<typeof GenericFormFields>;

export const Default: Story = {
  render: () => {
    const form = useForm<FormValues>({
      // resolver: zodResolver(schema),
      defaultValues: { title: "", description: "", published: false },
    });

    const onSubmit = form.handleSubmit((values) => {
      console.log(values);
    });

    return (
      <Form {...form}>
        <form onSubmit={onSubmit} className="space-y-4 max-w-md">
          <GenericFormFields<FormValues> form={form} fields={fields} />
          <Button type="submit">Submit</Button>
        </form>
      </Form>
    );
  },
};
