// DeleteAlertDialog.stories.tsx (Storybook)
import type { Meta, StoryObj } from '@storybook/react';
import { useState } from 'react';
import { DeleteAlertDialog } from './DeleteAlertDialog';

const meta: Meta<typeof DeleteAlertDialog> = {
  title: 'Components/DeleteAlertDialog',
  component: DeleteAlertDialog,
};
export default meta;

type Story = StoryObj<typeof DeleteAlertDialog>;

export const Default: Story = {
  render: () => {
    const [open, setOpen] = useState(false);
    return (
      <DeleteAlertDialog
        label="X"
        open={open}
        onOpenChange={setOpen}
        onDelete={() => alert('deleted')}
      />
    );
  },
};

export const OpenInitially: Story = {
  render: () => {
    const [open, setOpen] = useState(true);
    return (
      <DeleteAlertDialog
        label="Y"
        open={open}
        onOpenChange={setOpen}
        onDelete={() => alert('deleted Y')}
      />
    );
  },
};