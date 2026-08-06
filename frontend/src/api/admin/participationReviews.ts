import { apiRequest, apiVoidRequest } from '@/api/_shared/httpClient';
import {
  mapParticipationReviewDetail,
  mapParticipationReviewList,
} from './participationReviewMapper';
import type {
  ApproveParticipationReviewPayload,
  RejectParticipationReviewPayload,
} from './participationReviewTypes';

const PARTICIPATION_REVIEW_BASE_PATH = '/admin/companies/participate';
const PARTICIPATION_DECISION_BASE_PATH =
  '/admin/companies/participation';

export async function getMyParticipationReviews() {
  const response = await apiRequest<unknown>(
    `${PARTICIPATION_REVIEW_BASE_PATH}/self-sign`,
    { fallbackErrorMessage: 'レビュー可能な入会届一覧の取得に失敗しました。' },
  );

  return mapParticipationReviewList(response);
}

export async function getPendingParticipationReviews() {
  const response = await apiRequest<unknown>(
    `${PARTICIPATION_REVIEW_BASE_PATH}/pending`,
    { fallbackErrorMessage: '承認待ちの入会届一覧の取得に失敗しました。' },
  );

  return mapParticipationReviewList(response);
}

export function getParticipationReviewDetail(
  participationId: string | number,
) {
  return apiRequest<unknown>(
    `${PARTICIPATION_REVIEW_BASE_PATH}/${encodeURIComponent(String(participationId))}`,
    { fallbackErrorMessage: '入会届の詳細取得に失敗しました。' },
  ).then(mapParticipationReviewDetail);
}

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

export type {
  ApproveParticipationReviewPayload,
  ParticipationConfirmationItems,
  ParticipationReviewChecklist,
  ParticipationReviewChecklistField,
  ParticipationReviewDetail,
  ParticipationReviewEquipment,
  ParticipationReviewFile,
  ParticipationReviewState,
  ParticipationSustainabilityCommitment,
  RejectParticipationReviewPayload,
} from './participationReviewTypes';
export { participationReviewChecklistFields } from './participationReviewTypes';
