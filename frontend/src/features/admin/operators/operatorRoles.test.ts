import { describe, expect, it } from 'vitest';
import type { AdminOperator } from '@/api/admin/operators';
import {
  getOperatorRoleLabel,
  isAdminOperatorRole,
  resolveOperatorRole,
} from './operatorRoles';

const detailOperator: AdminOperator = {
  id: '1',
  username: 'operator1',
  name: '運用 太郎',
  role: 'admin',
  email: 'operator@example.com',
  firstName: '太郎',
  firstNameKana: 'タロウ',
  lastName: '運用',
  lastNameKana: 'ウンヨウ',
  phoneNumber: '0311112222',
  userPosition: '',
  siteId: null,
  siteName: '',
  companyId: 'COMP-1',
  permissions: [],
};

describe('operatorRoles', () => {
  it('normalizes admin checks and Japanese labels', () => {
    expect(isAdminOperatorRole(' ADMIN ')).toBe(true);
    expect(isAdminOperatorRole('operator')).toBe(false);
    expect(getOperatorRoleLabel('admin')).toBe('管理者');
    expect(getOperatorRoleLabel('operator')).toBe('運用者');
  });

  it('prefers the role from the operator list for the same user ID', () => {
    expect(
      resolveOperatorRole(detailOperator, [
        { ...detailOperator, role: 'operator' },
      ]),
    ).toBe('operator');
  });
});
