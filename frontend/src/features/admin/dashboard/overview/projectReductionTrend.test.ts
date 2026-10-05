import { describe, expect, it } from 'vitest';
import type { ProjectEmissionHistoryPoint } from '@/api/batch';
import type { Project } from '@/api/projects';
import {
  buildReductionTrendChartData,
  selectReductionTrendProjects,
} from './projectReductionTrend';

const makeProject = (id: string, emissionReduction?: number): Project => ({
  id,
  projectName: `プロジェクト${id}`,
  projectStatus: '実施中',
  emissionReduction,
  methodologyVersion: '1.0',
  methodCategory: 'EN',
  methodNumber: 'R-002',
  methodName: '太陽光発電',
  methodMainEmission: 'CO2',
  methodCombiFeasibility: false,
});

const point = (
  period: string,
  reduction: number | null,
): ProjectEmissionHistoryPoint => ({
  period,
  baselineEmission: null,
  projectEmission: null,
  reduction,
});

describe('selectReductionTrendProjects', () => {
  it('keeps the projects with the largest reported reductions', () => {
    const selected = selectReductionTrendProjects(
      [makeProject('a', 1), makeProject('b'), makeProject('c', 5), makeProject('d', 3)],
      2,
    );

    expect(selected.map((project) => project.id)).toEqual(['c', 'd']);
  });
});

describe('buildReductionTrendChartData', () => {
  it('merges histories by period and skips projects without reductions', () => {
    const projects = [makeProject('a', 5), makeProject('b', 4), makeProject('c', 3)];
    const data = buildReductionTrendChartData(
      projects,
      new Map([
        ['a', [point('2026-02-28', 2), point('2026-01-31', 1)]],
        ['b', [point('2026-01-31', null)]],
        ['c', [point('2026-02-28', 4)]],
      ]),
    );

    expect(data.series.map((entry) => [entry.key, entry.label])).toEqual([
      ['project0', 'プロジェクトa'],
      ['project1', 'プロジェクトc'],
    ]);
    expect(data.rows).toEqual([
      { period: '2026-01-31', project0: 1 },
      { period: '2026-02-28', project0: 2, project1: 4 },
    ]);
  });
});
