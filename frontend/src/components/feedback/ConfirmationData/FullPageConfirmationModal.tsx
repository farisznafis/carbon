import { Dialog, Transition } from "@headlessui/react";
import { Fragment } from "react";

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
        <div className="fixed inset-0 bg-black/30" />

        <div className="fixed inset-0 flex items-center justify-center">
          <Dialog.Panel className="w-full max-w-3xl rounded bg-white p-6 shadow-xl">
            <Dialog.Title className="text-lg font-medium text-gray-900">
              {title}
            </Dialog.Title>

            {description && (
              <p className="mt-2 text-sm text-gray-500">{description}</p>
            )}

            <dl className="mt-6 divide-y divide-gray-200">
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
                    <dt className="text-sm font-medium text-gray-500">
                      {key}
                    </dt>
                    <dd
                      className={
                        "mt-1 text-sm text-gray-900 sm:mt-0 sm:col-span-2" +
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
              <button
                type="button"
                onClick={onBack}
                className="rounded-md border border-gray-300 px-4 py-2 text-sm font-medium text-gray-700 hover:bg-gray-50"
              >
                Back
              </button>
              <button
                type="button"
                onClick={onConfirm}
                className="rounded-md bg-indigo-600 px-4 py-2 text-sm font-medium text-white hover:bg-indigo-700"
              >
                Confirm
              </button>
            </div>
          </Dialog.Panel>
        </div>
      </Dialog>
    </Transition.Root>
  );
}