const MASTER_DASHBOARD_SOURCE = 'master-dashboard';

export interface ProjectMembershipNavigationState {
  source: typeof MASTER_DASHBOARD_SOURCE;
}

export const masterDashboardProjectMembershipState: ProjectMembershipNavigationState = {
  source: MASTER_DASHBOARD_SOURCE,
};

export function isFromMasterDashboard(
  state: unknown,
): state is ProjectMembershipNavigationState {
  return (
    Boolean(state) &&
    typeof state === 'object' &&
    'source' in state &&
    state.source === MASTER_DASHBOARD_SOURCE
  );
}
