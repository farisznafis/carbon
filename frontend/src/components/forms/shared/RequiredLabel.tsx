import type { ComponentPropsWithoutRef, ReactNode } from 'react';
import { FormLabel } from '@/components/ui/form';
import { cn } from '@/lib/utils';

type RequiredLabelProps = ComponentPropsWithoutRef<typeof FormLabel> & {
  children: ReactNode;
  required?: boolean;
  requiredText?: string;
  badgeClassName?: string;
};

type RequiredBadgeProps = {
  children?: ReactNode;
  className?: string;
};

export function RequiredBadge({
  children = '*',
  className,
}: Readonly<RequiredBadgeProps>) {
  return (
    <span
      className={cn(
        'inline-flex shrink-0 items-center text-base font-semibold leading-none text-destructive',
        className,
      )}
    >
      {children}
    </span>
  );
}

export function RequiredLabel({
  children,
  required = true,
  requiredText = '*',
  className,
  badgeClassName,
  ...props
}: Readonly<RequiredLabelProps>) {
  return (
    <FormLabel
      className={cn('inline-flex max-w-full flex-wrap items-center gap-1', className)}
      {...props}
    >
      <span>{children}</span>
      {required && <RequiredBadge className={badgeClassName}>{requiredText}</RequiredBadge>}
    </FormLabel>
  );
}
