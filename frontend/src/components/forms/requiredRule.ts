/**
 * react-hook-form rule for a required field: blank or whitespace-only fails,
 * like the backend's @NotBlank, so an empty form fails here, not on the server.
 */
export const requiredRule = (message: string) => ({
  validate: (value: unknown) => String(value ?? '').trim() !== '' || message,
});
