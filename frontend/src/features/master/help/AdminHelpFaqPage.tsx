import { Link } from 'react-router-dom';
import { LifeBuoy } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { StatusBadge } from '@/components/data-display/StatusBadge';
import {
  Accordion,
  AccordionContent,
  AccordionItem,
  AccordionTrigger,
} from '@/components/ui/accordion';
import { usePageBreadcrumb } from '@/components/navigation/pageBreadcrumbContext';
import { useAuth } from '@/contexts/AuthContext';
import { paths } from '@/routes/paths';
import { getHomePath } from '@/utils/getHomePath';

// ─── Types ────────────────────────────────────────────────────────────────────

type FaqItem = {
  question: string;
  answer: string;
};

type FaqSection = {
  category: string;
  items: FaqItem[];
};

// ─── Data ─────────────────────────────────────────────────────────────────────

const FAQ_SECTIONS: FaqSection[] = [
  {
    category: 'アカウント・ログイン',
    items: [
      {
        question: 'パスワードを忘れた場合はどうすればいいですか？',
        answer:
          'ログイン画面の「パスワードを忘れた方はこちら」から再設定メールを送信できます。登録済みのメールアドレスを入力してください。',
      },
      {
        question: 'アカウントがロックされてしまいました。',
        answer:
          'ログイン失敗が一定回数を超えると自動的にロックされます。しばらく待つか、管理者にロック解除を依頼してください。',
      },
    ],
  },
  {
    category: '企業管理',
    items: [
      {
        question: '企業情報を編集するにはどうすればいいですか？',
        answer:
          '企業詳細ページ右上の「編集」ボタンをクリックすると編集フォームが開きます。変更後に「保存」を押してください。',
      },
      {
        question: '企業アカウントを停止する手順を教えてください。',
        answer:
          '企業詳細ページの「アカウントを停止する」ボタンをクリックし、確認ダイアログで停止理由を入力した後に「停止する」を押してください。',
      },
      {
        question: '停止した企業アカウントを再有効化できますか？',
        answer:
          '停止中の企業詳細ページから「アカウントを有効化する」ボタンをクリックし、停止理由を選択して解除してください。',
      },
    ],
  },
  {
    category: '申請・届出',
    items: [
      {
        question: '入会届の審査はどのくらい時間がかかりますか？',
        answer:
          '通常、受付から 3〜5 営業日以内に審査結果をメールでご連絡します。',
      },
      {
        question: '申請が「保留中」のまま変わりません。',
        answer:
          '追加書類が必要な場合や担当者が確認中の場合があります。ダッシュボードのコメント欄を確認するか、サポートまでお問い合わせください。',
      },
    ],
  },
  {
    category: 'システム・操作',
    items: [
      {
        question: 'データが正しく表示されません。',
        answer:
          'ブラウザのキャッシュをクリアするか、ページをリロードしてみてください。それでも解決しない場合はサポートにご連絡ください。',
      },
      {
        question: '推奨ブラウザを教えてください。',
        answer:
          'Google Chrome（最新版）、Microsoft Edge（最新版）、Safari（最新版）を推奨しています。Internet Explorer は非対応です。',
      },
    ],
  },
];

export default function AdminHelpFaqPage() {
  const { user } = useAuth();

  usePageBreadcrumb([
    { label: 'ホーム', to: getHomePath(user) },
    { label: 'ヘルプ・FAQ' },
  ]);

  return (
    <main className="min-h-screen bg-background px-4 py-8 sm:px-6">
      <div className="mx-auto w-full max-w-3xl space-y-6">

        {/* ── Page header ─────────────────────────────────────────────── */}
        <div className="space-y-3">
          <div className="flex items-center gap-3">
            <LifeBuoy className="h-6 w-6 text-muted-foreground" />
            <div>
              <p className="text-sm text-foreground tracking-wide">
                サポート
              </p>
              <h1 className="text-2xl font-bold tracking-tight text-foreground">
                ヘルプ・FAQ
              </h1>
            </div>
          </div>

          <p className="text-sm text-foreground max-w-xl">
            よくあるご質問と操作方法をまとめています。
            お探しの情報が見つからない場合はサポートまでお問い合わせください。
          </p>
        </div>

        {/* ── Contact banner ───────────────────────────────────────────── */}
        <Card className="bg-muted/50 border-muted">
          <CardContent className="flex flex-col sm:flex-row items-start sm:items-center justify-between gap-3 py-4">
            <div>
              <p className="text-sm font-medium text-foreground">
                解決しない場合はサポートへ
              </p>
              <p className="text-xs text-foreground mt-0.5">
                平日 9:00〜18:00 対応 / 返信は翌営業日以降になる場合があります
              </p>
            </div>
            <Button asChild size="sm" variant="outline">
              <Link to={paths.contact}>お問い合わせ</Link>
            </Button>
          </CardContent>
        </Card>

        {/* ── FAQ sections ─────────────────────────────────────────────── */}
        {FAQ_SECTIONS.map((section) => (
          <Card key={section.category}>
            <CardHeader className="pb-2">
              <CardTitle className="flex items-center gap-2 text-base font-bold">
                {section.category}
                <StatusBadge className="font-normal">
                  {section.items.length}件
                </StatusBadge>
              </CardTitle>
            </CardHeader>
            <CardContent>
              <Accordion type="single" collapsible>
                {section.items.map((item) => (
                  <AccordionItem key={item.question} value={item.question}>
                    <AccordionTrigger className="text-left text-sm hover:no-underline">
                      {item.question}
                    </AccordionTrigger>
                    <AccordionContent className="text-sm leading-relaxed text-foreground">
                      {item.answer}
                    </AccordionContent>
                  </AccordionItem>
                ))}
              </Accordion>
            </CardContent>
          </Card>
        ))}

      </div>
    </main>
  );
}
