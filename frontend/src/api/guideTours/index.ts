import { apiRequest } from '@/api/_shared/httpClient';

type GuideToursResponse = {
  statusCode?: string;
  seenGuideTours?: string[] | null;
};

/** Calls GET /guide-tours for the page tours the signed-in account has seen. */
export async function getSeenGuideTours(): Promise<string[]> {
  const response = await apiRequest<GuideToursResponse>('/guide-tours', {
    method: 'GET',
    forbiddenHandling: 'caller',
    fallbackErrorMessage: 'Failed to fetch guide tours',
  });

  return response.seenGuideTours ?? [];
}

/** Calls PUT /guide-tours/{guideTour}; idempotent, returns the updated list. */
export async function markGuideTourSeen(guideTour: string): Promise<string[]> {
  const response = await apiRequest<GuideToursResponse>(
    `/guide-tours/${encodeURIComponent(guideTour)}`,
    {
      method: 'PUT',
      forbiddenHandling: 'caller',
      fallbackErrorMessage: 'Failed to record guide tour',
    },
  );

  return response.seenGuideTours ?? [];
}
