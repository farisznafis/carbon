import { describe, expect, it } from 'vitest';
import {
  hasLegacyContractDraftValues,
  mapSustainabilityCommitmentDefault,
} from './sustainabilityCommitmentDefaultMapper';

describe('sustainabilityCommitmentDefaultMapper', () => {
  it('maps text defaults and intentionally ignores backend consent values', () => {
    expect(
      mapSustainabilityCommitmentDefault({
        companyName: ' ABC株式会社 ',
        repName: ' 山田太郎 ',
        repRole: ' 代表取締役 ',
        hasConsent: true,
        isAgreeTerms: true,
      }),
    ).toEqual({
      businessName: 'ABC株式会社',
      representativeName: '山田太郎',
      representativePosition: '代表取締役',
      contractAgreed: false,
    });
  });

  it('recognizes legacy edited drafts even when the source flag is absent', () => {
    expect(hasLegacyContractDraftValues({ businessName: 'ABC株式会社' })).toBe(
      true,
    );
    expect(
      hasLegacyContractDraftValues({
        businessName: '',
        representativeName: '',
        representativePosition: '',
        contractAgreed: false,
      }),
    ).toBe(false);
  });
});
