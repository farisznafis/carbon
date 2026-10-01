import { useMemo } from 'react';
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

/** Renders one project status as a labelled horizontal bar. */
function StatusBarRow({
  stage,
  barClassName,
}: Readonly<{
  stage: ProjectStatusFlowStage;
  barClassName: string;
}>) {
  return (
    <li className="grid grid-cols-[8.5rem_1fr_2.5rem] items-center gap-3 py-1.5">
      <span
        className="truncate text-small text-foreground"
        title={stage.status}
      >
        {stage.status}
      </span>

      <span
        className="h-3 overflow-hidden rounded-full bg-muted"
        aria-hidden="true"
      >
        <span
          className={cn(
            'block h-full rounded-full transition-[width]',
            barClassName,
          )}
          style={{
            width: `${stage.count > 0 ? Math.max(stage.ratio, 4) : 0}%`,
          }}
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
}: Readonly<{
  projects: readonly Project[];
}>) {
  const summary = useMemo(
    () => buildProjectStatusFlow(projects),
    [projects],
  );

  const visibleStages = summary.stages.filter(
    (stage) => stage.count > 0,
  );

  const visibleBranches = summary.branches.filter(
    (stage) => stage.count > 0,
  );

  const hasOffFlow =
    visibleBranches.length > 0 ||
    summary.otherCount > 0;

  const accessibleSummary = [
    ...visibleStages,
    ...visibleBranches,
  ]
    .map(
      (stage) =>
        `${stage.status}${stage.count}件`,
    )
    .concat(
      summary.otherCount > 0
        ? [
            `${OTHER_PROJECT_STATUS_LABEL}${summary.otherCount}件`,
          ]
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
            {summary.total.toLocaleString(
              'ja-JP',
            )}
          </strong>{' '}
          件
        </span>
      }
      isEmpty={summary.total === 0}
      emptyMessage="登録済みプロジェクトはありません。"
    >
      <div
        role="img"
        aria-label={`ステータス別件数：${accessibleSummary}`}
      >
        {visibleStages.length > 0 && (
          <ul aria-hidden="true">
            {visibleStages.map((stage) => (
              <StatusBarRow
                key={stage.status}
                stage={stage}
                barClassName="bg-primary"
              />
            ))}
          </ul>
        )}

        {hasOffFlow && (
          <div
            aria-hidden="true"
            className={cn(
              visibleStages.length > 0 &&
                'mt-4 border-t border-border pt-3',
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
                  barClassName={
                    BRANCH_BAR_CLASS_NAMES[
                      stage.status
                    ] ??
                    'bg-muted-foreground'
                  }
                />
              ))}

              {summary.otherCount > 0 && (
                <StatusBarRow
                  stage={{
                    status:
                      OTHER_PROJECT_STATUS_LABEL,
                    count:
                      summary.otherCount,
                    ratio: 0,
                  }}
                  barClassName="bg-muted-foreground"
                />
              )}
            </ul>
          </div>
        )}
      </div>
    </ChartCard>
  );
}