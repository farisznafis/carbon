/**
 * ProTable Component
 * ------------------
 * A reusable, generic table component for displaying lists of data with dynamic
 * column definitions, optional custom rendering, and optional action buttons.
 *
 * @template T - The type of each data row. Must extend Record<string, any>.
 *
 * @component
 *
 * @param {Object} props
 * @param {T[]} props.data
 *        The list of rows to display in the table.
 *
 * @param {Array<ProTableColumn<T>>} props.columns
 *        The column definitions that determine how each row field is displayed.
 *        Each column specifies a key in T and a human-readable label. Optional
 *        `render` allows custom rendering of cell content.
 *
 * @param {(row: T) => React.ReactNode} [props.actions]
 *        Optional render function for the "Actions" column. This allows adding
 *        edit/delete buttons or other UI controls per row.
 *
 * @param {keyof T} [props.searchKey]
 *        If provided, enables simple client-side search based on this field.
 *        Rows will be filtered when the search term matches the string version
 *        of this field.
 *
 * ---------------------------------------------------------------------------
 * Column Type:
 * ---------------------------------------------------------------------------
 * @typedef {Object} ProTableColumn<T>
 * @property {keyof T} key
 *        The object key in the row (e.g. "name", "email", "createdAt").
 *
 * @property {string} label
 *        The human-readable column title.
 *
 * @property {(value: any, row: T) => React.ReactNode} [render]
 *        Optional custom renderer for the cell. If provided, overrides the
 *        default text display.
 *
 * ---------------------------------------------------------------------------
 * Example Usage:
 * ---------------------------------------------------------------------------
 *
 * <ProTable<User>
 *   data={users}
 *   searchKey="name"
 *   columns={[
 *     { key: "name", label: "Name" },
 *     { key: "email", label: "Email" },
 *     {
 *       key: "role",
 *       label: "Role",
 *       render: (value) => <Badge>{value}</Badge>,
 *     },
 *   ]}
 *   actions={(user) => (
 *     <div className="flex gap-2">
 *       <Button onClick={() => onEdit(user)}>Edit</Button>
 *       <Button variant="destructive" onClick={() => onDelete(user.id)}>Delete</Button>
 *     </div>
 *   )}
 * />
 *
 * ---------------------------------------------------------------------------
 * Common Errors:
 * ---------------------------------------------------------------------------
 *
 * ❌ "Cannot convert undefined or null to object"
 * Happens when:
 *   - columns is undefined or empty
 *   - a column.key does not exist on the row data
 *   - data is undefined instead of []
 *
 * ✔ Ensure:
 *   - <ProTable data={data ?? []} columns={[...]} />
 *   - Column keys always exist on the row type T
 *
 * ---------------------------------------------------------------------------
 * Notes:
 * ---------------------------------------------------------------------------
 * - ProTable is fully generic; always call it like <ProTable<User> ... />
 * - Custom renderers allow formatting dates, tags, images, links, etc.
 * - The action column is optional and appears on the right when provided.
 */


import { useState, useMemo, ReactNode } from "react";
import { Button } from "@/components/ui/button";

export interface Column<T> {
  key: keyof T;
  id?: string;
  label: string;
  sortable?: boolean;
  truncate?: boolean;
  maxWidth?: string; // Tailwind max-width classes like "max-w-[150px]"
  render?: (value: T[keyof T], row: T) => ReactNode;
  className?: string;
}

export interface Action<T> {
  label: string;
  onClick: (row: T) => void;
  colorClass?: string; // Tailwind color e.g., "text-blue-500"
}

interface ProTableProps<T> {
  data: T[];
  columns: Column<T>[];
  actions?: Action<T>[];
  emptyState?: ReactNode;
  height?: string;
  onRowClick?: (row: T) => void;
  clickable?: boolean;
  title?: string | ReactNode;
  description?: string | ReactNode;
}

export function ProTable<T extends { id: string }>({
  data,
  columns,
  actions = [],
  emptyState,
  height = "max-h-[400px]",
  onRowClick,
  clickable = true,
  title,
  description,
}: Readonly<ProTableProps<T>>) {
  const [search, setSearch] = useState("");
  const [sortColumn, setSortColumn] = useState<keyof T>(columns[0].key);
  const [sortAsc, setSortAsc] = useState(false);

  // Filter + sort data
  const filteredData = useMemo(() => {
    const filtered = data.filter((row) => {
      if (!row || typeof row !== "object") return false;
      return Object.values(row).some((val) =>
        String(val).toLowerCase().includes(search.toLowerCase())
      );
    });

    filtered.sort((a, b) => {
      const aValue = a[sortColumn];
      const bValue = b[sortColumn];

      // For strings/numbers
      if (typeof aValue === "string" && typeof bValue === "string") {
        return sortAsc
          ? aValue.localeCompare(bValue)
          : bValue.localeCompare(aValue);
      }
      if (typeof aValue === "number" && typeof bValue === "number") {
        return sortAsc ? aValue - bValue : bValue - aValue;
      }
      return 0;
    });

    return filtered;
  }, [data, search, sortColumn, sortAsc]);

  const handleSort = (col: keyof T, sortable?: boolean) => {
    if (!sortable) return;
    if (sortColumn === col) setSortAsc(!sortAsc);
    else {
      setSortColumn(col);
      setSortAsc(true);
    }
  };

  return (
    <div>
      {/* Title + Description + Search */}
      {(title || description) && (
        <div className="flex flex-col sm:flex-row sm:justify-between sm:items-center mb-4 gap-2">
          <div>
            {title && <h2 className="text-xl font-bold">{title}</h2>}
            {description && (
              <span className="text-sm text-muted-foreground mt-1">{description}</span>
            )}
          </div>
          {/* <input
            type="text"
            placeholder="キーワード検索"
            className="border rounded px-3 py-1 w-full sm:w-64"
            value={search}
            onChange={(e) => setSearch(e.target.value)}
          /> */}
        </div>
      )}

      {/* Table with fixed height */}
      <div className={`overflow-y-auto ${height} border rounded`}>
        <table className="w-full table-auto border-collapse">
          <thead className="bg-gray-50 sticky top-0 z-10">
            <tr>
              {columns.map((col) => (
                <th
                  key={col.id ?? String(col.key)}
                  onClick={() => handleSort(col.key, col.sortable)}
                  className={`text-left text-sm font-medium text-gray-600 px-4 py-2 cursor-pointer select-none ${col.truncate ? "truncate" : ""
                    }`}
                  style={{ maxWidth: col.maxWidth }}
                >
                  <div className="flex items-center gap-1">
                    {col.label}
                    {sortColumn === col.key && col.sortable && (
                      <span>{sortAsc ? "▲" : "▼"}</span>
                    )}
                  </div>
                </th>
              ))}
              {actions.length > 0 && (
                <th className="text-right text-sm font-medium text-gray-600 px-4 py-2">

                </th>
              )}
            </tr>
          </thead>
          <tbody>
            {filteredData.length === 0 ? (
              <tr>
                <td
                  colSpan={columns.length + (actions.length > 0 ? 1 : 0)}
                  className="text-center py-8 text-muted-foreground"
                >
                  {emptyState || <p>No data available</p>}
                </td>
              </tr>
            ) : (
              filteredData.map((row, i) => (
                <tr
                  key={row.id}
                  className={`
                    ${i % 2 === 0 ? "bg-white" : "bg-gray-50"}
                    ${clickable ? "hover:bg-gray-100 cursor-pointer" : ""}
                  `}
                  onClick={clickable ? () => onRowClick?.(row) : undefined}
                >
                  {columns.map((col) => (
                    <td
                      key={col.id ?? String(col.key)}
                      className={`px-4 py-3 ${col.className || ""} ${col.truncate ? `truncate ${col.maxWidth || ""}` : ""
                        }`}
                    >
                      {col.render
                        ? col.render(row[col.key], row)
                        : String(row[col.key])}
                    </td>
                  ))}
                  {actions.length > 0 && (
                    <td className="w-[140px] whitespace-nowrap px-4 py-3">
                      <div className="flex items-center justify-end gap-2">
                        {actions.map((action) => (
                          <Button
                            key={action.label}
                            className={action.colorClass || 'text-blue-500'}
                            onClick={(e) => {
                              e.stopPropagation();
                              action.onClick(row);
                            }}
                          >
                            {action.label}
                          </Button>
                        ))}
                      </div>
                    </td>
                  )}
                </tr>
              ))
            )}
          </tbody>
        </table>
      </div>
    </div>
  );
}
