/**
 * ConfirmationLayout
 *
 * Standardized page layout for confirmation / review screens
 * shown before final submission.
 *
 * This component is responsible ONLY for:
 * - Page structure and visual consistency
 * - Standard confirmation copy placement
 * - Back / Confirm action positioning
 *
 * This component is intentionally NOT responsible for:
 * - Form state
 * - Validation
 * - Field comparison logic
 * - Data fetching or mutation
 *
 * ---
 *
 * Design principles:
 * - Confirmation pages should feel calm, predictable, and safe
 * - Visual hierarchy must emphasize "review" over "editing"
 * - Users should clearly understand how to go back and fix mistakes
 *
 * ---
 *
 * Usage guidelines:
 * - Use this component for ANY screen where users review data
 *   before final submission.
 * - Compose field rows using <ConfirmationItem />.
 * - Do NOT add conditional logic inside this component.
 * - Do NOT pass raw form schemas or field names.
 *
 * ---
 *
 * Example:
 *
 * <ConfirmationLayout
 *   pageName="社員情報の確認"
 *   title="入力内容"
 *   description="以下の内容をご確認ください。"
 *   onBack={handleBack}
 *   onConfirm={handleConfirm}
 *   isSubmitting={isSubmitting}
 * >
 *   <ConfirmationItem
 *     label="姓"
 *     value={data.lastName}
 *     originalValue={originalData.lastName}
 *   />
 * </ConfirmationLayout>
 */

import { ArrowLeft } from "lucide-react";
import { FormActions } from '@/components/forms/form-layout';
import { FormPageWrapper } from "@/components/layout/FormPageWrapper/FormPageWrapper";

interface ConfirmationLayoutProps {
  pageName: string;
  title: string;
  description: string;
  onBack: () => void;
  backText?: string;
  onConfirm: () => void;
  isSubmitting?: boolean;
  children: React.ReactNode;
}

export const ConfirmationLayout = ({
  pageName,
  title,
  description,
  onBack,
  backText = "戻る",
  onConfirm,
  isSubmitting,
  children,
}: ConfirmationLayoutProps) => {
  return (
    <FormPageWrapper
      pageName={pageName}
      title={title}
      description={description}
      onBack={onBack}
      backText={backText}
    >
      <div className="space-y-4">{children}</div>

      <FormActions
        className="pt-6"
        disabled={isSubmitting}
        pendingActionId={isSubmitting ? 'confirm' : null}
        actions={[
          {
            id: 'back',
            label: (
              <>
                <ArrowLeft className="h-4 w-4" />
                戻る
              </>
            ),
            variant: 'outline',
            onClick: onBack,
          },
          {
            id: 'confirm',
            label: '確定',
            pendingLabel: '確定中...',
            onClick: onConfirm,
          },
        ]}
      />
    </FormPageWrapper>
  );
};
