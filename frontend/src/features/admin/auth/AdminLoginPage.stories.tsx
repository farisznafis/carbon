// src/stories/AdminLogin.stories.tsx
import type { Meta, StoryObj } from "@storybook/react";
import { LoginForm } from "@/components/forms/shared/LoginForm";

const meta: Meta<typeof LoginForm> = {
  title: "Admin/LoginForm",
  component: LoginForm,
  args: {
    title: "Admin Login",
    submitLabel: "Login as Admin",
  },
};
export default meta;

type Story = StoryObj<typeof LoginForm>;

export const Default: Story = {
  args: {
    onSubmit: async ({ email, password }) => {
      // ダミー API 呼び出し
      await new Promise(r => setTimeout(r, 800));
      if (email !== "admin@example.com" || password !== "password") {
        throw new Error("Invalid admin credentials");
      }
      // Storybook なので実際の遷移はしない
      // window.location.href = "/admin/dashboard";
    },
  },
};

export const LoadingState: Story = {
  args: {
    onSubmit: async () => {
      // ローディング状態を確認するためわざと長く待つ
      await new Promise(r => setTimeout(r, 3000));
    },
  },
};

export const ErrorExample: Story = {
  args: {
    onSubmit: async () => {
      await new Promise(r => setTimeout(r, 500));
      throw new Error("Server error (sample)");
    },
  },
};