import type { Meta, StoryObj } from "@storybook/react";
import { useState } from "react";

import { FileInput, type FileInputProps } from "@/components/ui/file-input";

const meta: Meta<typeof FileInput> = {
  title: "Design System/File Input",
  component: FileInput,
  parameters: {
    layout: "centered",
  },
  args: {
    accept: ".pdf,.jpg,.png",
    helperText: "PDF、JPG、PNG形式に対応しています",
    label: "ファイルを選択",
  },
};

export default meta;

type Story = StoryObj<typeof FileInput>;

function ControlledFileInput(args: FileInputProps) {
  const [file, setFile] = useState<File | null>(args.value ?? null);

  return (
    <div className="w-[640px] max-w-[calc(100vw-2rem)]">
      <FileInput {...args} value={file} onFileChange={setFile} />
    </div>
  );
}

export const Empty: Story = {
  render: (args) => <ControlledFileInput {...args} />,
};

export const WithError: Story = {
  args: {
    error: "ファイルを選択してください",
  },
  render: (args) => <ControlledFileInput {...args} />,
};

export const Disabled: Story = {
  args: {
    disabled: true,
  },
  render: (args) => <ControlledFileInput {...args} />,
};
