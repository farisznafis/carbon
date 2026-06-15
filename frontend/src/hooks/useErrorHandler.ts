/**
 * Simplified Error Handler Hook
 * ==============================
 * React hook for handling errors with toast notifications
 */

import { useCallback } from "react";
import { useToast } from "@/hooks/use-toast";
import { handleError, parseError, ApiError } from "@/lib/errors";

interface UseErrorHandlerOptions {
  /**
   * Whether to show toast notifications automatically
   * @default true
   */
  showToast?: boolean;

  /**
   * Context for error logging
   */
  context?: string;

  /**
   * Callback function called after error is handled
   */
  onError?: (error: ApiError) => void;
}

/**
 * Hook to handle errors with toast notifications
 */
export function useErrorHandler(options: UseErrorHandlerOptions = {}) {
  const { showToast = true, context, onError } = options;
  const { toast } = useToast();

  /**
   * Handle an error and optionally display a toast
   */
  const handleApiError = useCallback(
    (error: unknown, customMessage?: string) => {
      const parsedError = handleError(error, context);

      // Show toast notification if enabled
      if (showToast) {
        toast({
          variant: "destructive",
          title: "エラー",
          description: customMessage || parsedError.message,
        });
      }

      // Call custom error handler if provided
      if (onError) {
        onError(parsedError);
      }

      return parsedError;
    },
    [context, onError, showToast, toast]
  );

  /**
   * Handle success messages
   */
  const handleSuccess = useCallback(
    (message: string, title?: string) => {
      if (showToast) {
        toast({
          title: title || "成功",
          description: message,
        });
      }
    },
    [showToast, toast]
  );

  return {
    handleError: handleApiError,
    handleSuccess,
  };
}
