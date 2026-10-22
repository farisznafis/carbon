import { useQuery } from '@tanstack/react-query';
import { getProjectFiles } from '@/api/admin/projectFiles';

export const projectPlanFilesQueryKey = (projectId: string) =>
  ['admin', 'projects', projectId, 'files', 'PROJECT_PLAN'] as const;

/** The uploaded 計画書 files of a project, newest first. */
export function useProjectPlanFiles(projectId: string) {
  return useQuery({
    queryKey: projectPlanFilesQueryKey(projectId),
    queryFn: () => getProjectFiles(projectId, 'PROJECT_PLAN'),
    enabled: Boolean(projectId),
    retry: false,
  });
}

const dateTimeFormat = new Intl.DateTimeFormat('ja-JP', {
  year: 'numeric',
  month: 'long',
  day: 'numeric',
  hour: '2-digit',
  minute: '2-digit',
});

export function formatUploadedAt(value?: string | null) {
  if (!value) return '—';
  const date = new Date(value);
  return Number.isNaN(date.getTime()) ? value : dateTimeFormat.format(date);
}
