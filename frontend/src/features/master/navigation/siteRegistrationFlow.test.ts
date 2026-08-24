// Verifies the typed router state used to return from site registration to its caller.
import { describe, expect, it } from 'vitest';
import type { SiteFormData } from '@/api/site';
import {
  createSiteRegistrationFlowState,
  createSiteRegistrationReturnState,
  getSiteRegistrationReturnTarget,
} from './siteRegistrationFlow';

const returnTarget = {
  path: '/projects/site001/PROJ001/register',
  state: { pendingEquipment: { id: 'equipment-1' } },
};

const siteDraft = {
  siteName: '東京拠点',
  siteNameKana: '',
  postalCode: '',
  prefectureName: '',
  cityName: '',
  townName: '',
  blockName: '',
  addressDetail: '',
  picFirstName: '',
  picFirstNameKana: '',
  picLastName: '',
  picLastNameKana: '',
  picEmail: '',
  picPhoneNumber: '',
  picPosition: '',
  userId: '',
} satisfies SiteFormData;

describe('siteRegistrationFlow', () => {
  it('reads a direct return target', () => {
    expect(
      getSiteRegistrationReturnTarget(
        createSiteRegistrationReturnState(returnTarget),
      ),
    ).toEqual(returnTarget);
  });

  it('preserves the return target through the nested user-registration flow', () => {
    const state = createSiteRegistrationFlowState(
      siteDraft,
      undefined,
      returnTarget,
    );

    expect(getSiteRegistrationReturnTarget(state)).toEqual(returnTarget);
  });

  it('rejects unrelated router state', () => {
    expect(getSiteRegistrationReturnTarget({ source: 'unknown' })).toBeNull();
  });
});
