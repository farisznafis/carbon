import { apiRequest, apiVoidRequest } from '@/api/_shared/httpClient';
import {
  mapParticipationReviewDetail,
  mapParticipationReviewList,
} from './participationReviewMapper';
import type {
  ApproveParticipationReviewPayload,
  ParticipationReviewDetail,
  ParticipationReviewStatus,
  RejectParticipationReviewPayload,
} from './participationReviewTypes';
import type { ProjectMembershipPdfDownload } from '@/api/projects/types';

const PARTICIPATION_REVIEW_BASE_PATH = '/admin/companies/participate';
const PARTICIPATION_DECISION_BASE_PATH =
  '/admin/companies/participation';
const PARTICIPATION_STATUS_BASE_PATH = '/admin/companies/participations';

/** GET /admin/companies/participate/open — loads applications awaiting the current admin's review. */
export async function getOpenParticipationReviews() {
  const response = await apiRequest<unknown>(
    `${PARTICIPATION_REVIEW_BASE_PATH}/open`,
    { fallbackErrorMessage: 'レビュー可能な入会届一覧の取得に失敗しました。' },
  );

  return mapParticipationReviewList(response);
}

/** GET /admin/companies/participations/approved — loads fully approved applications. */
export async function getApprovedParticipationReviews() {
  const response = await apiRequest<unknown>(
    `${PARTICIPATION_STATUS_BASE_PATH}/approved`,
    { fallbackErrorMessage: '承認済みの入会届一覧の取得に失敗しました。' },
  );

  return mapParticipationReviewList(response);
}

/** GET /admin/companies/participations/rejected — loads rejected applications. */
export async function getRejectedParticipationReviews() {
  const response = await apiRequest<unknown>(
    `${PARTICIPATION_STATUS_BASE_PATH}/rejected`,
    { fallbackErrorMessage: '却下された入会届一覧の取得に失敗しました。' },
  );

  return mapParticipationReviewList(response);
}

/** Loads all three membership statuses for a project-scoped review screen. */
export async function getParticipationReviewsByStatus(): Promise<
  Record<ParticipationReviewStatus, ParticipationReviewDetail[]>
> {
  const [open, approved, rejected] = await Promise.all([
    getOpenParticipationReviews(),
    getApprovedParticipationReviews(),
    getRejectedParticipationReviews(),
  ]);

  return { OPEN: open, APPROVED: approved, REJECTED: rejected };
}

/** GET /admin/companies/participate/{participationId} — loads one membership review detail. */
export function getParticipationReviewDetail(
  participationId: string | number,
) {
  return apiRequest<unknown>(
    `${PARTICIPATION_REVIEW_BASE_PATH}/${encodeURIComponent(String(participationId))}`,
    { fallbackErrorMessage: '入会届の詳細取得に失敗しました。' },
  ).then(mapParticipationReviewDetail);
}

/** POST /admin/companies/participation/{participationId}/approve — approves one membership application. */
export function approveParticipationReview(
  participationId: string | number,
  payload: ApproveParticipationReviewPayload,
) {
  return apiVoidRequest(
    `${PARTICIPATION_DECISION_BASE_PATH}/${encodeURIComponent(String(participationId))}/approve`,
    {
      method: 'POST',
      body: payload,
      fallbackErrorMessage: '入会届の承認に失敗しました。',
    },
  );
}

/** POST /admin/companies/participation/{participationId}/reject — rejects one membership application. */
export function rejectParticipationReview(
  participationId: string | number,
  payload: RejectParticipationReviewPayload,
) {
  return apiVoidRequest(
    `${PARTICIPATION_DECISION_BASE_PATH}/${encodeURIComponent(String(participationId))}/reject`,
    {
      method: 'POST',
      body: payload,
      fallbackErrorMessage: '入会届の却下に失敗しました。',
    },
  );
}

/** GET /project/{projectId}/participations/{participationId}/membership-pdf — gets the admin PDF URL. */
export async function downloadAdminParticipationMembershipPdf(
  projectId: string,
  participationId: string | number,
): Promise<ProjectMembershipPdfDownload> {
  const response = await apiRequest<ProjectMembershipPdfDownload>(
    `/project/${encodeURIComponent(projectId)}/participations/${encodeURIComponent(String(participationId))}/membership-pdf`,
    {
      forbiddenHandling: 'caller',
      fallbackErrorMessage: 'PDFの取得に失敗しました。',
    },
  );

  if (!response.pdfUrl) {
    throw new Error('PDFのダウンロードURLが見つかりませんでした。');
  }

  return {
    pdfUrl: response.pdfUrl,
    expiresIn: response.expiresIn ?? 0,
  };
}

export type {
  ApproveParticipationReviewPayload,
  ParticipationConfirmationItems,
  ParticipationReviewChecklist,
  ParticipationReviewChecklistField,
  ParticipationReviewDetail,
  ParticipationReviewEquipment,
  ParticipationReviewFile,
  ParticipationReviewStatus,
  ParticipationReviewState,
  ParticipationSustainabilityCommitment,
  RejectParticipationReviewPayload,
} from './participationReviewTypes';
export {
  participationReviewChecklistFields,
  participationReviewStatuses,
} from './participationReviewTypes';
