import { describe, expect, it } from 'vitest';
import {
  ASSIGNABLE_OPERATOR_PERMISSION_OPTIONS,
  groupAssignableOperatorPermissionOptions,
  groupOperatorPermissionOptions,
  groupOperatorPermissions,
  OPERATOR_PERMISSION_OPTIONS,
  sanitizeAssignableOperatorPermissions,
} from './operatorPermissions';

describe('groupOperatorPermissions', () => {
  it('uses the requested category order in the operator edit form', () => {
    expect(
      groupOperatorPermissionOptions().map((group) => group.category),
    ).toEqual([
      'プロジェクト管理',
      '参加企業・申請管理',
      'モニタリング管理',
      '運用者管理',
      '方法論・帳票',
    ]);
  });

  it('exposes the 19 active permissions without PROJECT_VIEW', () => {
    expect(OPERATOR_PERMISSION_OPTIONS).toHaveLength(19);
    expect(
      OPERATOR_PERMISSION_OPTIONS.some(
        (permission) => permission.code === ('PROJECT_VIEW' as string),
      ),
    ).toBe(false);
  });

  it('excludes operator create and update from assignable permissions', () => {
    expect(ASSIGNABLE_OPERATOR_PERMISSION_OPTIONS).toHaveLength(17);
    expect(groupAssignableOperatorPermissionOptions()).toEqual(
      expect.arrayContaining([
        expect.objectContaining({
          category: '運用者管理',
          permissions: [
            expect.objectContaining({ code: 'OPERATOR_USER_READ' }),
          ],
        }),
      ]),
    );
    expect(
      sanitizeAssignableOperatorPermissions([
        'PROJECT_READ',
        'OPERATOR_USER_CREATE',
        'OPERATOR_USER_UPDATE',
        'FUTURE_PERMISSION',
      ]),
    ).toEqual(['PROJECT_READ', 'FUTURE_PERMISSION']);
  });

  it('uses the stable Figma category order regardless of API order', () => {
    expect(
      groupOperatorPermissions([
        'MONITORING_READ',
        'PROJECT_UPDATE',
        'PROJECT_READ',
        'OPERATOR_USER_READ',
      ]),
    ).toEqual([
      {
        category: 'プロジェクト管理',
        permissions: ['PROJECT_READ', 'PROJECT_UPDATE'],
      },
      {
        category: 'モニタリング管理',
        permissions: ['MONITORING_READ'],
      },
      {
        category: '運用者管理',
        permissions: ['OPERATOR_USER_READ'],
      },
    ]);
  });

  it('keeps an unknown backend permission visible in the fallback group', () => {
    expect(groupOperatorPermissions(['FUTURE_PERMISSION'])).toEqual([
      {
        category: 'その他',
        permissions: ['FUTURE_PERMISSION'],
      },
    ]);
  });
});
