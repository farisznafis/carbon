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
  children = '必須',
  className,
}: Readonly<RequiredBadgeProps>) {
  return (
    <span
      className={cn(
        'inline-flex shrink-0 items-center rounded bg-destructive px-2 py-1 text-xs font-semibold leading-none text-destructive-foreground',
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
  requiredText = '必須',
  className,
  badgeClassName,
  ...props
}: Readonly<RequiredLabelProps>) {
  return (
    <FormLabel
      className={cn('flex w-full items-center justify-between gap-2', className)}
      {...props}
    >
      <span>{children}</span>
      {required && <RequiredBadge className={badgeClassName}>{requiredText}</RequiredBadge>}
    </FormLabel>
  );
}
