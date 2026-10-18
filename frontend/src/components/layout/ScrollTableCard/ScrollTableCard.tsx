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
    <Card className="max-w-lg border-border shadow-app-sm">
      <CardHeader className="pb-3">
        <CardTitle className="text-base font-bold tracking-tight">
          {cardTitle}
        </CardTitle>
      </CardHeader>

      <CardContent>
        <Card className="border-border bg-muted/40 shadow-none">
          <CardHeader className="pb-2">
            <CardTitle className="text-sm font-medium text-foreground">
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
                      className="cursor-pointer border-b transition-colors last:border-b-0 hover:bg-muted"
                      onClick={() => onRowClick?.(item)}
                    >
                      <TableCell className="flex items-center justify-between gap-3 px-5 py-4 text-base font-medium text-foreground">
                        <span>{item.label}</span>
                        <div className="flex items-center gap-2">
                          <Button
                            variant="ghost"
                            size="icon"
                            className="h-8 w-8 text-muted-foreground hover:text-foreground"
                            onClick={(e) => {
                              e.stopPropagation()
                              onEdit?.(item)
                            }}
                          >
                            <Pencil className="h-4 w-4" />
                          </Button>
                          <ChevronRight className="h-5 w-5 text-muted-foreground" />
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
