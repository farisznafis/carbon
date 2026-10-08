import { useQuery } from '@tanstack/react-query';
import { getProjectDocumentData } from '@/api/admin/projectDocumentData';
import type { ProjectFileType } from '@/api/admin/projectFiles';
import { projectFilesQueryPrefix } from './useProjectFiles';

/**
 * Under the project's file queries, so an upload or a delete in フォーム
 * refetches the data read from the files too.
 */
export const projectDocumentDataQueryKey = (projectId: string, fileType: ProjectFileType) =>
  [...projectFilesQueryPrefix(projectId), 'document-data', fileType] as const;

/** The data read from the latest file of one type; idle while `enabled` is false. */
export function useProjectDocumentData(
  projectId: string,
  fileType: ProjectFileType,
  enabled = true,
) {
  return useQuery({
    queryKey: projectDocumentDataQueryKey(projectId, fileType),
    queryFn: () => getProjectDocumentData(projectId, fileType),
    enabled: Boolean(projectId) && enabled,
    retry: false,
  });
}
