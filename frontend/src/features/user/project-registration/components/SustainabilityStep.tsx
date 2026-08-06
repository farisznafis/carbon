import { Card, CardContent } from "@/components/ui/card";
import { Checkbox } from "@/components/ui/checkbox";
import { Label } from "@/components/ui/label";

interface SustainabilityStepProps {
  sustainabilityAgreed: boolean;
  onSustainabilityAgreedChange: (agreed: boolean) => void;
}

export function SustainabilityStep({
  sustainabilityAgreed,
  onSustainabilityAgreedChange,
}: SustainabilityStepProps) {
  return (
    <div className="space-y-6">
      <div>
        <h2 className="text-2xl font-bold mb-2">
          環境社会配慮を行い持続可能性を確保することの同意書
        </h2>
        <p className="text-foreground">
          以下の内容をご確認の上、同意してください
        </p>
      </div>

      <Card>
        <CardContent className="pt-6">
          <div className="h-96 overflow-y-auto p-4 border rounded-lg bg-muted/30 space-y-4 text-sm text-foreground leading-relaxed">
            <p>
              国内における地球温暖化対策のための排出削減・吸収量認証制度（Ｊ－クレジット制度）に、
              方法論EN-R-002（太陽光発電設備の導入）に基づくプロジェクトを登録して実施するにあたり、
              人間の健康と安全、自然環境、社会への影響を回避または最小化し、
              受け入れることができないような影響をもたらすことがないよう、
              環境社会配慮を行い持続可能性を確保するため、
            </p>

            <ul className="list-disc list-inside space-y-1">
              <li>エネルギーの使用の合理化及び非化石エネルギーへの転換等に関する法律</li>
              <li>特定工場における公害防止組織の整備に関する法律</li>
              <li>環境基本法</li>
              <li>建築基準法</li>
              <li>電気事業法</li>
              <li>景観法</li>
              <li>消防法</li>
              <li>労働安全衛生法</li>
              <li>地球温暖化対策の推進に関する法律</li>
            </ul>

            <p>その他関連法令等を遵守することを誓約します。</p>

            <p className="text-right pr-4">以上</p>
          </div>

          <div className="flex items-start space-x-3 mt-6 justify-center">
            <Checkbox
              id="sustainabilityAgree"
              checked={sustainabilityAgreed}
              onCheckedChange={(checked) =>
                onSustainabilityAgreedChange(checked as boolean)
              }
            />
            <Label htmlFor="sustainabilityAgree" className="text-base cursor-pointer">
              同意する
            </Label>
          </div>
        </CardContent>
      </Card>
    </div>
  );
}