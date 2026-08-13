import { describe, expect, it } from 'vitest';
import type { SignupRevisionResponse } from '@/api/company';
import {
  getSignupRevisionErrorStep,
  mapFormDataToSignupRevision,
  mapSignupRevisionFieldErrors,
  mapSignupRevisionToFormData,
} from './signupRevisionMapper';

const response: SignupRevisionResponse = {
  emailVerificationChallengeId: 'challenge-id',
  corporateNumber: '1234567890123',
  corporateName: '株式会社サンプル',
  corporateNameKana: 'カブシキガイシャサンプル',
  companyNameEnglish: '',
  corporatePhoneNumber: '0312345678',
  postalCode: '1000001',
  prefecture: '東京都',
  municipality: '千代田区',
  blocknum: '1-1',
  buildingName: '',
  lastName: '山田',
  firstName: '太郎',
  lastNameKana: 'ヤマダ',
  firstNameKana: 'タロウ',
  position: '部長',
  phoneNumber: '09012345678',
  faxNumber: '',
  email: 'taro@example.com',
  methodology: ['UNKNOWN-METHOD'],
  activityInformation: '-',
};

describe('signup revision mapper', () => {
  it('maps backend aliases and preserves unknown methodologies', () => {
    const formData = mapSignupRevisionToFormData(response);

    expect(formData.companyName).toBe('株式会社サンプル');
    expect(formData.corporateEmail).toBe('');
    expect(formData.methodology).toEqual(['UNKNOWN-METHOD']);
    expect(formData.activityHas).toBe('no');
  });

  it('creates the exact PUT body and keeps the hidden challenge id', () => {
    const formData = mapSignupRevisionToFormData(response);
    formData.companyName = '株式会社修正版';

    const payload = mapFormDataToSignupRevision(formData, 'challenge-id');

    expect(payload).toEqual({
      ...response,
      corporateName: '株式会社修正版',
    });
    expect(payload).not.toHaveProperty('corporateEmail');
    expect(payload).not.toHaveProperty('activityHas');
  });

  it('omits the optional challenge id when GET did not return it', () => {
    const formData = mapSignupRevisionToFormData(response);

    const payload = mapFormDataToSignupRevision(formData, null);

    expect(payload).not.toHaveProperty('emailVerificationChallengeId');
  });

  it('maps backend field errors and returns the earliest affected step', () => {
    const errors = mapSignupRevisionFieldErrors({
      'request::corporateName': '法人名を入力してください。',
      'request::methodology[0]': '方法論を確認してください。',
    });

    expect(errors).toEqual({
      companyName: '法人名を入力してください。',
      methodology: '方法論を確認してください。',
    });
    expect(getSignupRevisionErrorStep(errors)).toBe(1);
  });
});
