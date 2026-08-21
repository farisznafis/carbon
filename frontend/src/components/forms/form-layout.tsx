import * as React from "react";

import { ButtonLoader } from '@/components/feedback/loading';
import { Button, type ButtonProps } from "@/components/ui/button";
import { cn } from "@/lib/utils";

type FormPageShellProps = React.HTMLAttributes<HTMLDivElement> & {
  contentClassName?: string;
};

export function FormPageShell({
  children,
  className,
  contentClassName,
  ...props
}: FormPageShellProps) {
  return (
    <main
      className={cn("min-h-screen bg-background px-4 py-8 sm:px-6 lg:px-8", className)}
      {...props}
    >
      <div className={cn("mx-auto w-full max-w-[768px]", contentClassName)}>
        {children}
      </div>
    </main>
  );
}

type FormStandalonePageProps = React.HTMLAttributes<HTMLElement> & {
  watermark?: string;
};

export function FormStandalonePage({
  children,
  className,
  watermark,
  ...props
}: FormStandalonePageProps) {
  return (
    <main
      className={cn("relative min-h-screen overflow-hidden bg-background px-4 py-8 sm:px-6 lg:px-8", className)}
      {...props}
    >
      {watermark && (
        <div
          aria-hidden="true"
          className="pointer-events-none absolute -left-2 top-12 hidden select-none text-[150px] font-light leading-none text-muted/35 sm:block lg:text-[180px]"
        >
          {watermark}
        </div>
      )}
      {children}
    </main>
  );
}

export function FormPageContent({
  className,
  ...props
}: React.HTMLAttributes<HTMLDivElement>) {
  return (
    <div
      className={cn("relative mx-auto w-full max-w-[768px] space-y-6", className)}
      {...props}
    />
  );
}

type FormPageTitleProps = React.HTMLAttributes<HTMLHeadingElement> & {
  as?: "h1" | "h2";
  align?: "left" | "center";
};

export function FormPageTitle({
  as: Component = "h2",
  align = "left",
  className,
  ...props
}: FormPageTitleProps) {
  return (
    <Component
      className={cn(
        "text-foreground",
        align === "center" ? "text-center" : "text-left",
        className,
      )}
      {...props}
    />
  );
}

type FormPageHeaderProps = React.HTMLAttributes<HTMLDivElement> & {
  icon?: React.ReactNode;
  title: string;
  description?: string;
};

export function FormPageHeader({
  icon,
  title,
  description,
  className,
  ...props
}: FormPageHeaderProps) {
  return (
    <header className={cn("text-center", className)} {...props}>
      {icon && (
        <div className="mx-auto mb-3 flex h-10 w-10 items-center justify-center rounded-lg bg-secondary text-primary">
          {icon}
        </div>
      )}
      <h2>{title}</h2>
      {description && (
        <p className="mt-2 text-sm font-medium leading-5 text-foreground">
          {description}
        </p>
      )}
    </header>
  );
}

export type WizardProgressStep = {
  number: number;
  title: string;
};

type WizardProgressProps = React.HTMLAttributes<HTMLDivElement> & {
  currentStep: number;
  steps: WizardProgressStep[];
};

export function WizardProgress({
  currentStep,
  steps,
  className,
  ...props
}: WizardProgressProps) {
  return (
    <nav aria-label="Progress" className={cn("w-full", className)} {...props}>
      <ol
        className="grid gap-2"
        style={{ gridTemplateColumns: `repeat(${steps.length}, minmax(0, 1fr))` }}
      >
        {steps.map((step) => {
          const isComplete = currentStep > step.number;
          const isActive = currentStep === step.number;
          const isReached = currentStep >= step.number;

          return (
            <li key={step.number} className="min-w-0">
              <div
                className={cn(
                  "h-1.5 rounded-full transition-colors",
                  isReached ? "bg-primary" : "bg-muted",
                )}
              />
              <div className="mt-2 flex min-w-0 flex-col items-center gap-1 text-center">
                <span
                  className={cn(
                    "relative flex h-4 w-4 items-center justify-center rounded-full",
                    isActive && "bg-primary/20",
                    isComplete && "bg-primary/20",
                    !isReached && "bg-muted",
                  )}
                >
                  <span
                    className={cn(
                      "h-2 w-2 rounded-full",
                      isActive && "bg-primary",
                      isComplete && "bg-primary",
                      !isReached && "bg-muted-foreground/40",
                    )}
                  />
                </span>
                <span
                  className={cn(
                    "truncate text-xs font-medium",
                    isReached ? "text-primary" : "text-foreground",
                  )}
                >
                  {step.title}
                </span>
              </div>
            </li>
          );
        })}
      </ol>
    </nav>
  );
}

export function FormCard({
  className,
  ...props
}: React.HTMLAttributes<HTMLDivElement>) {
  return (
    <div
      className={cn(
        "rounded-xl border border-border bg-card p-5 shadow-app-sm sm:p-8",
        className,
      )}
      {...props}
    />
  );
}

type FormSectionProps = Omit<React.HTMLAttributes<HTMLElement>, 'title'> & {
  number: number;
  title: React.ReactNode;
};

export function FormSection({
  number,
  title,
  children,
  className,
  ...props
}: FormSectionProps) {
  return (
    <section className={cn("space-y-4", className)} {...props}>
      <div className="flex items-center gap-3">
        <span className="flex h-10 w-10 items-center justify-center rounded-lg bg-secondary text-xl font-bold text-primary">
          {number}
        </span>
        <h3>{title}</h3>
      </div>
      <div className="space-y-6">{children}</div>
    </section>
  );
}

type FormSectionStackProps = React.HTMLAttributes<HTMLDivElement> & {
  spacing?: "default" | "relaxed";
};

export function FormSectionStack({
  spacing = "default",
  className,
  ...props
}: FormSectionStackProps) {
  return (
    <div
      className={cn(
        spacing === "default" ? "space-y-8" : "space-y-10",
        className,
      )}
      {...props}
    />
  );
}

type FormGridProps = React.HTMLAttributes<HTMLDivElement> & {
  columns?: 1 | 2;
};

export function FormGrid({
  columns = 2,
  className,
  ...props
}: FormGridProps) {
  return (
    <div
      className={cn(
        "grid grid-cols-1 gap-4",
        columns === 2 && "md:grid-cols-2",
        className,
      )}
      {...props}
    />
  );
}

export type FormActionContext = "standard" | "auth" | "compact";

export type FormActionDefinition = Omit<
  ButtonProps,
  "asChild" | "children" | "className" | "size"
> & {
  id: string;
  label: React.ReactNode;
  pendingLabel?: React.ReactNode;
};

type FormActionsProps = Omit<React.HTMLAttributes<HTMLDivElement>, "children"> & {
  actions: readonly FormActionDefinition[];
  context?: FormActionContext;
  disabled?: boolean;
  leadingAction?: React.ReactNode;
  pendingActionId?: string | null;
};

const FORM_ACTION_BUTTON_SIZE: Record<
  FormActionContext,
  NonNullable<ButtonProps["size"]>
> = {
  standard: "md",
  auth: "lg",
  compact: "sm",
};

const STANDARD_ACTION_GRID_CLASS: Record<number, string> = {
  1: "grid grid-cols-1 gap-3",
  2: "grid grid-cols-1 gap-3 sm:grid-cols-2",
  3: "grid grid-cols-1 gap-3 sm:grid-cols-3",
};

/**
 * Resolves the responsive action layout without generating dynamic Tailwind classes.
 */
function getFormActionsLayoutClass(
  actionCount: number,
  hasLeadingAction: boolean,
) {
  if (hasLeadingAction) {
    if (actionCount === 1) {
      return "grid grid-cols-[2.5rem_minmax(0,1fr)] gap-3";
    }

    return "grid grid-cols-[2.5rem_minmax(0,1fr)_minmax(0,1fr)] gap-3";
  }

  return STANDARD_ACTION_GRID_CLASS[Math.min(Math.max(actionCount, 1), 3)];
}

/**
 * Renders consistent form commit actions for standard pages, auth cards, and dialogs.
 */
export function FormActions({
  actions,
  className,
  context = "standard",
  disabled = false,
  leadingAction,
  pendingActionId = null,
  ...props
}: FormActionsProps) {
  const isPending = pendingActionId !== null;
  const buttonSize = FORM_ACTION_BUTTON_SIZE[context];
  const buttonWidthClass = "w-full";
  const authMotionClass =
    context === "auth"
      ? "transition-transform duration-300 hover:scale-[1.01] active:scale-[0.99] [@media(max-height:560px)]:h-9"
      : undefined;

  return (
    <div
      className={cn(
        "pt-0",
        getFormActionsLayoutClass(
          actions.length,
          Boolean(leadingAction),
        ),
        className,
      )}
      aria-busy={isPending || undefined}
      {...props}
    >
      {leadingAction ? (
        <fieldset
          className="contents"
          disabled={disabled || isPending}
          aria-label="補助アクション"
        >
          {leadingAction}
        </fieldset>
      ) : null}
      {actions.map(
        ({ id, label, pendingLabel, disabled: actionDisabled, ...buttonProps }) => {
          const isCurrentActionPending = pendingActionId === id;

          return (
            <Button
              key={id}
              type="button"
              size={buttonSize}
              className={cn(buttonWidthClass, authMotionClass)}
              disabled={disabled || isPending || actionDisabled}
              aria-busy={isCurrentActionPending || undefined}
              {...buttonProps}
            >
              {isCurrentActionPending ? <ButtonLoader /> : null}
              <span>{isCurrentActionPending ? (pendingLabel ?? label) : label}</span>
            </Button>
          );
        },
      )}
    </div>
  );
}
