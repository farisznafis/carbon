// Verifies the typed router state used to return from site registration to its caller.
import { describe, expect, it } from 'vitest';
import {
  createSiteRegistrationReturnState,
  getSiteRegistrationReturnTarget,
} from './siteRegistrationFlow';

const returnTarget = {
  path: '/projects/site001/PROJ001/register',
  state: { pendingEquipment: { id: 'equipment-1' } },
};

describe('siteRegistrationFlow', () => {
  it('reads a direct return target', () => {
    expect(
      getSiteRegistrationReturnTarget(
        createSiteRegistrationReturnState(returnTarget),
      ),
    ).toEqual(returnTarget);
  });

  it('rejects unrelated router state', () => {
    expect(getSiteRegistrationReturnTarget({ source: 'unknown' })).toBeNull();
  });
});
