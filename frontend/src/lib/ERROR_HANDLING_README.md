# Simplified Error Handling System

This system handles API errors that come in the format:
```json
{
  "statusCode": "INVALID_FORMAT",
  "message": "your username, email, or password is incorrect"
}
```

## Quick Start

### Basic Usage

```tsx
import { useErrorHandler } from "@/hooks/useErrorHandler";

function LoginComponent() {
  const { handleError, handleSuccess } = useErrorHandler();

  const handleLogin = async (credentials) => {
    try {
      const response = await fetch("/api/login", {
        method: "POST",
        body: JSON.stringify(credentials),
      });

      if (!response.ok) {
        const error = await response.json();
        handleError(error);
        return;
      }

      handleSuccess("ログインに成功しました");
    } catch (error) {
      handleError(error);
    }
  };

  return <button onClick={handleLogin}>Login</button>;
}
```

### Without Toast Notifications

```tsx
const { handleError } = useErrorHandler({ 
  showToast: false,
  onError: (error) => {
    console.log("Custom handling:", error.message);
  }
});
```

### With Context for Logging

```tsx
const { handleError } = useErrorHandler({ 
  context: "LoginPage" 
});
```

## API Reference

### `useErrorHandler(options?)`

Options:
- `showToast?: boolean` - Show toast notifications (default: true)
- `context?: string` - Context for console logging
- `onError?: (error: ApiError) => void` - Custom error callback

Returns:
- `handleError(error: unknown, customMessage?: string)` - Handle errors
- `handleSuccess(message: string, title?: string)` - Show success messages

### `parseError(error: unknown): ApiError`

Parses various error formats into the standard `ApiError` format.

### `handleError(error: unknown, context?: string): ApiError`

Parses error and logs to console with optional context.

## Expected API Error Format

```json
{
  "statusCode": "ERROR_CODE",
  "message": "User-friendly error message"
}
```

The message will be displayed directly to users in toast notifications.
