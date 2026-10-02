import {
  useCallback,
  useEffect,
  useMemo,
  useRef,
  useState,
} from 'react';
import { useGSAP } from '@gsap/react';
import gsap from 'gsap';
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
import { SkeletonList } from '@/components/feedback/loading';
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
import { MembershipPreparationDialog } from '@/features/user/project-registration/components/MembershipPreparationDialog';
import { MembershipStatusFilter } from '@/features/user/project-registration/components/MembershipStatusFilter';
import { ProjectMembershipList } from '@/features/user/project-registration/components/ProjectMembershipList';
import {
  isFromMasterDashboard,
  masterDashboardProjectMembershipState,
} from '@/features/user/project-registration/projectMembershipNavigation';
import {
  getMembershipStatus,
  MEMBERSHIP_STATUS_ORDER,
  type MembershipStatusFilter as MembershipStatusFilterValue,
  type ProjectMembershipListItem,
  type ProjectMembershipStatus,
} from '@/features/user/project-registration/projectMembershipStatus';
import { paths } from '@/routes/paths';
import { useDashboardStore } from '@/stores/dashboardStore';
import { downloadFromUrl } from '@/utils/download';
import { useToast } from '@/hooks/use-toast';

gsap.registerPlugin(useGSAP);

type ProjectMembershipContentProps = Readonly<{
  siteId: string;
  embedded?: boolean;
}>;

/**
 * Shares project membership data and actions
 * between the Master tab and standalone route.
 */
export function ProjectMembershipContent({
  siteId,
  embedded = false,
}: ProjectMembershipContentProps) {
  const location = useLocation();
  const navigate = useNavigate();
  const { toast } = useToast();
  const contentRef = useRef<HTMLDivElement>(null);

  const enteredFromMasterDashboard =
    embedded ||
    isFromMasterDashboard(location.state);

  const setCurrentProject = useDashboardStore(
    (state) => state.setCurrentProject,
  );

  const [projects, setProjects] =
    useState<SiteProject[]>([]);

  const [isLoading, setIsLoading] =
    useState(true);

  const [errorMessage, setErrorMessage] =
    useState('');

  const [statusFilter, setStatusFilter] =
    useState<MembershipStatusFilterValue>(
      'all',
    );

  const [
    selectedProject,
    setSelectedProject,
  ] =
    useState<SiteProject | null>(
      null,
    );

  const [
    downloadingProjectIds,
    setDownloadingProjectIds,
  ] = useState<Set<string>>(
    () => new Set(),
  );

  const loadMembershipData =
    useCallback(async () => {
      if (!siteId) {
        setErrorMessage(
          '拠点IDが見つかりません。',
        );
        setIsLoading(false);
        return;
      }

      setIsLoading(true);
      setErrorMessage('');

      try {
        const [, siteProjects] =
          await Promise.all([
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
   * Default display order:
   * 1. 未入会
   * 2. 申請中
   * 3. 入会中
   */
  const projectItems =
    useMemo<ProjectMembershipListItem[]>(
      () =>
        projects
          .map((project) => ({
            project,
            status:
              getMembershipStatus(
                project,
              ),
          }))
          .sort(
            (
              first,
              second,
            ) =>
              MEMBERSHIP_STATUS_ORDER[
                first.status
              ] -
              MEMBERSHIP_STATUS_ORDER[
                second.status
              ],
          ),
      [projects],
    );

  const visibleProjectItems =
    useMemo(() => {
      if (statusFilter === 'all') {
        return projectItems;
      }

      return projectItems.filter(
        ({ status }) =>
          status === statusFilter,
      );
    }, [
      projectItems,
      statusFilter,
    ]);

  const membershipStatusCounts =
    useMemo(
      () =>
        projectItems.reduce<
          Record<
            ProjectMembershipStatus,
            number
          >
        >(
          (
            counts,
            projectItem,
          ) => ({
            ...counts,
            [projectItem.status]:
              counts[
                projectItem.status
              ] + 1,
          }),
          {
            available: 0,
            pending: 0,
            joined: 0,
          },
        ),
      [projectItems],
    );

  useGSAP(
    () => {
      if (
        isLoading ||
        errorMessage ||
        typeof window.matchMedia !== 'function'
      ) {
        return;
      }

      const prefersReducedMotion =
        window.matchMedia(
          '(prefers-reduced-motion: reduce)',
        ).matches;

      if (prefersReducedMotion) {
        return;
      }

      gsap.fromTo(
        '[data-membership-reveal]',
        {
          autoAlpha: 0,
          y: embedded ? 0 : 16,
        },
        {
          autoAlpha: 1,
          y: 0,
          duration: 0.45,
          ease: 'power2.out',
          stagger: 0.06,
        },
      );

      gsap.fromTo(
        '[data-membership-project]',
        {
          autoAlpha: 0,
          y: embedded ? 0 : 20,
          scale: embedded ? 1 : 0.985,
        },
        {
          autoAlpha: 1,
          y: 0,
          scale: 1,
          duration: 0.5,
          ease: 'power2.out',
          stagger: 0.05,
        },
      );
    },
    {
      scope: contentRef,
      dependencies: [
        errorMessage,
        isLoading,
        siteId,
      ],
      revertOnUpdate: true,
    },
  );

  /** Opens the preparation dialog for the selected project. */
  const handleJoin = (
    project: SiteProject,
  ) => {
    setSelectedProject(project);
  };

  /**
   * Starts registration only after
   * the preparation dialog is confirmed.
   */
  const handleConfirmJoin = () => {
    if (!selectedProject) {
      return;
    }

    setCurrentProject(
      selectedProject,
    );

    navigate(
      paths.projectRegister(
        siteId,
        selectedProject.id,
      ),
      {
        state: {
          ...(enteredFromMasterDashboard
            ? masterDashboardProjectMembershipState
            : {}),
          returnTo:
            enteredFromMasterDashboard
              ? paths.masterDashboardMembership
              : paths.projectMembership(
                  siteId,
                ),
        },
      },
    );
  };

  /**
   * Opens the membership PDF for a joined
   * project without leaving the page.
   */
  const handleDownloadPdf =
    async (
      project: SiteProject,
    ) => {
      if (
        downloadingProjectIds.has(
          project.id,
        )
      ) {
        return;
      }

      setDownloadingProjectIds(
        (current) =>
          new Set(current).add(
            project.id,
          ),
      );

      try {
        const { pdfUrl } =
          await downloadProjectMembershipPdf(
            project.id,
          );

        downloadFromUrl(pdfUrl);
      } catch (error) {
        toast({
          title:
            'PDFを表示できませんでした',
          description:
            error instanceof Error &&
            error.message
              ? error.message
              : 'しばらくしてからもう一度お試しください。',
          variant: 'destructive',
        });
      } finally {
        setDownloadingProjectIds(
          (current) => {
            const next =
              new Set(current);

            next.delete(project.id);

            return next;
          },
        );
      }
    };

  /**
   * Returns the standalone page
   * to the entry context.
   */
  const handleBack = () => {
    navigate(
      enteredFromMasterDashboard
        ? paths.masterDashboardMembership
        : paths.siteDashboard(
            siteId,
          ),
    );
  };

  const emptyMessage =
    projects.length === 0
      ? '公開中のプロジェクトはありません。'
      : '選択したステータスのプロジェクトはありません。';

  const content = (
    <div
      ref={contentRef}
      className="min-w-0 space-y-6 overflow-x-hidden"
    >
      <header
        data-membership-reveal
        className="flex flex-col gap-4 sm:flex-row sm:items-end sm:justify-between"
      >
        <div className="max-w-3xl space-y-2">
          <FormPageTitle
            as={
              embedded
                ? 'h2'
                : 'h1'
            }
          >
            プロジェクトへの入会
          </FormPageTitle>

          <p className="text-base leading-7 text-foreground">
            公開中のプロジェクトを比較し、申請状況を確認しながら入会手続きを進めます。
          </p>
        </div>

        {!errorMessage && (
          <MembershipStatusFilter
            counts={
              membershipStatusCounts
            }
            activeStatus={
              statusFilter
            }
            onStatusChange={
              setStatusFilter
            }
            disabled={
              isLoading ||
              projects.length === 0
            }
          />
        )}
      </header>

      {isLoading ? (
        <SkeletonList
          items={4}
          className="min-h-[28rem]"
        />
      ) : errorMessage ? (
        <Card className="border-border shadow-none">
          <CardContent className="space-y-5 p-10 text-center">
            <p className="text-destructive">
              {errorMessage}
            </p>

            <Button
              type="button"
              variant="outline"
              onClick={
                loadMembershipData
              }
            >
              再読み込み
            </Button>
          </CardContent>
        </Card>
      ) : (
        <>
          <div
            className="min-w-0"
            aria-live="polite"
          >
            <ProjectMembershipList
              projectItems={
                visibleProjectItems
              }
              emptyMessage={
                emptyMessage
              }
              headingAs={
                embedded
                  ? 'h3'
                  : 'h2'
              }
              onJoin={
                handleJoin
              }
              onDownloadPdf={(
                project,
              ) =>
                void handleDownloadPdf(
                  project,
                )
              }
              isDownloadingPdf={(
                project,
              ) =>
                downloadingProjectIds.has(
                  project.id,
                )
              }
            />
          </div>

          <MembershipPreparationDialog
            project={
              selectedProject
            }
            onClose={() =>
              setSelectedProject(null)
            }
            onConfirm={
              handleConfirmJoin
            }
          />
        </>
      )}
    </div>
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
        className={
          floatingPageBackButtonClassName
        }
      />

      <FormPageContent className="max-w-[1200px] space-y-10 pt-16 sm:pt-0">
        {content}
      </FormPageContent>
    </FormStandalonePage>
  );
}

/**
 * Keeps the existing site-specific route
 * while reusing the membership content.
 */
export default function ProjectMembershipPage() {
  const { siteId = '' } =
    useParams<{
      siteId: string;
    }>();

  return (
    <ProjectMembershipContent
      siteId={siteId}
    />
  );
}