export type ApiErrorPayload = {
  statusCode?: string | number;
  message?: string | null;
  errors?: unknown;
};

export type ApiPagination = {
  page: number;
  limit: number;
  totalItems: number;
  totalPages: number;
  nextPageToken?: string | null;
  hasMore: boolean;
};

export type ApiResponse<T> = ApiErrorPayload & {
  data?: T;
};
