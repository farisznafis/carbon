import type { Meta, StoryObj } from "@storybook/react";
import { Mail, Plus } from "lucide-react";

import { Button } from "@/components/ui/button";

const meta: Meta<typeof Button> = {
  title: "Design System/Button",
  component: Button,
  parameters: {
    layout: "centered",
  },
};

export default meta;

type Story = StoryObj<typeof Button>;

export const Variants: Story = {
  render: () => (
    <div className="flex flex-wrap items-center gap-3">
      <Button>Continue</Button>
      <Button variant="destructive">Destructive</Button>
      <Button variant="secondary">Secondary</Button>
      <Button variant="subtle">Subtle</Button>
      <Button variant="ghost">Ghost</Button>
      <Button variant="cancel">Cancel</Button>
      <Button variant="link">Link</Button>
    </div>
  ),
};

export const Sizes: Story = {
  render: () => (
    <div className="flex flex-wrap items-center gap-3">
      <Button size="sm">Continue</Button>
      <Button>Continue</Button>
      <Button size="lg">Continue</Button>
      <Button size="icon" aria-label="Add item">
        <Plus />
      </Button>
    </div>
  ),
};

export const WithIconsAndStates: Story = {
  render: () => (
    <div className="flex flex-wrap items-center gap-3">
      <Button>
        <Mail />
        Login with Email
      </Button>
      <Button disabled>Disabled</Button>
      <Button variant="destructive" disabled>
        Disabled
      </Button>
    </div>
  ),
};
