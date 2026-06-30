import { usePagination } from "@/hooks/usePagination";
import { Action, Column, ProTable } from "@/components/data-display/ProTable/ProTable";
import { InlineHelp } from "@/components/feedback/InlineHelp/InlineHelp";
import { Building2 } from "lucide-react";
import { Button } from "@/components/ui/button";
import type { Site } from "@/api/site";
import { PaginationControls } from "@/components/data-display/Pagination/PaginationControls";

type SiteTableProps = {
  sites: Site[];
  onEdit?: (site: Site) => void;
  onRowClick?: (site: Site) => void;
};

export function SiteTable({ sites, onEdit, onRowClick }: SiteTableProps) {
  const siteColumns: Column<Site>[] = [
    {
      key: "siteName",
      label: "拠点名",
      sortable: true,
      truncate: true,
      maxWidth: "max-w-[180px]",
    },
    {
      key: "picFirstName",
      label: "担当者",
      sortable: true,
      truncate: true,
      maxWidth: "max-w-[180px]",
      render: (_, row) => `${row.picLastName} ${row.picFirstName}`,
    },
    {
      key: "address",
      label: "所在地",
      sortable: true,
      truncate: true,
      maxWidth: "max-w-[260px]",
      render: (_, row) => row.address || "なし",
    },
    {
      key: "project",
      label: "プロジェクト",
      sortable: false,
      truncate: true,
      maxWidth: "max-w-[400px]",
      render: (value) => {
        const projects = Array.isArray(value) ? value : [];
        if (projects.length === 0) return "なし";
        return projects.join(", ");
      },
    },
  ];

  const siteActions: Action<Site>[] = [
    {
      label: "更新",
      onClick: (row) => onEdit?.(row),
      colorClass: "text-white",
    },
  ];

  const {
    currentPage,
    totalPages,
    totalItems,
    itemsPerPage,
    paginatedItems: paginatedSites,
    goToPage,
    setItemsPerPage,
  } = usePagination(sites);

  return (
    <>
      <ProTable
        title={
          <p className="text-2xl font-bold flex items-center gap-2">
            <Building2 className="h-6 w-6" />
            拠点
            <InlineHelp
              content={
                <div className="text-sm inline">
                  拠点の行をクリックすると、その拠点のダッシュボードに移動します
                </div>
              }
            />
          </p>
        }
        data={paginatedSites}
        columns={siteColumns}
        actions={siteActions}
        emptyState={<p className="text-muted-foreground">まだ拠点が登録されていません</p>}
        onRowClick={onRowClick}
      />

      <div className="mt-4">
        <PaginationControls
          currentPage={currentPage}
          totalPages={totalPages}
          totalItems={totalItems}
          itemsPerPage={itemsPerPage}
          onPageChange={goToPage}
          onItemsPerPageChange={setItemsPerPage}
        />
      </div>
    </>
  );
}
