export type GuideStep = {
  title: string;
  description: string;
};

export const GUIDE_STEPS: GuideStep[] = [
  {
    title: 'ダッシュボードの見方',
    description:
      'ダッシュボードでは、企業ごとのステータスや最新の申請状況を一覧で確認できます。',
  },
  {
    title: '申請状況の確認',
    description:
      '各企業の申請ステータスをリアルタイムで確認できます。フィルターを使うと素早く絞り込めます。',
  },
  {
    title: '入会届の提出',
    description:
      '新規企業の入会届を提出する手順を説明します。必要な書類と入力項目を確認してください。',
  },
  {
    title: '設備情報の追加',
    description:
      '企業に紐づく設備情報（拠点・機器など）を追加・編集する方法を説明します。',
  },
  {
    title: 'ヘルプ・FAQの使い方',
    description:
      'よくある質問やトラブルシューティングはヘルプ・FAQページからいつでも参照できます。',
  },
];
