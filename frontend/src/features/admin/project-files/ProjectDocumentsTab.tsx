import { useEffect, useState } from 'react';
import { useLocation, useSearchParams } from 'react-router-dom';
import { Upload } from 'lucide-react';
import type { ProjectFileType } from '@/api/admin/projectFiles';
import { PageTabsList, PageTabsTrigger } from '@/components/navigation/PageTabs';
import { Button } from '@/components/ui/button';
import { Tabs, TabsContent } from '@/components/ui/tabs';
import { useAdminPermissions } from '@/features/admin/permissions/useAdminPermissions';
import { ProjectDocumentDataPanel } from './ProjectDocumentDataPanel';
import { ProjectFilesPanel, type ProjectFileUploadRequest } from './ProjectFilesPanel';
import { PROJECT_FILE_TYPES } from './projectFileTypes';

const DOCUMENT_VIEWS = [
  { value: 'forms', label: 'フォーム' },
  { value: 'acquisition', label: 'データ取得' },
] as const;

type DocumentView = (typeof DOCUMENT_VIEWS)[number]['value'];

export const DOCUMENT_VIEW_QUERY_PARAM = 'doc';
/** Set by other screens (the 概要 書類 card) to land here with the upload dialog open. */
export const DOCUMENT_UPLOAD_QUERY_PARAM = 'upload';

const resolveDocumentView = (value: string | null): DocumentView =>
  DOCUMENT_VIEWS.some((view) => view.value === value) ? (value as DocumentView) : 'forms';

const resolveUploadType = (value: string | null): ProjectFileType | null =>
  PROJECT_FILE_TYPES.find((definition) => definition.type === value)?.type ?? null;

/** The 書類 tab: uploaded forms, and the data read from them. */
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
      {/* The row carries the underline so the upload button sits on the same line as the tabs. */}
      <div className="flex items-center gap-3 border-b border-border">
        <PageTabsList aria-label="書類の表示" className="min-w-0 flex-1 border-b-0">
          {DOCUMENT_VIEWS.map((view) => (
            <PageTabsTrigger key={view.value} value={view.value}>
              {view.label}
            </PageTabsTrigger>
          ))}
        </PageTabsList>
        {activeView === 'forms' && (
          <Button
            type="button"
            size="sm"
            className="shrink-0"
            onClick={() =>
              permissions.guardAction('PROJECT_UPDATE', () => setUploadRequest({ type: null }))
            }
          >
            <Upload aria-hidden="true" />
            ファイルをアップロード
          </Button>
        )}
      </div>

      <TabsContent value="forms" className="m-0">
        <ProjectFilesPanel
          projectId={projectId}
          uploadRequest={uploadRequest}
          onUploadRequestChange={setUploadRequest}
        />
      </TabsContent>

      <TabsContent value="acquisition" className="m-0">
        <ProjectDocumentDataPanel projectId={projectId} />
      </TabsContent>
    </Tabs>
  );
}
