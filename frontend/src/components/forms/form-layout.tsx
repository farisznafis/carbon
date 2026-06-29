import * as React from "react";

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
      className={cn("min-h-screen bg-background px-4 py-10 sm:px-6 lg:px-8", className)}
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
      className={cn("relative min-h-screen overflow-hidden bg-background px-4 py-10 sm:px-6 lg:px-8", className)}
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
      className={cn("relative mx-auto w-full max-w-[768px] space-y-8", className)}
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
        <div className="mx-auto mb-4 flex h-12 w-12 items-center justify-center rounded-xl bg-secondary text-primary">
          {icon}
        </div>
      )}
      <h2>{title}</h2>
      {description && (
        <p className="mt-3 text-sm font-medium leading-6 text-muted-foreground">
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
      <ol className="grid grid-cols-4 gap-2">
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
                    isReached ? "text-primary" : "text-muted-foreground",
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
        "rounded-xl border border-border bg-card p-6 shadow-app-sm sm:p-10",
        className,
      )}
      {...props}
    />
  );
}

type FormSectionProps = React.HTMLAttributes<HTMLElement> & {
  number: number;
  title: string;
};

export function FormSection({
  number,
  title,
  children,
  className,
  ...props
}: FormSectionProps) {
  return (
    <section className={cn("space-y-5", className)} {...props}>
      <div className="flex items-center gap-3">
        <span className="flex h-10 w-10 items-center justify-center rounded-lg bg-secondary text-xl font-bold text-primary">
          {number}
        </span>
        <h3>{title}</h3>
      </div>
      <div className="space-y-5">{children}</div>
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
        spacing === "default" ? "space-y-10" : "space-y-12",
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
        "grid grid-cols-1 gap-5",
        columns === 2 && "md:grid-cols-2",
        className,
      )}
      {...props}
    />
  );
}

export function FormActions({
  className,
  ...props
}: React.HTMLAttributes<HTMLDivElement>) {
  return <div className={cn("pt-1", className)} {...props} />;
}
