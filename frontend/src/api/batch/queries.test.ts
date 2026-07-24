import { describe, expect, it } from 'vitest';
import {
  normalizeEmissionTrendResponse,
  normalizeProjectEmissionBreakdownResponse,
  normalizeProjectEmissionSummaryResponse,
} from './queries';

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

describe('normalizeProjectEmissionBreakdownResponse', () => {
  it('maps only the three breakdown fields from a wrapped response', () => {
    expect(
      normalizeProjectEmissionBreakdownResponse({
        data: {
          result: {
            empj_m: 12.345,
            empj_s_control: '8.123',
            empj_s_battery: 4.222,
          },
        },
      }),
    ).toEqual({
      mainEmission: 12.345,
      controlDeviceEmission: 8.123,
      batteryEmission: 4.222,
    });
  });

  it('preserves zero as a valid emission value', () => {
    expect(
      normalizeProjectEmissionBreakdownResponse({
        items: [{ empj_m: 0 }],
      }),
    ).toEqual({
      mainEmission: 0,
      controlDeviceEmission: null,
      batteryEmission: null,
    });
  });

  it('aggregates every item returned by the total endpoint', () => {
    expect(
      normalizeProjectEmissionBreakdownResponse({
        items: [
          {
            empj_m: 0,
            empj_s_control: 0.033,
            empj_s_battery: 0,
          },
          {
            empj_m: 80,
            empj_s_control: 0,
            empj_s_battery: 0,
          },
          {
            empj_m: 70.3,
            empj_s_control: '0.017',
            empj_s_battery: null,
          },
        ],
      }),
    ).toEqual({
      mainEmission: 150.3,
      controlDeviceEmission: 0.05,
      batteryEmission: 0,
    });
  });

  it('returns null when no breakdown field can be displayed', () => {
    expect(
      normalizeProjectEmissionBreakdownResponse({
        empj_m: null,
        empj_s_control: '',
        empj_s_battery: undefined,
      }),
    ).toBeNull();
  });
});

describe('normalizeProjectEmissionSummaryResponse', () => {
  it('maps the project-level result returned in data', () => {
    expect(
      normalizeProjectEmissionSummaryResponse({
        statusCode: '200',
        data: {
          embl: 100,
          empj: 31.6,
          empj_m: 20,
          empj_s_control: 8.1,
          empj_s_battery: 3.5,
        },
      }),
    ).toEqual({
      baselineEmission: 100,
      projectEmission: 31.6,
      breakdown: {
        mainEmission: 20,
        controlDeviceEmission: 8.1,
        batteryEmission: 3.5,
      },
    });
  });
});
