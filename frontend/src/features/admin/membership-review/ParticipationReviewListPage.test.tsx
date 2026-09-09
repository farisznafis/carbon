import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import {
  MemoryRouter,
  Route,
  Routes,
  useLocation,
} from 'react-router-dom';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { mapParticipationReviewDetail } from '@/api/admin/participationReviewMapper';
import ParticipationReviewListPage from './ParticipationReviewListPage';

const { getAdminProjectDetail, getParticipationReviewsByStatus } = vi.hoisted(
  () => ({
    getAdminProjectDetail: vi.fn(),
    getParticipationReviewsByStatus: vi.fn(),
  }),
);

vi.mock('@/api/admin', async (importOriginal) => {
  const actual = await importOriginal<typeof import('@/api/admin')>();
  return {
    ...actual,
    getAdminProjectDetail,
    getParticipationReviewsByStatus,
  };
});

vi.mock('@/contexts/AuthContext', () => ({
  useAuth: () => ({ user: { role: 'admin' } }),
}));

/** Creates a mapped membership review for list-page scenarios. */
function makeReview(
  participationId: number,
  projectId: number,
  companyName: string,
  primaryReviewer = '',
) {
  return mapParticipationReviewDetail({
    participationId,
    siteId: participationId,
    projectId,
    projectName: `プロジェクト${projectId}`,
    siteName: `拠点${participationId}`,
    companyName,
    submittedAt: '2026-07-30T06:50:55.732Z',
    equipment: {},
    confirmationItems: {},
    sustainabilityCommitment: {},
    review: {
      primaryApplReviewerName: primaryReviewer,
      secondaryApplReviewerName: '',
    },
  });
}

/** Exposes the current location so status-query navigation can be asserted. */
function LocationProbe() {
  const location = useLocation();
  return <output data-testid="location">{`${location.pathname}${location.search}`}</output>;
}

/** Renders the project-scoped membership list with its detail destination. */
function renderPage(initialEntry = '/admin/projects/7/memberships') {
  return render(
    <MemoryRouter
      initialEntries={[initialEntry]}
      future={{ v7_startTransition: true, v7_relativeSplatPath: true }}
    >
      <LocationProbe />
      <Routes>
        <Route
          path="/admin/projects/:projectId/memberships"
          element={<ParticipationReviewListPage />}
        />
        <Route
          path="/admin/projects/:projectId/memberships/:participationId"
          element={<div>detail page</div>}
        />
      </Routes>
    </MemoryRouter>,
  );
}

describe('ParticipationReviewListPage', () => {
  beforeEach(() => {
    getAdminProjectDetail.mockReset();
    getParticipationReviewsByStatus.mockReset();
    getAdminProjectDetail.mockResolvedValue({
      id: '7',
      projectName: 'プロジェクト7',
    });
    getParticipationReviewsByStatus.mockResolvedValue({
      OPEN: [
        makeReview(1, 7, '承認待ち会社'),
        makeReview(2, 8, '別プロジェクト会社'),
      ],
      APPROVED: [makeReview(3, 7, '承認済み会社', '一次確認者')],
      REJECTED: [makeReview(4, 7, '却下会社')],
    });
  });

  it('filters every status to the route project and switches among three tabs', async () => {
    const user = userEvent.setup();
    renderPage();

    expect(await screen.findByText('承認待ち会社')).toBeInTheDocument();
    expect(screen.queryByText('別プロジェクト会社')).not.toBeInTheDocument();
    expect(screen.getByRole('tab', { name: '承認待ち（1）' })).toBeInTheDocument();
    expect(screen.getByRole('tab', { name: '承認済み（1）' })).toBeInTheDocument();
    expect(screen.getByRole('tab', { name: '却下（1）' })).toBeInTheDocument();
    expect(screen.getByText('一次確認待ち')).toBeInTheDocument();

    await user.click(screen.getByRole('tab', { name: '承認済み（1）' }));
    expect(screen.getByText('承認済み会社')).toBeInTheDocument();
    expect(screen.getByText('承認済み')).toBeInTheDocument();
    expect(screen.getByTestId('location')).toHaveTextContent(
      '/admin/projects/7/memberships?status=APPROVED',
    );

    await user.click(screen.getByRole('button', { name: '詳細' }));
    expect(screen.getByTestId('location')).toHaveTextContent(
      '/admin/projects/7/memberships/3?status=APPROVED',
    );
  });

  it('restores a rejected tab directly from the status query', async () => {
    renderPage('/admin/projects/7/memberships?status=REJECTED');

    expect(await screen.findByText('却下会社')).toBeInTheDocument();
    expect(screen.getByText('却下')).toBeInTheDocument();
    expect(screen.getByRole('tab', { name: '却下（1）' })).toHaveAttribute(
      'data-state',
      'active',
    );
  });

  it('renders the shared 404 page when a page-loading request fails', async () => {
    getParticipationReviewsByStatus.mockRejectedValueOnce(
      new Error('gateway error'),
    );
    renderPage();

    expect(
      await screen.findByRole('heading', { name: 'ページが見つかりません' }),
    ).toBeInTheDocument();
    expect(screen.queryByText('gateway error')).not.toBeInTheDocument();
    expect(
      screen.getByRole('button', { name: 'ホームに戻る' }),
    ).toBeInTheDocument();
  });
});
