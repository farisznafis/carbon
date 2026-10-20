import { useEffect, useMemo, useRef, useState, type ReactNode } from 'react';
import { FileSearch, Search } from 'lucide-react';
import { useLocation, useSearchParams } from 'react-router-dom';
import { ApiError } from '@/api/_shared/errors';
import type { ProjectFileType } from '@/api/admin/projectFiles';
import { ActionButton } from '@/components/actions';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { SegmentTabsList, SegmentTabsTrigger } from '@/components/navigation/PageTabs';
import { Tabs } from '@/components/ui/tabs';
import { Skeleton } from '@/components/ui/skeleton';
import { TooltipProvider } from '@/components/ui/tooltip';
import { toast } from '@/hooks/use-toast';
import { DocumentDataSectionCard, type CopyHandler } from './DocumentDataSectionCard';
import { copyToClipboard, filterSections } from './documentDataCopy';
import { IndividualActivityView } from './IndividualActivityView';
import { PROJECT_FILE_TYPES } from './projectFileTypes';
import { useProjectDocumentData } from './useProjectDocumentData';

/** Which document the データ取得 view shows, kept in the URL for back/forward and links. */
export const DOCUMENT_DATA_TYPE_QUERY_PARAM = 'dataType';
/** How long a copy control shows its ✓. */
const COPIED_FEEDBACK_MS = 1500;

/** The forms whose values are entered in the SaaS; the other types are kept as files only. */
const DATA_FILE_TYPES: readonly ProjectFileType[] = ['PROJECT_PLAN', 'MONITORING_REPORT', 'ACTIVITY_RESULT_LIST'];
const DATA_FILE_DEFINITIONS = PROJECT_FILE_TYPES.filter((definition) => DATA_FILE_TYPES.includes(definition.type));

const isFileType = (value: string | null): value is ProjectFileType =>
  DATA_FILE_TYPES.some((type) => type === value);

const errorMessage = (error: unknown, fallback: string) =>
  error instanceof Error && error.message ? error.message : fallback;

/** The server answers 404 when nothing has been entered for the form yet. */
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
 * The データ取得 tab: the latest values entered in the SaaS for each of the
 * three forms, one at a time, each value copied on its own into an Excel form.
 * The forms as tabs above, the selected form's data below at full width.
 */
export function ProjectDocumentDataPanel({ projectId }: Readonly<{ projectId: string }>) {
  const location = useLocation();
  const [searchParams, setSearchParams] = useSearchParams();
  const [filter, setFilter] = useState('');
  const { copiedKey, announcement, copy } = useCopyFeedback();

  const requestedType = searchParams.get(DOCUMENT_DATA_TYPE_QUERY_PARAM);
  const activeType: ProjectFileType = isFileType(requestedType) ? requestedType : DATA_FILE_TYPES[0];
  const activeDefinition = DATA_FILE_DEFINITIONS.find((definition) => definition.type === activeType)!;

  // 個別活動実績報告リスト has its own per-equipment endpoints.
  const isIndividualActivity = activeType === 'ACTIVITY_RESULT_LIST';
  const dataQuery = useProjectDocumentData(projectId, activeType, !isIndividualActivity);
  const isLoading = dataQuery.isLoading;
  const hasFailed = dataQuery.isError && !isNoDataError(dataQuery.error);

  const sections = useMemo(() => dataQuery.data?.sections ?? [], [dataQuery.data]);
  const visibleSections = useMemo(() => filterSections(sections, filter), [sections, filter]);

  const selectType = (type: string) => {
    if (!isFileType(type)) return;
    const nextSearchParams = new URLSearchParams(searchParams);
    nextSearchParams.set(DOCUMENT_DATA_TYPE_QUERY_PARAM, type);
    setSearchParams(nextSearchParams, { replace: true, state: location.state });
    setFilter('');
  };

  const renderFormTabs = () => (
    <Tabs value={activeType} onValueChange={selectType}>
      {/* Three forms fit a segmented control; a side list would take width the data needs. */}
      <div className="overflow-x-auto">
        <SegmentTabsList aria-label="データを表示する書類" className="flex-nowrap">
          {DATA_FILE_DEFINITIONS.map((definition) => (
            <SegmentTabsTrigger key={definition.type} value={definition.type}>
              {definition.label}
            </SegmentTabsTrigger>
          ))}
        </SegmentTabsList>
      </div>
    </Tabs>
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
    if (isIndividualActivity) {
      return <IndividualActivityView projectId={projectId} copiedKey={copiedKey} onCopy={copy} />;
    }

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
        'まだデータがありません。',
        'この書類の内容が入力されると、ここに表示されます。',
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
      <div className="flex min-w-0 flex-col gap-4">
        {renderFormTabs()}

        <article
          aria-labelledby="document-data-title"
          className="min-w-0 rounded-lg border border-border bg-card"
        >
          <header className="flex flex-col gap-3 border-b border-border px-5 py-4 md:flex-row md:items-center md:justify-between">
            <div className="min-w-0 space-y-1">
              <h2 id="document-data-title" className="text-lg font-bold text-foreground">
                {activeDefinition.label}
              </h2>
              <p className="text-small text-muted-foreground">{activeDefinition.description}</p>
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

      <p role="status" aria-live="polite" className="sr-only">
        {announcement}
      </p>
    </TooltipProvider>
  );
}
