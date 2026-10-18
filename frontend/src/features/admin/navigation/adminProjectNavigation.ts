import { paths } from '@/routes/paths';

export type AdminProjectNavigationState = {
  returnTo: typeof paths.adminDashboard;
};

export const adminHomeProjectNavigationState: AdminProjectNavigationState = {
  returnTo: paths.adminDashboard,
};

/** Returns only the supported admin project origin from untrusted router state. */
export function resolveAdminProjectNavigationState(
  state: unknown,
): AdminProjectNavigationState | undefined {
  if (
    typeof state !== 'object' ||
    state === null ||
    !('returnTo' in state) ||
    state.returnTo !== paths.adminDashboard
  ) {
    return undefined;
  }

  return adminHomeProjectNavigationState;
}
