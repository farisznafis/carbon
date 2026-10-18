import { QueryClient } from '@tanstack/react-query';
import { ApiError } from '@/api/_shared/errors';

/** Shares one query cache across the application and authentication lifecycle. */
export const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      /** Avoids repeating requests that the backend has explicitly forbidden. */
      retry: (failureCount, error) =>
        !(error instanceof ApiError && error.status === 403) && failureCount < 3,
    },
  },
});
