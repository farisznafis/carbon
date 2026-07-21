# FileSelectButton

A styled file input button with label and selected file name display. Designed to work well with shadcn/ui `Form` and `react-hook-form`.

---

## Props

```ts
type FileSelectButtonProps = {
  id: string;
  value?: File | null;
  onChange: (file: File | null) => void;
  label?: string;      // default: "ファイルを選択"
  className?: string;
};
```

---

## Basic Usage

```tsx
import { useState } from 'react';
import { FileSelectButton } from './FileSelectButton';

function Example() {
  const [file, setFile] = useState<File | null>(null);

  return (
    <FileSelectButton
      id="file"
      value={file}
      onChange={setFile}
      label="アップロードするファイルを選択"
    />
  );
}
```

---

## Usage with shadcn/ui Form

```tsx
import { useForm, Controller } from 'react-hook-form';
import { z } from 'zod';
import { zodResolver } from '@hookform/resolvers/zod';
import {
  Form, FormField, FormItem, FormLabel, FormControl, FormMessage,
} from '@/components/ui/form';
import { Button } from '@/components/ui/button';
import { FileSelectButton } from './FileSelectButton';

const schema = z.object({
  file: z.instanceof(File).optional(),
});

type FormValues = z.infer<typeof schema>;

export function FileForm() {
  const form = useForm<FormValues>({
    resolver: zodResolver(schema),
    defaultValues: { file: undefined },
  });

  const onSubmit = (values: FormValues) => {
    console.log(values.file);
  };

  return (
    <Form {...form}>
      <form onSubmit={form.handleSubmit(onSubmit)} className="space-y-4">
        <FormField
          control={form.control}
          name="file"
          render={({ field }) => (
            <FormItem>
              <FormLabel>ファイル</FormLabel>
              <FormControl>
                <FileSelectButton
                  id="file"
                  value={field.value ?? null}
                  onChange={field.onChange}
                />
              </FormControl>
              <FormMessage />
            </FormItem>
          )}
        />
        <Button type="submit">送信</Button>
      </form>
    </Form>
  );
}
