import type {
  ProjectFile,
  ProjectFileListing,
  ProjectFileType,
} from '@/api/admin/projectFiles';

/** The server's per-type limit, used when it does not report one. */
export const DEFAULT_PROJECT_FILE_VERSION_LIMIT = 10;

/** The J-Credit stage a document belongs to, as the backend enum groups them. */
export type ProjectFileStage = 'registration' | 'monitoring' | 'review';

export const PROJECT_FILE_STAGES: readonly { stage: ProjectFileStage; label: string }[] = [
  { stage: 'registration', label: 'プロジェクト登録' },
  { stage: 'monitoring', label: 'モニタリング・検証・認証' },
  { stage: 'review', label: '審査中のやり取り' },
];

export type ProjectFileTypeDefinition = {
  type: ProjectFileType;
  label: string;
  description: string;
  stage: ProjectFileStage;
  /** A form we write ourselves; the rest come back from the VVB or the secretariat. */
  authoredByUs: boolean;
};

/** Every document the フォーム tab manages, in the order the J-Credit flow produces them. */
export const PROJECT_FILE_TYPES: readonly ProjectFileTypeDefinition[] = [
  {
    type: 'PROJECT_PLAN',
    label: 'プロジェクト計画書',
    description: '登録申請書・計画変更届を含む計画書の様式です。',
    stage: 'registration',
    authoredByUs: true,
  },
  {
    type: 'VALIDATION_REPORT',
    label: '妥当性確認報告書',
    description: '審査機関（VVB）が発行する妥当性確認の結果です。',
    stage: 'registration',
    authoredByUs: false,
  },
  {
    type: 'REGISTRATION_RESULT',
    label: '登録通知',
    description: '制度事務局・認証委員会からのプロジェクト登録の通知です。',
    stage: 'registration',
    authoredByUs: false,
  },
  {
    type: 'MONITORING_REPORT',
    label: 'モニタリング報告書',
    description: '認証申請書を含むモニタリング報告書の様式です。',
    stage: 'monitoring',
    authoredByUs: true,
  },
  {
    type: 'ACTIVITY_RESULT_LIST',
    label: '個別活動実績報告リスト',
    description: '別紙10-1の方法論ごとの様式で作成します。',
    stage: 'monitoring',
    authoredByUs: true,
  },
  {
    type: 'VERIFICATION_REPORT',
    label: '検証報告書',
    description: '審査機関（VVB）が発行する検証の結果です。',
    stage: 'monitoring',
    authoredByUs: false,
  },
  {
    type: 'CERTIFICATION_RESULT',
    label: '認証通知',
    description: '認証委員会からのクレジット認証の通知です。',
    stage: 'monitoring',
    authoredByUs: false,
  },
  {
    type: 'VVB_QUERY_SHEET',
    label: '質問状・指摘事項シート',
    description: '審査中に審査機関（VVB）から届く質問・指摘事項です。',
    stage: 'review',
    authoredByUs: false,
  },
  {
    type: 'ADDITIONAL_MATERIAL',
    label: '追加資料',
    description: '審査機関（VVB）から求められて提出する資料です。',
    stage: 'review',
    authoredByUs: true,
  },
];

export const projectFileTypeLabel = (type: string) =>
  PROJECT_FILE_TYPES.find((definition) => definition.type === type)?.label ?? type;

export type ProjectFileVersion = ProjectFile & { version: number };

export type ProjectFileGroup = ProjectFileTypeDefinition & {
  /** Newest first. */
  files: ProjectFileVersion[];
  activeCount: number;
  limit: number;
  canUpload: boolean;
};

/**
 * Splits the project's files into one group per document type, newest first,
 * each with its version number and how many more versions it may hold.
 *
 * Falls back to counting the files when the server sends no summary, and to
 * numbering them by upload order when it sends no version numbers.
 */
export function groupProjectFiles(listing: ProjectFileListing): ProjectFileGroup[] {
  return PROJECT_FILE_TYPES.map((definition) => {
    const ofType = listing.files.filter((file) => file.fileType === definition.type);
    const numbered = ofType.map((file, index) => ({
      ...file,
      version: file.versionNo ?? ofType.length - index,
    }));
    numbered.sort((a, b) => b.version - a.version);

    const summary = listing.versionSummary.find(
      (item) => item.fileType === definition.type,
    );
    const limit = summary?.limit ?? DEFAULT_PROJECT_FILE_VERSION_LIMIT;
    const activeCount = summary?.activeCount ?? numbered.length;

    return {
      ...definition,
      files: numbered,
      activeCount,
      limit,
      canUpload: summary?.canUpload ?? activeCount < limit,
    };
  });
}

export type ProjectFilePreviewKind = 'pdf' | 'excel';

/** How a file can be shown in the browser, or null when it can only be downloaded. */
export function previewKindOf(
  file: Pick<ProjectFile, 'originalFileName'>,
): ProjectFilePreviewKind | null {
  const name = (file.originalFileName ?? '').toLowerCase();
  if (name.endsWith('.pdf')) return 'pdf';
  if (name.endsWith('.xlsx')) return 'excel';
  return null;
}
