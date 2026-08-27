import type { SiteFormData } from '@/api/site';
import type { UserList } from '@/api/users';

const SITE_REGISTRATION_SOURCE = 'site-register';
const SITE_REGISTRATION_RETURN_SOURCE = 'site-registration-return';

export interface SiteRegistrationReturnTarget {
  path: string;
  state?: Record<string, unknown>;
}

interface SiteRegistrationReturnState {
  source: typeof SITE_REGISTRATION_RETURN_SOURCE;
  returnTarget: SiteRegistrationReturnTarget;
}

export interface SiteRegistrationFlowState {
  source: typeof SITE_REGISTRATION_SOURCE;
  siteDraft: SiteFormData;
  createdUser?: UserList;
  returnTarget?: SiteRegistrationReturnTarget;
}

/** Preserves the site form and its caller while temporarily opening user registration. */
export function createSiteRegistrationFlowState(
  siteDraft: SiteFormData,
  createdUser?: UserList,
  returnTarget?: SiteRegistrationReturnTarget,
): SiteRegistrationFlowState {
  return {
    source: SITE_REGISTRATION_SOURCE,
    siteDraft,
    createdUser,
    returnTarget,
  };
}

/** Reads the internal site-to-user registration state from React Router state. */
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

/** Creates navigation state that returns site registration to its calling page. */
export function createSiteRegistrationReturnState(
  returnTarget: SiteRegistrationReturnTarget,
): SiteRegistrationReturnState {
  return {
    source: SITE_REGISTRATION_RETURN_SOURCE,
    returnTarget,
  };
}

/** Resolves a caller return target from direct or nested site-registration state. */
export function getSiteRegistrationReturnTarget(
  state: unknown,
): SiteRegistrationReturnTarget | null {
  const flowState = getSiteRegistrationFlowState(state);
  if (flowState?.returnTarget) return flowState.returnTarget;

  if (!state || typeof state !== 'object') return null;
  const candidate = state as Partial<SiteRegistrationReturnState>;
  const returnTarget = candidate.returnTarget;

  if (
    candidate.source !== SITE_REGISTRATION_RETURN_SOURCE ||
    !returnTarget ||
    typeof returnTarget.path !== 'string'
  ) {
    return null;
  }

  return returnTarget;
}
