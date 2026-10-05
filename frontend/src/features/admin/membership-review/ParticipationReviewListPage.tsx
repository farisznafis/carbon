import { Navigate, useLocation, useParams, useSearchParams } from 'react-router-dom';
import { paths } from '@/routes/paths';

/**
 * Keeps the old /memberships URL working. The list now lives in the project
 * detail 入会届 tab, so this forwards there with the status filter and the
 * navigation state (e.g. "return to Home") intact.
 */
export default function ParticipationReviewListPage() {
  const location = useLocation();
  const { projectId = '' } = useParams();
  const [searchParams] = useSearchParams();

  return (
    <Navigate
      replace
      to={paths.adminProjectMembership(
        projectId,
        searchParams.get('status') ?? undefined,
      )}
      state={location.state}
    />
  );
}
