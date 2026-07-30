// FullPageConfirmationModal.stories.tsx
import type { Meta, StoryObj } from "@storybook/react";
import React, { useState } from "react";
import { FullPageConfirmationModal } from "./FullPageConfirmationModal";

const meta: Meta<typeof FullPageConfirmationModal> = {
  title: "Modals/FullPageConfirmationModal",
  component: FullPageConfirmationModal,
  tags: ["autodocs"],
  args: {
    open: true,
    title: "Confirm changes",
    description: "Please review the information below before confirming.",
  },
  argTypes: {
    open: { control: "boolean" },
    title: { control: "text" },
    description: { control: "text" },
    onBack: { action: "back" },
    onConfirm: { action: "confirm" },
  },
  parameters: {
    docs: {
      description: {
        component:
          "A full-page confirmation modal for reviewing key-value data before submitting.",
      },
    },
  },
};

export default meta;
type Story = StoryObj<typeof FullPageConfirmationModal>;

const sampleData = {
  Name: "John Doe",
  Email: "john.doe@example.com",
  Role: "Administrator",
};

const changedData = {
  Name: "John Doe",
  Email: "john.new@example.com",
  Role: "Editor",
};

/** Basic usage with unchanged data */
export const Default: Story = {
  args: {
    data: sampleData,
    originalData: sampleData,
  },
};

/** Shows highlighted fields with a change badge where data differs from originalData */
export const WithChanges: Story = {
  args: {
    data: changedData,
    originalData: sampleData,
  },
};

/** No data provided (empty state) */
export const Empty: Story = {
  args: {
    data: {},
    originalData: {},
    description: "There is no data to review.",
  },
};

/** Mimics a disabled / blocked confirmation flow by hiding confirm button via wrapper */
export const ReadOnly: Story = {
  render: (storyArgs) => (
    <FullPageConfirmationModal
      {...storyArgs}
      onConfirm={() => {}}
      data={sampleData}
      originalData={sampleData}
    />
  ),
  parameters: {
    docs: {
      description: {
        story:
          "Use this configuration when you want users to review information without confirming inside this modal.",
      },
    },
  },
};

/** Interactive example: open/close controlled inside the story */
export const Interactive: Story = {
  render: (storyArgs) => {
    const [open, setOpen] = useState(true);

    return (
      <>
        <button
          onClick={() => setOpen(true)}
          className="rounded-md border px-3 py-1 text-sm"
        >
          Open Modal
        </button>
        <FullPageConfirmationModal
          {...storyArgs}
          open={open}
          onBack={() => setOpen(false)}
          onConfirm={() => setOpen(false)}
          data={changedData}
          originalData={sampleData}
        />
      </>
    );
  },
  parameters: {
    docs: {
      description: {
        story:
          "Demonstrates how the modal can be controlled by parent state and how back/confirm close it.",
      },
    },
  },
};
