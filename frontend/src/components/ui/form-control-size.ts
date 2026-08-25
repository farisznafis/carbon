/**
 * Shared single-line form-control sizes used by Input and SelectTrigger.
 * Change this map when the product-wide field height or horizontal padding changes.
 */
export const FORM_CONTROL_SIZE_CLASSES = {
  sm: "h-10 px-3 py-2",
  md: "h-12 px-4 py-3",
  lg: "h-[58px] px-5 py-4",
} as const;

export const DEFAULT_FORM_CONTROL_SIZE = "md" as const;
