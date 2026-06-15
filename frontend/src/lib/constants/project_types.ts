// THIS USED FOR SCHEMA THAT USE PROJECT TYPES LIST
// already used on applicableMethodology.tsx

export const PROJECT_TYPES = [
  { label: "更新プロジェクトのみを対象とする", value: "更新プロジェクトのみを対象とする" }, // Applies only to update projects
  { label: "新設プロジェクトのみを対象とする", value: "新設プロジェクトのみを対象とする" }, // Applies only to new projects
  { label: "更新プロジェクトと新設プロジェクトの双方を対象とする", value: "更新プロジェクトと新設プロジェクトの双方を対象とする" }, // Applies to both update and new projects
];

export type Project_Type = (typeof PROJECT_TYPES)[number];

// for schema
export const PROJECT_TYPE_OPTIONS = [
  '更新プロジェクトのみを対象とする',
  '新設プロジェクトのみを対象とする',
  '更新プロジェクトと新設プロジェクトの双方を対象とする',
] as const;

export const PROJECT_TYPE_VALIDATION = {
  required_error: 'プロジェクト種類を選択してください',
};
