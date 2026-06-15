/**
 * Confirmation - A reusable confirmation component for forms, supporting full-page or modal display.
 *
 * This component displays a summary of form data, allowing users to review changes before confirming.
 * It can highlight changed fields, hide fields conditionally based on mode, and render either as a full page
 * or as a modal overlay.
 *
 * @template T - The type of the data object being displayed.
 *
 * @param {T} data - The current form data to display.
 * @param {T} [originalData] - Optional original data to compare against. Fields that differ from `data` are highlighted.
 * @param {FieldConfig<T>[]} fields - Array of field configurations:
 *                                      - `key`: property of the data object
 *                                      - `label`: text to display as the field label
 *                                      - `format?`: optional function to format the field value
 *                                      - `hideInModes?`: optional array of modes where the field should be hidden
 * @param {string} [mode] - Optional mode string to differentiate contexts (e.g., "profile", "admin-edit").
 * @param {boolean} [isSubmitting=false] - Whether the form is currently submitting. Disables buttons if true.
 * @param {() => void} onConfirm - Callback to handle the confirm action.
 * @param {() => void} [onBack] - Optional callback for the back/cancel action (used in full-page mode).
 * @param {string} [confirmLabel='確定'] - Label for the confirm button.
 * @param {string} [backLabel='戻る'] - Label for the back button.
 * @param {boolean} [asModal=false] - If true, renders the component as a modal overlay.
 * @param {boolean} [isOpen=false] - Only relevant when `asModal` is true. Controls modal visibility.
 * @param {() => void} [onClose] - Callback to close the modal (only used when `asModal` is true).
 *
 * @example
 * // Full Page Confirmation (Profile Edit)
 * <Confirmation
 *   data={tempUser}
 *   originalData={user}
 *   fields={[
 *     { key: 'lastName', label: '姓' },
 *     { key: 'firstName', label: '名' },
 *     { key: 'email', label: 'メールアドレス' },
 *   ]}
 *   mode="profile"
 *   onConfirm={handleConfirm}
 *   onBack={handleBack}
 * />
 *
 * @example
 * // Modal Confirmation
 * const [isModalOpen, setModalOpen] = useState(false);
 * 
 * <Confirmation
 *   asModal
 *   isOpen={isModalOpen}
 *   onClose={() => setModalOpen(false)}
 *   data={tempUser}
 *   originalData={user}
 *   fields={[
 *     { key: 'lastName', label: '姓' },
 *     { key: 'firstName', label: '名' },
 *     { key: 'email', label: 'メールアドレス' },
 *   ]}
 *   mode="profile"
 *   onConfirm={handleConfirm}
 * />
 * 
 * <Button onClick={() => setModalOpen(true)}>確認する</Button>
 */


import React from 'react';
import { Button } from "@/components/ui/button";
import { ArrowLeft, X } from 'lucide-react';
import  styles from "./Confirmation.module.css"

interface ConfirmationField {
  key: string;
  label: string;
  hideInModes?: string[];
  formatValue?: (
    value: React.ReactNode,
    data?: Record<string, React.ReactNode>,
  ) => React.ReactNode;
}

interface ConfirmationProps {
  data: Record<string, React.ReactNode>;
  originalData?: Record<string, React.ReactNode>;
  fields: ConfirmationField[];
  mode?: string;
  isSubmitting?: boolean;
  onConfirm: () => void;
  onBack: () => void;
  asModal?: boolean;
  isOpen?: boolean;
  onClose?: () => void;
}

export const Confirmation: React.FC<ConfirmationProps> = ({
  data,
  originalData,
  fields,
  mode,
  isSubmitting = false,
  onConfirm,
  onBack,
  asModal = false,
  isOpen = false,
  onClose,
}) => {
  // If modal, don't render when closed
  if (asModal && !isOpen) return null;

  //if no data, return null
  if (!data) return null;

  const handleBack = () => {
    if (asModal && onClose) {
      onClose();
    } else {
      onBack();
    }
  };

  return (
    <div className={asModal ? `${styles.backdrop}` : "min-h-screen py-8 px-4"}>
      <div
        className={[
          "bg-white shadow-lg w-full p-6",
          asModal ? `${styles.content}` : "max-w-lg mx-auto rounded-xl",
        ].join(" ")}
      >
        <h2 className="text-2xl font-bold text-center mb-6">
          {mode ? `${mode} confirmation` : "Confirm details"}
        </h2>

        <div className="space-y-4">
          {fields.map((field) => {
            if (field.hideInModes?.includes(mode || "")) return null;

            const isChanged = originalData //if there is originalData, checks to see changes
              ? data[field.key] !== originalData[field.key]
              : false;

            return (
              <div key={field.key}>
                <div className="text-sm font-medium text-muted-foreground mb-1">
                  {field.label}
                </div>
                <div className={isChanged ? "font-bold underline" : "font-bold"}>
                  {field.formatValue
                    ? field.formatValue(data[field.key], data)
                    : data[field.key]}
                </div>
              </div>
            );
          })}
        </div>

        <div className="flex flex-col sm:flex-row gap-2 mt-6 justify-end">
          <Button
            variant="outline"
            onClick={handleBack}
            className="w-full sm:w-auto"
            disabled={isSubmitting}
          >
            <ArrowLeft className="mr-2 h-4 w-4" />
            戻る
          </Button>
          <Button
            onClick={onConfirm}
            className="w-full sm:w-auto"
            disabled={isSubmitting}
          >
            {isSubmitting ? "確定中..." : "確定"}
          </Button>
        </div>
      </div>
    </div>
  );
};

