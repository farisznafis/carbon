import type { LucideIcon } from 'lucide-react';
import {
  BellRing,
  ClipboardCheck,
  Database,
  Building2,
} from 'lucide-react';

export type HomeCapability = Readonly<{
  icon: LucideIcon;
  title: string;
  description: string;
  summary: string;
  previewImage?: Readonly<{
    src: string;
    alt: string;
  }>;
}>;

export type HomeFlowStep = Readonly<{
  title: string;
  description: string;
}>;

export type HomeFaqItem = Readonly<{
  value: string;
  question: string;
  answer: string;
}>;

type HomeContactDetails = Readonly<{
  postalCode: string;
  address: string;
  email: string;
  phoneDisplay: string;
  phoneHref: string;
}>;

export const HOME_SECTION_IDS = {
  features: 'features',
  flow: 'flow',
  methods: 'methods',
  faq: 'faq',
} as const;

/* =========================================================
   CAPABILITIES
   ========================================================= */

export const HOME_CAPABILITIES: readonly HomeCapability[] = [
  {
    icon: Database,
    title: '排出量データ管理',
    description:
      '拠点・設備ごとのモニタリングデータを登録し、排出量と削減量を一つの画面で確認できます。',
    summary:
      '企業・拠点をまたぐ情報を、同じ視点で。',
    previewImage: {
      src: '/assets/home/feature-1.png',
      alt: '排出量データ管理の画面イメージ',
    },
  },
  {
    icon: ClipboardCheck,
    title: 'プロジェクト・J-クレジット管理',
    description:
      '参加可能なプロジェクトの確認から入会届、設備証憑、審査状況までまとめて管理できます。',
    summary:
      '参加から証憑まで、ひとつの文脈で。',
    previewImage: {
      src: '/assets/home/feature-2.png',
      alt: 'プロジェクト・J-クレジット管理の画面イメージ',
    },
  },
  {
    icon: BellRing,
    title: 'ダッシュボード・通知',
    description:
      '拠点・プロジェクトごとの実績を可視化し、申請や承認結果を通知で確認できます。',
    summary:
      '今見るべきことと、次のアクションを明確に。',
    previewImage: {
      src: '/assets/home/feature-3.png',
      alt: 'ダッシュボード・通知の画面イメージ',
    },
  },
  {
    icon: Building2,
    title: '拠点・担当者管理',
    description:
      '複数の拠点と担当者をまとめて管理し、各拠点のプロジェクト参加状況をスムーズに確認できます。',
    summary:
      '拠点と担当者を、ひとつの場所で。',
    previewImage: {
      src: '/assets/home/feature-4.png',
      alt: '拠点・担当者管理の画面イメージ',
    },
  },
];

/* =========================================================
   BUSINESS FLOW
   ========================================================= */

export const HOME_FLOW_STEPS: readonly HomeFlowStep[] = [
  {
    title: '企業・拠点登録',
    description:
      '企業情報と、プロジェクトに参加する拠点・担当者の情報を登録します。',
  },
  {
    title: 'プロジェクト選択',
    description:
      '参加可能なプロジェクトを確認し、自社・拠点に合うプロジェクトを選択します。',
  },
  {
    title: '入会届',
    description:
      '必要事項や同意事項を入力し、プロジェクトへの入会届を提出します。',
  },
  {
    title: '設備登録',
    description:
      '承認後、プロジェクトの対象となる設備情報を登録します。',
  },
  {
    title: '証憑管理',
    description:
      '設備や申請に必要な確認資料・証憑を登録し、まとめて管理します。',
  },
  {
    title: 'モニタリング',
    description:
      '電力使用量や発電量など、設備に紐づく実績データを登録します。',
  },
  {
    title: '削減量の可視化',
    description:
      '登録されたデータから削減実績やプロジェクトの進捗を確認します。',
  },
  // {
  //   title: '意思決定',
  //   description:
  //     '実績・進捗・対応状況を確認し、次に必要なアクションを判断します。',
  // },
];

/* =========================================================
   FAQ
   ========================================================= */

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

/* =========================================================
   CONTACT / LEGAL
   ========================================================= */

export const HOME_CONTACT_DETAILS: HomeContactDetails = {
  postalCode: '〒100-0005',
  address: '東京都千代田区丸の内1-1-1',
  email: 'info@company.com',
  phoneDisplay: '+81 70-1234-5678',
  phoneHref: '+817012345678',
};

export const HOME_LEGAL_LABELS = [
  '利用規約',
  'プライバシーポリシー',
] as const;