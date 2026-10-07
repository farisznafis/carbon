import { useLocation, useSearchParams } from 'react-router-dom';
import { dashboardEmptyStateClassName } from '@/components/layout/DashboardLayout';
import { PageTabsList, PageTabsTrigger } from '@/components/navigation/PageTabs';
import { Tabs, TabsContent } from '@/components/ui/tabs';
import { ProjectFilesPanel } from './ProjectFilesPanel';

const DOCUMENT_VIEWS = [
  { value: 'forms', label: 'フォーム' },
  { value: 'entry', label: 'データ入力' },
] as const;

type DocumentView = (typeof DOCUMENT_VIEWS)[number]['value'];

const DOCUMENT_VIEW_QUERY_PARAM = 'doc';

const resolveDocumentView = (value: string | null): DocumentView =>
  DOCUMENT_VIEWS.some((view) => view.value === value) ? (value as DocumentView) : 'forms';

/** The 書類 tab: uploaded forms, and (later) the data entered from them. */
export function ProjectDocumentsTab({ projectId }: Readonly<{ projectId: string }>) {
  const location = useLocation();
  const [searchParams, setSearchParams] = useSearchParams();
  const activeView = resolveDocumentView(searchParams.get(DOCUMENT_VIEW_QUERY_PARAM));

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
        <ProjectFilesPanel projectId={projectId} />
      </TabsContent>

      <TabsContent value="entry" className="m-0">
        {/* TODO: data entry is designed once its API is specified. */}
        <div className={dashboardEmptyStateClassName}>データ入力は準備中です。</div>
      </TabsContent>
    </Tabs>
  );
}
