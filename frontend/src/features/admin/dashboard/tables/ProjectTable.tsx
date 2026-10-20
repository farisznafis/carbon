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
import { statusBadgeToneClassNames } from '@/components/data-display/StatusBadge';
import { cn } from '@/lib/utils';
import { formatProjectDateTime, getProjectStatusTone } from '../projectStatusPresentation';

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
  'minmax(220px,1.5fr) minmax(200px,.9fr) minmax(200px,1.1fr) minmax(172px,.75fr) minmax(150px,.7fr)';

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

function isProjectStatus(
  status: string,
): status is ProjectStatus {
  return projectStatusOptions.includes(
    status as ProjectStatus,
  );
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
      <div className="font-medium text-foreground [word-break:keep-all]">
        {/* Only break after the hyphen so the number never splits; the text is unchanged. */}
        {project.methodCategory && project.methodNumber ? (
          <>
            {project.methodCategory}-<wbr />
            <span className="whitespace-nowrap">{project.methodNumber}</span>
          </>
        ) : (
          methodologyId || '-'
        )}
      </div>

      {project.methodologyVersion ? (
        <div className="mt-0.5 text-small text-muted-foreground">
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
                  className={cn(
                    'h-8 w-full rounded-full border px-3 text-table font-medium shadow-none focus:ring-1 [@media(pointer:coarse)]:h-11',
                    statusBadgeToneClassNames[getProjectStatusTone(String(project.projectStatus))],
                  )}
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