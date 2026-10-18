// FileSelectButton.stories.tsx
import type { Meta, StoryObj } from '@storybook/react';
import { useState } from 'react';
import { FileSelectButton } from './FileSelectButton';

const meta: Meta<typeof FileSelectButton> = {
  title: 'Components/FileSelectButton',
  component: FileSelectButton,
  args: {
    id: 'file',
    label: 'ファイルを選択',
  },
};
export default meta;

type Story = StoryObj<typeof FileSelectButton>;

export const Default: Story = {
  render: (args) => {
    const [file, setFile] = useState<File | null>(null);
    return (
      <FileSelectButton
        {...args}
        value={file}
        onChange={setFile}
      />
    );
  },
};

export const WithCustomLabel: Story = {
  render: (args) => {
    const [file, setFile] = useState<File | null>(null);
    return (
      <FileSelectButton
        {...args}
        label="アップロード"
        value={file}
        onChange={setFile}
      />
    );
  },
};