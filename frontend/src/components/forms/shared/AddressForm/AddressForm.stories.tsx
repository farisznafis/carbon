// AddressForm.stories.tsx
import type { Meta, StoryObj } from "@storybook/react";
import { AddressForm } from "./AddressForm";

const meta: Meta<typeof AddressForm> = {
  title: "Forms/AddressForm",
  component: AddressForm,
  tags: ["autodocs"],
  parameters: {
    layout: "centered",
  },
};

export default meta;

type Story = StoryObj<typeof AddressForm>;

export const Default: Story = {};

export const WithPrefilledPostcode: Story = {
  render: () => {
    return (
      <div className="w-[400px]">
        <AddressForm />
      </div>
    );
  },
};

export const PrefilledPostcode: Story = {
  args: {
    defaultValues: { postcode: "1000001" },
  },
};