// DataTable.stories.tsx
import type { Meta, StoryObj } from "@storybook/react"
import type { ReactNode } from "react"
import { DataTable } from "./DataTable"

const meta: Meta<typeof DataTable> = {
  title: "Components/DataTable",
  component: DataTable,
}
export default meta

type Story = StoryObj<typeof DataTable>
type StoryRow = Record<string, ReactNode>

const baseOnEdit = (row: StoryRow) => console.log("edit", row)


export const Basic: Story = {
  args: {
    labels: ["name", "email"],
    data: [
      { id: 1, name: "Alice", email: "alice@example.com" },
      { id: 2, name: "Bob", email: "bob@example.com" },
    ],
    onRowClick: (row) => console.log("Row clicked", row),
    onEditClick: (row) => console.log("Edit clicked", row),
  },
}

export const ManyRowsScrollable: Story = {
  args: {
    labels: ["name", "email"],
    data: Array.from({ length: 50 }).map((_, i) => ({
      id: i + 1,
      name: `User ${i + 1}`,
      email: `user${i + 1}@example.com`,
    })),
    bodyHeight: "max-h-64",
    onRowClick: (row) => console.log("Row clicked", row),
  },
}

// 0) No labels
export const NoLabels: Story = {
  args: {
    labels: [],
    data: [
      { any: "value1" },
      { any: "value2" },
    ],
    onEditClick: baseOnEdit,
  },
}

// 1) 2 labels
export const TwoLabels: Story = {
  args: {
    labels: ["name", "email"],
    data: [
      { name: "Alice", email: "alice@example.com" },
      { name: "Bob", email: "bob@example.com" },
    ],
    onEditClick: baseOnEdit,
  },
}

// 2) 6 labels
export const SixLabels: Story = {
  args: {
    labels: ["id", "name", "status", "total", "createdAt", "updatedAt"],
    data: [
      {
        id: 1,
        name: "Order #1",
        status: "pending",
        total: "$120",
        createdAt: "2024-01-01",
        updatedAt: "2024-01-05",
      },
      {
        id: 2,
        name: "Order #2",
        status: "shipped",
        total: "$90",
        createdAt: "2024-01-10",
        updatedAt: "2024-01-12",
      },
    ],
    onEditClick: baseOnEdit,
  },
}

// 3) No data
export const NoData: Story = {
  args: {
    labels: ["name", "email"],
    data: [],
    onEditClick: baseOnEdit,
  },
}

