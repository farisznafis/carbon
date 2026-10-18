/**
 * Simplified Error Handling System
 * =================================
 * Handles API errors with statusCode and message fields
 */

/**
 * API Error Response Format
 */
export interface ApiError {
  statusCode: string;
  message: string;
  errors?: Record<string, string>;
}

type ErrorResponsePayload = {
  statusCode?: unknown;
  message?: unknown;
  errors?: unknown;
};

type ErrorResponse = {
  status?: unknown;
  data?: ErrorResponsePayload | string;
};

function isApiError(value: unknown): value is ApiError {
  return (
    Boolean(value) &&
    typeof value === 'object' &&
    'statusCode' in value &&
    typeof (value as ErrorResponsePayload).statusCode === 'string' &&
    (
      !('message' in value) ||
      typeof (value as ErrorResponsePayload).message === 'string'
    )
  );
}

function isValidationErrors(value: unknown): value is Record<string, string> {
  return (
    Boolean(value) &&
    typeof value === 'object' &&
    Object.values(value).every((message) => typeof message === 'string')
  );
}

function normalizeApiError(value: ApiError): ApiError {
  return {
    statusCode: value.statusCode,
    message: value.message || value.statusCode,
    errors: isValidationErrors(value.errors) ? value.errors : undefined,
  };
}

function getErrorResponse(error: unknown): ErrorResponse | null {
  if (
    !error ||
    typeof error !== 'object' ||
    !('response' in error) ||
    !error.response ||
    typeof error.response !== 'object'
  ) {
    return null;
  }

  return error.response as ErrorResponse;
}

/**
 * Parse various error formats into ApiError
 */
export function parseError(error: unknown): ApiError {
  // Already in correct format
  if (isApiError(error)) {
    return normalizeApiError(error);
  }

  // Fetch Response object
  if (error instanceof Response) {
    return {
      statusCode: error.status.toString(),
      message: error.statusText || "An error occurred",
    };
  }

  // Error object
  if (error instanceof Error) {
    return {
      statusCode: "UNKNOWN_ERROR",
      message: error.message,
    };
  }

  // String error
  if (typeof error === "string") {
    return {
      statusCode: "UNKNOWN_ERROR",
      message: error,
    };
  }

  // Axios-like error
  const response = getErrorResponse(error);
  if (response) {
    if (response.data) {
      if (isApiError(response.data)) {
        return normalizeApiError(response.data);
      }
      if (typeof response.data === "string") {
        return {
          statusCode: String(response.status || "UNKNOWN_ERROR"),
          message: response.data,
        };
      }
    }
  }

  // Fallback
  return {
    statusCode: "UNKNOWN_ERROR",
    message: "An unexpected error occurred",
  };
}

export function getErrorToastContent(error: ApiError): {
  title: string;
  description: string;
} {
  if (error.errors && Object.keys(error.errors).length > 0) {
    return {
      title: error.statusCode,
      description: Object.entries(error.errors)
        .map(([field, message]) => `${field}: ${message}`)
        .join('\n'),
    };
  }

  return {
    title: error.statusCode,
    description: error.message,
  };
}

/**
 * Handle error and log to console
 */
export function handleError(error: unknown, context?: string): ApiError {
  const parsedError = parseError(error);
  
  if (context) {
    logger.error(`[${context}]`, parsedError);
  } else {
    logger.error("Error:", parsedError);
  }
  
  return parsedError;
}
import { logger } from './logger';
