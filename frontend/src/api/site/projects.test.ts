import { afterEach, describe, expect, it, vi } from 'vitest';
import {
  getSiteProjects,
} from './projects';

afterEach(() => {
  vi.unstubAllGlobals();
});

describe('getSiteProjects', () => {
  it('maps joined project information without requiring a participation ID', async () => {
    vi.stubGlobal(
      'fetch',
      vi.fn().mockResolvedValue(
        new Response(
          JSON.stringify({
            statusCode: 'OK',
            projects: [
              {
                id: 'PROJ/001',
                projectName: '太陽光発電プロジェクト',
                projectNumber: 'P001',
                projectStatus: '実施中',
                isCompanyJoined: true,
                hasSiteRelation: false,
              },
            ],
          }),
          { status: 200, headers: { 'Content-Type': 'application/json' } },
        ),
      ),
    );

    const projects = await getSiteProjects('site001');

    expect(projects[0]).toEqual(
      expect.objectContaining({
        id: 'PROJ/001',
        isCompanyJoined: true,
        hasSiteRelation: false,
      }),
    );
  });
});
