import type { LucideIcon } from 'lucide-react';
import {
  BarChart3,
  BellRing,
  ClipboardCheck,
  Database,
  FileOutput,
  FolderClock,
  Gauge,
  Leaf,
} from 'lucide-react';

export type HomeCapability = Readonly<{
  icon: LucideIcon;
  title: string;
  description: string;
}>;

export type HomeHighlight = Readonly<{
  icon: LucideIcon;
  title: string;
  description: string;
  tone: 'primary' | 'card';
  media?: 'dashboard';
}>;

export type HomeFlowStep = Readonly<{
  title: string;
  description: string;
  imageSrc: string;
}>;

export type HomeFaqItem = Readonly<{
  value: string;
  question: string;
  answer: string;
}>;

export const HOME_SECTION_IDS = {
  features: 'features',
  flow: 'flow',
  methods: 'methods',
  faq: 'faq',
} as const;

export const HOME_CAPABILITIES: readonly HomeCapability[] = [
  {
    icon: Database,
    title: '排出量データ管理',
    description:
      '拠点・設備ごとのモニタリングデータを登録し、排出量と削減量を一つの画面で確認できます。',
  },
  {
    icon: ClipboardCheck,
    title: 'プロジェクト・J-クレジット管理',
    description:
      '参加可能なプロジェクトの確認から入会届、設備証憑、審査状況までまとめて管理できます。',
  },
  {
    icon: BellRing,
    title: 'ダッシュボード・通知',
    description:
      '拠点・プロジェクトごとの実績を可視化し、申請や承認結果を通知で確認できます。',
  },
  {
    icon: FileOutput,
    title: 'レポート・データ確認',
    description:
      'モニタリング結果や入会届PDFなど、提出・確認に必要な情報へすぐアクセスできます。',
  },
];

export const HOME_HIGHLIGHTS: readonly HomeHighlight[] = [
  {
    icon: Leaf,
    title: 'プロジェクト参加を、もっとかんたんに',
    description:
      '参加可能なプロジェクトの確認から入会届の提出まで、オンラインでスムーズに進められます。',
    tone: 'primary',
    media: 'dashboard',
  },
  {
    icon: Gauge,
    title: 'ダッシュボードで進捗を把握',
    description:
      '拠点・プロジェクトごとの排出量や削減量をまとめて確認し、次の対応を判断できます。',
    tone: 'card',
  },
  {
    icon: FolderClock,
    title: '設備・証憑を履歴ごと管理',
    description:
      '設備情報、確認資料、変更履歴を関連付けて管理し、必要な情報をいつでも確認できます。',
    tone: 'card',
  },
  {
    icon: BarChart3,
    title: 'モニタリングから削減量まで一元化',
    description:
      '設備に紐づくモニタリングデータを登録し、削減実績まで一つの流れで確認できます。',
    tone: 'primary',
    media: 'dashboard',
  },
];

export const HOME_FLOW_STEPS: readonly HomeFlowStep[] = [
  {
    title: '企業・拠点を登録',
    description:
      '企業情報を登録し、プロジェクトに参加する拠点と担当者の情報を準備します。',
    imageSrc: '/assets/home/flow-emissions.png',
  },
  {
    title: 'プロジェクト入会届を提出',
    description:
      '参加するプロジェクトを選び、設備情報・確認資料・同意事項を入力して申請します。',
    imageSrc: '/assets/home/flow-offset.png',
  },
  {
    title: '設備・モニタリングデータを登録',
    description:
      '承認後、対象設備を選択して発電量や電力使用量などの実績データを登録します。',
    imageSrc: '/assets/home/flow-offset.png',
  },
  {
    title: '削減量・申請状況を確認',
    description:
      'ダッシュボードで削減実績、申請状況、通知を確認し、必要な対応へ進みます。',
    imageSrc: '/assets/home/flow-offset.png',
  },
];

export const HOME_FAQ_ITEMS: readonly HomeFaqItem[] = [
  {
    value: 'j-credit-guidance',
    question: 'J-クレジット制度に詳しくなくても利用できますか？',
    answer:
      'はい。画面の案内に沿って、企業・拠点・設備情報の登録からプロジェクト入会届、モニタリングデータ登録まで進められます。',
  },
  {
    value: 'multiple-sites',
    question: '複数の拠点を登録できますか？',
    answer:
      'はい。マスターユーザーは企業に属する複数の拠点を登録し、それぞれの設備・担当者・参加プロジェクトを管理できます。',
  },
  {
    value: 'evidence-management',
    question: '提出した証憑はどのように管理されますか？',
    answer:
      '証憑は対象の設備や申請に紐づけて管理され、確認画面や履歴から必要な情報を参照できます。',
  },
];

export const HOME_PENDING_LINKS = [
  'ブログ',
  'About',
  '利用規約',
  'プライバシーポリシー',
] as const;
