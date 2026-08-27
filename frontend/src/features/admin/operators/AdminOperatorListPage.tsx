import { useCallback, useEffect, useMemo, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { Info, Plus, Search, X } from 'lucide-react';
import type { AdminOperator } from '@/api/admin/operators';
import {
  DataGridTable,
  type DataGridTableColumn,
} from '@/components/data-display';
import { PaginationControls } from '@/components/data-display/Pagination/PaginationControls';
import { LoadingPanel } from '@/components/feedback/loading';
import { adminPageContainerClassName } from '@/components/layout/DashboardLayout';
import { Button } from '@/components/ui/button';
import { Card, CardContent } from '@/components/ui/card';
import { Input } from '@/components/ui/input';
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '@/components/ui/select';
import { useAdminOperatorsQuery } from '@/hooks/admin/useAdminOperatorsQuery';
import { usePagination } from '@/hooks/usePagination';
import { paths } from '@/routes/paths';
import {
  getOperatorPermissionCategory,
  getOperatorPermissionLabel,
} from './operatorPermissions';

const ALL_FILTER_VALUE = '__all__';
const NO_PERMISSION_FILTER_VALUE = '__none__';
const EMPTY_OPERATORS: AdminOperator[] = [];

/** Produces the Japanese display name in surname-first order. */
function getOperatorDisplayName(operator: AdminOperator): string {
  return (
    [operator.lastName, operator.firstName].filter(Boolean).join(' ') ||
    operator.name ||
    '-'
  );
}

/** Produces the two-line permission summary required by the operator table. */
function OperatorPermissionSummary({
  permissions,
}: Readonly<{ permissions: string[] }>) {
  if (permissions.length === 0) {
    return <span className="text-muted-foreground">権限なし</span>;
  }

  const primaryCategory = getOperatorPermissionCategory(permissions[0]);
  const remainingCount = permissions.length - 1;

  return (
    <div className="space-y-0.5">
      <p className="font-medium text-foreground">{permissions.length}機能</p>
      <p className="truncate text-xs text-muted-foreground">
        {primaryCategory}
        {remainingCount > 0 ? ` +${remainingCount}` : ''}
      </p>
    </div>
  );
}

/** Renders the dismissible informational note shown above the operator list. */
function OperatorInformationNote({ onClose }: Readonly<{ onClose: () => void }>) {
  return (
    <section className="flex items-start gap-3 rounded-lg border border-primary/25 bg-primary/5 p-4">
      {/* <span className="mt-0.5 flex h-5 w-5 shrink-0 items-center justify-center rounded-full bg-primary text-primary-foreground"> */}
        <Info className="h-5 w-5 text-primary" aria-hidden="true" />
      {/* </span> */}
      <div className="min-w-0 flex-1">
        <h2 className="text-sm font-bold text-primary">補足</h2>
        <p className="mt-1 text-sm leading-6 text-foreground">
          運用者は、管理者から付与された権限の範囲内で日常の運用業務を行います。
        </p>
      </div>
      <Button
        type="button"
        variant="ghost"
        size="icon"
        className="-mr-2 -mt-2 h-8 w-8 shrink-0"
        onClick={onClose}
        aria-label="補足を閉じる"
      >
        <X className="h-4 w-4" aria-hidden="true" />
      </Button>
    </section>
  );
}

/** Renders the Figma empty state while retaining the surrounding list surface. */
function OperatorEmptyState({ onAdd }: Readonly<{ onAdd: () => void }>) {
  return (
    <div className="flex flex-col items-center px-6 py-14 text-center sm:py-16">
      <span className="flex h-[72px] w-[72px] items-center justify-center rounded-full bg-secondary text-primary">
        <Plus className="h-8 w-8" aria-hidden="true" />
      </span>
      <h3 className="mt-6 text-2xl font-bold text-foreground">
        運用者がまだ登録されていません
      </h3>
      <p className="mt-3 max-w-lg text-base leading-7 text-muted-foreground">
        運用者は、管理者から付与された権限の範囲内で
        <br className="hidden sm:block" />
        日常の運用業務を行うユーザーです。
      </p>
      <Button type="button" size="md" className="mt-8" onClick={onAdd}>
        <Plus className="h-4 w-4" aria-hidden="true" />
        最初の運用者を追加
      </Button>
    </div>
  );
}

/** Renders the API-backed operator management list and its empty state. */
export default function AdminOperatorListPage() {
  const navigate = useNavigate();
  const operatorsQuery = useAdminOperatorsQuery();
  const operators = operatorsQuery.data?.users ?? EMPTY_OPERATORS;
  const [permissionFilter, setPermissionFilter] = useState(ALL_FILTER_VALUE);
  const [searchTerm, setSearchTerm] = useState('');
  const [showInformation, setShowInformation] = useState(true);

  const permissionOptions = useMemo(
    () =>
      Array.from(
        new Set(operators.flatMap((operator) => operator.permissions)),
      ).sort((a, b) =>
        getOperatorPermissionLabel(a).localeCompare(
          getOperatorPermissionLabel(b),
          'ja',
        ),
      ),
    [operators],
  );

  const filteredOperators = useMemo(() => {
    const normalizedSearch = searchTerm.trim().toLocaleLowerCase('ja');

    return operators.filter((operator) => {
      if (
        permissionFilter === NO_PERMISSION_FILTER_VALUE &&
        operator.permissions.length > 0
      ) {
        return false;
      }
      if (
        permissionFilter !== ALL_FILTER_VALUE &&
        permissionFilter !== NO_PERMISSION_FILTER_VALUE &&
        !operator.permissions.includes(permissionFilter)
      ) {
        return false;
      }
      if (!normalizedSearch) return true;

      const searchableText = [
        getOperatorDisplayName(operator),
        operator.username,
        operator.email,
      ]
        .join(' ')
        .toLocaleLowerCase('ja');

      return searchableText.includes(normalizedSearch);
    });
  }, [operators, permissionFilter, searchTerm]);

  const pagination = usePagination(filteredOperators, { itemsPerPage: 10 });
  const { setCurrentPage } = pagination;

  useEffect(() => {
    setCurrentPage(1);
  }, [permissionFilter, searchTerm, setCurrentPage]);

  /** Navigates every add-operator CTA to the same registration route. */
  const handleAddOperator = useCallback(
    () => navigate(paths.adminOperatorRegister),
    [navigate],
  );

  /** Opens the admin-specific read-only profile for one operator. */
  const handleViewProfile = useCallback(
    (operator: AdminOperator) =>
      navigate(paths.adminOperatorProfile(operator.id)),
    [navigate],
  );

  const columns = useMemo<DataGridTableColumn<AdminOperator>[]>(
    () => [
      {
        id: 'name',
        label: '運用者名',
        sortable: true,
        sortValue: getOperatorDisplayName,
        render: (operator) => (
          <span className="font-medium text-foreground">
            {getOperatorDisplayName(operator)}
          </span>
        ),
      },
      {
        id: 'username',
        label: 'ユーザネーム',
        sortable: true,
        sortValue: (operator) => operator.username,
        render: (operator) => operator.username || '-',
      },
      {
        id: 'email',
        label: 'メールアドレス',
        sortable: true,
        sortValue: (operator) => operator.email,
        render: (operator) => (
          <span className="break-all">{operator.email || '-'}</span>
        ),
      },
      {
        id: 'permissions',
        label: '権限',
        render: (operator) => (
          <OperatorPermissionSummary permissions={operator.permissions} />
        ),
      },
      {
        id: 'action',
        label: '操作',
        render: (operator) => (
          <Button
            type="button"
            variant="outline"
            size="sm"
            onClick={() => handleViewProfile(operator)}
          >
            プロファイル
          </Button>
        ),
      },
    ],
    [handleViewProfile],
  );

  return (
    <main className="min-h-[calc(100dvh-5rem)] bg-background py-8">
      <div className={`${adminPageContainerClassName} space-y-4`}>
        <header className="flex flex-col gap-4 sm:flex-row sm:items-end sm:justify-between">
          <div>
            <h1 className="text-3xl font-bold leading-tight text-foreground">
              運用者管理
            </h1>
            <p className="mt-2 text-base leading-6 text-foreground">
              運用業務を担当するユーザーのアカウントを管理します。
            </p>
          </div>
          <Button
            type="button"
            size="md"
            className="self-start sm:self-auto"
            onClick={handleAddOperator}
          >
            <Plus className="h-4 w-4" aria-hidden="true" />
            運用者を追加
          </Button>
        </header>

        {showInformation && (
          <OperatorInformationNote onClose={() => setShowInformation(false)} />
        )}

        <Card className="rounded-lg border-border bg-card shadow-app-sm">
          <CardContent className="p-5 sm:p-7 lg:px-9">
            <div className="flex items-baseline gap-4">
              <h2 className="text-2xl font-bold text-foreground">運用者一覧</h2>
              <span className="text-lg text-muted-foreground">
                {operators.length}件
              </span>
            </div>

            {operatorsQuery.isLoading ? (
              <LoadingPanel
                className="mt-6 min-h-72 border-0 bg-transparent"
                message="運用者一覧を読み込んでいます"
              />
            ) : operatorsQuery.isError ? (
              <div
                role="alert"
                className="mt-6 flex min-h-56 flex-col items-center justify-center gap-4 rounded-lg border border-border bg-background p-6 text-center"
              >
                <p className="text-sm text-foreground">
                  運用者一覧を表示できませんでした。
                </p>
                <Button
                  type="button"
                  variant="outline"
                  size="md"
                  onClick={() => operatorsQuery.refetch()}
                >
                  再試行
                </Button>
              </div>
            ) : (
              <>
                <div className="mt-6 flex flex-col gap-3 lg:flex-row lg:items-center lg:justify-between">
                  <div className="w-full sm:max-w-xs">
                    <Select
                      value={permissionFilter}
                      onValueChange={setPermissionFilter}
                    >
                      <SelectTrigger
                        className="h-10 bg-card"
                        aria-label="権限で絞り込む"
                      >
                        <SelectValue placeholder="権限" />
                      </SelectTrigger>
                      <SelectContent>
                        <SelectItem value={ALL_FILTER_VALUE}>すべての権限</SelectItem>
                        {permissionOptions.map((permission) => (
                          <SelectItem key={permission} value={permission}>
                            {getOperatorPermissionLabel(permission)}
                          </SelectItem>
                        ))}
                        <SelectItem value={NO_PERMISSION_FILTER_VALUE}>
                          権限なし
                        </SelectItem>
                      </SelectContent>
                    </Select>
                  </div>
                  <div className="relative w-full lg:max-w-md">
                    <Search
                      className="pointer-events-none absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-muted-foreground"
                      aria-hidden="true"
                    />
                    <Input
                      type="search"
                      value={searchTerm}
                      onChange={(event) => setSearchTerm(event.target.value)}
                      placeholder="氏名・ユーザー名・メールアドレスで検索"
                      className="h-10 bg-card pl-9"
                    />
                  </div>
                </div>

                {operators.length === 0 ? (
                  <OperatorEmptyState onAdd={handleAddOperator} />
                ) : (
                  <div className="mt-6">
                    <DataGridTable
                      rows={pagination.paginatedItems}
                      columns={columns}
                      gridTemplateColumns="1.1fr 1fr 1.6fr 1.2fr 104px"
                      minWidth={880}
                      getRowKey={(operator) => operator.id}
                      emptyMessage="条件に一致する運用者はいません。"
                    />
                    <PaginationControls
                      currentPage={pagination.currentPage}
                      totalPages={pagination.totalPages}
                      totalItems={pagination.totalItems}
                      itemsPerPage={pagination.itemsPerPage}
                      rowsPerPageOptions={[10, 20, 50]}
                      onPageChange={pagination.goToPage}
                      onItemsPerPageChange={pagination.setItemsPerPage}
                      className="mt-4"
                    />
                  </div>
                )}
              </>
            )}
          </CardContent>
        </Card>
      </div>
    </main>
  );
}
