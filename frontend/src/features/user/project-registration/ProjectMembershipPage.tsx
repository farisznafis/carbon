import { useCallback, useEffect, useMemo, useState } from 'react';
import { useLocation, useNavigate, useParams } from 'react-router-dom';
import {
  getSiteData,
  getSiteProjects,
  type SiteProject,
} from '@/api/site';
import { FormPageWrapper } from '@/components/layout/FormPageWrapper/FormPageWrapper';
import { Button } from '@/components/ui/button';
import { Card, CardContent } from '@/components/ui/card';
import { ProjectMembershipCard } from '@/features/user/project-registration/components/ProjectMembershipCard';
import {
  isFromMasterDashboard,
  masterDashboardProjectMembershipState,
} from '@/features/user/project-registration/projectMembershipNavigation';
import { paths } from '@/routes/paths';
import { useDashboardStore } from '@/stores/dashboardStore';

type MembershipSectionProps = Readonly<{
  title: string;
  description: string;
  projects: SiteProject[];
  status: 'available' | 'joined';
  emptyMessage: string;
  onJoin?: (project: SiteProject) => void;
}>;

function MembershipSection({
  title,
  description,
  projects,
  status,
  emptyMessage,
  onJoin,
}: MembershipSectionProps) {
  return (
    <section>
      <div className="mb-5">
        <div className="flex flex-wrap items-baseline gap-3">
          <h2 className="text-foreground">{title}</h2>
          <span className="text-small text-muted-foreground">
            {projects.length}件
          </span>
        </div>
        <p className="text-small mt-2 text-muted-foreground">{description}</p>
      </div>

      {projects.length > 0 ? (
        <div className="space-y-5">
          {projects.map((project) => (
            <ProjectMembershipCard
              key={project.id}
              project={project}
              status={status}
              onJoin={onJoin}
            />
          ))}
        </div>
      ) : (
        <Card className="rounded-lg border-dashed border-border shadow-none">
          <CardContent className="p-8 text-center text-muted-foreground">
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
  const enteredFromMasterDashboard = isFromMasterDashboard(location.state);
  const setCurrentProject = useDashboardStore(
    (state) => state.setCurrentProject,
  );
  const [siteName, setSiteName] = useState('');
  const [projects, setProjects] = useState<SiteProject[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [errorMessage, setErrorMessage] = useState('');

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
      state: enteredFromMasterDashboard
        ? masterDashboardProjectMembershipState
        : undefined,
    });
  };

  const handleBack = () => {
    navigate(
      enteredFromMasterDashboard
        ? paths.masterDashboard
        : paths.siteDashboard(siteId),
    );
  };

  return (
    <FormPageWrapper
      pageName="プロジェクト入会届"
      title=""
      onBack={handleBack}
      contentMaxWidth="max-w-[1200px]"
      wrapInCard={false}
    >
      <div className="space-y-10">
        {siteName && (
          <p className="text-center text-muted-foreground">
            拠点名：{siteName}
          </p>
        )}

        {isLoading ? (
          <Card className="rounded-lg border-border shadow-none">
            <CardContent className="p-10 text-center text-muted-foreground">
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
          <>
            <MembershipSection
              title="未参加のプロジェクト"
              description="この拠点は以下のプロジェクトにまだ参加していません。参加を希望する場合は、入会届へ進んでください。"
              projects={availableProjects}
              status="available"
              emptyMessage="未参加のプロジェクトはありません。"
              onJoin={handleJoin}
            />
            <MembershipSection
              title="参加中のプロジェクト"
              description="この拠点が現在参加しているプロジェクトです。再度入会届を提出する必要はありません。"
              projects={joinedProjects}
              status="joined"
              emptyMessage="参加中のプロジェクトはありません。"
            />
          </>
        )}
      </div>
    </FormPageWrapper>
  );
}
