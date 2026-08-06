// DataTable.tsx
import * as React from "react"
import {
    Table,
    TableHeader,
    TableRow,
    TableHead,
    TableBody,
    TableCell,
} from "@/components/ui/table"
import { Button } from "@/components/ui/button"

type DataTableRow = Record<string, React.ReactNode> & {
    id?: string | number
}

type DataTableProps = {
    labels: string[]
    data: DataTableRow[]
    emptyMessage?: string
    onEditClick?: (row: DataTableRow) => void
    onRowClick?: (row: DataTableRow) => void
    bodyHeight?: string // e.g. "max-h-64"
}

const getWidthClass = (colCount: number, index: number) => {
    if (colCount <= 1) return "w-full"
    if (index === 1) return "w-2/6"
    return "w-1/6"
}

export function DataTable({
    labels,
    data,
    emptyMessage,
    onEditClick,
    onRowClick,
    bodyHeight = "max-h-64",
}: Readonly<DataTableProps>) {
    const colCount = labels.length + 1

    return (
        <div className="w-full">
            <Table className="table-fixed">
                <TableHeader>
                    <TableRow>
                        {labels.map((label, i) => (
                            <TableHead
                                key={label}
                                className={`${getWidthClass(colCount, i)} truncate`}
                            >
                                {label}
                            </TableHead>
                        ))}
                        <TableHead className={getWidthClass(colCount, labels.length)}>

                        </TableHead>
                    </TableRow>
                </TableHeader>
            </Table>

            <div className={`overflow-y-auto ${bodyHeight}`}>
                <Table className="table-fixed">
                    <TableBody>
                        {data.length === 0 ? (
                            <TableRow>
                                <TableCell colSpan={colCount} className="text-center py-8 text-foreground">
                                    {emptyMessage || 'データはありません'}
                                </TableCell>
                            </TableRow>
                        ) : (
                            data.map((row, rowIndex) => (
                                <TableRow
                                    key={row.id ?? rowIndex}
                                    className="cursor-pointer"
                                    onClick={() => onRowClick?.(row)}>
                                    {labels.map((label, i) => (
                                        <TableCell
                                            key={label}
                                            className={`${getWidthClass(colCount, i)} truncate`}
                                        >
                                            {row[label]}
                                        </TableCell>
                                    ))}
                                    <TableCell
                                        className={getWidthClass(colCount, labels.length)}
                                    >
                                        <Button
                                            size="sm"
                                            variant="outline"
                                            className="text-xs"
                                            onClick={() => onEditClick?.(row)}
                                        >
                                            更新
                                        </Button>
                                    </TableCell>
                                </TableRow>
                            ))
                        )}
                    </TableBody>
                </Table>
            </div>
        </div>
    )
}
