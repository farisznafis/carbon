import type { Meta, StoryObj } from "@storybook/react";
import { FakeGraph, FakeGraphBar } from "./FakeGraphBar";

const meta: Meta<typeof FakeGraph> = {
  title: "Components/FakeGraph",
  component: FakeGraph,
};
export default meta;

type Story = StoryObj<typeof FakeGraph>;

const dummyData: FakeGraphBar[] = [
  { label: "A", value: 10, color: "#4F46E5", description: "Alpha" },
  { label: "B", value: 20, color: "#EC4899", description: "Bravo" },
  { label: "C", value: 5, color: "#10B981", description: "Charlie" },
];

export const Default: Story = {
  args: {
    data: dummyData,
    ariaLabel: "Dummy bar chart",
    height: 240,
  },
};