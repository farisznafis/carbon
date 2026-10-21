import type { Meta, StoryObj } from "@storybook/react";
import { Eye } from "lucide-react";

import { Input } from "@/components/ui/input";
import { Textarea } from "@/components/ui/textarea";

const meta: Meta<typeof Input> = {
  title: "Design System/Form Input",
  component: Input,
  parameters: {
    layout: "centered",
  },
};

export default meta;

type Story = StoryObj<typeof Input>;

export const States: Story = {
  render: () => (
    <div className="w-[420px] max-w-[calc(100vw-2rem)] space-y-6">
      <div className="space-y-2">
        <label className="text-small block text-foreground">Your Message Title</label>
        <Input placeholder="Email" />
        <p className="text-sm leading-6 text-foreground">
          Enter your email address
        </p>
      </div>

      <div className="space-y-2">
        <label className="text-small block text-foreground">With value</label>
        <div className="relative">
          <Input className="pr-11" defaultValue="sample@example.com" />
          <Eye className="pointer-events-none absolute right-4 top-1/2 h-4 w-4 -translate-y-1/2 text-muted-foreground" />
        </div>
      </div>

      <div className="space-y-2">
        <label className="text-small block text-foreground">Error</label>
        <Input aria-invalid placeholder="Email" />
        <p className="text-sm leading-6 text-destructive-strong">
          有効なメールアドレスを入力してください
        </p>
      </div>

      <div className="space-y-2">
        <label className="text-small block text-foreground">Disabled</label>
        <Input disabled placeholder="Disabled input" />
      </div>
    </div>
  ),
};

export const TextareaStates: Story = {
  render: () => (
    <div className="w-[420px] max-w-[calc(100vw-2rem)] space-y-2">
      <label className="text-small block text-foreground">Description</label>
      <Textarea placeholder="Enter details" />
      <p className="text-sm leading-6 text-foreground">
        補足情報がある場合は入力してください
      </p>
    </div>
  ),
};
