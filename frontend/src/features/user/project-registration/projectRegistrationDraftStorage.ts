import type { AgreementFormValues } from '@/features/user/project-registration/components/agreementsConfig';
import type { ContractStepFormValues } from '@/features/user/project-registration/components/ContractStep';
import type { Equipment } from '@/features/user/project-registration/components/types';
import type { FinalTermsValues } from '@/features/user/project-registration/components/FinalTermsStep';

const PROJECT_REGISTRATION_DRAFT_DATABASE = 'gx-project-registration';
const PROJECT_REGISTRATION_DRAFT_STORE = 'drafts';
export const PROJECT_REGISTRATION_DRAFT_VERSION = 1;

export type ProjectRegistrationDraft = {
  version: typeof PROJECT_REGISTRATION_DRAFT_VERSION;
  currentStep: number;
  equipment: Equipment[];
  agreementFormValues: Partial<AgreementFormValues>;
  contractFormValues: Partial<ContractStepFormValues>;
  contractHasUserChanges?: boolean;
  finalTermsValues: Partial<FinalTermsValues>;
  updatedAt: string;
};

/** Creates a user- and route-scoped key so drafts cannot overlap between applications. */
export function createProjectRegistrationDraftKey(
  userId: string,
  siteId: string,
  projectId: string,
): string {
  return [userId || 'anonymous', siteId, projectId].map(encodeURIComponent).join(':');
}

/** Opens the IndexedDB store used for project-registration drafts and uploaded File objects. */
function openProjectRegistrationDraftDatabase(): Promise<IDBDatabase | null> {
  if (typeof indexedDB === 'undefined') return Promise.resolve(null);

  return new Promise((resolve, reject) => {
    const request = indexedDB.open(
      PROJECT_REGISTRATION_DRAFT_DATABASE,
      PROJECT_REGISTRATION_DRAFT_VERSION,
    );

    request.onupgradeneeded = () => {
      const database = request.result;
      if (!database.objectStoreNames.contains(PROJECT_REGISTRATION_DRAFT_STORE)) {
        database.createObjectStore(PROJECT_REGISTRATION_DRAFT_STORE);
      }
    };
    request.onsuccess = () => resolve(request.result);
    request.onerror = () => reject(request.error);
  });
}

/** Loads a saved project-registration draft, including File objects, for the current application. */
export async function loadProjectRegistrationDraft(
  draftKey: string,
): Promise<ProjectRegistrationDraft | null> {
  const database = await openProjectRegistrationDraftDatabase();
  if (!database) return null;

  return new Promise((resolve, reject) => {
    const transaction = database.transaction(
      PROJECT_REGISTRATION_DRAFT_STORE,
      'readonly',
    );
    const request = transaction.objectStore(PROJECT_REGISTRATION_DRAFT_STORE).get(draftKey);

    request.onsuccess = () => {
      const draft = request.result as ProjectRegistrationDraft | undefined;
      resolve(
        draft?.version === PROJECT_REGISTRATION_DRAFT_VERSION ? draft : null,
      );
    };
    request.onerror = () => reject(request.error);
    transaction.oncomplete = () => database.close();
    transaction.onerror = () => database.close();
  });
}

/** Saves the latest form state to IndexedDB so refreshing does not discard values or files. */
export async function saveProjectRegistrationDraft(
  draftKey: string,
  draft: ProjectRegistrationDraft,
): Promise<void> {
  const database = await openProjectRegistrationDraftDatabase();
  if (!database) return;

  return new Promise((resolve, reject) => {
    const transaction = database.transaction(
      PROJECT_REGISTRATION_DRAFT_STORE,
      'readwrite',
    );
    transaction.objectStore(PROJECT_REGISTRATION_DRAFT_STORE).put(draft, draftKey);
    transaction.oncomplete = () => {
      database.close();
      resolve();
    };
    transaction.onerror = () => {
      database.close();
      reject(transaction.error);
    };
  });
}

/** Deletes a completed application draft so a later application starts cleanly. */
export async function deleteProjectRegistrationDraft(
  draftKey: string,
): Promise<void> {
  const database = await openProjectRegistrationDraftDatabase();
  if (!database) return;

  return new Promise((resolve, reject) => {
    const transaction = database.transaction(
      PROJECT_REGISTRATION_DRAFT_STORE,
      'readwrite',
    );
    transaction.objectStore(PROJECT_REGISTRATION_DRAFT_STORE).delete(draftKey);
    transaction.oncomplete = () => {
      database.close();
      resolve();
    };
    transaction.onerror = () => {
      database.close();
      reject(transaction.error);
    };
  });
}
