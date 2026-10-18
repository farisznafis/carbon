import { afterEach, describe, expect, it, vi } from 'vitest';
import {
  approveParticipationReview,
  downloadAdminParticipationMembershipPdf,
  getApprovedParticipationReviews,
  getOpenParticipationReviews,
  getParticipationReviewDetail,
  getRejectedParticipationReviews,
  rejectParticipationReview,
} from './participationReviews';
import type { ParticipationReviewChecklist } from './participationReviewTypes';

const checklist: ParticipationReviewChecklist = {
  isSolarContractDoc: true,
  isPcsSpecDoc: true,
  isBatterySpecDoc: true,
  isOperationStartDoc: true,
  isGenerationDataDoc: true,
  isReversePowerDoc: true,
  isInvestmentProofDoc: true,
  isApplicationCompleteCheck: true,
  isFormsAttachCheck: true,
  isEmissionsReductionActivityListCheck: true,
  isOriginalSpecCheck: true,
  isSubsidyCheck: true,
};

const rawReview = {
  participationId: 12,
  siteId: 3,
  projectId: 7,
  projectName: 'プロジェクトA',
  siteName: '拠点A',
  companyName: '会社A',
  submittedAt: '2026-07-30T06:50:55.732Z',
  equipment: {
    id: 9,
    siteId: '3',
    files: [],
    evidenceDocuments: [],
  },
  confirmationItems: {},
  sustainabilityCommitment: {},
  review: {
    ...checklist,
    primaryApplReviewerName: '',
    secondaryApplReviewerName: '',
  },
};

afterEach(() => {
  vi.unstubAllGlobals();
});

function jsonResponse(body: unknown) {
  return new Response(JSON.stringify(body), {
    status: 200,
    headers: { 'Content-Type': 'application/json' },
  });
}

describe('participation review API', () => {
  it('loads and maps open, approved, rejected, and detail endpoints', async () => {
    const fetchMock = vi
      .fn()
      .mockResolvedValueOnce(jsonResponse([rawReview]))
      .mockResolvedValueOnce(jsonResponse([rawReview]))
      .mockResolvedValueOnce(jsonResponse([rawReview]))
      .mockResolvedValueOnce(jsonResponse(rawReview));
    vi.stubGlobal('fetch', fetchMock);

    const open = await getOpenParticipationReviews();
    const approved = await getApprovedParticipationReviews();
    const rejected = await getRejectedParticipationReviews();
    const detail = await getParticipationReviewDetail(12);

    expect(fetchMock.mock.calls.map(([url]) => url)).toEqual([
      '/api/admin/companies/participate/open',
      '/api/admin/companies/participations/approved',
      '/api/admin/companies/participations/rejected',
      '/api/admin/companies/participate/12',
    ]);
    expect(open[0]).toMatchObject({
      participationId: 12,
      projectId: 7,
      projectName: 'プロジェクトA',
    });
    expect(approved[0]?.review.isSolarContractDoc).toBe(true);
    expect(rejected[0]?.participationId).toBe(12);
    expect(detail.equipment.siteId).toBe('3');
  });

  it('uses separate approve and reject payloads and encodes the ID', async () => {
    const fetchMock = vi
      .fn()
      .mockResolvedValue(new Response(null, { status: 204 }));
    vi.stubGlobal('fetch', fetchMock);

    await approveParticipationReview('12/34', {
      reviewerName: '山田 太郎',
      reviewerInkan: '山田',
      ...checklist,
    });
    await rejectParticipationReview('12/34', {
      reviewerName: '山田 太郎',
      reviewerInkan: '山田',
      rejectReason: '資料不足',
      ...checklist,
    });

    expect(fetchMock).toHaveBeenNthCalledWith(
      1,
      '/api/admin/companies/participation/12%2F34/approve',
      expect.objectContaining({
        method: 'POST',
        body: JSON.stringify({
          reviewerName: '山田 太郎',
          reviewerInkan: '山田',
          ...checklist,
        }),
      }),
    );
    expect(fetchMock).toHaveBeenNthCalledWith(
      2,
      '/api/admin/companies/participation/12%2F34/reject',
      expect.objectContaining({
        method: 'POST',
        body: JSON.stringify({
          reviewerName: '山田 太郎',
          reviewerInkan: '山田',
          rejectReason: '資料不足',
          ...checklist,
        }),
      }),
    );
  });

  it('loads an admin membership PDF using both route identifiers', async () => {
    const fetchMock = vi.fn().mockResolvedValue(
      jsonResponse({
        pdfUrl: 'https://example.com/membership.pdf',
        expiresIn: 300,
      }),
    );
    vi.stubGlobal('fetch', fetchMock);

    const result = await downloadAdminParticipationMembershipPdf(
      'PRJ/001',
      '12/34',
    );

    expect(fetchMock).toHaveBeenCalledWith(
      '/api/project/PRJ%2F001/participations/12%2F34/membership-pdf',
      expect.objectContaining({
        fallbackErrorMessage: 'PDFの取得に失敗しました。',
      }),
    );
    expect(result.pdfUrl).toBe('https://example.com/membership.pdf');
  });

  it('rejects a malformed list response', async () => {
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue(jsonResponse({})));

    await expect(getOpenParticipationReviews()).rejects.toThrow(
      '入会届一覧のレスポンス形式が不正です。',
    );
  });
});

