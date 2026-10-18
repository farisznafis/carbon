import type { ProjectStatus } from '@/api/admin/projects';
import type { Project } from '@/api/projects';
import { useState } from 'react';
import {
  DataGridTable,
  type DataGridTableColumn,
} from '@/components/data-display/DataGridTable';
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '@/components/ui/select';
import { ProjectCompletionDialog } from './ProjectCompletionDialog';
import { formatProjectDateTime } from '../projectStatusPresentation';

type ProjectTableProps = {
  projects: Project[];
  /** Opens the project detail; used by both the 詳細 button and the row click. */
  onEdit?: (project: Project) => void;
  onStatusChange?: (
    project: Project,
    status: ProjectStatus,
  ) => void | Promise<void>;
  updatingProjectStatusId?: string | null;
};

const projectTableGridColumns =
  'minmax(240px,1.6fr) minmax(100px,.5fr) minmax(240px,1.35fr) minmax(140px,.7fr) minmax(155px,.75fr)';

const projectStatusOptions: ProjectStatus[] = [
  '作成済み',
  '準備中',
  '変更中',
  '妥当性確認処理',
  '却下',
  '実施中',
  '検証中',
  'クレジット申請中',
  '終了',
];

const statusColors: Record<
  ProjectStatus,
  { bg: string; text: string; border: string }
> = {
  作成済み: {
    bg: '#F1F5F9',
    text: '#465467',
    border: '#CBD5E1',
  },
  準備中: {
    bg: '#FEF3C7',
    text: '#8A3908',
    border: '#FCD34D',
  },
  変更中: {
    bg: '#DBEAFE',
    text: '#0C3CC3',
    border: '#93C5FD',
  },
  妥当性確認処理: {
    bg: '#E0E7FF',
    text: '#3D32C0',
    border: '#A5B4FC',
  },
  却下: {
    bg: '#FEE2E2',
    text: '#9B0909',
    border: '#FCA5A5',
  },
  実施中: {
    bg: '#DCFCE7',
    text: '#006024',
    border: '#86EFAC',
  },
  検証中: {
    bg: '#CCFBF1',
    text: '#005B54',
    border: '#5EEAD4',
  },
  クレジット申請中: {
    bg: '#F3E8FF',
    text: '#6F13BF',
    border: '#D8B4FE',
  },
  終了: {
    bg: '#E5E7EB',
    text: '#000000',
    border: '#D1D5DB',
  },
};

const projectStatusAliases: Partial<Record<string, ProjectStatus>> = {
  CREATED: '作成済み',
  PREPARING: '準備中',
  CHANGING: '変更中',
  VALIDATING: '妥当性確認処理',
  REJECTED: '却下',
  ACTIVE: '実施中',
  RUNNING: '実施中',
  VERIFYING: '検証中',
  VERIFICATION: '検証中',
  CREDIT_APPLICATION: 'クレジット申請中',
  ENDED: '終了',
  INACTIVE: '終了',
};

function isProjectStatus(
  status: string,
): status is ProjectStatus {
  return projectStatusOptions.includes(
    status as ProjectStatus,
  );
}

function getProjectStatusColors(
  status: Project['projectStatus'],
) {
  const normalizedStatus =
    String(status).trim();

  if (isProjectStatus(normalizedStatus)) {
    return statusColors[normalizedStatus];
  }

  const aliasedStatus =
    projectStatusAliases[
      normalizedStatus.toUpperCase()
    ];

  return statusColors[
    aliasedStatus ?? '作成済み'
  ];
}

function getStatusOptions(
  currentStatus: string,
): string[] {
  if (
    !currentStatus ||
    isProjectStatus(currentStatus)
  ) {
    return projectStatusOptions;
  }

  return [
    currentStatus,
    ...projectStatusOptions,
  ];
}

function isTerminalProjectStatus(
  status: string,
) {
  const normalizedStatus = status
    .trim()
    .toUpperCase();

  return [
    '終了',
    'ENDED',
    'INACTIVE',
  ].includes(normalizedStatus);
}

function displayValue(value: unknown) {
  if (
    value === null ||
    value === undefined ||
    value === ''
  ) {
    return '-';
  }

  return String(value);
}

/** Plain single-line cell for the methodology columns. */
function TextCell({
  value,
}: {
  value: unknown;
}) {
  return (
    <span className="block truncate text-body text-foreground">
      {displayValue(value)}
    </span>
  );
}

function MethodologyIdentityCell({
  project,
}: {
  project: Project;
}) {
  const methodologyId = [
    project.methodCategory,
    project.methodNumber,
  ]
    .filter(Boolean)
    .join('-');

  return (
    <div className="min-w-0 leading-5">
      <div className="font-medium text-foreground">
        {methodologyId || '-'}
      </div>

      {project.methodologyVersion ? (
        <div className="mt-0.5 text-small text-foreground">
          {project.methodologyVersion}
        </div>
      ) : null}
    </div>
  );
}

export function ProjectTable({
  projects,
  onEdit,
  onStatusChange,
  updatingProjectStatusId,
}: ProjectTableProps) {
  const [
    completionProject,
    setCompletionProject,
  ] = useState<Project | null>(null);

  const handleStatusChange = (
    project: Project,
    status: ProjectStatus,
  ) => {
    if (status === '終了') {
      setCompletionProject(project);
      return;
    }

    void onStatusChange?.(
      project,
      status,
    );
  };

  const confirmCompletion =
    async () => {
      if (
        !completionProject ||
        !onStatusChange
      ) {
        return;
      }

      await onStatusChange(
        completionProject,
        '終了',
      );

      setCompletionProject(null);
    };

  const columns: DataGridTableColumn<Project>[] =
    [
      {
        id: 'projectName',
        label: 'プロジェクト名',
        sortable: true,
        sortValue: (project) =>
          project.projectName,
        render: (project) => (
          <span
            className="line-clamp-2 text-body font-bold leading-5 text-foreground"
            title={displayValue(
              project.projectName,
            )}
          >
            {displayValue(
              project.projectName,
            )}
          </span>
        ),
      },

      {
        id: 'methodologyIdentity',
        label: '方法論ID',
        sortable: true,
        sortValue: (project) =>
          [
            project.methodCategory,
            project.methodNumber,
            project.methodologyVersion,
          ]
            .filter(Boolean)
            .join(' '),

        render: (project) => (
          <MethodologyIdentityCell
            project={project}
          />
        ),
      },

      {
        id: 'methodName',
        label: '方法論名',
        sortable: true,
        sortValue: (project) =>
          project.methodName,

        render: (project) => (
          <span
            className="line-clamp-2 text-body leading-5 text-foreground"
            title={displayValue(
              project.methodName,
            )}
          >
            {displayValue(
              project.methodName,
            )}
          </span>
        ),
      },

      {
        id: 'projectStatus',
        label: 'ステータス',
        sortable: true,
        sortValue: (project) =>
          project.projectStatus,

        render: (project) => {
          const statusColor =
            getProjectStatusColors(
              project.projectStatus,
            );

          const statusIsTerminal =
            isTerminalProjectStatus(
              project.projectStatus,
            );

          const isUpdating =
            updatingProjectStatusId ===
            project.id;

          return (
            <span
              className="block min-w-0"
              data-row-click-ignore
            >
              <Select
                value={
                  project.projectStatus
                }
                disabled={
                  isUpdating ||
                  statusIsTerminal
                }
                onValueChange={(value) =>
                  handleStatusChange(
                    project,
                    value as ProjectStatus,
                  )
                }
              >
                <SelectTrigger
                  className="h-8 w-full rounded-full border px-3 text-table font-bold shadow-none focus:ring-1"
                  style={{
                    backgroundColor: statusColor.bg,
                    color: statusColor.text,
                    borderColor: statusColor.border,
                  }}
                >
                  <SelectValue />
                </SelectTrigger>

                <SelectContent>
                  {getStatusOptions(
                    project.projectStatus,
                  ).map((status) => (
                    <SelectItem
                      key={status}
                      value={status}
                    >
                      {status}
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
            </span>
          );
        },
      },

      {
        id: 'updatedAt',
        label: '最終更新',
        sortable: true,

        sortValue: (project) =>
          project.updatedAt ??
          project.createdAt,

        render: (project) => (
          <span className="block whitespace-nowrap text-body text-foreground">
            {formatProjectDateTime(
              project.updatedAt ??
                project.createdAt,
            )}
          </span>
        ),
      },
    ];

  return (
    <>
      <DataGridTable
        rows={projects}
        columns={columns}
        gridTemplateColumns={
          projectTableGridColumns
        }
        minWidth={1120}
        getRowKey={(project) =>
          project.id
        }
        emptyMessage="まだプロジェクトが登録されていません。"
        onRowClick={onEdit}
      />

      <ProjectCompletionDialog
        project={
          completionProject
        }
        isSubmitting={
          Boolean(
            completionProject,
          ) &&
          updatingProjectStatusId ===
            completionProject?.id
        }
        onOpenChange={(
          open,
        ) => {
          if (!open) {
            setCompletionProject(
              null,
            );
          }
        }}
        onConfirm={() =>
          void confirmCompletion()
        }
      />
    </>
  );
}