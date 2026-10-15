import { useState, type ReactNode } from 'react';
import * as AccordionPrimitive from '@radix-ui/react-accordion';
import { useQueryClient } from '@tanstack/react-query';
import { ArrowRight, ChevronRight, Download, Eye, FileText } from 'lucide-react';
import { Link } from 'react-router-dom';
import { getProjectMembershipPdfs, type ProjectMembershipPdf } from '@/api/admin/projectCompanies';
import { ActionButton } from '@/components/actions';
import { StatusBadge } from '@/components/data-display/StatusBadge';
import { AccordionContent, AccordionItem } from '@/components/ui/accordion';
import { Button } from '@/components/ui/button';
import { Skeleton } from '@/components/ui/skeleton';
import { Tooltip, TooltipContent, TooltipTrigger } from '@/components/ui/tooltip';
import {
  PROJECT_COMPANIES_STALE_MS,
  projectCompaniesQueryKey,
  useProjectCompaniesQuery,
} from '@/hooks/admin/useProjectCompaniesQuery';
import { toast } from '@/hooks/use-toast';
import { paths } from '@/routes/paths';
import { downloadFromUrl } from '@/utils/download';
import { cn } from '@/lib/utils';
import { FilePreviewDialog, type FilePreviewTarget } from './ProjectFilePreviewDialog';
import { RowIconButton, sectionRowGridClassName } from './ProjectFileRow';

export const MEMBERSHIP_SECTION_VALUE = 'MEMBERSHIP';

const LABEL = '入会届';

/** A row that does not expand: loading, failed, or no companies yet. */
function StaticRow({ status, action }: Readonly<{ status: ReactNode; action?: ReactNode }>) {
  return (
    <div className="flex min-h-11 items-center gap-3 py-1.5 pl-4 pr-3">
      <span aria-hidden="true" className="h-4 w-4 shrink-0" />
      <span className={sectionRowGridClassName}>
        <span className="truncate text-sm font-medium text-foreground">{LABEL}</span>
        <span className="text-small text-muted-foreground sm:col-span-2">{status}</span>
      </span>
      {action}
    </div>
  );
}

/**
 * The 入会届 each participating company submitted: one PDF per company, so the
 * rows are companies, not versions, and nothing is uploaded or deleted here.
 * Reviewing them stays in the 入会届 tab; each row links to its application.
 */
export function ProjectMembershipPdfSection({ projectId }: Readonly<{ projectId: string }>) {
  const queryClient = useQueryClient();
  const [previewCompany, setPreviewCompany] = useState<ProjectMembershipPdf | null>(null);

  const query = useProjectCompaniesQuery(projectId);

  /** A URL still within its lifetime: the cached list's, or a fresh list's. */
  const resolveUrl = async (participationId: number) => {
    const companies = await queryClient.fetchQuery({
      queryKey: projectCompaniesQueryKey(projectId),
      queryFn: () => getProjectMembershipPdfs(projectId),
      staleTime: PROJECT_COMPANIES_STALE_MS,
    });
    const url = companies.find((company) => company.participationId === participationId)?.pdfUrl;
    if (!url) throw new Error('PDFが見つかりませんでした。');
    return url;
  };

  const download = async (company: ProjectMembershipPdf) => {
    try {
      downloadFromUrl(await resolveUrl(company.participationId));
    } catch (error) {
      toast({
        title: 'ファイルを取得できませんでした',
        description:
          error instanceof Error && error.message ? error.message : '時間をおいて再度お試しください。',
        variant: 'destructive',
      });
    }
  };

  if (query.isLoading) {
    return (
      <StaticRow
        status={
          <span role="status" aria-label="入会届を読み込んでいます">
            <Skeleton className="h-4 w-32" />
          </span>
        }
      />
    );
  }

  if (query.isError) {
    return (
      <StaticRow
        status={<span className="text-destructive">取得できませんでした</span>}
        action={<ActionButton kind="retry" onClick={() => void query.refetch()} />}
      />
    );
  }

  const companies = query.data ?? [];
  if (companies.length === 0) return <StaticRow status="参加企業なし" />;

  const previewTarget: FilePreviewTarget | null = previewCompany
    ? {
        cacheKey: ['admin', 'projects', projectId, 'membership-pdf', previewCompany.participationId],
        fileName: `${LABEL}_${previewCompany.companyName}.pdf`,
        description: `${LABEL} ・ ${previewCompany.companyName}`,
        kind: 'pdf',
        resolveUrl: () => resolveUrl(previewCompany.participationId),
        download: () => void download(previewCompany),
      }
    : null;

  return (
    <>
      <AccordionItem value={MEMBERSHIP_SECTION_VALUE} className="border-b-0">
        <AccordionPrimitive.Header className="flex items-center pr-3 transition-colors hover:bg-muted/60">
          <AccordionPrimitive.Trigger className="group flex min-w-0 flex-1 items-center gap-3 py-3 pl-4 text-left focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-inset focus-visible:ring-ring">
            <ChevronRight
              aria-hidden="true"
              className="h-4 w-4 shrink-0 text-muted-foreground transition-transform duration-200 group-data-[state=open]:rotate-90 motion-reduce:transition-none"
            />
            <span className={sectionRowGridClassName}>
              <span className="truncate text-sm font-medium text-foreground">{LABEL}</span>
              <span className="flex items-center gap-2 text-small text-muted-foreground sm:contents">
                {/* 社, not 件: one PDF per company, so the count is of companies. */}
                <StatusBadge className="w-fit tabular-nums">{companies.length}社</StatusBadge>
                <span className="whitespace-nowrap">
                  参加企業ごと・閲覧のみ
                </span>
              </span>
            </span>
          </AccordionPrimitive.Trigger>
        </AccordionPrimitive.Header>
        <AccordionContent className="border-t border-border bg-muted/30 p-0">
          <ul className="divide-y divide-border" aria-label={`${LABEL}の一覧`}>
            {companies.map((company) => {
              const ready = Boolean(company.pdfUrl);
              const name = `${company.companyName}の${LABEL}`;
              return (
                <li
                  key={company.participationId}
                  className="flex flex-col gap-2 px-4 py-3 sm:flex-row sm:items-center sm:gap-4"
                >
                  <div className="flex min-w-0 flex-1 items-center gap-3">
                    <FileText
                      aria-hidden="true"
                      className={cn('h-5 w-5 shrink-0', ready ? 'text-destructive' : 'text-muted-foreground')}
                    />
                    <div className="min-w-0">
                      <p className="break-all text-sm font-medium text-foreground">
                        {company.companyName}
                      </p>
                      {ready ? null : (
                        <p className="text-small text-muted-foreground">PDF準備中</p>
                      )}
                    </div>
                  </div>

                  <div className="flex shrink-0 items-center gap-2 self-end sm:self-auto">
                    <RowIconButton
                      label={`${name}を表示`}
                      tooltip={ready ? '表示' : 'PDFの準備ができていません'}
                      disabled={!ready}
                      onClick={() => setPreviewCompany(company)}
                    >
                      <Eye aria-hidden="true" />
                    </RowIconButton>
                    <RowIconButton
                      label={`${name}をダウンロード`}
                      tooltip={ready ? 'ダウンロード' : 'PDFの準備ができていません'}
                      disabled={!ready}
                      onClick={() => void download(company)}
                    >
                      <Download aria-hidden="true" />
                    </RowIconButton>
                    {/* Where the delete sits on versioned files: here, the way to the review. */}
                    <span aria-hidden="true" className="mx-1 h-5 w-px bg-border" />
                    <Tooltip>
                      <TooltipTrigger asChild>
                        <Button asChild size="icon" variant="ghost">
                          <Link
                            to={paths.adminProjectMembershipDetail(projectId, company.participationId)}
                            aria-label={`${company.companyName}の${LABEL}詳細を開く`}
                          >
                            <ArrowRight aria-hidden="true" />
                          </Link>
                        </Button>
                      </TooltipTrigger>
                      <TooltipContent>入会届の詳細を開く</TooltipContent>
                    </Tooltip>
                  </div>
                </li>
              );
            })}
          </ul>
        </AccordionContent>
      </AccordionItem>

      <FilePreviewDialog
        target={previewTarget}
        onOpenChange={(open) => !open && setPreviewCompany(null)}
      />
    </>
  );
}
