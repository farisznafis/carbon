/**
 * Keeps an action-link session (master setup, signup revision, invitation) in
 * sessionStorage so a refresh can continue the form. The email token is
 * removed from the URL once read, so without this a refresh lands on the
 * "invalid link" state. The deadline is fixed at first opening by the backend.
 *
 * Storage can be unavailable (private mode, blocked site data); every access
 * is wrapped so the page then simply behaves as before.
 */
export type ActionSessionPage = 'master-setup' | 'signup-revision' | 'user-invitation';

export type StoredActionSession = {
  actionSessionToken: string;
  expiresAt: number;
};

const storageKey = (page: ActionSessionPage) => `action-session:${page}`;

export function saveActionSession(
  page: ActionSessionPage,
  actionSessionToken: string,
  expiresInSeconds: number,
): StoredActionSession {
  const session = { actionSessionToken, expiresAt: Date.now() + expiresInSeconds * 1000 };
  try {
    sessionStorage.setItem(storageKey(page), JSON.stringify(session));
  } catch {
    // The session still works for this page view; only a refresh loses it.
  }
  return session;
}

export function clearActionSession(page: ActionSessionPage) {
  try {
    sessionStorage.removeItem(storageKey(page));
  } catch {
    // Nothing stored that we could remove.
  }
}

/** The stored session, or null when there is none, it is malformed, or its time is up (then removed). */
export function readActionSession(page: ActionSessionPage): StoredActionSession | null {
  let raw: string | null = null;
  try {
    raw = sessionStorage.getItem(storageKey(page));
  } catch {
    return null;
  }
  if (!raw) return null;

  try {
    const session = JSON.parse(raw) as Partial<StoredActionSession>;
    if (
      typeof session.actionSessionToken === 'string' &&
      session.actionSessionToken &&
      typeof session.expiresAt === 'number' &&
      session.expiresAt > Date.now()
    ) {
      return { actionSessionToken: session.actionSessionToken, expiresAt: session.expiresAt };
    }
  } catch {
    // Malformed: fall through and drop it.
  }
  clearActionSession(page);
  return null;
}

/** "10/23 14:35" — the fixed deadline, so it needs no ticking timer. */
export function formatActionSessionDeadline(expiresAt: number) {
  return new Intl.DateTimeFormat('ja-JP', {
    month: 'numeric',
    day: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
  }).format(expiresAt);
}
