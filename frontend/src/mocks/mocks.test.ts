import { setupServer } from 'msw/node';
import { afterAll, beforeAll, describe, expect, it, vi } from 'vitest';
import * as authApi from '@/api/auth';
import { getProfile } from '@/api/profile/profile';
import { getSites } from '@/api/site/list';
import { getSiteData } from '@/api/site/dashboard';
import { getMyProjectParticipations, getSiteProjects } from '@/api/site/projects';
import {
  getSiteEquipmentHistory,
  getSiteEquipmentList,
  getSiteProjectEquipment,
  getSiteProjectEquipmentPresignedUrl,
} from '@/api/site/equipment';
import { getUsers } from '@/api/users/list';
import { getProjectDetails, getProjectMemberTermsInfo, getProjects, downloadProjectMembershipPdf } from '@/api/projects/queries';
import {
  getAdminProjectMonitoringRecords,
  getMonitoringCancelRequests,
  getMonitoringRecent,
  getMonitoringRecords,
} from '@/api/monitoring/queries';
import {
  getBatchHistory,
  getEmissionTrend,
  getProjectEmissionHistory,
  getProjectEmissionSummary,
} from '@/api/batch/queries';
import { getNotifications, getUnreadNotificationCount } from '@/api/notifications';
import {
  getAdminCompanies,
  getAdminCompanyDetail,
  getAdminProjectDetail,
  getCompanySuspensions,
  getParticipationReviewDetail,
  getParticipationReviewsByStatus,
  getProjectPlanForm,
} from '@/api/admin';
import { getAdminOperatorProfile, getAdminOperators } from '@/api/admin/operators';
import { getProjectFileDownloadUrl, getProjectFileListing } from '@/api/admin/projectFiles';
import { getProjectDocumentData } from '@/api/admin/projectDocumentData';
import { getProjectMembershipPdfs } from '@/api/admin/projectCompanies';
import { lookupPostalCode } from '@/lib/postalCode';
import { storage, storageKeys } from '@/lib/storage';
import { handlers } from './handlers';

const server = setupServer(...handlers);

// Every mock response waits MOCK_LATENCY_MS, so the multi-call cases need a longer budget.
vi.setConfig({ testTimeout: 30_000 });

beforeAll(() => {
  server.listen({ onUnhandledRequest: 'error' });
  // Node's fetch rejects the app's relative `/api` URLs, so resolve them against the jsdom origin.
  const patchedFetch = globalThis.fetch;
  globalThis.fetch = (input, init) =>
    patchedFetch(typeof input === 'string' ? new URL(input, window.location.origin) : input, init);
});

afterAll(() => server.close());

describe('mock API covers the user portal', () => {
  it('logs in and serves the signed-in profile', async () => {
    const login = await authApi.login('C0001', 'master@green-energy.example.jp', 'any');
    storage.set(storageKeys.authToken, login.token);
    storage.setJson(storageKeys.user, { companyId: 'C0001' });

    expect(login.role).toBe('master');
    expect((await getProfile()).company?.nameJA).toBe('グリーンエナジー株式会社');
  });

  it('serves sites, projects, equipment and monitoring', async () => {
    expect((await getSites()).sites.length).toBeGreaterThan(0);
    expect((await getSiteData('S001')).metrics?.sumEmissionReduction).toBeTruthy();
    expect((await getSiteProjects('S001')).some((project) => project.hasSiteRelation)).toBe(true);
    expect(await getMyProjectParticipations('P001')).not.toHaveLength(0);
    expect(await getSiteProjectEquipment('S001', 'P001')).toHaveLength(1);
    expect(await getSiteEquipmentList('S002')).not.toHaveLength(0);
    expect(await getSiteEquipmentHistory('S001', 701)).toHaveLength(1);
    expect(await getSiteProjectEquipmentPresignedUrl('demo/a.pdf')).toMatch(/^http/);
    expect((await getUsers()).users).not.toHaveLength(0);
    expect(await getProjects()).not.toHaveLength(0);
    expect((await getProjectDetails('P001')).projectName).toBeTruthy();
    expect((await getProjectMemberTermsInfo('P001')).projectName).toBeTruthy();
    expect((await downloadProjectMembershipPdf('P001')).pdfUrl).toMatch(/^http/);
    expect(await getMonitoringRecent({ projectId: 'P001', siteId: 'S001' })).not.toHaveLength(0);
    expect((await getMonitoringRecords({ siteId: 'S001', projectId: 'P001' })).items).toHaveLength(10);
    expect(await getEmissionTrend('S001', 'P001')).not.toHaveLength(0);
    expect(await getProjectEmissionHistory('P001')).not.toHaveLength(0);
    expect(await getProjectEmissionSummary('P001')).not.toBeNull();
    expect((await getNotifications()).notifications).not.toHaveLength(0);
    expect(await getUnreadNotificationCount()).toBeGreaterThan(0);
  });
});

describe('mock postal code lookup works offline', () => {
  it('answers known and unlisted codes', async () => {
    expect(await lookupPostalCode('1000005')).toEqual({ prefecture: '東京都', city: '千代田区', town: '丸の内' });
    expect((await lookupPostalCode('5559999'))?.prefecture).toBe('大阪府');
  });
});

describe('mock API covers the admin portal', () => {
  it('logs in as an administrator with every permission', async () => {
    const login = await authApi.adminLogin('admin', 'any');
    storage.set(storageKeys.authToken, login.token);

    const profile = await getAdminOperatorProfile(String(login.admin?.operatorId));
    expect(profile.role).toBe('admin');
    expect((await getAdminOperators()).users).toHaveLength(3);
  });

  it('serves companies, projects, files and reviews', async () => {
    expect((await getAdminCompanies({ status: ['PENDING'] })).companies).toHaveLength(2);
    expect((await getAdminCompanyDetail('C0001')).sites).not.toHaveLength(0);
    expect((await getCompanySuspensions('C0003')).suspensions).toHaveLength(1);
    expect((await getAdminProjectDetail('P001')).projectNumber).toBe('JCL-2025-001');
    expect((await getProjectPlanForm('P001', 'emission-reduction-plan')).status).toBe('COMPLETE');
    expect((await getProjectFileListing('P001')).files).not.toHaveLength(0);
    expect(await getProjectFileDownloadUrl('P001', 3001)).toMatch(/\.xlsx$/);
    expect((await getProjectDocumentData('P001', 'PROJECT_PLAN')).sections).not.toHaveLength(0);
    expect(await getProjectMembershipPdfs('P001')).not.toHaveLength(0);
    expect((await getAdminProjectMonitoringRecords({ projectId: 'P001' })).pagination.totalItems).toBeGreaterThan(10);
    expect(await getMonitoringCancelRequests()).not.toHaveLength(0);
    expect((await getBatchHistory()).items).toHaveLength(10);

    const reviews = await getParticipationReviewsByStatus();
    expect(reviews.OPEN).not.toHaveLength(0);
    expect((await getParticipationReviewDetail(reviews.OPEN[0].participationId)).projectName).toBeTruthy();
  });
});
