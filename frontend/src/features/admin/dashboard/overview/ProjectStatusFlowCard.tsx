import { useMemo, type ReactNode } from 'react';
import type { Project } from '@/api/projects';
import { ChartCard } from '@/components/data-display/charts';
import { cn } from '@/lib/utils';
import {
  buildProjectStatusFlow,
  OTHER_PROJECT_STATUS_LABEL,
  type ProjectStatusFlowStage,
} from './projectStatusFlow';

const BRANCH_BAR_CLASS_NAMES: Readonly<Record<string, string>> = {
  変更中: 'bg-warning',
  却下: 'bg-destructive',
};

/** One labelled horizontal bar; the step marker draws the flow connector. */
function StatusBarRow({
  stage,
  marker,
  barClassName,
  showConnector,
}: Readonly<{
  stage: ProjectStatusFlowStage;
  marker: ReactNode;
  barClassName: string;
  showConnector: boolean;
}>) {
  return (
    <li className="relative grid grid-cols-[1.5rem_8.5rem_1fr_2.5rem] items-center gap-3 py-1.5">
      {showConnector && (
        <span
          aria-hidden="true"
          className="absolute left-3 top-1/2 h-full w-px -translate-x-1/2 bg-border"
        />
      )}
      <span className="relative z-10 flex h-6 w-6 items-center justify-center rounded-full border border-border bg-card text-subtle font-bold text-muted-foreground">
        {marker}
      </span>
      <span className="truncate text-small text-foreground" title={stage.status}>
        {stage.status}
      </span>
      <span className="h-3 overflow-hidden rounded-full bg-muted" aria-hidden="true">
        <span
          className={cn('block h-full rounded-full transition-[width]', barClassName)}
          style={{ width: `${stage.count > 0 ? Math.max(stage.ratio, 4) : 0}%` }}
        />
      </span>
      <span className="text-right text-small font-bold tabular-nums text-foreground">
        {stage.count.toLocaleString('ja-JP')}
      </span>
    </li>
  );
}

/** Shows how many projects sit at each step of the project process flow. */
export function ProjectStatusFlowCard({
  projects,
}: Readonly<{ projects: readonly Project[] }>) {
  const summary = useMemo(() => buildProjectStatusFlow(projects), [projects]);
  // Keep each stage's position in the full flow so the step numbers stay meaningful.
  const visibleStages = summary.stages
    .map((stage, index) => ({ stage, step: index + 1 }))
    .filter(({ stage }) => stage.count > 0);
  const visibleBranches = summary.branches.filter((stage) => stage.count > 0);
  const hasOffFlow = visibleBranches.length > 0 || summary.otherCount > 0;
  const accessibleSummary = [
    ...visibleStages.map(({ stage }) => stage),
    ...visibleBranches,
  ]
    .map((stage) => `${stage.status}${stage.count}件`)
    .concat(
      summary.otherCount > 0
        ? [`${OTHER_PROJECT_STATUS_LABEL}${summary.otherCount}件`]
        : [],
    )
    .join('、');

  return (
    <ChartCard
      title="プロジェクト状況"
      description="作成から終了までの処理の流れに沿って、各ステータスの件数を表示します。"
      headline={
        <span className="text-small text-muted-foreground">
          合計{' '}
          <strong className="text-foreground">
            {summary.total.toLocaleString('ja-JP')}
          </strong>{' '}
          件
        </span>
      }
      isEmpty={summary.total === 0}
      emptyMessage="登録済みプロジェクトはありません。"
    >
      <div role="img" aria-label={`ステータス別件数：${accessibleSummary}`}>
        {visibleStages.length > 0 && (
          <ol aria-hidden="true">
            {visibleStages.map(({ stage, step }, index) => (
              <StatusBarRow
                key={stage.status}
                stage={stage}
                marker={step}
                barClassName="bg-primary"
                showConnector={index < visibleStages.length - 1}
              />
            ))}
          </ol>
        )}

        {hasOffFlow && (
          <div
            aria-hidden="true"
            className={cn(
              visibleStages.length > 0 && 'mt-4 border-t border-border pt-3',
            )}
          >
            <p className="mb-1 text-subtle text-muted-foreground">
              フロー外（要確認）
            </p>
            <ul>
              {visibleBranches.map((stage) => (
                <StatusBarRow
                  key={stage.status}
                  stage={stage}
                  marker="!"
                  barClassName={BRANCH_BAR_CLASS_NAMES[stage.status] ?? 'bg-muted-foreground'}
                  showConnector={false}
                />
              ))}
              {summary.otherCount > 0 && (
                <StatusBarRow
                  stage={{
                    status: OTHER_PROJECT_STATUS_LABEL,
                    count: summary.otherCount,
                    ratio: 0,
                  }}
                  marker="?"
                  barClassName="bg-muted-foreground"
                  showConnector={false}
                />
              )}
            </ul>
          </div>
        )}
      </div>
    </ChartCard>
  );
}
