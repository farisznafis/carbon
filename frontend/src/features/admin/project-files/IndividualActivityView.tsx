import { useMemo, useRef, useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import { FileSearch, FileText, Search } from 'lucide-react';
import {
  getIndividualActivityDetail,
  getIndividualActivityRows,
  type IndividualActivityDetail,
  type IndividualActivityRow,
  type ReportReadiness,
} from '@/api/admin/individualActivity';
import { ActionButton } from '@/components/actions';
import { StatusBadge } from '@/components/data-display/StatusBadge';
import {
  PageTabsList,
  PageTabsTrigger,
  SegmentTabsList,
  SegmentTabsTrigger,
  TabCount,
} from '@/components/navigation/PageTabs';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select';
import { Skeleton } from '@/components/ui/skeleton';
import { Tabs, TabsContent } from '@/components/ui/tabs';
import { getSiteProjectEquipmentPresignedUrl } from '@/api/site';
import { toast } from '@/hooks/use-toast';
import { downloadFromUrl } from '@/utils/download';
import { cn } from '@/lib/utils';
import { DocumentDataSectionCard, type CopyHandler } from './DocumentDataSectionCard';
import { EMPTY_VALUE, filterSections } from './documentDataCopy';
import { FilePreviewDialog, type FilePreviewTarget } from './ProjectFilePreviewDialog';
import { previewKindOf } from './projectFileTypes';
import {
  ACTIVITY_GROUPS,
  ELIGIBILITY_LABELS,
  MONITORING_EVIDENCE,
  READINESS_LABELS,
  SHEETS,
  evidenceFilesOf,
  evidenceLabel,
  sheetOf,
  toActivityGroups,
  type BatterySheet,
  type EvidenceLink,
} from './individualActivitySections';

const READINESS_FILTERS: ReportReadiness[] = ['MISSING_EVIDENCE', 'NO_BATCH_RESULT', 'READY'];
const ALL = 'ALL';
const SHEET_ORDER: BatterySheet[] = ['without', 'with'];
/** Below this the list and the detail stack, so choosing a row scrolls to its detail. */
const STACKED_QUERY = '(max-width: 1199px)';

type GroupKey = (typeof ACTIVITY_GROUPS)[number]['key'];

const errorMessage = (error: unknown, fallback: string) =>
  error instanceof Error && error.message ? error.message : fallback;

const formatKwh = (value: number | null) =>
  value === null || value === undefined ? EMPTY_VALUE : value.toLocaleString('ja-JP');

function LoadError({ error, fallback, onRetry }: Readonly<{ error: unknown; fallback: string; onRetry: () => void }>) {
  return (
    <div
      role="alert"
      className="flex flex-col items-center gap-3 rounded-lg border border-destructive/30 bg-destructive/5 p-6 text-center"
    >
      <p className="text-small font-medium text-foreground">{errorMessage(error, fallback)}</p>
      <ActionButton kind="retry" onClick={onRetry} />
    </div>
  );
}

/** Evidence comes as S3 keys; they are signed only when opened, since a signed URL expires. */
const resolveEvidenceUrl = (location: string) =>
  /^https?:\/\//.test(location) ? Promise.resolve(location) : getSiteProjectEquipmentPresignedUrl(location);

type ViewProps = Readonly<{ projectId: string; copiedKey: string | null; onCopy: CopyHandler }>;

/**
 * 個別活動実績報告リスト in データ取得. The equipment list on the left, one tab
 * per 会員情報 sheet of the Excel; the chosen equipment's columns on the right,
 * where every blank says which file to read it from.
 */
export function IndividualActivityView({ projectId, copiedKey, onCopy }: ViewProps) {
  const [sheet, setSheet] = useState<BatterySheet>('without');
  const [filter, setFilter] = useState('');
  const [readiness, setReadiness] = useState<ReportReadiness | typeof ALL>(ALL);
  const [selectedId, setSelectedId] = useState<number | null>(null);
  // Kept across equipment, so the operator can walk one group through every row.
  const [groupKey, setGroupKey] = useState<GroupKey>('member');
  const [itemFilter, setItemFilter] = useState('');
  const detailRef = useRef<HTMLElement>(null);

  const rowsQuery = useQuery({
    queryKey: ['admin', 'individual-activity', projectId, 'rows'],
    queryFn: () => getIndividualActivityRows(projectId),
    enabled: Boolean(projectId),
  });

  const rowsBySheet = useMemo(() => {
    const bySheet: Record<BatterySheet, IndividualActivityRow[]> = { without: [], with: [] };
    for (const row of rowsQuery.data ?? []) bySheet[sheetOf(row.hasBattery)].push(row);
    return bySheet;
  }, [rowsQuery.data]);

  const sheetRows = rowsBySheet[sheet];
  const visibleRows = useMemo(() => {
    const needle = filter.trim().toLowerCase();
    return sheetRows.filter(
      (row) =>
        (readiness === ALL || row.reportReadiness === readiness) &&
        (!needle ||
          [row.companyName, row.siteAddress].some((value) => (value ?? '').toLowerCase().includes(needle))),
    );
  }, [sheetRows, filter, readiness]);

  // The chosen row, or the first one shown when it was filtered out or nothing is chosen yet.
  const selected = visibleRows.find((row) => row.equipmentId === selectedId) ?? visibleRows[0];

  const select = (row: IndividualActivityRow) => {
    setSelectedId(row.equipmentId);
    if (window.matchMedia?.(STACKED_QUERY).matches) {
      detailRef.current?.scrollIntoView({ behavior: 'smooth', block: 'start' });
    }
  };

  if (rowsQuery.isLoading) {
    return (
      <div role="status" aria-label="個別活動データを読み込んでいます" className="space-y-2">
        {[0, 1, 2, 3].map((index) => (
          <Skeleton key={index} className="h-14 w-full" />
        ))}
      </div>
    );
  }

  if (rowsQuery.isError) {
    return (
      <LoadError
        error={rowsQuery.error}
        fallback="個別活動データを取得できませんでした。"
        onRetry={() => void rowsQuery.refetch()}
      />
    );
  }

  if ((rowsQuery.data ?? []).length === 0) {
    return (
      <div className="flex flex-col items-center gap-3 px-6 py-12 text-center">
        <span aria-hidden="true" className="flex size-10 items-center justify-center rounded-full bg-muted text-muted-foreground">
          <FileSearch className="size-5" />
        </span>
        <div className="space-y-1">
          <p className="text-sm font-medium text-foreground">まだ設備がありません。</p>
          <p className="text-small text-muted-foreground">参加企業の設備が登録されると、ここに表示されます。</p>
        </div>
      </div>
    );
  }

  return (
    <div className="grid min-w-0 gap-4 xl:grid-cols-[18rem_minmax(0,1fr)] xl:gap-5">
      <aside aria-label="設備一覧" className="flex min-w-0 flex-col gap-3">
        <Tabs
          value={sheet}
          onValueChange={(value) => {
            setSheet(value as BatterySheet);
            setSelectedId(null);
          }}
        >
          <SegmentTabsList aria-label="Excelのシート" className="flex w-full flex-nowrap">
            {SHEET_ORDER.map((key) => (
              <SegmentTabsTrigger key={key} value={key} className="flex-1 justify-center px-3">
                {SHEETS[key].label}
                <TabCount count={rowsBySheet[key].length} />
              </SegmentTabsTrigger>
            ))}
          </SegmentTabsList>
        </Tabs>
        <p className="text-xs text-muted-foreground">Excel「{SHEETS[sheet].sheetName}」シート</p>

        <div className="relative">
          <Search
            aria-hidden="true"
            className="pointer-events-none absolute left-3 top-1/2 size-4 -translate-y-1/2 text-muted-foreground"
          />
          <Input
            type="search"
            value={filter}
            onChange={(event) => setFilter(event.target.value)}
            placeholder="会員名・住所で検索"
            aria-label="会員名・住所で検索"
            className="h-9 pl-9"
          />
        </div>
        <Select value={readiness} onValueChange={(value) => setReadiness(value as ReportReadiness | typeof ALL)}>
          <SelectTrigger aria-label="報告状況で絞り込み" className="h-9">
            <SelectValue />
          </SelectTrigger>
          <SelectContent>
            <SelectItem value={ALL}>すべての報告状況（{sheetRows.length}）</SelectItem>
            {READINESS_FILTERS.map((status) => (
              <SelectItem key={status} value={status}>
                {READINESS_LABELS[status].label}（{sheetRows.filter((row) => row.reportReadiness === status).length}）
              </SelectItem>
            ))}
          </SelectContent>
        </Select>

        {visibleRows.length === 0 ? (
          <div className="flex flex-col items-center gap-3 rounded-lg border border-dashed border-border px-4 py-6 text-center">
            <p className="text-small text-muted-foreground">
              {sheetRows.length === 0 ? `${SHEETS[sheet].label}の設備はありません。` : '条件に一致する設備はありません。'}
            </p>
            {sheetRows.length > 0 && (
              <Button
                type="button"
                size="sm"
                variant="outline"
                onClick={() => {
                  setFilter('');
                  setReadiness(ALL);
                }}
              >
                絞り込みをクリア
              </Button>
            )}
          </div>
        ) : (
          <ul className="max-h-80 divide-y divide-border overflow-y-auto rounded-lg border border-border xl:max-h-[min(42rem,70vh)]">
            {visibleRows.map((row) => {
              const isSelected = row === selected;
              const status = row.reportReadiness && READINESS_LABELS[row.reportReadiness];
              const name = row.companyName ?? '会員名未登録';
              return (
                <li key={row.equipmentId}>
                  <button
                    type="button"
                    aria-current={isSelected ? 'true' : undefined}
                    onClick={() => select(row)}
                    className={cn(
                      'flex w-full items-start gap-3 px-3 py-2.5 text-left transition-colors',
                      'focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-inset focus-visible:ring-ring',
                      isSelected ? 'bg-accent' : 'hover:bg-muted/60',
                    )}
                  >
                    <span className="min-w-0 flex-1">
                      <span
                        className={cn('block truncate text-sm font-medium text-foreground', isSelected && 'font-bold text-primary-strong')}
                        title={name}
                      >
                        {name}
                      </span>
                      <span className="block truncate text-xs text-muted-foreground" title={row.siteAddress ?? undefined}>
                        {row.siteAddress ?? EMPTY_VALUE}
                      </span>
                    </span>
                    {status && (
                      <StatusBadge tone={status.tone} className="shrink-0 whitespace-nowrap px-2 py-0.5">
                        {status.label}
                      </StatusBadge>
                    )}
                  </button>
                </li>
              );
            })}
          </ul>
        )}
      </aside>

      <section ref={detailRef} aria-label="設備の詳細" className="min-w-0 scroll-mt-24">
        {selected ? (
          <EquipmentDetail
            key={selected.equipmentId}
            projectId={projectId}
            row={selected}
            sheet={sheet}
            groupKey={groupKey}
            onGroupChange={setGroupKey}
            filter={itemFilter}
            onFilterChange={setItemFilter}
            copiedKey={copiedKey}
            onCopy={onCopy}
          />
        ) : (
          <div className="flex h-full min-h-40 items-center justify-center rounded-lg border border-dashed border-border p-6 text-small text-muted-foreground">
            設備を選択してください。
          </div>
        )}
      </section>
    </div>
  );
}

type DetailProps = Readonly<{
  projectId: string;
  row: IndividualActivityRow;
  sheet: BatterySheet;
  groupKey: GroupKey;
  onGroupChange: (key: GroupKey) => void;
  filter: string;
  onFilterChange: (filter: string) => void;
  copiedKey: string | null;
  onCopy: CopyHandler;
}>;

function EquipmentDetail({
  projectId,
  row,
  sheet,
  groupKey,
  onGroupChange,
  filter,
  onFilterChange,
  copiedKey,
  onCopy,
}: DetailProps) {
  const [preview, setPreview] = useState<EvidenceLink | null>(null);
  const detailQuery = useQuery({
    queryKey: ['admin', 'individual-activity', projectId, 'detail', row.equipmentId],
    queryFn: () => getIndividualActivityDetail(projectId, row.equipmentId),
  });
  const eligibility = row.eligibilityStatus && ELIGIBILITY_LABELS[row.eligibilityStatus];
  const status = row.reportReadiness && READINESS_LABELS[row.reportReadiness];

  const previewTarget: FilePreviewTarget | null = preview
    ? {
        cacheKey: ['individual-activity-evidence', preview.location],
        fileName: evidenceLabel(preview.kind),
        description: row.companyName ?? '証憑ファイル',
        kind: previewKindOf({ originalFileName: preview.fileName }),
        resolveUrl: () => resolveEvidenceUrl(preview.location),
        download: () =>
          void resolveEvidenceUrl(preview.location).then(downloadFromUrl, (error: unknown) =>
            toast({
              title: 'ファイルをダウンロードできませんでした',
              description: errorMessage(error, 'ファイルの取得に失敗しました。'),
              variant: 'destructive',
            }),
          ),
      }
    : null;

  return (
    <div className="overflow-hidden rounded-lg border border-border bg-card">
      <header className="flex flex-col gap-3 border-b border-border px-4 py-3 md:flex-row md:items-center md:justify-between">
        <div className="min-w-0 space-y-1.5">
          <h3 className="truncate text-base font-semibold text-foreground">{row.companyName ?? '会員名未登録'}</h3>
          <p className="text-small text-muted-foreground [overflow-wrap:anywhere]">{row.siteAddress ?? EMPTY_VALUE}</p>
          <div className="flex flex-wrap items-center gap-x-3 gap-y-1.5 text-small text-muted-foreground">
            {eligibility && <StatusBadge tone={eligibility.tone}>{eligibility.label}</StatusBadge>}
            {status && <StatusBadge tone={status.tone}>{status.label}</StatusBadge>}
            <span>
              発電 <span className="font-medium tabular-nums text-foreground">{formatKwh(row.generationKwh)}</span> kWh
            </span>
            <span>
              売電 <span className="font-medium tabular-nums text-foreground">{formatKwh(row.saleKwh)}</span> kWh
            </span>
          </div>
        </div>
        <div className="relative w-full shrink-0 md:w-60">
          <Search
            aria-hidden="true"
            className="pointer-events-none absolute left-3 top-1/2 size-4 -translate-y-1/2 text-muted-foreground"
          />
          <Input
            type="search"
            value={filter}
            onChange={(event) => onFilterChange(event.target.value)}
            placeholder="項目名・値で絞り込み"
            aria-label="項目名・値で絞り込み"
            className="h-9 pl-9"
          />
        </div>
      </header>

      {detailQuery.isLoading && (
        <div role="status" aria-label="設備の詳細を読み込んでいます" className="space-y-2 p-4">
          <Skeleton className="h-9 w-full" />
          {[0, 1, 2, 3].map((line) => (
            <Skeleton key={line} className="h-9 w-full" />
          ))}
        </div>
      )}

      {detailQuery.isError && (
        <div className="p-4">
          <LoadError
            error={detailQuery.error}
            fallback="設備の詳細を取得できませんでした。"
            onRetry={() => void detailQuery.refetch()}
          />
        </div>
      )}

      {detailQuery.data && (
        <DetailGroups
          detail={detailQuery.data}
          sheet={sheet}
          groupKey={groupKey}
          onGroupChange={onGroupChange}
          filter={filter}
          onClearFilter={() => onFilterChange('')}
          copiedKey={copiedKey}
          onCopy={onCopy}
          onPreview={setPreview}
        />
      )}

      <FilePreviewDialog target={previewTarget} onOpenChange={(open) => !open && setPreview(null)} />
    </div>
  );
}

/** The files one source of evidence holds: the equipment's, or the monitoring's. */
function EvidenceBar({
  title,
  files,
  onPreview,
}: Readonly<{ title: string; files: EvidenceLink[]; onPreview: (file: EvidenceLink) => void }>) {
  return (
    <div className="flex flex-wrap items-center gap-2 rounded-lg border border-border bg-muted/30 px-3 py-2.5">
      <span className="text-xs font-semibold text-muted-foreground">{title}</span>
      {files.length > 0 ? (
        files.map((file) => (
          <Button
            key={file.location}
            type="button"
            size="sm"
            variant="outline"
            className="h-8 bg-card"
            onClick={() => onPreview(file)}
          >
            <FileText aria-hidden="true" />
            {evidenceLabel(file.kind)}
          </Button>
        ))
      ) : (
        <span className="text-small text-muted-foreground">登録されていません</span>
      )}
    </div>
  );
}

type GroupsProps = Readonly<{
  detail: IndividualActivityDetail;
  sheet: BatterySheet;
  groupKey: GroupKey;
  onGroupChange: (key: GroupKey) => void;
  filter: string;
  onClearFilter: () => void;
  copiedKey: string | null;
  onCopy: CopyHandler;
  onPreview: (file: EvidenceLink) => void;
}>;

function DetailGroups({
  detail,
  sheet,
  groupKey,
  onGroupChange,
  filter,
  onClearFilter,
  copiedKey,
  onCopy,
  onPreview,
}: GroupsProps) {
  const filtering = filter.trim() !== '';
  const groups = useMemo(
    () =>
      toActivityGroups(detail, sheet).map((group) => {
        const sections = filterSections(group.sections, filter);
        const matchCount = sections.reduce(
          (sum, section) => sum + section.items.length + (section.table?.rows.length ?? 0),
          0,
        );
        return { ...group, sections, matchCount };
      }),
    [detail, sheet, filter],
  );
  const files = useMemo(() => evidenceFilesOf(detail), [detail]);

  return (
    <Tabs value={groupKey} onValueChange={(value) => onGroupChange(value as GroupKey)}>
      <div className="px-4">
        <PageTabsList aria-label="項目のグループ" className="gap-5">
          {groups.map((group) => (
            <PageTabsTrigger key={group.key} value={group.key} className="text-small">
              {group.title}
              {filtering && <TabCount count={group.matchCount} />}
            </PageTabsTrigger>
          ))}
        </PageTabsList>
      </div>

      {groups.map((group) => (
        <TabsContent key={group.key} value={group.key} className="mt-0 space-y-5 p-4">
          {!filtering && (group.key === 'equipment' || group.key === 'monitoring') && (
            <EvidenceBar
              title={group.key === 'equipment' ? '設備の証憑' : 'モニタリングの証憑'}
              files={files.filter((file) => (file.kind === MONITORING_EVIDENCE) === (group.key === 'monitoring'))}
              onPreview={onPreview}
            />
          )}
          {filtering && group.sections.length === 0 && (
            <div className="flex flex-col items-center gap-3 rounded-lg border border-dashed border-border px-4 py-6 text-center">
              <p className="text-small text-muted-foreground">
                「{filter.trim()}」に一致する項目はこのグループにありません。
              </p>
              <Button type="button" size="sm" variant="outline" onClick={onClearFilter}>
                絞り込みをクリア
              </Button>
            </div>
          )}
          {group.sections.map((section) => (
            <DocumentDataSectionCard
              key={section.key}
              section={section}
              copiedKey={copiedKey}
              onCopy={onCopy}
            />
          ))}
        </TabsContent>
      ))}
    </Tabs>
  );
}
