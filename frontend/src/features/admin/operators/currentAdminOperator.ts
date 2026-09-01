import type { AdminOperator } from '@/api/admin/operators';
import type { User } from '@/api/users';

/** Normalizes account identifiers before comparing records from different API responses. */
function normalizeAccountIdentifier(value: string | null | undefined): string {
  return value?.trim().toLocaleLowerCase('en-US') ?? '';
}

/** Returns a candidate only when one identifier resolves to exactly one account. */
function findUniqueOperator(
  operators: readonly AdminOperator[],
  matches: (operator: AdminOperator) => boolean,
): AdminOperator | null {
  const matchingOperators = operators.filter(matches);
  return matchingOperators.length === 1 ? matchingOperators[0] : null;
}

/** Resolves the signed-in admin account by ID, then username, then email without guessing ambiguous matches. */
export function findCurrentAdminOperator(
  operators: readonly AdminOperator[],
  sessionUser: Pick<User, 'id' | 'username' | 'email'>,
): AdminOperator | null {
  const sessionId = normalizeAccountIdentifier(sessionUser.id);
  if (sessionId) {
    const idMatch = findUniqueOperator(
      operators,
      (operator) => normalizeAccountIdentifier(operator.id) === sessionId,
    );
    if (idMatch) return idMatch;
  }

  const sessionUsername = normalizeAccountIdentifier(sessionUser.username);
  if (sessionUsername) {
    const usernameMatch = findUniqueOperator(
      operators,
      (operator) =>
        normalizeAccountIdentifier(operator.username) === sessionUsername,
    );
    if (usernameMatch) return usernameMatch;
  }

  const sessionEmail = normalizeAccountIdentifier(sessionUser.email);
  if (!sessionEmail) return null;

  return findUniqueOperator(
    operators,
    (operator) => normalizeAccountIdentifier(operator.email) === sessionEmail,
  );
}
