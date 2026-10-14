import { describe, expect, it } from 'vitest';
import type { Site, SiteProject } from '@/api/site';
import type { UserList } from '@/api/users';
import { formatChange, formatRelativeTime } from '@/features/master/shared/masterFormatters';
import { buildMasterHome, pairKey, toJstMonth, toMonthlyEmissions } from './masterHomeModel';

const site = (siteId: string): Site =>
  ({ siteId, siteName: `拠点${siteId}`, prefectureName: '東京都', cityName: '渋谷区', picLastName: '', picFirstName: '' }) as Site;
const project = (id: string, participationStatus: SiteProject['participationStatus'], hasSiteRelation: boolean) =>
  ({ id, projectName: `P${id}`, participationStatus, hasSiteRelation }) as SiteProject;
const user = (id: string, role: UserList['role'], siteId: string | null) =>
  ({ id, role, siteId, lastName: '山田', firstName: id }) as UserList;

describe('buildMasterHome', () => {
  // 2026-10-31 20:00 UTC is already November in JST.
  const now = new Date('2026-10-31T20:00:00Z');

  it('derives KPIs, actions and site rows from target pairs', () => {
    const home = buildMasterHome({
      sites: [site('S1'), site('S2'), site('S3')],
      users: [user('m', 'master', null), user('u1', 'user', 'S1'), user('u2', 'user', null)],
      siteProjects: [
        { siteId: 'S1', projects: [project('A', 'APPROVED', true), project('B', 'OPEN', false)] },
        { siteId: 'S2', projects: [project('A', 'APPROVED', true), project('C', 'APPROVED', false)] },
        { siteId: 'S3', projects: [project('B', 'OPEN', false)] },
      ],
      latestSubmissions: new Map([
        [pairKey('S1', 'A'), '2026-11-01T01:00:00Z'],
        [pairKey('S2', 'A'), '2026-10-15'],
      ]),
      unreadCount: 3,
      now,
    });

    expect(home.siteCount).toBe(3);
    expect(home.participatingSiteCount).toBe(2);
    expect(home.approvedProjectCount).toBe(2);
    expect(home.pendingProjectCount).toBe(1);
    expect([home.submittedCount, home.targets.length]).toEqual([1, 2]);
    expect(home.actions.map((action) => action.kind)).toEqual(['pending', 'unsubmitted', 'noSite', 'unread']);
    expect(home.actions[3].text).toBe('未読のお知らせが3件あります');
    expect(home.siteRows.map((row) => [row.projectCount, row.userCount, row.thisMonth])).toEqual([
      [1, 1, 'submitted'],
      [1, 0, 'unsubmitted'],
      [0, 0, 'none'],
    ]);
  });
});

describe('toMonthlyEmissions', () => {
  const pair = (siteId: string) => ({ siteId, siteName: siteId, projectId: 'P', projectName: 'P' });
  const item = (processingDate: string, empj: number | null) => ({
    processingDate,
    empj,
    empjM: null,
    empjSControl: null,
    empjSBattery: null,
  });

  it('sums empj per JST month with the contributing site count', () => {
    expect(
      toMonthlyEmissions([
        { pair: pair('S1'), items: [item('2026-09-30T16:00:00Z', 1), item('2026-08-10', 2)] },
        { pair: pair('S2'), items: [item('2026-10-05', 3.5), item('2026-10-06', null)] },
      ]),
    ).toEqual([
      { month: '2026-08', empj: 2, siteCount: 1 },
      { month: '2026-10', empj: 4.5, siteCount: 2 },
    ]);
    expect(toJstMonth('2026-07-23T10:00:28.139719300Z')).toBe('2026-07');
    expect(toJstMonth('not a date')).toBeNull();
  });
});

describe('formatters', () => {
  const now = new Date('2026-10-14T12:00:00Z');
  it('formats relative times and signed changes', () => {
    expect(formatRelativeTime('2026-10-14T11:50:00Z', now)).toBe('10分前');
    expect(formatRelativeTime('2026-10-14T10:00:00Z', now)).toBe('2時間前');
    expect(formatRelativeTime('2026-10-13T11:00:00Z', now)).toBe('昨日');
    expect(formatChange('前月比', -7.06)).toBe('前月比 −7.1%');
    expect(formatChange('前月比', null)).toBeNull();
  });
});
