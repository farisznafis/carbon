# GenericFormField

GenericFormFields renders form fields using shadcn/ui Form and react-hook-form, leaving the submit buttons and layout to the parent.
```ts

import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { Form } from "@/components/ui/form";
import { Button } from "@/components/ui/button";
import { GenericFormFields, FieldConfig } from "./GenericFormFields";

type FormValues = {
  title: string;
  description?: string;
  published: boolean;
};

const fields: FieldConfig<FormValues>[] = [
  { name: "title", label: "Title", type: "text" },
  { name: "description", label: "Description", type: "textarea" },
  { name: "published", label: "Published", type: "checkbox" },
];

function Example() {
  const form = useForm<FormValues>({
    // resolver: zodResolver(/* schema */),
    defaultValues: { title: "", description: "", published: false },
  });

  const onSubmit = form.handleSubmit((values) => {
    console.log(values);
  });

  return (
    <Form {...form}>
      <form onSubmit={onSubmit} className="space-y-4">
        <GenericFormFields<FormValues> form={form} fields={fields} />
        <div className="flex justify-end gap-2">
          <Button type="submit">Submit</Button>
        </div>
      </form>
    </Form>
  );
}