import { useMemo } from 'react';
import { useQuery } from '@tanstack/react-query';
import {
  getParticipationReviewsByStatus,
  type AdminProjectDetail,
  type ParticipationReviewDetail,
  type ParticipationReviewStatus,
} from '@/api/admin';
import { isForbiddenApiError } from '@/api/_shared/errors';
import { isParticipationReviewForProject } from '@/features/admin/membership-review/participationReviewProjectMatch';

export const participationReviewsByStatusQueryKey = [
  'admin',
  'participation-reviews',
  'by-status',
] as const;

export type ParticipationReviewsByStatus = Record<
  ParticipationReviewStatus,
  ParticipationReviewDetail[]
>;

const EMPTY_REVIEWS_BY_STATUS: ParticipationReviewsByStatus = {
  OPEN: [],
  APPROVED: [],
  REJECTED: [],
};

/**
 * Loads every membership application once and narrows it to one project, so
 * the project detail banner, status counts, tab badge and 入会届 list share a
 * single request. The API has no per-project filter, hence the client-side match.
 */
export function useProjectParticipationReviews(
  projectId: string | undefined,
  project: Pick<AdminProjectDetail, 'id' | 'projectName'> | undefined,
  enabled: boolean,
) {
  const query = useQuery({
    queryKey: participationReviewsByStatusQueryKey,
    queryFn: getParticipationReviewsByStatus,
    enabled: enabled && Boolean(projectId) && Boolean(project),
    staleTime: 30_000,
    retry: false,
  });

  const reviewsByStatus = useMemo<ParticipationReviewsByStatus>(() => {
    if (!query.data || !projectId || !project) return EMPTY_REVIEWS_BY_STATUS;

    const belongsToProject = (review: ParticipationReviewDetail) =>
      isParticipationReviewForProject(review, projectId, project);

    return {
      OPEN: query.data.OPEN.filter(belongsToProject),
      APPROVED: query.data.APPROVED.filter(belongsToProject),
      REJECTED: query.data.REJECTED.filter(belongsToProject),
    };
  }, [project, projectId, query.data]);

  return {
    reviewsByStatus,
    counts: {
      OPEN: reviewsByStatus.OPEN.length,
      APPROVED: reviewsByStatus.APPROVED.length,
      REJECTED: reviewsByStatus.REJECTED.length,
    },
    /** True only once data has arrived; counts are meaningless before that. */
    isReady: query.isSuccess,
    isLoading: query.isLoading,
    isError: query.isError,
    isForbidden: isForbiddenApiError(query.error),
    refetch: query.refetch,
  };
}
