import { describe, expect, it } from 'vitest';
import { normalizeEmissionTrendResponse } from './queries';

const expectedTrendPoint = {
  x: '2026-01-31',
  y: 10.123,
};

describe('normalizeEmissionTrendResponse', () => {
  it('normalizes a single result returned at the response root', () => {
    expect(
      normalizeEmissionTrendResponse({
        processing_date: '2026-01-31',
        empj: 10.123,
      }),
    ).toEqual([expectedTrendPoint]);
  });

  it('normalizes an array returned in results', () => {
    expect(
      normalizeEmissionTrendResponse({
        results: [
          {
            processing_date: '2026-01-31',
            empj: 10.123,
          },
        ],
      }),
    ).toEqual([expectedTrendPoint]);
  });

  it('normalizes a single result returned in data.results', () => {
    expect(
      normalizeEmissionTrendResponse({
        data: {
          results: {
            processing_date: '2026-01-31',
            empj: '10.123',
          },
        },
      }),
    ).toEqual([expectedTrendPoint]);
  });

  it('normalizes and chronologically sorts the current items response', () => {
    expect(
      normalizeEmissionTrendResponse({
        items: [
          {
            created_at: '2026-07-14T17:16:27.986Z',
            empj: 0.033,
          },
          {
            created_at: '2026-07-09T13:13:01.865Z',
            empj: 80,
          },
        ],
      }),
    ).toEqual([
      { x: '2026-07-09T13:13:01.865Z', y: 80 },
      { x: '2026-07-14T17:16:27.986Z', y: 0.033 },
    ]);
  });

  it('ignores incomplete results instead of displaying invalid points', () => {
    expect(
      normalizeEmissionTrendResponse({
        results: [
          { processing_date: '2026-01-31', empj: null },
          { processing_date: null, empj: 10.123 },
        ],
      }),
    ).toEqual([]);
  });
});
