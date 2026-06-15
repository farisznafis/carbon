import type { Meta, StoryObj } from "@storybook/react";
import { ProTable } from './ProTable';
import type { Column, Action } from "./ProTable";

interface User {
  id: string;
  name: string;
  email: string;
  age: number;
}

const sampleData: User[] = [
  { id: "1", name: "Alice", email: "alice@example.com", age: 28 },
  { id: "2", name: "Bob", email: "bob@example.com", age: 34 },
  { id: "3", name: "Charlie", email: "charlie@example.com", age: 22 }
];

const columns: Column<User>[] = [
  { key: "name", label: "Name", sortable: true },
  { key: "email", label: "Email", truncate: true, maxWidth: "max-w-[150px]" },
  { key: "age", label: "Age", sortable: true }
];

const actions: Action<User>[] = [
  {
    label: "View",
    colorClass: "text-blue-500",
    onClick: (row) => alert(`Viewing ${row.name}`)
  },
  {
    label: "Delete",
    colorClass: "text-red-500",
    onClick: (row) => alert(`Deleting ${row.name}`)
  }
];

const meta: Meta<typeof ProTable<User>> = {
  title: "Components/ProTable",
  component: ProTable,
  tags: ["autodocs"],
  argTypes: {},
}; 

export default meta;
type Story = StoryObj<typeof ProTable<User>>;

export const Default: Story = {
  args: {
    title: "User Table",
    description: "A list of users with sortable columns and actions.",
    data: sampleData,
    columns,
    actions,
    height: "h-96",
  },
};

export const Empty: Story = {
  args: {
    title: "Empty Table",
    data: [],
    columns,
    emptyState: <div className="text-gray-500 p-4">No users found.</div>,
  },
};