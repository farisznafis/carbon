import { describe, expect, it } from 'vitest';
import type { Project } from '@/api/projects';
import {
  buildProjectStatusFlow,
  PROJECT_STATUS_FLOW,
} from './projectStatusFlow';

const makeProject = (id: string, projectStatus: string): Project => ({
  id,
  projectName: `プロジェクト${id}`,
  projectStatus,
  methodologyVersion: '1.0',
  methodCategory: 'EN',
  methodNumber: 'R-002',
  methodName: '太陽光発電',
  methodMainEmission: 'CO2',
  methodCombiFeasibility: false,
});

describe('buildProjectStatusFlow', () => {
  it('orders main-flow stages and scales bars against the largest stage', () => {
    const summary = buildProjectStatusFlow([
      makeProject('1', '実施中'),
      makeProject('2', 'ACTIVE'),
      makeProject('3', '作成済み'),
      makeProject('4', '却下'),
    ]);

    expect(summary.total).toBe(4);
    expect(summary.stages.map((stage) => stage.status)).toEqual(
      PROJECT_STATUS_FLOW,
    );
    expect(summary.stages.find((stage) => stage.status === '実施中')).toEqual({
      status: '実施中',
      count: 2,
      ratio: 100,
    });
    expect(
      summary.stages.find((stage) => stage.status === '作成済み')?.ratio,
    ).toBe(50);
    expect(summary.branches).toEqual([
      { status: '変更中', count: 0, ratio: 0 },
      { status: '却下', count: 1, ratio: 50 },
    ]);
    expect(summary.otherCount).toBe(0);
  });

  it('counts unknown statuses separately', () => {
    const summary = buildProjectStatusFlow([makeProject('1', '保留')]);

    expect(summary.otherCount).toBe(1);
    expect(summary.stages.every((stage) => stage.ratio === 0)).toBe(true);
  });
});
