
import { Action, Column, ProTable } from "@/components/data-display/ProTable/ProTable"
import type { Company } from "@/api/users"
// import your table component
// import { DataTable } from "@/components/data-table"

type CompanyTableProps = {
    companies: Company[]
    onEdit?: (company: Company) => void
    onRowClick?: (company: Company) => void
}


export function CompanyTable({ companies, onEdit, onRowClick }: CompanyTableProps) {

    //table columns for 参加企業
    const siteColumns: Column<Company>[] = [
        { key: "nameJA", label: "企業名", sortable: true, truncate: true, maxWidth: "max-w-[150px]" },
        { key: "repLastName", label: "代表者名", render: (value, row) => `${row.repLastName} ${row.repFirstName}`, sortable: true, truncate: true, maxWidth: "max-w-[150px]" },
        { key: "repEmail", label: "代表者メールアドレス", sortable: true, truncate: true, maxWidth: "max-w-[200px]" },
        { key: "applicationDate", label: "申請日", sortable: true, truncate: true, maxWidth: "max-w-[150px]"},

    ];

    //table actions for 参加企業
    const siteActions: Action<Company>[] = [
        { label: "更新", onClick: (row) => onEdit(row), colorClass: "text-white" },
    ];

    return (
        <>
            {/*Company Table*/}
            <ProTable
                title={<p className="text-2xl font-bold flex items-center gap-2">アカウント管理</p>}
                data={companies}
                columns={siteColumns}
                actions={siteActions}
                height="max-h-[300px]"
                emptyState={<p className="text-muted-foreground">まだ企業が登録されていません</p>}
                onRowClick={onRowClick}
            />
        </>
    )
}
