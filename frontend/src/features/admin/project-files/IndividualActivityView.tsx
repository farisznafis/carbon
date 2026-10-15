import { useMemo, useState } from 'react';
import * as AccordionPrimitive from '@radix-ui/react-accordion';
import { useQuery, useQueryClient } from '@tanstack/react-query';
import { Check, ChevronRight, ClipboardCopy, FileSearch, FileText, Loader2, PenLine, Search } from 'lucide-react';
import {
  getIndividualActivityDetail,
  getIndividualActivityRows,
  type IndividualActivityRow,
  type ReportReadiness,
} from '@/api/admin/individualActivity';
import type { DocumentDataItem } from '@/api/admin/projectDocumentData';
import { ActionButton } from '@/components/actions';
import { StatusBadge } from '@/components/data-display/StatusBadge';
import {
  SegmentButton,
  SegmentGroup,
  SegmentTabsList,
  SegmentTabsTrigger,
  TabCount,
} from '@/components/navigation/PageTabs';
import { Accordion, AccordionContent, AccordionItem } from '@/components/ui/accordion';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Skeleton } from '@/components/ui/skeleton';
import { Tabs } from '@/components/ui/tabs';
import { Tooltip, TooltipContent, TooltipTrigger } from '@/components/ui/tooltip';
import { toast } from '@/hooks/use-toast';
import { downloadFromUrl } from '@/utils/download';
import { DocumentDataSectionCard, type CopyHandler } from './DocumentDataSectionCard';
import { EMPTY_VALUE } from './documentDataCopy';
import { FilePreviewDialog, type FilePreviewTarget } from './ProjectFilePreviewDialog';
import { previewKindOf } from './projectFileTypes';
import {
  ELIGIBILITY_LABELS,
  READINESS_LABELS,
  SHEETS,
  countBlanks,
  evidenceLinks,
  sheetOf,
  sheetRowId,
  toSheetRow,
  type ActivityItem,
  type BatterySheet,
  type EvidenceLink,
} from './individualActivitySections';

const READINESS_FILTERS: ReportReadiness[] = ['MISSING_EVIDENCE', 'NO_BATCH_RESULT', 'READY'];
const SHEET_ORDER: BatterySheet[] = ['without', 'with'];
const EVIDENCE_LABELS = { equipment: '設備の証憑', monitoring: 'モニタリングの証憑' } as const;

type SheetRow = IndividualActivityRow & { rowId: string };

const errorMessage = (error: unknown, fallback: string) =>
  error instanceof Error && error.message ? error.message : fallback;

const formatKwh = (value: number | null) =>
  value === null || value === undefined ? EMPTY_VALUE : value.toLocaleString('ja-JP');

const detailQueryOptions = (projectId: string, equipmentId: number) => ({
  queryKey: ['admin', 'individual-activity', projectId, 'detail', equipmentId],
  queryFn: () => getIndividualActivityDetail(projectId, equipmentId),
});

/** A blank says what to do about it: type it in, or read it off the evidence. */
function MissingValue({ item }: Readonly<{ item: DocumentDataItem }>) {
  const { source } = item as ActivityItem;
  if (source === 'manual') {
    return (
      <span className="inline-flex items-center gap-1.5 text-small font-normal text-muted-foreground">
        <PenLine aria-hidden="true" className="size-3.5 shrink-0" />
        Excelで手入力
      </span>
    );
  }
  return (
    <span className="inline-flex items-center gap-1.5 text-small font-medium text-[hsl(var(--warning-800))]">
      <FileSearch aria-hidden="true" className="size-3.5 shrink-0" />
      {source === 'member' ? '未登録・会員に確認' : `${EVIDENCE_LABELS[source]}で確認`}
    </span>
  );
}

/** The label with its Excel column, so the operator finds the cell at a glance. */
function ColumnLabel({ item }: Readonly<{ item: DocumentDataItem }>) {
  return (
    <span className="inline-flex items-baseline gap-2">
      <span className="inline-flex min-w-7 justify-center rounded bg-muted px-1 font-mono text-xs text-secondary-foreground">
        {(item as ActivityItem).column}
      </span>
      <span>{item.label}</span>
    </span>
  );
}

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

type ViewProps = Readonly<{ projectId: string; copiedKey: string | null; onCopy: CopyHandler }>;

/**
 * 個別活動実績報告リスト in データ取得, one tab per 会員情報 sheet of the Excel.
 * Each equipment is a sheet row: copied whole into the ID cell, or opened to
 * copy single values and see which blanks to look up.
 */
export function IndividualActivityView({ projectId, copiedKey, onCopy }: ViewProps) {
  const [sheet, setSheet] = useState<BatterySheet>('without');
  const [filter, setFilter] = useState('');
  const [readiness, setReadiness] = useState<ReportReadiness | null>(null);
  const [preview, setPreview] = useState<EvidenceLink | null>(null);

  const rowsQuery = useQuery({
    queryKey: ['admin', 'individual-activity', projectId, 'rows'],
    queryFn: () => getIndividualActivityRows(projectId),
    enabled: Boolean(projectId),
  });

  // IDs are numbered before any filter, so a row keeps its ID while the list is narrowed.
  const rowsBySheet = useMemo(() => {
    const bySheet: Record<BatterySheet, SheetRow[]> = { without: [], with: [] };
    for (const row of rowsQuery.data ?? []) {
      const target = bySheet[sheetOf(row.hasBattery)];
      target.push({ ...row, rowId: sheetRowId(sheetOf(row.hasBattery), target.length) });
    }
    return bySheet;
  }, [rowsQuery.data]);

  const sheetRows = rowsBySheet[sheet];
  const visibleRows = useMemo(() => {
    const needle = filter.trim().toLowerCase();
    return sheetRows.filter(
      (row) =>
        (!readiness || row.reportReadiness === readiness) &&
        (!needle ||
          [row.rowId, row.companyName, row.siteAddress].some((value) => (value ?? '').toLowerCase().includes(needle))),
    );
  }, [sheetRows, filter, readiness]);

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

  const changeSheet = (value: string) => {
    setSheet(value as BatterySheet);
    setReadiness(null);
  };

  const previewTarget: FilePreviewTarget | null = preview
    ? {
        cacheKey: ['individual-activity-evidence', preview.url],
        fileName: preview.fileName,
        description: preview.fileType ?? '証憑ファイル',
        kind: previewKindOf({ originalFileName: preview.fileName }),
        resolveUrl: () => Promise.resolve(preview.url),
        download: () => downloadFromUrl(preview.url),
      }
    : null;

  return (
    <div className="space-y-4">
      <Tabs value={sheet} onValueChange={changeSheet} className="space-y-2">
        <SegmentTabsList aria-label="Excelのシート">
          {SHEET_ORDER.map((key) => (
            <SegmentTabsTrigger key={key} value={key}>
              {SHEETS[key].label}
              <TabCount count={rowsBySheet[key].length} />
            </SegmentTabsTrigger>
          ))}
        </SegmentTabsList>
        <p className="text-small text-muted-foreground">
          Excelの「{SHEETS[sheet].sheetName}」シートに対応します。「行をコピー」して、同じIDの行のC列（ID）に貼り付けると1行分がまとめて入ります。
        </p>
      </Tabs>

      <div className="flex flex-col gap-3 md:flex-row md:items-center md:justify-between">
        <SegmentGroup aria-label="報告状況で絞り込み">
          <SegmentButton active={readiness === null} onClick={() => setReadiness(null)}>
            すべて
          </SegmentButton>
          {READINESS_FILTERS.map((status) => (
            <SegmentButton
              key={status}
              active={readiness === status}
              tone={READINESS_LABELS[status].tone}
              onClick={() => setReadiness(status)}
            >
              {READINESS_LABELS[status].label}
              <TabCount count={sheetRows.filter((row) => row.reportReadiness === status).length} />
            </SegmentButton>
          ))}
        </SegmentGroup>
        <div className="relative w-full shrink-0 md:w-64">
          <Search
            aria-hidden="true"
            className="pointer-events-none absolute left-3 top-1/2 size-4 -translate-y-1/2 text-muted-foreground"
          />
          <Input
            type="search"
            value={filter}
            onChange={(event) => setFilter(event.target.value)}
            placeholder="ID・会員名・住所で検索"
            aria-label="ID・会員名・住所で検索"
            className="h-9 pl-9"
          />
        </div>
      </div>

      {visibleRows.length === 0 ? (
        <div className="flex flex-col items-center gap-3 rounded-lg border border-border px-4 py-8 text-center">
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
                setReadiness(null);
              }}
            >
              絞り込みをクリア
            </Button>
          )}
        </div>
      ) : (
        <Accordion type="multiple" className="divide-y divide-border overflow-hidden rounded-lg border border-border">
          {visibleRows.map((row) => (
            <ActivityRow
              key={row.equipmentId}
              row={row}
              sheet={sheet}
              projectId={projectId}
              copiedKey={copiedKey}
              onCopy={onCopy}
              onPreview={setPreview}
            />
          ))}
        </Accordion>
      )}

      <FilePreviewDialog target={previewTarget} onOpenChange={(open) => !open && setPreview(null)} />
    </div>
  );
}

type RowProps = Readonly<{
  row: SheetRow;
  sheet: BatterySheet;
  projectId: string;
  copiedKey: string | null;
  onCopy: CopyHandler;
  onPreview: (file: EvidenceLink) => void;
}>;

function ActivityRow(props: RowProps) {
  const { row, sheet, projectId, copiedKey, onCopy } = props;
  const queryClient = useQueryClient();
  const [isCopying, setIsCopying] = useState(false);
  const eligibility = row.eligibilityStatus && ELIGIBILITY_LABELS[row.eligibilityStatus];
  const status = row.reportReadiness && READINESS_LABELS[row.reportReadiness];
  const name = row.companyName ?? '会員名未登録';
  const copyKey = `row::${row.equipmentId}`;
  const copied = copiedKey === copyKey;

  // Fetches the detail the same way opening the row does, so a later open is instant.
  const copyRow = async () => {
    setIsCopying(true);
    try {
      const detail = await queryClient.fetchQuery(detailQueryOptions(projectId, row.equipmentId));
      onCopy(copyKey, toSheetRow(detail, sheet, row.rowId).tsv, `ID ${row.rowId}の行`);
    } catch (error) {
      toast({
        title: '行をコピーできませんでした',
        description: errorMessage(error, '設備の詳細を取得できませんでした。'),
        variant: 'destructive',
      });
    } finally {
      setIsCopying(false);
    }
  };

  return (
    <AccordionItem value={String(row.equipmentId)} className="border-b-0">
      <AccordionPrimitive.Header className="flex items-center gap-2 pr-3 transition-colors hover:bg-muted/60">
        <AccordionPrimitive.Trigger className="group grid min-w-0 flex-1 grid-cols-[auto_auto_minmax(0,1fr)] items-center gap-x-3 gap-y-2 py-3 pl-4 text-left focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-inset focus-visible:ring-ring lg:grid-cols-[auto_auto_minmax(0,1fr)_auto_auto]">
          <ChevronRight
            aria-hidden="true"
            className="size-4 shrink-0 text-muted-foreground transition-transform duration-200 group-data-[state=open]:rotate-90 motion-reduce:transition-none"
          />
          <span className="inline-flex min-w-12 justify-center rounded-md border border-border bg-card px-2 py-0.5 font-mono text-small font-semibold tabular-nums text-foreground">
            {row.rowId}
          </span>
          <span className="min-w-0">
            <span className="block truncate text-sm font-semibold text-foreground" title={name}>{name}</span>
            <span className="block truncate text-small text-muted-foreground" title={row.siteAddress ?? undefined}>
              {row.siteAddress ?? EMPTY_VALUE}
            </span>
          </span>
          <span className="col-start-3 flex flex-wrap items-center gap-1.5 lg:col-start-auto">
            {eligibility && <StatusBadge tone={eligibility.tone}>{eligibility.label}</StatusBadge>}
            {status && <StatusBadge tone={status.tone}>{status.label}</StatusBadge>}
          </span>
          <span className="col-start-3 flex gap-4 text-small text-muted-foreground lg:col-start-auto lg:w-52 lg:justify-end">
            <span>
              発電 <span className="font-medium tabular-nums text-foreground">{formatKwh(row.generationKwh)}</span> kWh
            </span>
            <span>
              売電 <span className="font-medium tabular-nums text-foreground">{formatKwh(row.saleKwh)}</span> kWh
            </span>
          </span>
        </AccordionPrimitive.Trigger>
        <Tooltip>
          <TooltipTrigger asChild>
            <Button
              type="button"
              size="sm"
              variant="outline"
              className={copied ? 'shrink-0 text-success-strong hover:text-success-strong' : 'shrink-0'}
              aria-label={`ID ${row.rowId}の行をコピー`}
              disabled={isCopying}
              onClick={() => void copyRow()}
            >
              {isCopying ? (
                <Loader2 aria-hidden="true" className="animate-spin" />
              ) : copied ? (
                <Check aria-hidden="true" />
              ) : (
                <ClipboardCopy aria-hidden="true" />
              )}
              <span className="hidden sm:inline">{copied ? 'コピーしました' : '行をコピー'}</span>
            </Button>
          </TooltipTrigger>
          <TooltipContent>C列（ID）のセルを選択して貼り付け</TooltipContent>
        </Tooltip>
      </AccordionPrimitive.Header>
      <AccordionContent className="border-t border-border bg-muted/30 p-0">
        <ActivityDetail {...props} />
      </AccordionContent>
    </AccordionItem>
  );
}

/** Mounted only while its row is open, so the detail is fetched on first open. */
function ActivityDetail({ projectId, row, sheet, copiedKey, onCopy, onPreview }: RowProps) {
  const detailQuery = useQuery(detailQueryOptions(projectId, row.equipmentId));
  const detail = detailQuery.data;
  const sheetRow = useMemo(() => (detail ? toSheetRow(detail, sheet, row.rowId) : null), [detail, sheet, row.rowId]);

  if (detailQuery.isLoading) {
    return (
      <div role="status" aria-label="設備の詳細を読み込んでいます" className="space-y-2 p-4">
        <Skeleton className="h-4 w-40" />
        {[0, 1, 2].map((line) => (
          <Skeleton key={line} className="h-9 w-full" />
        ))}
      </div>
    );
  }

  if (detailQuery.isError || !detail || !sheetRow) {
    return (
      <div className="p-4">
        <LoadError
          error={detailQuery.error}
          fallback="設備の詳細を取得できませんでした。"
          onRetry={() => void detailQuery.refetch()}
        />
      </div>
    );
  }

  const { toCheck, manual } = countBlanks(sheetRow.groups);
  const evidenceOf = { equipment: detail.equipment?.evidenceFiles, monitoring: detail.monitoring?.evidenceFiles };

  return (
    <div className="space-y-6 p-4">
      <p className="flex flex-wrap gap-x-4 gap-y-1 text-small text-muted-foreground">
        <span>
          シートの列 <span className="font-mono text-foreground">{sheetRow.range}</span>
        </span>
        <span className="inline-flex items-center gap-1.5">
          <FileSearch aria-hidden="true" className="size-3.5 text-[hsl(var(--warning-800))]" />
          要確認 <span className="font-semibold tabular-nums text-foreground">{toCheck}</span>件
        </span>
        <span className="inline-flex items-center gap-1.5">
          <PenLine aria-hidden="true" className="size-3.5" />
          Excelで手入力 <span className="font-semibold tabular-nums text-foreground">{manual}</span>件
        </span>
      </p>

      {sheetRow.groups.map((group) => {
        const headingId = `activity-${row.equipmentId}-${group.key}`;
        const files = group.evidence ? evidenceLinks(evidenceOf[group.evidence]) : [];
        return (
          <section key={group.key} aria-labelledby={headingId} className="space-y-3">
            <h4 id={headingId} className="border-b border-border pb-1.5 text-xs font-semibold tracking-wide text-muted-foreground">
              {group.title}
            </h4>

            {group.evidence && (
              <div className="flex flex-wrap items-center gap-2 rounded-lg border border-border bg-card px-3 py-2">
                <span className="text-small font-medium text-foreground">{EVIDENCE_LABELS[group.evidence]}</span>
                {files.length > 0 ? (
                  files.map((file, index) => (
                    <Button
                      key={`${file.url}-${index}`}
                      type="button"
                      size="sm"
                      variant="outline"
                      className="h-8 max-w-[16rem]"
                      title={file.fileName}
                      onClick={() => onPreview(file)}
                    >
                      <FileText aria-hidden="true" />
                      <span className="truncate">{file.fileName}</span>
                    </Button>
                  ))
                ) : (
                  <span className="text-small text-muted-foreground">まだ登録されていません</span>
                )}
              </div>
            )}

            {group.sections.map((section) => (
              <DocumentDataSectionCard
                key={section.key}
                section={section}
                copiedKey={copiedKey}
                onCopy={onCopy}
                renderMissing={(item) => <MissingValue item={item} />}
                renderLabel={(item) => <ColumnLabel item={item} />}
              />
            ))}
          </section>
        );
      })}
    </div>
  );
}
