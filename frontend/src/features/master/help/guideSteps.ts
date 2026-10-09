import { matchPath } from 'react-router-dom';
import { paths } from '@/routes/paths';

export type GuideContext =
  | 'master-dashboard'
  | 'site-dashboard'
  | 'project-detail';

export type GuideStep = {
  /** Matches a `data-tour` attribute; steps whose target is not on screen are skipped. */
  target: string;
  title: string;
  description: string;
  /** Where the explanation sits relative to the highlighted element. */
  side?: 'top' | 'right' | 'bottom' | 'left';
};

export type GuideContent = {
  title: string;
  steps: GuideStep[];
};

const ACCOUNT_MENU_STEP: GuideStep = {
  target: 'account-menu',
  title: 'アカウント',
  description: 'プロファイルの確認やパスワード変更、ログアウトはここから行います。',
  side: 'bottom',
};

const HELP_MENU_STEP: GuideStep = {
  target: 'help-menu',
  title: 'ヘルプ・ガイド',
  description: 'このガイドはここからいつでも再表示できます。よくある質問やお問い合わせもこちらです。',
  side: 'right',
};

export const GUIDE_CONTENT: Record<GuideContext, GuideContent> = {
  'master-dashboard': {
    title: 'マスターダッシュボード',
    steps: [
      {
        target: 'nav-home',
        title: 'ホーム',
        description: 'ダッシュボードのトップです。どの画面からでもここに戻れます。',
        side: 'right',
      },
      {
        target: 'nav-analytics',
        title: 'アナリティクス',
        description: '企業全体の排出量や削減の状況を確認します。',
        side: 'right',
      },
      {
        target: 'nav-membership',
        title: 'プロジェクトの入会届',
        description: '公開中のプロジェクトを確認し、拠点ごとに入会の手続きを進めます。',
        side: 'right',
      },
      {
        target: 'nav-sites',
        title: '拠点',
        description: '拠点の追加・編集と、各拠点のダッシュボードの確認を行います。',
        side: 'right',
      },
      {
        target: 'nav-users',
        title: '社員',
        description: '社員アカウントの招待や編集、パスワードの変更を行います。',
        side: 'right',
      },
      ACCOUNT_MENU_STEP,
      HELP_MENU_STEP,
    ],
  },
  'site-dashboard': {
    title: '拠点ダッシュボード',
    steps: [
      {
        target: 'site-header',
        title: '拠点情報',
        description: '拠点名・担当者・所在地を確認できます。',
      },
      {
        target: 'site-edit',
        title: '拠点情報を更新',
        description: '拠点の名称や所在地、担当者を変更します。',
      },
      {
        target: 'joined-projects',
        title: '入会中のプロジェクト',
        description: 'カードを選ぶと、プロジェクトの進捗や排出量の詳細を確認できます。',
      },
      {
        target: 'available-projects',
        title: '未入会プロジェクト',
        description: 'この拠点がまだ入会していないプロジェクトです。',
      },
      {
        target: 'join-project',
        title: 'プロジェクトに入会する',
        description: '入会するプロジェクトを選び、申請の手続きを始めます。',
      },
      ACCOUNT_MENU_STEP,
      HELP_MENU_STEP,
    ],
  },
  'project-detail': {
    title: 'プロジェクト詳細',
    steps: [
      {
        target: 'project-header',
        title: 'プロジェクト情報',
        description: 'ステータスや方法論など、プロジェクトの基本情報です。',
      },
      {
        target: 'project-monitoring',
        title: 'モニタリングデータ',
        description: 'モニタリングデータの登録はここから行います。',
      },
      {
        target: 'project-tabs',
        title: 'タブ',
        description: '排出量の分析、登録済みの設備、モニタリングデータを切り替えて確認します。誤ったモニタリングデータの取消申請は「モニタリングデータ」から行います。',
      },
      {
        target: 'project-emissions',
        title: '排出量分析',
        description: '排出量の推移と内訳から、プロジェクトの状況を確認できます。',
        side: 'top',
      },
      HELP_MENU_STEP,
    ],
  },
};

const GUIDE_CONTEXT_ROUTES: ReadonlyArray<{
  path: string;
  context: GuideContext;
}> = [
  { path: paths.masterDashboard, context: 'master-dashboard' },
  { path: paths.userDashboard, context: 'site-dashboard' },
  { path: paths.siteDashboardRoute, context: 'site-dashboard' },
  { path: paths.siteProjectDetailRoute, context: 'project-detail' },
  { path: paths.legacyProjectRoute, context: 'project-detail' },
];

/** Resolves which page guide applies to the current route, if any. */
export function getGuideContextForPath(pathname: string): GuideContext | null {
  return (
    GUIDE_CONTEXT_ROUTES.find(({ path }) =>
      Boolean(matchPath({ path, end: true }, pathname)),
    )?.context ?? null
  );
}
