import { FormActions, FormCard } from "@/components/forms/form-layout";
import {
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from "@/components/ui/card";
import { CheckCircle2 } from "lucide-react";

interface CompletionStepProps {
  readonly onNavigateToDashboard: () => void;
}

export function CompletionStep({ onNavigateToDashboard }: CompletionStepProps) {
  return (
    <FormCard className="mx-auto w-full max-w-lg">
      <CardHeader className="px-0 pt-0">
        <CheckCircle2
          className="mb-2 h-9 w-9 text-primary"
          aria-hidden="true"
        />
        <CardTitle>申請完了</CardTitle>
        <CardDescription className="leading-6">
          プロジェクト参加申請が完了しました。審査結果は後日メールにてお知らせいたします。
        </CardDescription>
      </CardHeader>
      {/* <CardContent className="px-0 pb-0">
        <FormActions
          actions={[
            {
              id: "dashboard",
              label: "ダッシュボードに戻る",
              onClick: onNavigateToDashboard,
            },
          ]}
        />
      </CardContent> */}
    </FormCard>
  );
}