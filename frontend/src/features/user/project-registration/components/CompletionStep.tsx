import { Button } from "@/components/ui/button";
import { CheckCircle } from "lucide-react";

interface CompletionStepProps {
  readonly onNavigateToDashboard: () => void;
}

export function CompletionStep({ onNavigateToDashboard }: CompletionStepProps) {
  return (
    <div className="space-y-6 text-center py-12">
      <div className="flex justify-center">
        <div className="rounded-full bg-success/10 p-6">
          <CheckCircle className="h-16 w-16 text-success" />
        </div>
      </div>

      <div>
        <h2 className="text-3xl font-bold mb-4">申請完了</h2>
        <p className="text-lg text-foreground mb-2">プロジェクト参加申請が完了しました</p>
        <p className="text-foreground">審査結果は後日メールにてお知らせいたします</p>
      </div>

      <div className="pt-8">
        <Button onClick={onNavigateToDashboard} size="lg">
          ダッシュボードに戻る
        </Button>
      </div>
    </div>
  );
}
