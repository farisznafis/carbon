const SITE_REGISTRATION_RETURN_SOURCE = 'site-registration-return';

export interface SiteRegistrationReturnTarget {
  path: string;
  state?: Record<string, unknown>;
}

interface SiteRegistrationReturnState {
  source: typeof SITE_REGISTRATION_RETURN_SOURCE;
  returnTarget: SiteRegistrationReturnTarget;
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
