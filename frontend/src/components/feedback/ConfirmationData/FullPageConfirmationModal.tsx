import { Dialog, Transition } from "@headlessui/react";
import { Fragment } from "react";
import { Button } from "@/components/ui/button";

type FullPageConfirmationModalProps = {
  open: boolean;
  title: string;
  description?: string;
  data: Record<string, React.ReactNode>;
  originalData?: Record<string, React.ReactNode>;
  onBack: () => void;
  onConfirm: () => void;
};

export function FullPageConfirmationModal({
  open,
  title,
  description,
  data,
  originalData,
  onBack,
  onConfirm,
}: FullPageConfirmationModalProps) {
  return (
    <Transition.Root show={open} as={Fragment}>
      <Dialog as="div" className="relative z-50" onClose={onBack}>
        <div className="fixed inset-0 bg-foreground/30" />

        <div className="fixed inset-0 flex items-center justify-center">
          <Dialog.Panel className="w-full max-w-3xl rounded-xl bg-card p-6 text-card-foreground shadow-app-md">
            <Dialog.Title className="text-lg font-medium text-foreground">
              {title}
            </Dialog.Title>

            {description && (
              <p className="mt-2 text-sm text-muted-foreground">{description}</p>
            )}

            <dl className="mt-6 divide-y divide-border">
              {Object.entries(data).map(([key, value]) => {
                const originalValue = originalData?.[key];

                const isChanged =
                  originalData !== undefined &&
                  originalValue !== undefined &&
                  originalValue !== value;

                return (
                  <div
                    key={key}
                    className="py-3 sm:grid sm:grid-cols-3 sm:gap-4"
                  >
                    <dt className="text-sm font-medium text-muted-foreground">
                      {key}
                    </dt>
                    <dd
                      className={
                        "mt-1 text-sm text-foreground sm:mt-0 sm:col-span-2" +
                        (isChanged ? " underline" : "")
                      }
                    >
                      {value}
                    </dd>
                  </div>
                );
              })}
            </dl>

            <div className="mt-6 flex justify-end gap-3">
              <Button
                type="button"
                variant="outline"
                onClick={onBack}
              >
                Back
              </Button>
              <Button
                type="button"
                onClick={onConfirm}
              >
                Confirm
              </Button>
            </div>
          </Dialog.Panel>
        </div>
      </Dialog>
    </Transition.Root>
  );
}
