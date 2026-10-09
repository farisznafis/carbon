import { storage, storageKeys } from '@/lib/storage';
import type { GuideContext } from './guideSteps';

/**
 * Which page tours each account has already seen, kept in this browser only
 * until the backend can store it per account.
 */
type SeenGuideTours = Record<string, true>;

const toKey = (userId: string, context: GuideContext) => `${userId}:${context}`;

function readSeenGuideTours(): SeenGuideTours {
  try {
    return storage.getJson<SeenGuideTours>(storageKeys.guideToursSeen) ?? {};
  } catch {
    return {};
  }
}

export function hasSeenGuideTour(userId: string, context: GuideContext): boolean {
  return Boolean(readSeenGuideTours()[toKey(userId, context)]);
}

export function markGuideTourSeen(userId: string, context: GuideContext): void {
  try {
    storage.setJson(storageKeys.guideToursSeen, {
      ...readSeenGuideTours(),
      [toKey(userId, context)]: true,
    });
  } catch {
    // Storage can be unavailable (private mode, quota); the tour then shows again next time.
  }
}
