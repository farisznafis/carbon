import type {
  ProjectFile,
  ProjectFileListing,
  ProjectFileType,
} from '@/api/admin/projectFiles';

/** The server's per-type limit, used when it does not report one. */
export const DEFAULT_PROJECT_FILE_VERSION_LIMIT = 10;

export type ProjectFileTypeDefinition = {
  type: ProjectFileType;
  label: string;
  description: string;
};

/** The documents the フォーム tab manages, in the order the J-Credit flow produces them. */
export const PROJECT_FILE_TYPES: readonly ProjectFileTypeDefinition[] = [
  {
    type: 'PROJECT_PLAN',
    label: 'プロジェクト計画書',
    description: '登録申請書・計画変更届を含む計画書の様式です。',
  },
  {
    type: 'MONITORING_REPORT',
    label: 'モニタリング報告書',
    description: '認証申請書を含むモニタリング報告書の様式です。',
  },
  {
    type: 'ACTIVITY_RESULT_LIST',
    label: '個別活動実績報告リスト',
    description: '別紙10-1。方法論ごとの様式で作成します。',
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

/** Only PDFs can be shown in the browser; Excel files must be downloaded. */
export const isPreviewableFile = (file: Pick<ProjectFile, 'originalFileName'>) =>
  (file.originalFileName ?? '').toLowerCase().endsWith('.pdf');
