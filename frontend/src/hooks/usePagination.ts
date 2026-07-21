import { useEffect, useMemo, useState } from 'react';

type UsePaginationOptions = {
  itemsPerPage?: number;
  initialPage?: number;
};

export function usePagination<T>(
  items: T[] = [],
  { itemsPerPage = 5, initialPage = 1 }: UsePaginationOptions = {},
) {
  const [currentPage, setCurrentPage] = useState(initialPage);
  const [pageSize, setPageSize] = useState(itemsPerPage);

  const totalItems = items.length;
  const totalPages = Math.max(1, Math.ceil(totalItems / pageSize));
  const startItem = totalItems === 0 ? 0 : (currentPage - 1) * pageSize + 1;
  const endItem = Math.min(currentPage * pageSize, totalItems);

  useEffect(() => {
    if (currentPage > totalPages) {
      setCurrentPage(totalPages);
    }
  }, [currentPage, totalPages]);

  const paginatedItems = useMemo(() => {
    const start = (currentPage - 1) * pageSize;
    const end = currentPage * pageSize;
    return items.slice(start, end);
  }, [items, currentPage, pageSize]);

  const goToPage = (page: number) => {
    const nextPage = Math.min(Math.max(page, 1), totalPages);
    setCurrentPage(nextPage);
  };

  const setItemsPerPage = (nextItemsPerPage: number) => {
    setPageSize(nextItemsPerPage);
    setCurrentPage(1);
  };

  const nextPage = () => goToPage(currentPage + 1);
  const prevPage = () => goToPage(currentPage - 1);

  return {
    currentPage,
    totalPages,
    totalItems,
    startItem,
    endItem,
    paginatedItems,
    setCurrentPage,
    goToPage,
    nextPage,
    prevPage,
    itemsPerPage: pageSize,
    setItemsPerPage,
  };
}
