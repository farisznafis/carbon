import { DataTable } from "./DataTable"

# DataTable

`DataTable` renders a table with fixed header and scrollable body.

## Props

- `labels: string[]` – column labels / keys
- `data: Record<string, any>[]` – row data
- `bodyHeight?: string` – tailwind height for body (default: `"max-h-64"`)
- `onEditClick?: (row) => void` – called when Edit button is clicked
- `onRowClick?: (row) => void` – called when a row is clicked

## Basic

<DataTable
  labels={["name", "email"]}
  data={[
    { id: 1, name: "Alice", email: "alice@example.com" },
    { id: 2, name: "Bob", email: "bob@example.com" },
  ]}
  onRowClick={(row) => alert(`Row clicked: ${row.name}`)}
  onEditClick={(row) => alert(`Edit: ${row.name}`)}
/>

## Many rows (scrolling body)

<DataTable
  labels={["name", "email"]}
  data={Array.from({ length: 50 }).map((_, i) => ({
    id: i + 1,
    name: `User ${i + 1}`,
    email: `user${i + 1}@example.com`,
  }))}
  bodyHeight="max-h-64"
  onRowClick={(row) => alert(`Row clicked: ${row.name}`)}
/>