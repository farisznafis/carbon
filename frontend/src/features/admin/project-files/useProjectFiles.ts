import { useMemo } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import {
  deleteProjectFile,
  getProjectFileDownloadUrl,
  getProjectFileListing,
  uploadProjectFile,
  type ProjectFile,
  type ProjectFileType,
} from '@/api/admin/projectFiles';
import { toast } from '@/hooks/use-toast';
import { groupProjectFiles } from './projectFileTypes';

/** Opens a fresh signed URL for one file; asked on click because it expires. */
export async function downloadProjectFile(projectId: string, file: Pick<ProjectFile, 'id'>) {
  try {
    const url = await getProjectFileDownloadUrl(projectId, file.id);
    window.open(url, '_blank', 'noopener,noreferrer');
  } catch (error) {
    toast({
      title: 'ファイルを取得できませんでした',
      description:
        error instanceof Error && error.message ? error.message : '時間をおいて再度お試しください。',
      variant: 'destructive',
    });
  }
}

/** Prefix shared by every file query of a project, including the 計画書 card's. */
export const projectFilesQueryPrefix = (projectId: string) =>
  ['admin', 'projects', projectId, 'files'] as const;

export const projectFileListingQueryKey = (projectId: string) =>
  [...projectFilesQueryPrefix(projectId), 'listing'] as const;

/** The project's files grouped by document type, with upload and delete. */
export function useProjectFiles(projectId: string) {
  const queryClient = useQueryClient();

  const listingQuery = useQuery({
    queryKey: projectFileListingQueryKey(projectId),
    queryFn: () => getProjectFileListing(projectId),
    enabled: Boolean(projectId),
    retry: false,
  });

  const groups = useMemo(
    () => (listingQuery.data ? groupProjectFiles(listingQuery.data) : []),
    [listingQuery.data],
  );

  // Refreshes every file query of the project, so the 概要 計画書 card follows too.
  const refreshFiles = () =>
    queryClient.invalidateQueries({ queryKey: projectFilesQueryPrefix(projectId) });

  const uploadMutation = useMutation({
    mutationFn: ({ fileType, file }: { fileType: ProjectFileType; file: File }) =>
      uploadProjectFile(projectId, fileType, file),
    onSuccess: refreshFiles,
  });

  const deleteMutation = useMutation({
    mutationFn: (fileId: number) => deleteProjectFile(projectId, fileId),
    onSuccess: refreshFiles,
  });

  return { listingQuery, groups, uploadMutation, deleteMutation };
}
