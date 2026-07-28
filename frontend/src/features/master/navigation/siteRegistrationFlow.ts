import type { SiteFormData } from '@/api/site';
import type { UserList } from '@/api/users';

const SITE_REGISTRATION_SOURCE = 'site-register';

export interface SiteRegistrationFlowState {
  source: typeof SITE_REGISTRATION_SOURCE;
  siteDraft: SiteFormData;
  createdUser?: UserList;
}

export function createSiteRegistrationFlowState(
  siteDraft: SiteFormData,
  createdUser?: UserList,
): SiteRegistrationFlowState {
  return {
    source: SITE_REGISTRATION_SOURCE,
    siteDraft,
    createdUser,
  };
}

export function getSiteRegistrationFlowState(
  state: unknown,
): SiteRegistrationFlowState | null {
  if (!state || typeof state !== 'object') return null;

  const candidate = state as Partial<SiteRegistrationFlowState>;

  if (
    candidate.source !== SITE_REGISTRATION_SOURCE ||
    !candidate.siteDraft ||
    typeof candidate.siteDraft !== 'object'
  ) {
    return null;
  }

  return candidate as SiteRegistrationFlowState;
}
