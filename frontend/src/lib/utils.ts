import { clsx, type ClassValue } from "clsx"
import { extendTailwindMerge } from "tailwind-merge"

/** Teaches tailwind-merge the design-system tokens from tailwind.config.ts so they merge as sizes, not colors. */
const twMerge = extendTailwindMerge({
  extend: {
    theme: {
      text: [
        "page-title",
        "form-title",
        "section-title",
        "subsection-title",
        "marketing-title",
        "home-section-title",
        "hero-title",
        "metric",
      ],
      spacing: ["card", "section", "marketing-section", "header", "gutter", "page-y", "grid"],
    },
  },
})

export function cn(...inputs: ClassValue[]) {
  return twMerge(clsx(inputs))
}
