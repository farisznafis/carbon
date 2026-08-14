import { useCallback, useEffect, useMemo, useState } from 'react';
import { useLocation, useNavigate, useParams } from 'react-router-dom';
import {
  getSiteData,
  getSiteProjects,
  downloadProjectMembershipPdf,
  type SiteProject,
} from '@/api/site';
import {
  FormPageContent,
  FormPageTitle,
  FormStandalonePage,
} from '@/components/forms/form-layout';
import {
  PageBackButton,
  floatingPageBackButtonClassName,
} from '@/components/navigation/PageBackButton';
import { Button } from '@/components/ui/button';
import { Card, CardContent } from '@/components/ui/card';
import { Tabs, TabsContent, TabsList, TabsTrigger } from '@/components/ui/tabs';
import { ProjectMembershipCard } from '@/features/user/project-registration/components/ProjectMembershipCard';
import {
  isFromMasterDashboard,
  masterDashboardProjectMembershipState,
} from '@/features/user/project-registration/projectMembershipNavigation';
import { paths } from '@/routes/paths';
import { useDashboardStore } from '@/stores/dashboardStore';
import { downloadBlob } from '@/utils/download';
import { useToast } from '@/hooks/use-toast';

type MembershipSectionProps = Readonly<{
  description: string;
  projects: SiteProject[];
  status: 'available' | 'joined';
  emptyMessage: string;
  onJoin?: (project: SiteProject) => void;
  onDownloadPdf?: (project: SiteProject) => void;
  downloadingProjectIds?: ReadonlySet<string>;
}>;

function MembershipSection({
  description,
  projects,
  status,
  emptyMessage,
  onJoin,
  onDownloadPdf,
  downloadingProjectIds,
}: MembershipSectionProps) {
  return (
    <section>
      <p className="text-small mb-5 text-foreground">{description}</p>

      {projects.length > 0 ? (
        <div className="space-y-5">
          {projects.map((project) => (
            <ProjectMembershipCard
              key={project.id}
              project={project}
              status={status}
              onJoin={onJoin}
              onDownloadPdf={onDownloadPdf}
              isDownloadingPdf={downloadingProjectIds?.has(project.id)}
            />
          ))}
        </div>
      ) : (
        <Card className="rounded-lg border-dashed border-border shadow-none">
          <CardContent className="p-8 text-center text-foreground">
            {emptyMessage}
          </CardContent>
        </Card>
      )}
    </section>
  );
}

export default function ProjectMembershipPage() {
  const { siteId = '' } = useParams<{ siteId: string }>();
  const location = useLocation();
  const navigate = useNavigate();
  const { toast } = useToast();
  const enteredFromMasterDashboard = isFromMasterDashboard(location.state);
  const setCurrentProject = useDashboardStore(
    (state) => state.setCurrentProject,
  );
  const [siteName, setSiteName] = useState('');
  const [projects, setProjects] = useState<SiteProject[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [errorMessage, setErrorMessage] = useState('');
  const [downloadingProjectIds, setDownloadingProjectIds] = useState<Set<string>>(
    () => new Set(),
  );

  const loadMembershipData = useCallback(async () => {
    if (!siteId) {
      setErrorMessage('拠点IDが見つかりません。');
      setIsLoading(false);
      return;
    }

    setIsLoading(true);
    setErrorMessage('');

    try {
      const [siteData, siteProjects] = await Promise.all([
        getSiteData(siteId),
        getSiteProjects(siteId),
      ]);

      setSiteName(siteData.site?.siteName ?? '');
      setProjects(siteProjects);
    } catch (error) {
      setErrorMessage(
        error instanceof Error
          ? error.message
          : 'プロジェクト情報の取得に失敗しました。',
      );
    } finally {
      setIsLoading(false);
    }
  }, [siteId]);

  useEffect(() => {
    void loadMembershipData();
  }, [loadMembershipData]);

  const availableProjects = useMemo(
    () => projects.filter((project) => !project.isJoined),
    [projects],
  );
  const joinedProjects = useMemo(
    () => projects.filter((project) => project.isJoined),
    [projects],
  );

  const handleJoin = (project: SiteProject) => {
    setCurrentProject(project);
    navigate(paths.projectRegister(siteId, project.id), {
      state: {
        ...(enteredFromMasterDashboard
          ? masterDashboardProjectMembershipState
          : {}),
        returnTo: paths.projectMembership(siteId),
      },
    });
  };

  const handleDownloadPdf = async (project: SiteProject) => {
    if (!project.participationId || downloadingProjectIds.has(project.id)) return;

    setDownloadingProjectIds((current) => new Set(current).add(project.id));
    try {
      const { blob, fileName } = await downloadProjectMembershipPdf(
        project.id,
        project.participationId,
      );
      downloadBlob(blob, fileName);
    } catch (error) {
      toast({
        title: 'PDFをダウンロードできませんでした',
        description:
          error instanceof Error && error.message
            ? error.message
            : 'しばらくしてからもう一度お試しください。',
        variant: 'destructive',
      });
    } finally {
      setDownloadingProjectIds((current) => {
        const next = new Set(current);
        next.delete(project.id);
        return next;
      });
    }
  };

  const handleBack = () => {
    navigate(
      enteredFromMasterDashboard
        ? paths.masterDashboard
        : paths.siteDashboard(siteId),
    );
  };

  return (
    <FormStandalonePage className="overflow-visible">
      <PageBackButton
        onClick={handleBack}
        label=""
        aria-label="前の画面へ戻る"
        className={floatingPageBackButtonClassName}
      />

      <FormPageContent className="max-w-[1200px] space-y-10 pt-16 sm:pt-0">
        <header className="space-y-2">
          <FormPageTitle as="h1">プロジェクト入会届</FormPageTitle>
          {/* {siteName && (
            <p className="text-small text-foreground">対象拠点：{siteName}</p>
          )} */}
        </header>

        {isLoading ? (
          <Card className="rounded-lg border-border shadow-none">
            <CardContent className="p-10 text-center text-foreground">
              読み込み中...
            </CardContent>
          </Card>
        ) : errorMessage ? (
          <Card className="rounded-lg border-border shadow-none">
            <CardContent className="space-y-5 p-10 text-center">
              <p className="text-destructive">{errorMessage}</p>
              <Button type="button" variant="outline" onClick={loadMembershipData}>
                再読み込み
              </Button>
            </CardContent>
          </Card>
        ) : (
          <Tabs defaultValue="available">
            <TabsList className="grid h-auto w-full grid-cols-2 justify-start sm:inline-flex sm:h-10 sm:w-auto">
              <TabsTrigger
                value="available"
                className="min-w-0 flex-col gap-0.5 px-2 sm:flex-row sm:gap-0 sm:px-3"
              >
                入会可能なプロジェクト
                <span className="rounded-full bg-muted px-1.5 py-0.5 text-xs font-medium text-foreground sm:ml-1.5">
                  {availableProjects.length}
                </span>
              </TabsTrigger>
              <TabsTrigger
                value="joined"
                className="min-w-0 flex-col gap-0.5 px-2 sm:flex-row sm:gap-0 sm:px-3"
              >
                入会中のプロジェクト
                <span className="rounded-full bg-muted px-1.5 py-0.5 text-xs font-medium text-foreground sm:ml-1.5">
                  {joinedProjects.length}
                </span>
              </TabsTrigger>
            </TabsList>

            <TabsContent value="available" className="mt-6">
              <MembershipSection
                description="この拠点は以下のプロジェクトにまだ入会していません。入会を希望する場合は、入会届へ進んでください。"
                projects={availableProjects}
                status="available"
                emptyMessage="入会可能なプロジェクトはありません。"
                onJoin={handleJoin}
              />
            </TabsContent>

            <TabsContent value="joined" className="mt-6">
              <MembershipSection
                description="この拠点が現在入会しているプロジェクトです。再度入会届を提出する必要はありません。"
                projects={joinedProjects}
                status="joined"
                emptyMessage="入会中のプロジェクトはありません。"
                onDownloadPdf={(project) => void handleDownloadPdf(project)}
                downloadingProjectIds={downloadingProjectIds}
              />
            </TabsContent>
          </Tabs>
        )}
      </FormPageContent>
    </FormStandalonePage>
  );
}
