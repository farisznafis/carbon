import { type ReactNode, useCallback, useEffect, useMemo, useState } from 'react';
import { useLocation, useNavigate, useParams } from 'react-router-dom';
import {
  getSiteData,
  getSiteProjects,
  type SiteProject,
} from '@/api/site';
import { downloadProjectMembershipPdf } from '@/api/projects';
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
import {
  Dialog,
  DialogClose,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from '@/components/ui/dialog';
import { Tabs, TabsContent, TabsList, TabsTrigger } from '@/components/ui/tabs';
import { ProjectMembershipCard } from '@/features/user/project-registration/components/ProjectMembershipCard';
import {
  isFromMasterDashboard,
  masterDashboardProjectMembershipState,
} from '@/features/user/project-registration/projectMembershipNavigation';
import { paths } from '@/routes/paths';
import { useDashboardStore } from '@/stores/dashboardStore';
import { downloadFromUrl } from '@/utils/download';
import { useToast } from '@/hooks/use-toast';
import {
  dashboardTabsListClassName,
  dashboardTabTriggerClassName,
} from '@/components/layout/DashboardLayout';

// Keeps the preparation checklist content structured and easy to maintain in one place.
const MEMBERSHIP_PREPARATION_SECTIONS = [
  {
    title: '設備情報',
    items: [
      '設備の稼働開始日',
      '太陽光パネルの公称最大出力',
      'パワーコンディショナーのメーカー、型式、台数、製造番号',
      'パワーコンディショナーの夜間待機電力',
      '蓄電池の設置有無',
      '蓄電池のメーカー、型式、容量（設置している場合）',
      '設備の総投資額',
      '補助金の受給有無、名称、金額（受給している場合）',
      'プロジェクト実施前の電力購入単価',
    ],
  },
  {
    title: '確認資料',
    items: [
      '設備の稼働開始日を確認できる資料',
      '太陽光パネルの仕様書またはカタログ',
      'パワーコンディショナーの仕様書',
      '蓄電池の仕様書またはカタログ（設置している場合）',
      '設備の総投資額を確認できる資料',
      '補助金交付決定を確認できる資料（補助金を受給している場合）',
      '電力購入単価を確認できる資料',
    ],
    note: '対応ファイル形式：PDF、CSV、JPG、JPEG、PNG',
  },
  {
    title: '確認・同意事項',
    items: [
      '各確認事項への回答および同意',
      '事業者名、代表者名、代表者役職',
      '環境社会配慮および持続可能性に関する同意',
      'プロジェクト入会規約への同意',
    ],
    note: '該当する場合、特定排出者コードや省エネ法に関する事業者番号・工場番号の入力が必要です。',
  },
] as const;

type MembershipSectionProps = Readonly<{
  description: string;
  projects: SiteProject[];
  status: 'available' | 'joined';
  emptyMessage: string;
  onJoin?: (project: SiteProject) => void;
  onDownloadPdf?: (project: SiteProject) => void;
  isDownloadingPdf?: (project: SiteProject) => boolean;
  beforeProjectList?: ReactNode;
}>;

// Renders one membership category while allowing category-specific guidance before its project list.
function MembershipSection({
  description,
  projects,
  status,
  emptyMessage,
  onJoin,
  onDownloadPdf,
  isDownloadingPdf,
  beforeProjectList,
}: MembershipSectionProps) {
  return (
    <section>
      <p className="mb-5 whitespace-pre-line text-small text-foreground">
        {description}
      </p>

      {beforeProjectList && <div className="mb-5">{beforeProjectList}</div>}

      {projects.length > 0 ? (
        <div className="space-y-5">
          {projects.map((project) => (
            <ProjectMembershipCard
              key={project.id}
              project={project}
              status={status}
              onJoin={onJoin}
              onDownloadPdf={onDownloadPdf}
              isDownloadingPdf={isDownloadingPdf?.(project) ?? false}
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
  const [isPreparationDialogOpen, setIsPreparationDialogOpen] = useState(false);
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
    () => projects.filter((project) => !project.isCompanyJoined),
    [projects],
  );
  const joinedProjects = useMemo(
    () => projects.filter((project) => project.isCompanyJoined),
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

  // Opens the general preparation guide without selecting or changing a project.
  const handleOpenPreparationDialog = () => {
    setIsPreparationDialogOpen(true);
  };

  // Opens the membership PDF for a joined project without leaving the page.
  const handleDownloadPdf = async (project: SiteProject) => {
    if (downloadingProjectIds.has(project.id)) return;

    setDownloadingProjectIds((current) => new Set(current).add(project.id));
    try {
      const { pdfUrl } = await downloadProjectMembershipPdf(project.id);
      downloadFromUrl(pdfUrl);
    } catch (error) {
      toast({
        title: 'PDFを表示できませんでした',
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
          <p className="text-base text-foreground">
            参加を希望するプロジェクトを選択し、入会届を作成します。
          </p>
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
          <>
            <Tabs defaultValue="available">
              <TabsList className={dashboardTabsListClassName}>
                <TabsTrigger
                  value="available"
                  className={`${dashboardTabTriggerClassName} group min-w-[240px] gap-2`}
                >
                  <span>入会可能なプロジェクト</span>

                  <span
                    className="
                      inline-flex min-w-6 items-center justify-center
                      rounded-full bg-secondary
                      px-2 py-0.5
                      text-xs font-semibold
                      text-secondary-foreground
                      transition-colors

                      group-data-[state=active]:bg-primary-foreground/20
                      group-data-[state=active]:text-primary-foreground
                    "
                  >
                    {availableProjects.length}
                  </span>
                </TabsTrigger>

                <TabsTrigger
                  value="joined"
                  className={`${dashboardTabTriggerClassName} group min-w-[240px] gap-2`}
                >
                  <span>入会中のプロジェクト</span>

                  <span
                    className="
                      inline-flex min-w-6 items-center justify-center
                      rounded-full bg-secondary
                      px-2 py-0.5
                      text-xs font-semibold
                      text-secondary-foreground
                      transition-colors

                      group-data-[state=active]:bg-primary-foreground/20
                      group-data-[state=active]:text-primary-foreground
                    "
                  >
                    {joinedProjects.length}
                  </span>
                </TabsTrigger>
              </TabsList>

              <TabsContent value="available" className="mt-6">
                <MembershipSection
                  description={'この拠点は以下のプロジェクトにまだ入会していません。入会を希望する場合は、入会届へ進んでください。'}
                  projects={availableProjects}
                  status="available"
                  emptyMessage="入会可能なプロジェクトはありません。"
                  onJoin={handleJoin}
                  beforeProjectList={(
                    <Card className="rounded-lg border-border bg-secondary/40 shadow-none">
                      <CardContent className="flex flex-col gap-5 p-5 sm:flex-row sm:items-center sm:justify-between">
                        <div className="min-w-0 space-y-2">
                          <h2 className="text-base font-semibold leading-6 text-foreground">
                            入会届の提出前にご準備ください
                          </h2>
                          <p className="max-w-3xl text-small font-normal text-muted-foreground">
                            入会届の提出には、設備情報の入力と確認資料のアップロードが必要です。
                            あらかじめ必要なものをご確認いただくと、スムーズにお手続きいただけます。
                          </p>
                        </div>
                        <Button
                          type="button"
                          variant="outline"
                          size="md"
                          className="w-full shrink-0 sm:w-auto"
                          onClick={handleOpenPreparationDialog}
                        >
                          必要書類を詳しく確認
                        </Button>
                      </CardContent>
                    </Card>
                  )}
                />
              </TabsContent>

              <TabsContent value="joined" className="mt-6">
                <MembershipSection
                  description="この企業が現在入会しているプロジェクトです。再度入会届を提出する必要はありません。"
                  projects={joinedProjects}
                  status="joined"
                  emptyMessage="入会中のプロジェクトはありません。"
                  onDownloadPdf={(project) => void handleDownloadPdf(project)}
                  isDownloadingPdf={(project) =>
                    downloadingProjectIds.has(project.id)
                  }
                />
              </TabsContent>
            </Tabs>

            <Dialog
              open={isPreparationDialogOpen}
              onOpenChange={setIsPreparationDialogOpen}
            >
              <DialogContent className="flex max-h-[calc(100dvh-2rem)] w-[calc(100vw-2rem)] max-w-2xl flex-col gap-0 overflow-hidden rounded-xl p-0">
                <DialogHeader className="shrink-0 border-b border-border p-6 pr-12">
                  <DialogTitle>入会届の提出に必要なもの</DialogTitle>
                  <DialogDescription className="text-small font-normal text-muted-foreground">
                    申請を始める前に、以下の情報と確認資料をご準備ください。
                  </DialogDescription>
                </DialogHeader>

                <div className="min-h-0 flex-1 overflow-y-auto px-6 py-5">
                  <div className="space-y-6">
                    {MEMBERSHIP_PREPARATION_SECTIONS.map((section) => (
                      <section key={section.title} className="space-y-3">
                        <h3 className="text-base font-semibold leading-6 text-foreground">
                          {section.title}
                        </h3>
                        <ul className="list-disc space-y-2 pl-5 text-small font-normal text-foreground marker:text-primary">
                          {section.items.map((item) => (
                            <li key={item} className="pl-1">
                              {item}
                            </li>
                          ))}
                        </ul>
                        {'note' in section && (
                          <p className="rounded-lg bg-muted px-4 py-3 text-small font-normal text-foreground">
                            {section.note}
                          </p>
                        )}
                      </section>
                    ))}
                  </div>
                </div>

                <DialogFooter className="shrink-0 border-t border-border p-6">
                  <DialogClose asChild>
                    <Button
                      type="button"
                      variant="outline"
                      size="md"
                      className="w-full sm:w-auto"
                    >
                      閉じる
                    </Button>
                  </DialogClose>
                </DialogFooter>
              </DialogContent>
            </Dialog>
          </>
        )}
      </FormPageContent>
    </FormStandalonePage>
  );
}
