import { useMemo } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import {
  deleteProjectFile,
  getProjectFileListing,
  uploadProjectFile,
  type ProjectFileType,
} from '@/api/admin/projectFiles';
import { groupProjectFiles } from './projectFileTypes';

/** Prefix shared by every file query of a project, including the 計画書 card's. */
const projectFilesQueryPrefix = (projectId: string) =>
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
