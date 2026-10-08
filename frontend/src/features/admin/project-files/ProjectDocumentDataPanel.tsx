import { useEffect, useMemo, useRef, useState, type ReactNode } from 'react';
import { FileSearch, Search, Upload } from 'lucide-react';
import { Link, useLocation, useSearchParams } from 'react-router-dom';
import { ApiError } from '@/api/_shared/errors';
import type { ProjectFileType } from '@/api/admin/projectFiles';
import { ActionButton } from '@/components/actions';
import { StatusBadge, type StatusBadgeTone } from '@/components/data-display/StatusBadge';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Select, SelectContent, SelectGroup, SelectItem, SelectLabel, SelectTrigger, SelectValue } from '@/components/ui/select';
import { Skeleton } from '@/components/ui/skeleton';
import { TooltipProvider } from '@/components/ui/tooltip';
import { formatUploadedAt } from '@/features/admin/project-plan/planFiles';
import { toast } from '@/hooks/use-toast';
import { cn } from '@/lib/utils';
import { DocumentDataSectionCard, type CopyHandler } from './DocumentDataSectionCard';
import { copyToClipboard, filterSections } from './documentDataCopy';
import { PROJECT_FILE_STAGES, PROJECT_FILE_TYPES } from './projectFileTypes';
import { useProjectDocumentData } from './useProjectDocumentData';
import { useProjectFiles } from './useProjectFiles';
import { DOCUMENT_UPLOAD_QUERY_PARAM, DOCUMENT_VIEW_QUERY_PARAM } from './ProjectDocumentsTab';

/** Which document the データ取得 view shows, kept in the URL for back/forward and links. */
export const DOCUMENT_DATA_TYPE_QUERY_PARAM = 'dataType';
/** How long a copy control shows its ✓. */
const COPIED_FEEDBACK_MS = 1500;

const isFileType = (value: string | null): value is ProjectFileType =>
  PROJECT_FILE_TYPES.some((definition) => definition.type === value);

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
  const { copiedKey, announcement, copy } = useCopyFeedback();

  const requestedType = searchParams.get(DOCUMENT_DATA_TYPE_QUERY_PARAM);
  const firstUploaded = groups.find((group) => group.files.length > 0)?.type;
  const activeType: ProjectFileType = isFileType(requestedType)
    ? requestedType
    : firstUploaded ?? PROJECT_FILE_TYPES[0].type;
  const activeDefinition = PROJECT_FILE_TYPES.find((definition) => definition.type === activeType)!;
  const latestFile = groups.find((group) => group.type === activeType)?.files[0];
  const uploadedCount = groups.filter((group) => group.files.length > 0).length;

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
        <span className="text-small tabular-nums text-muted-foreground">
          {uploadedCount} / {PROJECT_FILE_TYPES.length} アップロード済
        </span>
      </div>

      <nav aria-label="データを表示する書類" className="space-y-4 p-2 pb-3">
        {PROJECT_FILE_STAGES.map(({ stage, label }) => (
          <div key={stage} className="space-y-1">
            <p className="px-3 pt-2 text-xs font-semibold text-muted-foreground">{label}</p>
            <ul className="space-y-0.5">
              {PROJECT_FILE_TYPES.filter((definition) => definition.stage === stage).map((definition) => {
                const isActive = definition.type === activeType;
                const latest = groups.find((group) => group.type === definition.type)?.files[0];
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
          </div>
        ))}
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
          {PROJECT_FILE_STAGES.map(({ stage, label }) => (
            <SelectGroup key={stage}>
              <SelectLabel>{label}</SelectLabel>
              {PROJECT_FILE_TYPES.filter((definition) => definition.stage === stage).map((definition) => (
                <SelectItem key={definition.type} value={definition.type}>
                  {definition.label}
                </SelectItem>
              ))}
            </SelectGroup>
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
      <div className="grid min-w-0 gap-4 lg:grid-cols-[20rem_minmax(0,1fr)] lg:gap-6">
        {renderFileList()}

        <div className="min-w-0 space-y-4">
          {renderMobileSelect()}

          <article
            aria-labelledby="document-data-title"
            className="min-w-0 rounded-lg border border-border bg-card shadow-app-sm"
          >
            <header className="flex items-start justify-between gap-3 border-b border-border px-5 py-4">
              <div className="min-w-0 space-y-1">
                <h2
                  id="document-data-title"
                  className="text-lg font-semibold text-foreground [overflow-wrap:anywhere]"
                >
                  {latestFile?.originalFileName ?? activeDefinition.label}
                </h2>
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
              {status && (
                <StatusBadge tone={status.tone} className="shrink-0">
                  {status.label}
                </StatusBadge>
              )}
            </header>

            <div className="space-y-5 px-5 py-5">
              {sections.length > 0 && (
                <div className="relative sm:max-w-xs">
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
                    className="pl-9"
                  />
                </div>
              )}
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
