import * as React from 'react';
import { Slot } from '@radix-ui/react-slot';
import { cva, type VariantProps } from 'class-variance-authority';

import { cn } from '@/lib/utils';

const buttonVariants = cva(
  'inline-flex items-center justify-center gap-2 whitespace-nowrap rounded-lg text-sm font-medium ring-offset-background transition-colors focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2 disabled:pointer-events-none disabled:border-transparent disabled:bg-disabled disabled:text-disabled-foreground [&_svg]:pointer-events-none [&_svg]:size-4 [&_svg]:shrink-0 disabled:bg-none disabled:shadow-none',
  {
    variants: {
      variant: {
        default:
          'bg-primary text-primary-foreground hover:bg-primary-strong',

        primary:
          'bg-primary text-primary-foreground hover:bg-primary-strong',

        success:
          'bg-success text-success-foreground hover:bg-success-strong',

        destructive:
          'bg-destructive text-destructive-foreground hover:bg-destructive-strong',

        // A reject/decline choice shown next to the primary action; the solid variant confirms it.
        'destructive-outline':
          'border border-destructive-border bg-card text-destructive-strong hover:bg-destructive-subtle',

        secondary:
          'bg-foreground text-background shadow-app-sm hover:bg-foreground/90',

        outline:
          'border border-input bg-card text-foreground hover:border-primary/50 hover:bg-accent hover:text-accent-foreground',

        subtle:
          'border border-input bg-card text-foreground hover:bg-muted',

        cancel:
          'border border-input bg-card text-foreground hover:bg-muted',

        ghost:
          'bg-transparent text-muted-foreground hover:bg-muted hover:text-foreground',

        link: 'text-primary-strong underline-offset-4 hover:underline',
      },
      size: {
        // Touch screens get 44px targets (WCAG 2.5.8 / Apple HIG); mouse layouts keep their density.
        default: 'h-10 px-5 py-2 text-sm [@media(pointer:coarse)]:h-11',
        md: 'h-10 px-5 py-2 text-sm [@media(pointer:coarse)]:h-11',
        sm: 'h-9 px-3 text-sm [@media(pointer:coarse)]:h-11',
        lg: 'h-12 px-6 text-base',
        icon: 'h-10 w-10 p-0 [@media(pointer:coarse)]:h-11 [@media(pointer:coarse)]:w-11',
      },
    },
    defaultVariants: {
      variant: 'default',
      size: 'default',
    },
  },
);

export interface ButtonProps
  extends
    React.ButtonHTMLAttributes<HTMLButtonElement>,
    VariantProps<typeof buttonVariants> {
  asChild?: boolean;
}

const Button = React.forwardRef<HTMLButtonElement, ButtonProps>(
  ({ className, variant, size, asChild = false, ...props }, ref) => {
    const Comp = asChild ? Slot : 'button';
    return (
      <Comp
        className={cn(buttonVariants({ variant, size, className }))}
        ref={ref}
        {...props}
      />
    );
  },
);
Button.displayName = 'Button';

export { Button, buttonVariants };
