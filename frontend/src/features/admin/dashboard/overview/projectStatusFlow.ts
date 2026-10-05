import type { ProjectStatus } from '@/api/admin/projects';
import type { Project } from '@/api/projects';
import { normalizeProjectStatus } from '../projectStatusPresentation';

/** Statuses on the main path, in the order a project moves through them. */
export const PROJECT_STATUS_FLOW: readonly ProjectStatus[] = [
  '作成済み',
  '準備中',
  '妥当性確認処理',
  '実施中',
  '検証中',
  'クレジット申請中',
  '終了',
];

/** Statuses that leave the main path and need an operator decision. */
export const PROJECT_STATUS_BRANCHES: readonly ProjectStatus[] = [
  '変更中',
  '却下',
];

export const OTHER_PROJECT_STATUS_LABEL = 'その他';

export type ProjectStatusFlowStage = {
  status: string;
  count: number;
  /** Share of the largest stage, for bar length (0–100). */
  ratio: number;
};

export type ProjectStatusFlowSummary = {
  total: number;
  stages: ProjectStatusFlowStage[];
  branches: ProjectStatusFlowStage[];
  otherCount: number;
};

/** Counts projects per status and arranges them along the process flow. */
export function buildProjectStatusFlow(
  projects: readonly Project[],
): ProjectStatusFlowSummary {
  const counts = new Map<string, number>();
  projects.forEach((project) => {
    const status = normalizeProjectStatus(project.projectStatus);
    counts.set(status, (counts.get(status) ?? 0) + 1);
  });

  const knownStatuses = new Set<string>([
    ...PROJECT_STATUS_FLOW,
    ...PROJECT_STATUS_BRANCHES,
  ]);
  const otherCount = [...counts.entries()].reduce(
    (total, [status, count]) => total + (knownStatuses.has(status) ? 0 : count),
    0,
  );
  const maxCount = Math.max(
    0,
    ...[...knownStatuses].map((status) => counts.get(status) ?? 0),
  );
  const toStage = (status: string): ProjectStatusFlowStage => {
    const count = counts.get(status) ?? 0;
    return {
      status,
      count,
      ratio: maxCount > 0 ? (count / maxCount) * 100 : 0,
    };
  };

  return {
    total: projects.length,
    stages: PROJECT_STATUS_FLOW.map(toStage),
    branches: PROJECT_STATUS_BRANCHES.map(toStage),
    otherCount,
  };
}
