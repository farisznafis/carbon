import {
  useCallback,
  useEffect,
  useMemo,
  useState,
} from 'react';
import {
  useLocation,
  useNavigate,
  useParams,
} from 'react-router-dom';
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
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '@/components/ui/select';
import {
  ProjectMembershipCard,
  type ProjectMembershipStatus,
} from '@/features/user/project-registration/components/ProjectMembershipCard';
import {
  isFromMasterDashboard,
  masterDashboardProjectMembershipState,
} from '@/features/user/project-registration/projectMembershipNavigation';
import { paths } from '@/routes/paths';
import { useDashboardStore } from '@/stores/dashboardStore';
import { downloadFromUrl } from '@/utils/download';
import { useToast } from '@/hooks/use-toast';

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

type MembershipStatusFilter =
  | 'all'
  | ProjectMembershipStatus;

type ProjectMembershipListItem = Readonly<{
  project: SiteProject;
  status: ProjectMembershipStatus;
}>;

const MEMBERSHIP_STATUS_ORDER: Record<
  ProjectMembershipStatus,
  number
> = {
  available: 0,
  pending: 1,
  joined: 2,
};

/** Resolves the application state using the API participation and relation fields. */
function getMembershipStatus(
  project: SiteProject,
): ProjectMembershipStatus {
  if (project.participationStatus === 'OPEN') {
    return 'pending';
  }

  if (project.participationStatus === 'APPROVED') {
    return 'joined';
  }

  if (project.participationStatus === 'REJECTED') {
    return 'available';
  }

  if (project.isCompanyJoined || project.hasSiteRelation) {
    return 'joined';
  }

  return 'available';
}

type MembershipSectionProps = Readonly<{
  projectItems: ProjectMembershipListItem[];
  emptyMessage: string;
  onJoin: (project: SiteProject) => void;
  onDownloadPdf: (project: SiteProject) => void;
  isDownloadingPdf: (project: SiteProject) => boolean;
}>;

/** Renders the projects after sorting and filtering have been applied. */
function MembershipSection({
  projectItems,
  emptyMessage,
  onJoin,
  onDownloadPdf,
  isDownloadingPdf,
}: MembershipSectionProps) {
  return (
    <section aria-label="公開中のプロジェクト">
      {projectItems.length > 0 ? (
        <div className="space-y-4">
          {projectItems.map(({ project, status }) => (
            <ProjectMembershipCard
              key={project.id}
              project={project}
              status={status}
              onJoin={onJoin}
              onDownloadPdf={onDownloadPdf}
              isDownloadingPdf={isDownloadingPdf(project)}
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

type ProjectMembershipContentProps = Readonly<{
  siteId: string;
  embedded?: boolean;
}>;

/** Shares project membership data and actions between the Master tab and standalone route. */
export function ProjectMembershipContent({
  siteId,
  embedded = false,
}: ProjectMembershipContentProps) {
  const location = useLocation();
  const navigate = useNavigate();
  const { toast } = useToast();

  const enteredFromMasterDashboard =
    embedded || isFromMasterDashboard(location.state);

  const setCurrentProject = useDashboardStore(
    (state) => state.setCurrentProject,
  );

  const [projects, setProjects] = useState<SiteProject[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [errorMessage, setErrorMessage] = useState('');

  const [statusFilter, setStatusFilter] =
    useState<MembershipStatusFilter>('all');

  const [selectedProject, setSelectedProject] =
    useState<SiteProject | null>(null);

  const [downloadingProjectIds, setDownloadingProjectIds] =
    useState<Set<string>>(() => new Set());

  const loadMembershipData = useCallback(async () => {
    if (!siteId) {
      setErrorMessage('拠点IDが見つかりません。');
      setIsLoading(false);
      return;
    }

    setIsLoading(true);
    setErrorMessage('');

    try {
      const [, siteProjects] = await Promise.all([
        getSiteData(siteId),
        getSiteProjects(siteId),
      ]);

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

  /**
   * Default order:
   * 1. 未入会
   * 2. 申請中
   * 3. 入会中
   */
  const projectItems = useMemo<ProjectMembershipListItem[]>(
    () =>
      projects
        .map((project) => ({
          project,
          status: getMembershipStatus(project),
        }))
        .sort(
          (first, second) =>
            MEMBERSHIP_STATUS_ORDER[first.status] -
            MEMBERSHIP_STATUS_ORDER[second.status],
        ),
    [projects],
  );

  const visibleProjectItems = useMemo(() => {
    if (statusFilter === 'all') {
      return projectItems;
    }

    return projectItems.filter(
      ({ status }) => status === statusFilter,
    );
  }, [projectItems, statusFilter]);

  /** Opens the preparation dialog for the selected project. */
  const handleJoin = (project: SiteProject) => {
    setSelectedProject(project);
  };

  /** Starts registration only after the preparation dialog is confirmed. */
  const handleConfirmJoin = () => {
    if (!selectedProject) {
      return;
    }

    setCurrentProject(selectedProject);

    navigate(
      paths.projectRegister(siteId, selectedProject.id),
      {
        state: {
          ...(enteredFromMasterDashboard
            ? masterDashboardProjectMembershipState
            : {}),
          returnTo: enteredFromMasterDashboard
            ? paths.masterDashboardMembership
            : paths.projectMembership(siteId),
        },
      },
    );
  };

  /** Opens the membership PDF for a joined project without leaving the page. */
  const handleDownloadPdf = async (project: SiteProject) => {
    if (downloadingProjectIds.has(project.id)) {
      return;
    }

    setDownloadingProjectIds(
      (current) => new Set(current).add(project.id),
    );

    try {
      const { pdfUrl } =
        await downloadProjectMembershipPdf(project.id);

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

  /** Returns the standalone page to the entry context. */
  const handleBack = () => {
    navigate(
      enteredFromMasterDashboard
        ? paths.masterDashboardMembership
        : paths.siteDashboard(siteId),
    );
  };

  const emptyMessage =
    projects.length === 0
      ? '公開中のプロジェクトはありません。'
      : '選択したステータスのプロジェクトはありません。';

  const content = (
    <>
      <header className="flex flex-col gap-4 sm:flex-row sm:items-end sm:justify-between">
        <div className="space-y-2">
          <FormPageTitle as={embedded ? 'h2' : 'h1'}>
            プロジェクトへの入会
          </FormPageTitle>

          <p className="text-base text-foreground">
            入会するプロジェクトを選択し、入会手続きを進めます。
          </p>
        </div>

        <div className="flex w-full items-center gap-2 sm:w-auto">
          <span className="shrink-0 text-small text-muted-foreground">
            ステータス
          </span>

          <Select
            value={statusFilter}
            onValueChange={(value) =>
              setStatusFilter(
                value as MembershipStatusFilter,
              )
            }
            disabled={isLoading || projects.length === 0}
          >
            <SelectTrigger
              className="w-full sm:w-[180px]"
              aria-label="入会ステータスで絞り込む"
            >
              <SelectValue />
            </SelectTrigger>

            <SelectContent>
              <SelectItem value="all">
                すべて
              </SelectItem>

              <SelectItem value="available">
                未入会
              </SelectItem>

              <SelectItem value="pending">
                申請中
              </SelectItem>

              <SelectItem value="joined">
                入会中
              </SelectItem>
            </SelectContent>
          </Select>
        </div>
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
            <p className="text-destructive">
              {errorMessage}
            </p>

            <Button
              type="button"
              variant="outline"
              onClick={loadMembershipData}
            >
              再読み込み
            </Button>
          </CardContent>
        </Card>
      ) : (
        <>
          <MembershipSection
            projectItems={visibleProjectItems}
            emptyMessage={emptyMessage}
            onJoin={handleJoin}
            onDownloadPdf={(project) =>
              void handleDownloadPdf(project)
            }
            isDownloadingPdf={(project) =>
              downloadingProjectIds.has(project.id)
            }
          />

          <Dialog
            open={selectedProject !== null}
            onOpenChange={(open) => {
              if (!open) {
                setSelectedProject(null);
              }
            }}
          >
            <DialogContent className="flex max-h-[calc(100dvh-2rem)] w-[calc(100vw-2rem)] max-w-2xl flex-col gap-0 overflow-hidden rounded-xl p-0">
              <DialogHeader className="shrink-0 border-b border-border p-6 pr-12">
                <DialogTitle>
                  入会届の提出に必要なもの
                </DialogTitle>

                <DialogDescription className="text-small font-normal text-muted-foreground">
                  <span className="block font-semibold text-foreground">
                    {selectedProject?.projectName || '-'}
                  </span>

                  <span className="mt-1 block">
                    申請を始める前に、以下の情報と確認資料をご準備ください。
                  </span>
                </DialogDescription>
              </DialogHeader>

              <div className="min-h-0 flex-1 overflow-y-auto px-6 py-5">
                <div className="space-y-6">
                  {MEMBERSHIP_PREPARATION_SECTIONS.map(
                    (section) => (
                      <section
                        key={section.title}
                        className="space-y-3"
                      >
                        <h3 className="text-base font-semibold leading-6 text-foreground">
                          {section.title}
                        </h3>

                        <ul className="list-disc space-y-2 pl-5 text-small font-normal text-foreground marker:text-primary">
                          {section.items.map((item) => (
                            <li
                              key={item}
                              className="pl-1"
                            >
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
                    ),
                  )}
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

                <Button
                  type="button"
                  size="md"
                  className="w-full sm:w-auto"
                  onClick={handleConfirmJoin}
                >
                  入会届へ進む
                </Button>
              </DialogFooter>
            </DialogContent>
          </Dialog>
        </>
      )}
    </>
  );

  if (embedded) {
    return (
      <div className="min-w-0 space-y-8">
        {content}
      </div>
    );
  }

  return (
    <FormStandalonePage className="overflow-visible">
      <PageBackButton
        onClick={handleBack}
        label=""
        aria-label="前の画面へ戻る"
        className={floatingPageBackButtonClassName}
      />

      <FormPageContent className="max-w-[1200px] space-y-10 pt-16 sm:pt-0">
        {content}
      </FormPageContent>
    </FormStandalonePage>
  );
}

/** Keeps the existing site-specific route while reusing the membership content. */
export default function ProjectMembershipPage() {
  const { siteId = '' } =
    useParams<{ siteId: string }>();

  return (
    <ProjectMembershipContent
      siteId={siteId}
    />
  );
}