import { describe, expect, it } from 'vitest';
import type { AdminOperator } from '@/api/admin/operators';
import { findCurrentAdminOperator } from './currentAdminOperator';

const operators: AdminOperator[] = [
  {
    id: '1',
    username: 'admin.primary',
    name: '管理 太郎',
    role: 'admin',
    email: 'admin@example.com',
    firstName: '太郎',
    firstNameKana: 'タロウ',
    lastName: '管理',
    lastNameKana: 'カンリ',
    phoneNumber: '0311112222',
    userPosition: '',
    siteId: null,
    siteName: '',
    companyId: 'COMP-1',
    permissions: [],
  },
  {
    id: '2',
    username: 'operator.second',
    name: '運用 花',
    role: 'operator',
    email: 'operator@example.com',
    firstName: '花',
    firstNameKana: 'ハナ',
    lastName: '運用',
    lastNameKana: 'ウンヨウ',
    phoneNumber: '0322223333',
    userPosition: '',
    siteId: null,
    siteName: '',
    companyId: 'COMP-1',
    permissions: [],
  },
];

describe('findCurrentAdminOperator', () => {
  it('prioritizes an exact ID match over username and email', () => {
    expect(
      findCurrentAdminOperator(operators, {
        id: '1',
        username: 'operator.second',
        email: 'operator@example.com',
      }),
    ).toBe(operators[0]);
  });

  it('falls back to normalized username and then email', () => {
    expect(
      findCurrentAdminOperator(operators, {
        id: 'missing',
        username: ' OPERATOR.SECOND ',
        email: '',
      }),
    ).toBe(operators[1]);

    expect(
      findCurrentAdminOperator(operators, {
        id: 'missing',
        username: 'missing',
        email: ' ADMIN@EXAMPLE.COM ',
      }),
    ).toBe(operators[0]);
  });

  it('returns null instead of guessing an ambiguous fallback match', () => {
    const duplicatedEmailOperators = [
      ...operators,
      { ...operators[1], id: '3', username: 'operator.third' },
    ];

    expect(
      findCurrentAdminOperator(duplicatedEmailOperators, {
        id: 'missing',
        username: 'missing',
        email: 'operator@example.com',
      }),
    ).toBeNull();
  });
});
