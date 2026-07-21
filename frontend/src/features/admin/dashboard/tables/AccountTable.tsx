
import { Action, Column, ProTable } from "@/components/data-display/ProTable/ProTable"
import { Button } from "@/components/ui/button"
import type { UserProfile } from "@/api/users"
// import your table component
// import { DataTable } from "@/components/data-table"

type AccountTableProps = {
    accounts: UserProfile[]
    onEdit?: (company: UserProfile) => void
    onRowClick?: (company: UserProfile) => void
}


export function AccountTable({ accounts, onEdit, onRowClick }: Readonly<AccountTableProps>) {

    //table columns for プロジェクト
    const siteColumns: Column<UserProfile>[] = [
        { key: "lastName", label: "名前", sortable: true, truncate: true, maxWidth: "max-w-[150px]" },
        // { key: "address", label: "所在地", sortable: true, truncate: true, maxWidth: "max-w-[150px]", className: "whitespace-normal text-xs leading-snug" },
        // {
        //   key: "picLastName", label: "責任者", render: (value, row) => `${row.picLastName} ${row.picFirstName}`,
        //   sortable: true, truncate: true, maxWidth: "max-w-[200px]"
        // },
        // { key: "account", label: "プロジェクト", sortable: true, truncate: true },
    ];

    //table actions for プロジェクト
    const siteActions: Action<UserProfile>[] = [
        { label: "更新", onClick: (row) => onEdit(row), colorClass: "text-white" },
    ];

    return (
        <>
            {/*Account Table*/}
            <ProTable
                title={<p className="text-2xl font-bold flex items-center gap-2">アカウント一覧</p>}
                data={accounts}
                columns={siteColumns}
                actions={siteActions}
                height="max-h-[300px]"
                emptyState={<p className="text-muted-foreground">まだアカウントが登録されていません</p>}
                onRowClick={onRowClick}
            />
        </>
    )
}
