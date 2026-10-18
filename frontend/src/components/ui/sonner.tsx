import { useTheme } from "next-themes";
import { Toaster as Sonner, toast } from "sonner";

type ToasterProps = React.ComponentProps<typeof Sonner>;

const Toaster = ({ ...props }: ToasterProps) => {
  const { theme = "system" } = useTheme();

  return (
    <Sonner
      theme={theme as ToasterProps["theme"]}
      className="toaster group"
      toastOptions={{
        classNames: {
          toast:
            "group toast group-[.toaster]:bg-card group-[.toaster]:text-foreground group-[.toaster]:border-border group-[.toaster]:shadow-lg",
          success: "group-[.toaster]:border-success group-[.toaster]:bg-card",
          error: "group-[.toaster]:border-destructive group-[.toaster]:bg-card",
          warning: "group-[.toaster]:border-warning group-[.toaster]:bg-card",
          info: "group-[.toaster]:border-primary group-[.toaster]:bg-card",
          description: "group-[.toast]:text-foreground",
          actionButton: "group-[.toast]:bg-primary group-[.toast]:text-primary-foreground",
          cancelButton: "group-[.toast]:bg-muted group-[.toast]:text-foreground",
        },
      }}
      {...props}
    />
  );
};

export { Toaster, toast };
