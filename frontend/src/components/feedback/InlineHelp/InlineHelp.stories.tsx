// InlineHelp.stories.tsx
import type { Meta, StoryObj } from "@storybook/react";
import { InlineHelp } from "@/components/feedback/InlineHelp/InlineHelp";

const meta: Meta<typeof InlineHelp> = {
  title: "Components/InlineHelp",
  component: InlineHelp,
  args: {
    content: "This is some helpful explanatory text about this field.",
  },
};
export default meta;

type Story = StoryObj<typeof InlineHelp>;

export const Default: Story = {};

export const LongText: Story = {
  args: {
    content:
      "This is a much longer help text to demonstrate wrapping behavior and spacing around the close button in the top right corner of the tooltip box.",
  },
};

