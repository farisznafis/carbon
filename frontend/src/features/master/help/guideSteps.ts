export type GuideContext =
  | 'master-dashboard'
  | 'site-dashboard'
  | 'project-detail';

export type GuideStep = {
  title: string;
  description: string;
  mediaSrc?: string;
  mediaPoster?: string;
};

export type GuideContent = {
  title: string;
  description: string;
  steps: GuideStep[];
};

const createPlaceholderSteps = (
  pageName: string,
  totalSteps: number,
): GuideStep[] =>
  Array.from({ length: totalSteps }, (_, index) => ({
    title: `${pageName}ガイド ステップ${index + 1}`,
    description: `${pageName}で利用できる機能と操作方法の説明をここに表示します。正式なガイド文言は後から設定できます。`,
  }));

export const GUIDE_CONTENT: Record<GuideContext, GuideContent> = {
  'master-dashboard': {
    title: 'マスターダッシュボードガイド',
    description: '企業全体の拠点・ユーザー管理について案内します。',
    steps: createPlaceholderSteps('マスターダッシュボード', 8),
  },
  'site-dashboard': {
    title: '拠点ダッシュボードガイド',
    description: '拠点情報と参加プロジェクトの確認方法について案内します。',
    steps: createPlaceholderSteps('拠点ダッシュボード', 5),
  },
  'project-detail': {
    title: 'プロジェクト詳細ガイド',
    description: 'プロジェクト情報・設備・モニタリングの確認方法について案内します。',
    steps: createPlaceholderSteps('プロジェクト詳細', 6),
  },
};
