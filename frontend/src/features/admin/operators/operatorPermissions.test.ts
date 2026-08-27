import { describe, expect, it } from 'vitest';
import {
  groupOperatorPermissions,
  OPERATOR_PERMISSION_OPTIONS,
} from './operatorPermissions';

describe('groupOperatorPermissions', () => {
  it('exposes the 19 active permissions without PROJECT_VIEW', () => {
    expect(OPERATOR_PERMISSION_OPTIONS).toHaveLength(19);
    expect(
      OPERATOR_PERMISSION_OPTIONS.some(
        (permission) => permission.code === ('PROJECT_VIEW' as string),
      ),
    ).toBe(false);
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
