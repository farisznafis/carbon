import * as React from 'react';
import { Slot } from '@radix-ui/react-slot';
import { cva, type VariantProps } from 'class-variance-authority';

import { cn } from '@/lib/utils';

const buttonVariants = cva(
  'inline-flex items-center justify-center gap-2 whitespace-nowrap rounded-full text-sm font-medium ring-offset-background transition-colors focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2 disabled:pointer-events-none disabled:border-transparent disabled:bg-disabled disabled:text-disabled-foreground [&_svg]:pointer-events-none [&_svg]:size-4 [&_svg]:shrink-0 disabled:bg-none disabled:shadow-none',
  {
    variants: {
      variant: {
        default:
          'bg-gradient-to-b from-primary to-primary-strong text-primary-foreground shadow-app-sm hover:brightness-[0.98]',

        primary:
          'bg-gradient-to-b from-primary to-primary-strong text-primary-foreground shadow-app-sm hover:brightness-[0.98]',

        success:
          'bg-success text-success-foreground shadow-app-sm hover:bg-success-strong',

        destructive:
          'bg-destructive text-destructive-foreground shadow-app-sm hover:bg-destructive-strong',

        secondary:
          'bg-foreground text-background shadow-app-sm hover:bg-foreground/90',

        outline:
          'border border-border bg-card text-foreground shadow-app-sm hover:border-primary/50 hover:bg-accent hover:text-accent-foreground',

        subtle:
          'border border-border bg-card text-foreground shadow-app-sm hover:bg-muted',

        cancel:
          'border border-border bg-card text-foreground shadow-app-sm hover:bg-muted',

        ghost:
          'bg-transparent text-muted-foreground hover:bg-muted hover:text-foreground',

        link: 'text-primary-strong underline-offset-4 hover:underline',
      },
      size: {
        default: 'h-9 px-5 py-2 text-sm',
        md: 'h-10 px-5 py-2 text-sm font-normal leading-6',
        sm: 'h-8 px-3 text-xs',
        lg: 'h-12 px-6 text-base',
        icon: 'h-9 w-9 p-0',
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
