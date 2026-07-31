import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { mapParticipationReviewDetail } from '@/api/admin/participationReviewMapper';
import ParticipationReviewListPage from './ParticipationReviewListPage';

const {
  getMyParticipationReviews,
  getPendingParticipationReviews,
} = vi.hoisted(() => ({
  getMyParticipationReviews: vi.fn(),
  getPendingParticipationReviews: vi.fn(),
}));

vi.mock('@/api/admin', async (importOriginal) => {
  const actual = await importOriginal<typeof import('@/api/admin')>();
  return {
    ...actual,
    getMyParticipationReviews,
    getPendingParticipationReviews,
  };
});

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

describe('ParticipationReviewListPage', () => {
  beforeEach(() => {
    getMyParticipationReviews.mockReset();
    getPendingParticipationReviews.mockReset();
    getMyParticipationReviews.mockResolvedValue([
      makeReview(1, 7, '自分の対象会社'),
      makeReview(2, 8, '別プロジェクト会社'),
    ]);
    getPendingParticipationReviews.mockResolvedValue([
      makeReview(1, 7, '自分の対象会社'),
      makeReview(3, 7, '全件のみの会社', '一次確認者'),
      makeReview(4, 8, '別プロジェクト会社'),
    ]);
  });

  it('renders the complete API lists and switches between mine and pending', async () => {
    const user = userEvent.setup();
    render(
      <MemoryRouter
        initialEntries={['/admin/projects/7/memberships']}
        future={{ v7_startTransition: true, v7_relativeSplatPath: true }}
      >
        <Routes>
          <Route
            path="/admin/projects/:projectId/memberships"
            element={<ParticipationReviewListPage />}
          />
        </Routes>
      </MemoryRouter>,
    );

    expect(await screen.findByText('自分の対象会社')).toBeInTheDocument();
    expect(screen.getByText('別プロジェクト会社')).toBeInTheDocument();
    expect(screen.queryByText('全件のみの会社')).not.toBeInTheDocument();
    expect(screen.getAllByText('一次確認待ち')).toHaveLength(2);
    expect(
      screen.getByText(
        'あなたがまだ承認していない、対応が必要な入会届を表示しています。',
      ),
    ).toBeInTheDocument();

    await user.click(
      screen.getByRole('tab', { name: '承認待ち全件（3）' }),
    );

    expect(screen.getByText('全件のみの会社')).toBeInTheDocument();
    expect(screen.getByText('二次確認待ち')).toBeInTheDocument();
    expect(screen.getByText('別プロジェクト会社')).toBeInTheDocument();
    expect(
      screen.getByText(
        '他の管理者の承認待ちを含む、最終承認前の入会届をすべて表示しています。',
      ),
    ).toBeInTheDocument();
  });
});
