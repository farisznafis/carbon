import {
  Pagination,
  PaginationContent,
  PaginationEllipsis,
  PaginationItem,
  PaginationLink,
  PaginationNext,
  PaginationPrevious,
} from '@/components/ui/pagination';
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '@/components/ui/select';
import { cn } from '@/lib/utils';

type PaginationItemValue = number | 'ellipsis';

type PaginationControlsProps = {
  currentPage: number;
  totalPages: number;
  onPageChange: (page: number) => void;
  totalItems?: number;
  itemsPerPage?: number;
  rowsPerPageOptions?: number[];
  onItemsPerPageChange?: (itemsPerPage: number) => void;
  siblingCount?: number;
  showFirstLast?: boolean;
  className?: string;
  hideWhenSinglePage?: boolean;
};

const DEFAULT_ROWS_PER_PAGE_OPTIONS = [5, 10, 25, 50];

function getVisiblePageNumbers(
  currentPage: number,
  totalPages: number,
  siblingCount: number,
  showFirstLast: boolean,
): PaginationItemValue[] {
  if (totalPages <= 1) return [];

  const visibleCount = siblingCount * 2 + 3 + (showFirstLast ? 2 : 0);

  if (totalPages <= visibleCount) {
    return Array.from({ length: totalPages }, (_, index) => index + 1);
  }

  const pages: PaginationItemValue[] = [];

  if (showFirstLast) {
    pages.push(1);
  }

  const left = Math.max(2, currentPage - siblingCount);
  const right = Math.min(totalPages - 1, currentPage + siblingCount);

  if (left > 2) {
    pages.push('ellipsis');
  }

  for (let page = left; page <= right; page += 1) {
    pages.push(page);
  }

  if (right < totalPages - 1) {
    pages.push('ellipsis');
  }

  if (showFirstLast) {
    pages.push(totalPages);
  }

  return pages;
}

export function PaginationControls({
  currentPage,
  totalPages,
  onPageChange,
  totalItems,
  itemsPerPage,
  rowsPerPageOptions = DEFAULT_ROWS_PER_PAGE_OPTIONS,
  onItemsPerPageChange,
  siblingCount = 1,
  showFirstLast = true,
  className,
  hideWhenSinglePage = true,
}: PaginationControlsProps) {
  const hasRowsPerPageControl = Boolean(itemsPerPage && onItemsPerPageChange);
  const hasTotalRange = typeof totalItems === 'number' && itemsPerPage;
  const shouldHide =
    hideWhenSinglePage &&
    totalPages <= 1 &&
    !hasRowsPerPageControl &&
    !hasTotalRange;

  if (shouldHide) {
    return null;
  }

  const safeTotalPages = Math.max(totalPages, 1);
  const safeCurrentPage = Math.min(Math.max(currentPage, 1), safeTotalPages);
  const visiblePages = getVisiblePageNumbers(
    safeCurrentPage,
    safeTotalPages,
    siblingCount,
    showFirstLast,
  );

  const goToPage = (page: number) => {
    const nextPage = Math.min(Math.max(page, 1), safeTotalPages);
    onPageChange(nextPage);
  };

  const startItem =
    hasTotalRange && totalItems > 0
      ? (safeCurrentPage - 1) * itemsPerPage + 1
      : 0;
  const endItem =
    hasTotalRange && totalItems > 0
      ? Math.min(safeCurrentPage * itemsPerPage, totalItems)
      : 0;

  return (
    <div
      className={cn(
        'flex flex-col gap-3 text-sm text-muted-foreground md:flex-row md:items-center md:justify-end',
        className,
      )}
    >
      {hasRowsPerPageControl && (
        <div className='flex items-center gap-2'>
          <span>1ページあたりの件数</span>
          <Select
            value={String(itemsPerPage)}
            onValueChange={(value) => onItemsPerPageChange(Number(value))}
          >
            <SelectTrigger className='h-9 w-[84px]'>
              <SelectValue />
            </SelectTrigger>
            <SelectContent>
              {rowsPerPageOptions.map((option) => (
                <SelectItem key={option} value={String(option)}>
                  {option}
                </SelectItem>
              ))}
            </SelectContent>
          </Select>
        </div>
      )}

      {hasTotalRange && (
        <div className='whitespace-nowrap'>
          {totalItems}件中 {startItem}-{endItem}件
        </div>
      )}

      <Pagination className='mx-0 w-auto justify-end'>
        <PaginationContent>
          <PaginationItem>
            <PaginationPrevious
              href='#'
              onClick={(event) => {
                event.preventDefault();
                goToPage(safeCurrentPage - 1);
              }}
              className={
                safeCurrentPage === 1 ? 'pointer-events-none opacity-50' : ''
              }
            />
          </PaginationItem>

          {visiblePages.map((page, index) =>
            page === 'ellipsis' ? (
              <PaginationItem key={`ellipsis-${index}`}>
                <PaginationEllipsis />
              </PaginationItem>
            ) : (
              <PaginationItem key={page}>
                <PaginationLink
                  href='#'
                  isActive={safeCurrentPage === page}
                  onClick={(event) => {
                    event.preventDefault();
                    goToPage(page);
                  }}
                >
                  {page}
                </PaginationLink>
              </PaginationItem>
            ),
          )}

          <PaginationItem>
            <PaginationNext
              href='#'
              onClick={(event) => {
                event.preventDefault();
                goToPage(safeCurrentPage + 1);
              }}
              className={
                safeCurrentPage === safeTotalPages
                  ? 'pointer-events-none opacity-50'
                  : ''
              }
            />
          </PaginationItem>
        </PaginationContent>
      </Pagination>
    </div>
  );
}
