/**
 * 申請区分 — which filing a plan is — and what the wizard shows for it.
 *
 * The official 登録申請書 serves three filings. Which one a plan is follows
 * from where the project stands, so the operator never picks it: the server
 * owns the value (ApplicationCategoryDerivation) and sends it read-only.
 *
 * Until plans are versioned every plan is the first filing, プロジェクト登録.
 * The pages and fields below only mean something for a change filed against
 * an earlier approved plan, so a registration does not show them.
 */
export type ApplicationCategory = '登録' | '計画変更' | '認証権限保有主体変更';

export const CURRENT_APPLICATION_CATEGORY: ApplicationCategory = '登録';

/** 5.プロジェクト計画変更情報 and 7.認証権限保有主体変更届. */
const CHANGE_ONLY_STEPS: ReadonlySet<number> = new Set([5, 7]);

/** 変更有無 flags and 計画変更日: meaningless without a plan to compare against. */
const isChangeOnlyField = (name: string) => {
  const bare = name.split('.').pop() ?? name;
  return bare.startsWith('isChange') || bare === 'applicationChangeDate';
};

type CategoryStep = {
  id: number;
  fields: ReadonlyArray<{ name: unknown }>;
};

/**
 * The steps a plan of this category files, each without the fields it has no
 * use for. Step ids are kept as they are — they are the official sheet order
 * and every page mapping is keyed by them — so the list may have gaps.
 */
export function stepsForApplicationCategory<TStep extends CategoryStep>(
  steps: ReadonlyArray<TStep>,
  category: ApplicationCategory = CURRENT_APPLICATION_CATEGORY,
): TStep[] {
  if (category !== '登録') return [...steps];

  return steps
    .filter((step) => !CHANGE_ONLY_STEPS.has(step.id))
    .map(
      (step) =>
        ({
          ...step,
          fields: step.fields.filter((field) => !isChangeOnlyField(String(field.name))),
        }) as TStep,
    );
}

/**
 * The step before or after `current` in a list that may have gaps, or
 * `current` itself at either end.
 */
export function adjacentStepId(
  steps: ReadonlyArray<{ id: number }>,
  current: number,
  direction: 1 | -1,
): number {
  const index = steps.findIndex((step) => step.id === current);
  if (index === -1) return steps[0]?.id ?? current;
  return steps[Math.min(Math.max(index + direction, 0), steps.length - 1)].id;
}

/** `target` if the list has it, otherwise the nearest step at or below it. */
export function clampStepId(steps: ReadonlyArray<{ id: number }>, target: number): number {
  if (steps.some((step) => step.id === target)) return target;
  const below = steps.filter((step) => step.id < target);
  return (below.length > 0 ? below[below.length - 1] : steps[0])?.id ?? target;
}
