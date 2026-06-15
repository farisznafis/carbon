import { Card, CardHeader, CardTitle, CardContent } from "@/components/ui/card"
import { Table, TableBody, TableCell, TableRow } from "@/components/ui/table"
import { Button } from "@/components/ui/button"
import { ChevronRight, Pencil } from "lucide-react"



type Item = {
  id: string | number
  label: string
}

type ScrollTableCardProps = {
  cardTitle: string
  tableTitle: string
  items: Item[]
  onEdit?: (item: Item) => void
  onRowClick?: (item: Item) => void
}

export function ScrollTableCard({
  cardTitle,
  tableTitle,
  items,
  onEdit,
  onRowClick,
}: ScrollTableCardProps) {
  return (
    <Card className="max-w-lg border border-neutral-200/70 shadow-sm">
      <CardHeader className="pb-3">
        <CardTitle className="text-base font-semibold tracking-tight">
          {cardTitle}
        </CardTitle>
      </CardHeader>

      <CardContent>
        <Card className="border-neutral-200/80 shadow-none bg-neutral-50/60">
          <CardHeader className="pb-2">
            <CardTitle className="text-sm font-medium text-neutral-800">
              {tableTitle}
            </CardTitle>
          </CardHeader>
          <CardContent className="p-0">
            <div className="max-h-64 overflow-y-auto">
              <Table>
                <TableBody>
                  {items.map((item) => (
                    <TableRow
                      key={item.id}
                      className="cursor-pointer border-b last:border-b-0 hover:bg-neutral-100 transition-colors"
                      onClick={() => onRowClick?.(item)}
                    >
                      <TableCell className="flex items-center justify-between gap-3 px-5 py-4 text-base font-medium text-neutral-900">
                        <span>{item.label}</span>
                        <div className="flex items-center gap-2">
                          <Button
                            variant="ghost"
                            size="icon"
                            className="h-8 w-8 text-neutral-500 hover:text-neutral-900"
                            onClick={(e) => {
                              e.stopPropagation()
                              onEdit?.(item)
                            }}
                          >
                            <Pencil className="h-4 w-4" />
                          </Button>
                          <ChevronRight className="h-5 w-5 text-neutral-400" />
                        </div>
                      </TableCell>
                    </TableRow>
                  ))}
                </TableBody>
              </Table>
            </div>
          </CardContent>
        </Card>
      </CardContent>
    </Card>
  )
}