import {
  render,
  screen,
} from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import {
  MemoryRouter,
  Route,
  Routes,
} from 'react-router-dom';
import {
  beforeEach,
  describe,
  expect,
  it,
  vi,
} from 'vitest';
import type { SiteProject } from '@/api/site';
import ProjectMembershipPage from './ProjectMembershipPage';

const {
  getSiteData,
  getSiteProjects,
  downloadProjectMembershipPdf,
  downloadFromUrl,
} = vi.hoisted(() => ({
  getSiteData: vi.fn(),
  getSiteProjects: vi.fn(),
  downloadProjectMembershipPdf:
    vi.fn(),
  downloadFromUrl: vi.fn(),
}));

vi.mock(
  '@/api/site',
  async (importOriginal) => {
    const actual =
      await importOriginal<
        typeof import('@/api/site')
      >();

    return {
      ...actual,
      getSiteData,
      getSiteProjects,
    };
  },
);

vi.mock(
  '@/api/projects',
  async (importOriginal) => {
    const actual =
      await importOriginal<
        typeof import('@/api/projects')
      >();

    return {
      ...actual,
      downloadProjectMembershipPdf,
    };
  },
);

vi.mock('@/utils/download', () => ({
  downloadFromUrl,
}));

vi.mock('@/hooks/use-toast', () => ({
  useToast: () => ({
    toast: vi.fn(),
  }),
}));

vi.mock(
  '@/stores/dashboardStore',
  () => ({
    useDashboardStore: (
      selector: (state: {
        setCurrentProject: ReturnType<
          typeof vi.fn
        >;
      }) => unknown,
    ) =>
      selector({
        setCurrentProject:
          vi.fn(),
      }),
  }),
);

type TestMembershipStatus =
  | 'available'
  | 'pending'
  | 'joined';

function createProject(
  id: string,
  projectName: string,
  status: TestMembershipStatus,
): SiteProject {
  if (status === 'pending') {
    return {
      id,
      projectName,
      projectNumber: id,
      projectStatus: '実施中',
      isCompanyJoined: true,
      hasSiteRelation: false,
      participationStatus:
        'OPEN',
      methodName: '方法論',
      methodNumber: '001',
      methodologyVersion:
        'Ver.1.0',
      methodMainEmission:
        'CO2',
    } as SiteProject;
  }

  if (status === 'joined') {
    return {
      id,
      projectName,
      projectNumber: id,
      projectStatus: '実施中',
      isCompanyJoined: true,
      hasSiteRelation: false,
      participationStatus:
        'APPROVED',
      methodName: '方法論',
      methodNumber: '001',
      methodologyVersion:
        'Ver.1.0',
      methodMainEmission:
        'CO2',
    } as SiteProject;
  }

  return {
    id,
    projectName,
    projectNumber: id,
    projectStatus: '実施中',
    isCompanyJoined: false,
    hasSiteRelation: false,
    methodName: '方法論',
    methodNumber: '001',
    methodologyVersion:
      'Ver.1.0',
    methodMainEmission: 'CO2',
  } as SiteProject;
}

function renderPage() {
  return render(
    <MemoryRouter
      initialEntries={[
        '/projects/site001/register',
      ]}
      future={{
        v7_startTransition: true,
        v7_relativeSplatPath: true,
      }}
    >
      <Routes>
        <Route
          path="/projects/:siteId/register"
          element={
            <ProjectMembershipPage />
          }
        />
      </Routes>
    </MemoryRouter>,
  );
}

describe(
  'ProjectMembershipPage',
  () => {
    beforeEach(() => {
      getSiteData.mockReset();
      getSiteProjects.mockReset();

      downloadProjectMembershipPdf.mockReset();

      downloadFromUrl.mockReset();

      getSiteData.mockResolvedValue(
        {
          site: {
            siteId: 'site001',
            siteName: '東京拠点',
          },
          metrics: null,
        },
      );

      downloadProjectMembershipPdf.mockResolvedValue(
        {
          pdfUrl:
            'https://example.com/membership.pdf',
          expiresIn: 300,
        },
      );
    });

    it(
      'shows every membership state by default in the required order',
      async () => {
        getSiteProjects.mockResolvedValue(
          [
            createProject(
              'PROJ003',
              '入会中プロジェクト',
              'joined',
            ),
            createProject(
              'PROJ002',
              '申請中プロジェクト',
              'pending',
            ),
            createProject(
              'PROJ001',
              '未入会プロジェクト',
              'available',
            ),
          ],
        );

        renderPage();

        expect(
          await screen.findByRole(
            'heading',
            {
              name:
                '未入会プロジェクト',
            },
          ),
        ).toBeVisible();

        const projectHeadings =
          screen.getAllByRole(
            'heading',
            {
              level: 3,
            },
          );

        const projectNames =
          projectHeadings
            .map(
              (heading) =>
                heading.textContent,
            )
            .filter(
              (text) =>
                text?.includes(
                  'プロジェクト',
                ),
            );

        expect(projectNames).toEqual([
          '未入会プロジェクト',
          '申請中プロジェクト',
          '入会中プロジェクト',
        ]);
      },
    );

    it(
      'shows compact status filter counts',
      async () => {
        getSiteProjects.mockResolvedValue(
          [
            createProject(
              'PROJ001',
              '未入会1',
              'available',
            ),
            createProject(
              'PROJ002',
              '未入会2',
              'available',
            ),
            createProject(
              'PROJ003',
              '申請中',
              'pending',
            ),
            createProject(
              'PROJ004',
              '入会中',
              'joined',
            ),
          ],
        );

        renderPage();

        await screen.findByText(
          '未入会1',
        );

        expect(
          screen.getByRole(
            'button',
            {
              name: 'すべて 4件',
            },
          ),
        ).toHaveAttribute(
          'aria-pressed',
          'true',
        );

        expect(
          screen.getByRole(
            'button',
            {
              name: '未入会 2件',
            },
          ),
        ).toBeVisible();

        expect(
          screen.getByRole(
            'button',
            {
              name: '申請中 1件',
            },
          ),
        ).toBeVisible();

        expect(
          screen.getByRole(
            'button',
            {
              name: '入会中 1件',
            },
          ),
        ).toBeVisible();
      },
    );

    it(
      'filters projects by membership status',
      async () => {
        const user =
          userEvent.setup();

        getSiteProjects.mockResolvedValue(
          [
            createProject(
              'PROJ001',
              '未入会プロジェクト',
              'available',
            ),
            createProject(
              'PROJ002',
              '申請中プロジェクト',
              'pending',
            ),
            createProject(
              'PROJ003',
              '入会中プロジェクト',
              'joined',
            ),
          ],
        );

        renderPage();

        await screen.findByText(
          '未入会プロジェクト',
        );

        await user.click(
          screen.getByRole(
            'button',
            {
              name: '申請中 1件',
            },
          ),
        );

        expect(
          screen.getByRole(
            'heading',
            {
              name:
                '申請中プロジェクト',
            },
          ),
        ).toBeVisible();

        expect(
          screen.queryByRole(
            'heading',
            {
              name:
                '未入会プロジェクト',
            },
          ),
        ).not.toBeInTheDocument();

        expect(
          screen.queryByRole(
            'heading',
            {
              name:
                '入会中プロジェクト',
            },
          ),
        ).not.toBeInTheDocument();

        await user.click(
          screen.getByRole(
            'button',
            {
              name: 'すべて 3件',
            },
          ),
        );

        expect(
          screen.getByRole(
            'heading',
            {
              name:
                '未入会プロジェクト',
            },
          ),
        ).toBeVisible();

        expect(
          screen.getByRole(
            'heading',
            {
              name:
                '入会中プロジェクト',
            },
          ),
        ).toBeVisible();
      },
    );

    it(
      'opens the preparation dialog from an available project',
      async () => {
        const user =
          userEvent.setup();

        getSiteProjects.mockResolvedValue(
          [
            createProject(
              'PROJ001',
              '未入会プロジェクト',
              'available',
            ),
          ],
        );

        renderPage();

        await screen.findByText(
          '未入会プロジェクト',
        );

        expect(
          screen.queryByRole(
            'dialog',
            {
              name:
                '入会届の提出に必要なもの',
            },
          ),
        ).not.toBeInTheDocument();

        await user.click(
          screen.getByRole(
            'button',
            {
              name:
                'このプロジェクトに入会する',
            },
          ),
        );

        expect(
          screen.getByRole(
            'dialog',
            {
              name:
                '入会届の提出に必要なもの',
            },
          ),
        ).toBeVisible();

        expect(
          screen.getByText(
            '未入会プロジェクト',
          ),
        ).toBeVisible();

        await user.click(
          screen.getByRole(
            'button',
            {
              name: '閉じる',
            },
          ),
        );

        expect(
          screen.queryByRole(
            'dialog',
            {
              name:
                '入会届の提出に必要なもの',
            },
          ),
        ).not.toBeInTheDocument();
      },
    );

    it(
      'uses project list flags without requesting participation details',
      async () => {
        getSiteProjects.mockResolvedValue(
          [
            createProject(
              'PROJ001',
              '入会中プロジェクト',
              'joined',
            ),
          ],
        );

        renderPage();

        expect(
          await screen.findByRole(
            'heading',
            {
              name:
                '入会中プロジェクト',
            },
          ),
        ).toBeVisible();
      },
    );

    it(
      'opens the membership PDF for a joined project',
      async () => {
        const user =
          userEvent.setup();

        getSiteProjects.mockResolvedValue(
          [
            createProject(
              'PROJ001',
              '未入会プロジェクト',
              'available',
            ),
            createProject(
              'PROJ002',
              '入会中プロジェクト',
              'joined',
            ),
          ],
        );

        renderPage();

        await screen.findByText(
          '入会中プロジェクト',
        );

        await user.click(
          screen.getByRole(
            'button',
            {
              name: 'PDFを表示',
            },
          ),
        );

        expect(
          downloadProjectMembershipPdf,
        ).toHaveBeenCalledWith(
          'PROJ002',
        );

        expect(
          downloadFromUrl,
        ).toHaveBeenCalledWith(
          'https://example.com/membership.pdf',
        );
      },
    );

    it(
      'shows an empty state when no public project exists',
      async () => {
        getSiteProjects.mockResolvedValue(
          [],
        );

        renderPage();

        expect(
          await screen.findByText(
            '公開中のプロジェクトはありません。',
          ),
        ).toBeVisible();
      },
    );
  },
);