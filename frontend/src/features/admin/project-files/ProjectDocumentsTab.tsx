import { useEffect, useState } from 'react';
import { useLocation, useSearchParams } from 'react-router-dom';
import type { ProjectFileType } from '@/api/admin/projectFiles';
import { dashboardEmptyStateClassName } from '@/components/layout/DashboardLayout';
import { PageTabsList, PageTabsTrigger } from '@/components/navigation/PageTabs';
import { Tabs, TabsContent } from '@/components/ui/tabs';
import { useAdminPermissions } from '@/features/admin/permissions/useAdminPermissions';
import { ProjectFilesPanel, type ProjectFileUploadRequest } from './ProjectFilesPanel';
import { PROJECT_FILE_TYPES } from './projectFileTypes';

const DOCUMENT_VIEWS = [
  { value: 'forms', label: 'フォーム' },
  { value: 'acquisition', label: 'データ取得' },
] as const;

type DocumentView = (typeof DOCUMENT_VIEWS)[number]['value'];

const DOCUMENT_VIEW_QUERY_PARAM = 'doc';
/** Set by other screens (the 概要 書類 card) to land here with the upload dialog open. */
export const DOCUMENT_UPLOAD_QUERY_PARAM = 'upload';

const resolveDocumentView = (value: string | null): DocumentView =>
  DOCUMENT_VIEWS.some((view) => view.value === value) ? (value as DocumentView) : 'forms';

const resolveUploadType = (value: string | null): ProjectFileType | null =>
  PROJECT_FILE_TYPES.find((definition) => definition.type === value)?.type ?? null;

/** The 書類 tab: uploaded forms, and (later) the data entered from them. */
export function ProjectDocumentsTab({ projectId }: Readonly<{ projectId: string }>) {
  const location = useLocation();
  const permissions = useAdminPermissions();
  const [searchParams, setSearchParams] = useSearchParams();
  const activeView = resolveDocumentView(searchParams.get(DOCUMENT_VIEW_QUERY_PARAM));
  const [uploadRequest, setUploadRequest] = useState<ProjectFileUploadRequest>(null);

  const requestedUploadType = searchParams.get(DOCUMENT_UPLOAD_QUERY_PARAM);

  // A link asked for the upload dialog: open it once, then drop the request from the URL.
  useEffect(() => {
    if (requestedUploadType === null) return;
    permissions.guardAction('PROJECT_UPDATE', () =>
      setUploadRequest({ type: resolveUploadType(requestedUploadType) }),
    );
    const nextSearchParams = new URLSearchParams(searchParams);
    nextSearchParams.delete(DOCUMENT_UPLOAD_QUERY_PARAM);
    nextSearchParams.set(DOCUMENT_VIEW_QUERY_PARAM, 'forms');
    setSearchParams(nextSearchParams, { replace: true, state: location.state });
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [requestedUploadType]);

  const changeView = (value: string) => {
    const nextSearchParams = new URLSearchParams(searchParams);
    nextSearchParams.set(DOCUMENT_VIEW_QUERY_PARAM, resolveDocumentView(value));
    setSearchParams(nextSearchParams, { replace: true, state: location.state });
  };

  return (
    <Tabs value={activeView} onValueChange={changeView} className="min-w-0 space-y-5">
      <PageTabsList aria-label="書類の表示">
        {DOCUMENT_VIEWS.map((view) => (
          <PageTabsTrigger key={view.value} value={view.value}>
            {view.label}
          </PageTabsTrigger>
        ))}
      </PageTabsList>

      <TabsContent value="forms" className="m-0">
        <ProjectFilesPanel
          projectId={projectId}
          uploadRequest={uploadRequest}
          onUploadRequestChange={setUploadRequest}
        />
      </TabsContent>

      <TabsContent value="acquisition" className="m-0">
        {/* TODO: data acquisition is designed once its API is specified. */}
        <div className={dashboardEmptyStateClassName}>データ取得は準備中です。</div>
      </TabsContent>
    </Tabs>
  );
}
