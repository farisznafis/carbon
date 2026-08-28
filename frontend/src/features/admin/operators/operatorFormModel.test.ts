import { describe, expect, it } from 'vitest';
import type { UserFormData } from '@/api/users';
import {
  mapOperatorCreatePayload,
  mapOperatorUpdatePayload,
} from './operatorFormModel';

const formValues: UserFormData = {
  username: 'admin.primary',
  lastName: '管理',
  firstName: '太郎',
  lastNameKana: 'カンリ',
  firstNameKana: 'タロウ',
  phoneNumber: '03-5432-1235',
  email: 'admin@example.com',
};

describe('mapOperatorUpdatePayload', () => {
  it('omits permissionCodes when permission editing is unavailable', () => {
    expect(mapOperatorUpdatePayload(formValues)).toEqual({
      username: 'admin.primary',
      lastName: '管理',
      firstName: '太郎',
      lastNameKana: 'カンリ',
      firstNameKana: 'タロウ',
      phoneNumber: '0354321235',
      email: 'admin@example.com',
    });
  });

  it('preserves an explicit empty permission selection for an operator', () => {
    expect(mapOperatorUpdatePayload(formValues, [])).toMatchObject({
      permissionCodes: [],
    });
  });

  it('never sends operator create or update permissions', () => {
    expect(
      mapOperatorUpdatePayload(formValues, [
        'PROJECT_READ',
        'OPERATOR_USER_CREATE',
        'OPERATOR_USER_UPDATE',
      ]),
    ).toMatchObject({
      permissionCodes: ['PROJECT_READ'],
    });
    expect(
      mapOperatorCreatePayload(formValues, [
        'PROJECT_READ',
        'OPERATOR_USER_CREATE',
        'OPERATOR_USER_UPDATE',
      ]),
    ).toMatchObject({
      permissionCodes: ['PROJECT_READ'],
    });
  });
});
