import React from "react";
import { Meta, StoryFn } from "@storybook/react";
import { FormPageWrapper } from "./FormPageWrapper";
import { Input } from "@/components/ui/input";
import { Button } from "@/components/ui/button";

export default {
  title: "Layouts/FormPageWrapper",
  component: FormPageWrapper,
  parameters: {
    layout: "fullscreen",
  },
} as Meta<typeof FormPageWrapper>;

const DummyFormContent = () => (
  <form className="space-y-6">
    <div className="space-y-2">
      <label className="text-sm font-medium">電力使用量（kWh）</label>
      <Input placeholder="例: 1180" />
    </div>

    <div className="space-y-2">
      <label className="text-sm font-medium">水使用量（m³）</label>
      <Input placeholder="例: 25" />
    </div>

    <Button type="submit" className="w-full">
      送信
    </Button>
  </form>
);

const Template: StoryFn<typeof FormPageWrapper> = (args) => (
  <FormPageWrapper {...args}>{args.children}</FormPageWrapper>
);

/**
 * Default form layout
 * - Standard page title
 * - Default required-fields description
 */
export const Default = Template.bind({});
Default.args = {
  pageName: "月次データ入力",
  title: "電力使用量の入力",
  children: <DummyFormContent />,
};

/**
 * Custom description copy
 * - Useful for partial forms or special instructions
 */
export const CustomDescription = Template.bind({});
CustomDescription.args = {
  pageName: "プロジェクト登録",
  title: "基本情報の入力",
  description: (
    <>
      入力内容をご確認のうえ、
      <span className="font-bold">正確に</span>ご記入ください。
    </>
  ),
  children: <DummyFormContent />,
};

/**
 * Long titles (edge case)
 * - Ensures wrapping and spacing remain correct
 */
export const LongTitles = Template.bind({});
LongTitles.args = {
  pageName: "中小企業向け温室効果ガス排出量算定支援システム",
  title: "月次エネルギー使用量および関連データの入力",
  children: <DummyFormContent />,
};

/**
 * Minimal form content
 * - Single field
 * - Ensures vertical rhythm still feels correct
 */
export const MinimalForm = Template.bind({});
MinimalForm.args = {
  pageName: "確認",
  title: "最終確認",
  children: (
    <form className="space-y-6">
      <div className="space-y-2">
        <label className="text-sm font-medium">確認コード</label>
        <Input />
      </div>
      <Button className="w-full">確定</Button>
    </form>
  ),
};

/**
 * Dense form (edge case)
 * - Many fields to test scroll behavior
 */
export const DenseForm = Template.bind({});
DenseForm.args = {
  pageName: "詳細入力",
  title: "設備情報",
  children: (
    <form className="space-y-4">
      {Array.from({ length: 6 }).map((_, i) => (
        <div key={i} className="space-y-2">
          <label className="text-sm font-medium">
            フィールド {i + 1}
          </label>
          <Input />
        </div>
      ))}
      <Button className="w-full">保存</Button>
    </form>
  ),
};
