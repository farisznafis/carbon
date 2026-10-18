import * as React from "react";
import { cva, type VariantProps } from "class-variance-authority";

import {
  DEFAULT_FORM_CONTROL_SIZE,
  FORM_CONTROL_SIZE_CLASSES,
} from "@/components/ui/form-control-size";
import { cn } from "@/lib/utils";

const inputVariants = cva(
  "flex w-full rounded-md border border-input bg-card [@media(pointer:coarse)]:min-h-11 text-base text-foreground ring-offset-background transition-colors file:border-0 file:bg-transparent file:text-sm file:font-medium file:text-foreground placeholder:text-placeholder focus:placeholder:text-transparent focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2 aria-invalid:border-destructive aria-invalid:ring-destructive/20 disabled:cursor-not-allowed disabled:border-transparent disabled:bg-disabled disabled:text-disabled-foreground disabled:shadow-none disabled:placeholder:text-disabled-foreground md:text-sm",
  {
    variants: {
      inputSize: {
        ...FORM_CONTROL_SIZE_CLASSES,
      },
    },
    defaultVariants: {
      inputSize: DEFAULT_FORM_CONTROL_SIZE,
    },
  },
);

export interface InputProps
  extends Omit<React.ComponentProps<"input">, "size">,
    VariantProps<typeof inputVariants> {}

const Input = React.forwardRef<HTMLInputElement, InputProps>(
  ({ className, type, inputSize, ...props }, ref) => {
    return (
      <input
        type={type}
        className={cn(
          inputVariants({ inputSize }),
          className,
        )}
        ref={ref}
        {...props}
      />
    );
  },
);
Input.displayName = "Input";

export { Input };
