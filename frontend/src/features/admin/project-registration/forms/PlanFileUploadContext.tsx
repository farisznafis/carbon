import { createContext, useContext, type ReactNode } from 'react';

/**
 * Uploads one file for a plan page and returns the key to store in the form.
 *
 * The key still points under tmp/; the server promotes it when the page saves.
 */
export type PlanFileUploader = (
  fieldName: string,
  file: File,
) => Promise<{ s3Key: string; originalName: string }>;

const PlanFileUploadContext = createContext<PlanFileUploader | null>(null);

/**
 * Supplies the uploader to whichever fields need it.
 *
 * A context rather than a prop because the field configs are data, walked
 * generically by the renderer — threading an uploader through every field
 * would mean touching all 35 pages' configs to serve the handful with files.
 */
export function PlanFileUploadProvider({
  uploader,
  children,
}: {
  uploader: PlanFileUploader | null;
  children: ReactNode;
}) {
  return (
    <PlanFileUploadContext.Provider value={uploader}>
      {children}
    </PlanFileUploadContext.Provider>
  );
}

/** Null outside the plan wizard, where file inputs keep their raw-File behaviour. */
export function usePlanFileUploader() {
  return useContext(PlanFileUploadContext);
}
