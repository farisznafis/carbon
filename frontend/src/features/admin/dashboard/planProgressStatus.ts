import type { ProjectPlanProgressSummary } from '@/api/admin/projects';

/** True when a project plan has pages but not all of them are filled in yet. */
export function isPlanEntryIncomplete(
  progress: ProjectPlanProgressSummary | undefined,
): progress is ProjectPlanProgressSummary {
  return Boolean(progress && !progress.complete && progress.totalForms > 0);
}

export const INCOMPLETE_PLAN_LABEL = '記入未完了';
