import { useEffect, useMemo, useRef, useState, type ReactNode } from 'react';
import { FileSearch, Search, Upload } from 'lucide-react';
import { Link, useLocation, useSearchParams } from 'react-router-dom';
import { ApiError } from '@/api/_shared/errors';
import type { ProjectFileType } from '@/api/admin/projectFiles';
import { ActionButton } from '@/components/actions';
import { StatusBadge, type StatusBadgeTone } from '@/components/data-display/StatusBadge';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select';
import { Skeleton } from '@/components/ui/skeleton';
import { TooltipProvider } from '@/components/ui/tooltip';
import { formatUploadedAt } from '@/features/admin/project-plan/planFiles';
import { toast } from '@/hooks/use-toast';
import { cn } from '@/lib/utils';
import { DocumentDataSectionCard, type CopyHandler } from './DocumentDataSectionCard';
import { copyToClipboard, filterSections } from './documentDataCopy';
import { PROJECT_FILE_TYPES } from './projectFileTypes';
import { useProjectDocumentData } from './useProjectDocumentData';
import { useProjectFiles } from './useProjectFiles';
import { DOCUMENT_UPLOAD_QUERY_PARAM, DOCUMENT_VIEW_QUERY_PARAM } from './ProjectDocumentsTab';

/** Which document the データ取得 view shows, kept in the URL for back/forward and links. */
export const DOCUMENT_DATA_TYPE_QUERY_PARAM = 'dataType';
/** How long a copy control shows its ✓. */
const COPIED_FEEDBACK_MS = 1500;

/** The forms the server reads values out of; the other types are kept as files only. */
const DATA_FILE_TYPES: readonly ProjectFileType[] = ['PROJECT_PLAN', 'MONITORING_REPORT', 'ACTIVITY_RESULT_LIST'];
const DATA_FILE_DEFINITIONS = PROJECT_FILE_TYPES.filter((definition) => DATA_FILE_TYPES.includes(definition.type));
const ALL_TYPES = 'ALL';

const isFileType = (value: string | null): value is ProjectFileType =>
  DATA_FILE_TYPES.some((type) => type === value);

const errorMessage = (error: unknown, fallback: string) =>
  error instanceof Error && error.message ? error.message : fallback;

/** The server answers 404 when the latest file holds nothing it can read (a PDF, say). */
const isNoDataError = (error: unknown) => error instanceof ApiError && error.status === 404;

/** Copies text, shows a ✓ on the control that asked, and tells screen readers once. */
function useCopyFeedback() {
  const [copiedKey, setCopiedKey] = useState<string | null>(null);
  const [announcement, setAnnouncement] = useState('');
  const timer = useRef<ReturnType<typeof setTimeout>>();

  useEffect(() => () => clearTimeout(timer.current), []);

  const copy: CopyHandler = (key, text, label) => {
    void copyToClipboard(text).then(
      () => {
        clearTimeout(timer.current);
        setCopiedKey(key);
        setAnnouncement(`${label}をコピーしました`);
        timer.current = setTimeout(() => setCopiedKey(null), COPIED_FEEDBACK_MS);
      },
      () =>
        toast({
          title: 'コピーできませんでした',
          description: 'ブラウザがクリップボードへのアクセスを許可していません。',
          variant: 'destructive',
        }),
    );
  };

  return { copiedKey, announcement, copy };
}

/**
 * The データ取得 tab: the values read from the latest file of each form, one
 * document at a time, each value copied on its own into an Excel form.
 * Files on the left (a select on narrow screens), the data on the right.
 */
export function ProjectDocumentDataPanel({ projectId }: Readonly<{ projectId: string }>) {
  const location = useLocation();
  const [searchParams, setSearchParams] = useSearchParams();
  const { listingQuery, groups } = useProjectFiles(projectId);
  const [filter, setFilter] = useState('');
  const [typeFilter, setTypeFilter] = useState<string>(ALL_TYPES);
  const [fileNameQuery, setFileNameQuery] = useState('');
  const { copiedKey, announcement, copy } = useCopyFeedback();

  const dataGroups = groups.filter((group) => DATA_FILE_TYPES.includes(group.type));
  const requestedType = searchParams.get(DOCUMENT_DATA_TYPE_QUERY_PARAM);
  const firstUploaded = dataGroups.find((group) => group.files.length > 0)?.type;
  const activeType: ProjectFileType = isFileType(requestedType)
    ? requestedType
    : firstUploaded ?? DATA_FILE_TYPES[0];
  const activeDefinition = DATA_FILE_DEFINITIONS.find((definition) => definition.type === activeType)!;
  const latestFile = dataGroups.find((group) => group.type === activeType)?.files[0];
  // Every version counts, though each row shows its type's latest file.
  const fileCount = dataGroups.reduce((total, group) => total + group.files.length, 0);

  const fileNameNeedle = fileNameQuery.trim().toLowerCase();
  const listedDefinitions = DATA_FILE_DEFINITIONS.filter((definition) => {
    if (typeFilter !== ALL_TYPES && definition.type !== typeFilter) return false;
    if (!fileNameNeedle) return true;
    const fileName = dataGroups.find((group) => group.type === definition.type)?.files[0]?.originalFileName;
    return (fileName ?? '').toLowerCase().includes(fileNameNeedle);
  });

  // Unknown until the listing loads; when it fails, ask for the data anyway.
  const hasFile = listingQuery.isSuccess ? Boolean(latestFile) : null;
  const dataQuery = useProjectDocumentData(
    projectId,
    activeType,
    !listingQuery.isLoading && hasFile !== false,
  );
  const isLoading = listingQuery.isLoading || dataQuery.isLoading;
  const hasFailed = dataQuery.isError && !isNoDataError(dataQuery.error);

  const sections = useMemo(() => dataQuery.data?.sections ?? [], [dataQuery.data]);
  const visibleSections = useMemo(() => filterSections(sections, filter), [sections, filter]);

  const status = ((): { label: string; tone: StatusBadgeTone } | null => {
    if (hasFile === false) return { label: '未アップロード', tone: 'neutral' };
    if (isLoading) return null;
    if (hasFailed) return { label: '取得エラー', tone: 'danger' };
    return sections.length > 0
      ? { label: '解析済み', tone: 'success' }
      : { label: '解析不可', tone: 'warning' };
  })();

  const selectType = (type: string) => {
    if (!isFileType(type)) return;
    const nextSearchParams = new URLSearchParams(searchParams);
    nextSearchParams.set(DOCUMENT_DATA_TYPE_QUERY_PARAM, type);
    setSearchParams(nextSearchParams, { replace: true, state: location.state });
    setFilter('');
  };

  const uploadSearch = () => {
    const nextSearchParams = new URLSearchParams(searchParams);
    nextSearchParams.set(DOCUMENT_VIEW_QUERY_PARAM, 'forms');
    nextSearchParams.set(DOCUMENT_UPLOAD_QUERY_PARAM, activeType);
    nextSearchParams.delete(DOCUMENT_DATA_TYPE_QUERY_PARAM);
    return `?${nextSearchParams.toString()}`;
  };

  const renderFileList = () => (
    <aside
      aria-labelledby="document-data-files-heading"
      className="hidden overflow-hidden rounded-lg border border-border bg-card shadow-app-sm lg:block"
    >
      <div className="flex items-baseline justify-between gap-3 border-b border-border px-4 py-3">
        <h3 id="document-data-files-heading" className="text-sm font-semibold text-foreground">
          書類ファイル
        </h3>
        <span className="text-small tabular-nums text-muted-foreground">{fileCount}件</span>
      </div>

      <div className="flex gap-2 border-b border-border p-3">
        <Select value={typeFilter} onValueChange={setTypeFilter}>
          <SelectTrigger aria-label="ファイル種類で絞り込み" className="h-9 w-36 shrink-0">
            <SelectValue />
          </SelectTrigger>
          <SelectContent>
            <SelectItem value={ALL_TYPES}>すべての種類</SelectItem>
            {DATA_FILE_DEFINITIONS.map((definition) => (
              <SelectItem key={definition.type} value={definition.type}>
                {definition.label}
              </SelectItem>
            ))}
          </SelectContent>
        </Select>
        <div className="relative min-w-0 flex-1">
          <Search
            aria-hidden="true"
            className="pointer-events-none absolute left-3 top-1/2 size-4 -translate-y-1/2 text-muted-foreground"
          />
          <Input
            type="search"
            value={fileNameQuery}
            onChange={(event) => setFileNameQuery(event.target.value)}
            placeholder="ファイル名を検索"
            aria-label="ファイル名を検索"
            className="h-9 pl-9"
          />
        </div>
      </div>

      <nav aria-label="データを表示する書類" className="p-2">
        {listedDefinitions.length === 0 ? (
          <p className="px-3 py-6 text-center text-small text-muted-foreground">
            一致するファイルはありません。
          </p>
        ) : (
          <ul className="space-y-0.5">
            {listedDefinitions.map((definition) => {
              const isActive = definition.type === activeType;
              const latest = dataGroups.find((group) => group.type === definition.type)?.files[0];
              return (
                <li key={definition.type}>
                  <button
                    type="button"
                    aria-current={isActive ? 'true' : undefined}
                    onClick={() => selectType(definition.type)}
                    className={cn(
                      'relative flex min-h-14 w-full items-center gap-3 rounded-md py-2.5 pl-3 pr-2 text-left transition-colors focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring',
                      isActive
                        ? 'bg-primary/10 before:absolute before:inset-y-2 before:left-0 before:w-0.5 before:rounded-full before:bg-primary-strong'
                        : 'hover:bg-muted',
                    )}
                  >
                    <span className="min-w-0 flex-1 space-y-0.5">
                      <span className="block truncate text-xs text-muted-foreground">{definition.label}</span>
                      {latest ? (
                        <span
                          className={cn(
                            'block truncate text-sm font-medium',
                            isActive ? 'text-primary-strong' : 'text-foreground',
                          )}
                          title={latest.originalFileName ?? undefined}
                        >
                          {latest.originalFileName ?? definition.label}
                        </span>
                      ) : (
                        <span className="block text-sm text-muted-foreground">未アップロード</span>
                      )}
                    </span>
                    {latest && (
                      <StatusBadge className="shrink-0 px-2 py-0.5 tabular-nums">v{latest.version}</StatusBadge>
                    )}
                  </button>
                </li>
              );
            })}
          </ul>
        )}
      </nav>
    </aside>
  );

  const renderMobileSelect = () => (
    <div className="lg:hidden">
      <Select value={activeType} onValueChange={selectType}>
        <SelectTrigger aria-label="データを表示する書類">
          <SelectValue />
        </SelectTrigger>
        <SelectContent>
          {DATA_FILE_DEFINITIONS.map((definition) => (
            <SelectItem key={definition.type} value={definition.type}>
              {definition.label}
            </SelectItem>
          ))}
        </SelectContent>
      </Select>
    </div>
  );

  const renderEmpty = (icon: ReactNode, title: string, body: string, action?: ReactNode) => (
    <div className="flex flex-col items-center gap-3 px-6 py-12 text-center">
      <span aria-hidden="true" className="flex size-10 items-center justify-center rounded-full bg-muted text-muted-foreground">
        {icon}
      </span>
      <div className="space-y-1">
        <p className="text-sm font-medium text-foreground">{title}</p>
        <p className="text-small text-muted-foreground">{body}</p>
      </div>
      {action}
    </div>
  );

  const renderBody = () => {
    if (isLoading) {
      return (
        <div role="status" aria-label="データを読み込んでいます" className="space-y-6">
          {[0, 1].map((index) => (
            <div key={index} className="space-y-2">
              <Skeleton className="h-4 w-40" />
              {[0, 1, 2].map((row) => (
                <Skeleton key={row} className="h-9 w-full" />
              ))}
            </div>
          ))}
        </div>
      );
    }

    if (hasFile === false) {
      return renderEmpty(
        <Upload className="size-5" />,
        'この書類はまだアップロードされていません。',
        'フォームタブでファイルをアップロードすると、読み取ったデータがここに表示されます。',
        <Button asChild size="sm" variant="outline" className="text-primary-strong">
          <Link to={{ search: uploadSearch() }} state={location.state}>
            <Upload aria-hidden="true" />
            {activeDefinition.label}をアップロード
          </Link>
        </Button>,
      );
    }

    if (hasFailed) {
      return (
        <div
          role="alert"
          className="flex flex-col items-center gap-3 rounded-lg border border-destructive/30 bg-destructive/5 p-6 text-center"
        >
          <p className="text-small font-medium text-foreground">
            {errorMessage(dataQuery.error, 'データを取得できませんでした。')}
          </p>
          <ActionButton kind="retry" onClick={() => void dataQuery.refetch()} />
        </div>
      );
    }

    if (sections.length === 0) {
      return renderEmpty(
        <FileSearch className="size-5" />,
        '読み取れるデータがありません。',
        'PDFでアップロードされた書類からはデータを取得できません。Excel様式（.xlsx）をアップロードしてください。',
      );
    }

    if (visibleSections.length === 0) {
      return renderEmpty(
        <Search className="size-5" />,
        `「${filter.trim()}」に一致する項目はありません。`,
        '別のキーワードで絞り込むか、条件をクリアしてください。',
        <Button type="button" size="sm" variant="outline" onClick={() => setFilter('')}>
          絞り込みをクリア
        </Button>,
      );
    }

    return (
      <div className="space-y-6">
        {visibleSections.map((section) => (
          <DocumentDataSectionCard key={section.key} section={section} copiedKey={copiedKey} onCopy={copy} />
        ))}
      </div>
    );
  };

  return (
    <TooltipProvider delayDuration={250}>
      <div className="grid min-w-0 gap-4 lg:grid-cols-[clamp(18rem,30vw,28rem)_minmax(0,1fr)] lg:gap-6">
        {renderFileList()}

        {/* gap, not space-y: the hidden mobile select would push the card below the list. */}
        <div className="flex min-w-0 flex-col gap-4">
          {renderMobileSelect()}

          <article
            aria-labelledby="document-data-title"
            className="min-w-0 rounded-lg border border-border bg-card shadow-app-sm"
          >
            <header className="flex flex-col gap-3 border-b border-border px-5 py-4 md:flex-row md:items-center md:justify-between">
              <div className="min-w-0 space-y-1">
                {/* The badge sits with the file name: it is the state of that file. */}
                <div className="flex flex-wrap items-center gap-x-2 gap-y-1">
                  <h2
                    id="document-data-title"
                    className="min-w-0 text-lg font-semibold text-foreground [overflow-wrap:anywhere]"
                  >
                    {latestFile?.originalFileName ?? activeDefinition.label}
                  </h2>
                  {status && (
                    <StatusBadge tone={status.tone} className="shrink-0 px-2 py-0.5">
                      {status.label}
                    </StatusBadge>
                  )}
                </div>
                <p className="flex flex-wrap items-center gap-x-1.5 text-small text-muted-foreground">
                  {latestFile ? (
                    <>
                      <span>{activeDefinition.label}</span>
                      <span aria-hidden="true">・</span>
                      <span className="tabular-nums">v{latestFile.version}</span>
                      <span aria-hidden="true">・</span>
                      <span className="tabular-nums">{formatUploadedAt(latestFile.uploadedAt)}</span>
                    </>
                  ) : (
                    <span>{activeDefinition.description}</span>
                  )}
                </p>
              </div>
              {sections.length > 0 && (
                <div className="relative w-full shrink-0 md:w-64">
                  <Search
                    aria-hidden="true"
                    className="pointer-events-none absolute left-3 top-1/2 size-4 -translate-y-1/2 text-muted-foreground"
                  />
                  <Input
                    type="search"
                    value={filter}
                    onChange={(event) => setFilter(event.target.value)}
                    placeholder="項目名・値で絞り込み"
                    aria-label="項目名・値で絞り込み"
                    className="h-9 pl-9"
                  />
                </div>
              )}
            </header>

            <div className="px-5 py-5">
              {renderBody()}
            </div>
          </article>
        </div>
      </div>

      <p role="status" aria-live="polite" className="sr-only">
        {announcement}
      </p>
    </TooltipProvider>
  );
}
