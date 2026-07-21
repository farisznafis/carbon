import { useMemo, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import {
  AlertTriangle,
  ArrowLeft,
  Building2,
  Clock3,
  ShieldCheck,
} from 'lucide-react';
import type {
  AdminCompanyAccountActionTarget,
  CompanyStatus,
  CompanySuspension,
  SuspendCompanyPayload,
  SuspensionReasonCode,
} from '@/api/admin';
import {
  getCompanySuspensions,
  resolveCompanySuspension,
  suspendAdminCompany,
} from '@/api/admin';
import { ApiError } from '@/api/_shared/errors';
import { Badge } from '@/components/ui/badge';
import { Button } from '@/components/ui/button';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import {
  AlertDialog,
  AlertDialogAction,
  AlertDialogCancel,
  AlertDialogContent,
  AlertDialogDescription,
  AlertDialogFooter,
  AlertDialogHeader,
  AlertDialogTitle,
} from '@/components/ui/alert-dialog';
import { Tabs, TabsContent, TabsList, TabsTrigger } from '@/components/ui/tabs';
import { useAuth } from '@/contexts/AuthContext';
import { CompanySuspendDialog } from '@/features/admin/dashboard/tables/CompanySuspendDialog';
import { CompanySuspensionResolveDialog } from '@/features/admin/dashboard/tables/CompanySuspensionResolveDialog';
import { useAdminCompanyDetailQuery } from '@/hooks/admin/useAdminCompanyDetailQuery';
import { useToast } from '@/hooks/use-toast';
import { paths } from '@/routes/paths';
import { AdminCompanySitesTable } from './components/AdminCompanySitesTable';
import { AdminCompanyUsersTable } from './components/AdminCompanyUsersTable';
import {
  formatAdminCompanyAddress,
  formatAdminCompanyDateTime,
  formatAdminRole,
  formatCompanyStatus,
  formatSubscriptionStatus,
} from './companyDetailDisplay';

// ─── Constants ───────────────────────────────────────────────────────────────

const SUSPEND_REASON_CODE: SuspensionReasonCode = 'POLICY_VIOLATION';

const DEFAULT_SUSPEND_PAYLOAD: SuspendCompanyPayload = {
  reasonCode: SUSPEND_REASON_CODE,
  reasonDetail: '',
};

const companyStatusClassNames: Record<CompanyStatus, string> = {
  PENDING: 'border-transparent bg-amber-100 text-amber-800 hover:bg-amber-100',
  REJECTED: 'border-transparent bg-red-100 text-red-800 hover:bg-red-100',
  ACTIVE: 'border-transparent bg-emerald-100 text-emerald-800 hover:bg-emerald-100',
  SUSPENDED: 'border-transparent bg-slate-100 text-slate-700 hover:bg-slate-100',
};

// ─── Helpers ─────────────────────────────────────────────────────────────────

function getErrorMessage(error: unknown, fallback: string) {
  if (error instanceof ApiError) return error.message || fallback;
  if (error instanceof Error) return error.message || fallback;
  return fallback;
}

function getFieldErrors(error: unknown) {
  const errors = error instanceof ApiError ? error.payload?.errors : null;
  if (!errors || typeof errors !== 'object' || Array.isArray(errors)) return {};
  return Object.fromEntries(
    Object.entries(errors).filter(
      (entry): entry is [string, string] => typeof entry[1] === 'string',
    ),
  );
}

// ─── Sub-components ───────────────────────────────────────────────────────────

function StatusBadge({ status }: { status: CompanyStatus }) {
  return (
    <Badge className={companyStatusClassNames[status]}>
      {formatCompanyStatus(status)}
    </Badge>
  );
}

/** Definition-list row: label left / top, value right / bottom */
function DefinitionRow({
  label,
  children,
}: {
  label: string;
  children: React.ReactNode;
}) {
  return (
    <div className="grid grid-cols-1 gap-0.5 sm:grid-cols-[180px_1fr] sm:gap-2 sm:items-start py-2 border-b border-border/50 last:border-0">
      <dt className="text-sm text-muted-foreground">{label}</dt>
      <dd className="text-sm font-medium text-foreground break-words">{children}</dd>
    </div>
  );
}

function DetailLoadingState() {
  return (
    <div className="flex min-h-[60vh] items-center justify-center text-sm text-muted-foreground">
      読み込み中...
    </div>
  );
}

function DetailErrorState({
  message,
  onBack,
  onRetry,
}: {
  message: string;
  onBack: () => void;
  onRetry?: () => void;
}) {
  return (
    <main className="min-h-screen bg-background px-4 py-10 sm:px-6">
      <div className="mx-auto w-full max-w-7xl">
        <Card className="mx-auto max-w-lg rounded-xl">
          <CardHeader>
            <CardTitle className="text-base font-bold text-foreground">
              企業情報を取得できませんでした
            </CardTitle>
          </CardHeader>
          <CardContent className="space-y-6">
            <p className="text-sm text-muted-foreground">{message}</p>
            <div className="flex flex-wrap gap-3">
              <Button type="button" variant="outline" onClick={onBack}>
                戻る
              </Button>
              {onRetry && (
                <Button type="button" onClick={onRetry}>
                  再試行
                </Button>
              )}
            </div>
          </CardContent>
        </Card>
      </div>
    </main>
  );
}

// ─── Page ─────────────────────────────────────────────────────────────────────

export default function AdminCompanyDetailPage() {
  const { companyId } = useParams();
  const navigate = useNavigate();
  const { user } = useAuth();
  const { toast } = useToast();
  const companyDetailQuery = useAdminCompanyDetailQuery(companyId);

  const [submitting, setSubmitting] = useState(false);
  const [fieldErrors, setFieldErrors] = useState<Record<string, string>>({});

  // Suspend dialog
  const [suspendDialogOpen, setSuspendDialogOpen] = useState(false);
  const [suspendPayload, setSuspendPayload] = useState<SuspendCompanyPayload>(
    DEFAULT_SUSPEND_PAYLOAD,
  );

  // Resolve dialog
  const [resolveDialogOpen, setResolveDialogOpen] = useState(false);
  const [resolveLoading, setResolveLoading] = useState(false);
  const [activeSuspensions, setActiveSuspensions] = useState<CompanySuspension[]>([]);
  const [selectedSuspensionId, setSelectedSuspensionId] = useState<number | null>(null);
  const [resolutionNote, setResolutionNote] = useState('');

  // Danger zone confirmation dialog
  const [dangerConfirmOpen, setDangerConfirmOpen] = useState(false);

  const detail = companyDetailQuery.data;

  const actionTarget = useMemo<AdminCompanyAccountActionTarget | null>(() => {
    if (!detail) return null;
    return {
      companyId: detail.company.companyId,
      nameJA: detail.company.nameJA,
      email: detail.company.email,
    };
  }, [detail]);

  const isOwnCompany =
    user?.companyId != null &&
    detail != null &&
    String(user.companyId) === String(detail.company.companyId);

  // ── Handlers ───────────────────────────────────────────────────────────────

  const handleSuspendDialogOpenChange = (open: boolean) => {
    setSuspendDialogOpen(open);
    if (!open) {
      setSuspendPayload(DEFAULT_SUSPEND_PAYLOAD);
      setFieldErrors({});
    }
  };

  const handleOpenSuspendDialog = () => {
    if (isOwnCompany) {
      toast({
        title: 'このアカウントは停止できません',
        description: '管理者自身が所属する企業は停止できません。',
        variant: 'destructive',
      });
      return;
    }
    // Show the danger zone confirmation first
    setDangerConfirmOpen(true);
  };

  const handleDangerConfirmed = () => {
    setDangerConfirmOpen(false);
    setSuspendDialogOpen(true);
  };

  const handleConfirmSuspend = async () => {
    if (!actionTarget) return;
    setSubmitting(true);
    setFieldErrors({});
    try {
      await suspendAdminCompany(actionTarget.companyId, {
        reasonCode: SUSPEND_REASON_CODE,
        reasonDetail: suspendPayload.reasonDetail,
      });
      handleSuspendDialogOpenChange(false);
      await companyDetailQuery.refetch();
      toast({ title: 'アカウントを停止しました', variant: 'success' });
    } catch (error) {
      setFieldErrors(getFieldErrors(error));
      toast({
        title: 'アカウント停止に失敗しました',
        description: getErrorMessage(error, 'アカウント停止に失敗しました。'),
        variant: 'destructive',
      });
    } finally {
      setSubmitting(false);
    }
  };

  const handleResolveDialogOpenChange = (open: boolean) => {
    setResolveDialogOpen(open);
    if (!open) {
      setActiveSuspensions([]);
      setSelectedSuspensionId(null);
      setResolutionNote('');
      setFieldErrors({});
    }
  };

  const handleOpenResolveDialog = async () => {
    if (!actionTarget) return;
    setResolveDialogOpen(true);
    setResolveLoading(true);
    setFieldErrors({});
    try {
      const response = await getCompanySuspensions(actionTarget.companyId, {
        activeOnly: true,
        page: 1,
        limit: 20,
      });
      setActiveSuspensions(response.suspensions);
      setSelectedSuspensionId(response.suspensions[0]?.id ?? null);
    } catch (error) {
      toast({
        title: '停止理由を取得できませんでした',
        description: getErrorMessage(error, '停止理由の取得に失敗しました。'),
        variant: 'destructive',
      });
    } finally {
      setResolveLoading(false);
    }
  };

  const handleConfirmResolve = async () => {
    if (!actionTarget || selectedSuspensionId === null) return;
    const suspension = activeSuspensions.find((s) => s.id === selectedSuspensionId);
    if (!suspension) return;
    setSubmitting(true);
    setFieldErrors({});
    try {
      await resolveCompanySuspension(
        actionTarget.companyId,
        selectedSuspensionId,
        {
          resolutionNote,
          ...(suspension.reasonCode === 'BILLING'
            ? { paymentMethod: 'CREDIT_CARD' as const }
            : {}),
        },
      );
      handleResolveDialogOpenChange(false);
      await companyDetailQuery.refetch();
      toast({ title: 'アカウントを有効化しました', variant: 'success' });
    } catch (error) {
      setFieldErrors(getFieldErrors(error));
      toast({
        title: 'アカウントの有効化に失敗しました',
        description: getErrorMessage(error, 'アカウントの有効化に失敗しました。'),
        variant: 'destructive',
      });
    } finally {
      setSubmitting(false);
    }
  };

  const handleBack = () => navigate(paths.adminDashboard);

  // ── Guard states ───────────────────────────────────────────────────────────

  if (!companyId) {
    return (
      <DetailErrorState
        message="企業IDが指定されていません。"
        onBack={handleBack}
      />
    );
  }

  if (companyDetailQuery.isLoading) return <DetailLoadingState />;

  if (!detail || companyDetailQuery.isError) {
    return (
      <DetailErrorState
        message={getErrorMessage(
          companyDetailQuery.error,
          '企業情報を取得できませんでした。',
        )}
        onBack={handleBack}
        onRetry={() => void companyDetailQuery.refetch()}
      />
    );
  }

  // ── Derived values ─────────────────────────────────────────────────────────

  const { company, masterAccount, users, sites } = detail;

  const masterFullName =
    masterAccount.fullName ||
    [masterAccount.lastName, masterAccount.firstName].filter(Boolean).join(' ');

  const masterFullNameKana =
    masterAccount.fullNameKana ||
    [masterAccount.lastNameKana, masterAccount.firstNameKana]
      .filter(Boolean)
      .join(' ');

  const formattedAddress = formatAdminCompanyAddress(company);
  const isSuspended = company.companyStatus === 'SUSPENDED';
  const dangerZoneTitle = isSuspended ? 'アカウント操作' : '危険な操作';
  const dangerZoneActionLabel = isSuspended ? 'アカウント有効化' : 'アカウント停止';
  const dangerZoneActionDescription = isSuspended
    ? '停止中のアカウントを再び有効にします。'
    : 'この企業のアカウントを停止すると、ログインできなくなります。';

  // ── Render ─────────────────────────────────────────────────────────────────

  return (
    <main className="min-h-screen bg-background px-4 py-8 sm:px-6">
      <div className="mx-auto w-full max-w-7xl space-y-6">

        {/* ── Page Header ─────────────────────────────────────────────────── */}
        <div className="space-y-3">
          {/* Back link */}
          <button
            type="button"
            onClick={handleBack}
            className="inline-flex items-center gap-1.5 text-sm text-muted-foreground hover:text-foreground transition-colors"
          >
            <ArrowLeft className="h-4 w-4" />
            一覧に戻る
          </button>

          {/* Title row */}
          <div className="flex items-start justify-between gap-4">
            <div className="space-y-1.5 min-w-0">
              <p className="text-sm text-muted-foreground tracking-wide">
                企業情報の詳細
              </p>
              <h1 className="text-2xl font-semibold tracking-tight text-foreground truncate">
                {company.nameJA || `企業ID: ${company.companyId}`}
              </h1>

              {/* Inline metadata chips */}
              <div className="flex flex-wrap items-center gap-x-4 gap-y-1.5 pt-0.5">
                <span className="inline-flex items-center gap-1 text-sm text-muted-foreground">
                  ステータス：
                  <StatusBadge status={company.companyStatus} />
                </span>

                {company.isActive !== undefined && (
                  <span className="inline-flex items-center gap-1 text-sm text-muted-foreground">
                    アカウント状態：
                    <Badge variant={company.isActive ? 'default' : 'secondary'}>
                      {company.isActive ? 'アカウント有効' : 'アカウント無効'}
                    </Badge>
                  </span>
                )}

                {company.corpNumber && (
                  <span className="text-sm text-muted-foreground">
                    法人番号：
                    <span className="font-medium text-foreground ml-1">
                      {company.corpNumber}
                    </span>
                  </span>
                )}

                {company.companyId && (
                  <span className="text-sm text-muted-foreground">
                    企業ID：
                    <span className="font-medium text-foreground ml-1">
                      {company.companyId}
                    </span>
                  </span>
                )}
              </div>
            </div>

          </div>
        </div>

        <div className="grid grid-cols-1 items-stretch gap-6 lg:grid-cols-2">
          {/* ── 企業情報 ─────────────────────────────────────────────────── */}
          <Card>
            <CardHeader className="pb-2">
              <div className="flex items-center gap-2">
                <Building2 className="h-5 w-5 text-muted-foreground" />
                <CardTitle className="text-lg font-semibold">企業情報</CardTitle>
              </div>
            </CardHeader>
            <CardContent>
              <dl className="divide-y divide-border/50">
                 <DefinitionRow label="企業名">{company.nameJA || '―'}</DefinitionRow>
                 <DefinitionRow label="企業名（カナ）">{company.nameFurigana || '―'}</DefinitionRow>
                 <DefinitionRow label="企業名（英語）">{company.nameEN || '―'}</DefinitionRow>
                 <DefinitionRow label="法人番号">{company.corpNumber || '―'}</DefinitionRow>
                 <DefinitionRow label="メールアドレス">{company.email || '―'}</DefinitionRow>
                 <DefinitionRow label="電話番号">{company.phoneNumber || '―'}</DefinitionRow>
                 <DefinitionRow label="FAX番号">{company.faxNumber || '―'}</DefinitionRow>
                 <DefinitionRow label="所在地">{formattedAddress || '―'}</DefinitionRow>
              </dl>
            </CardContent>
          </Card>

          {/* ── マスターアカウント ────────────────────────────────────────── */}
          <Card>
            <CardHeader className="pb-2">
              <div className="flex items-center justify-between gap-4">
                <div className="flex items-center gap-2">
                  <ShieldCheck className="h-5 w-5 text-muted-foreground" />
                  <CardTitle className="text-lg font-semibold">
                    マスターアカウント
                  </CardTitle>
                </div>
                {masterAccount.isActive !== undefined && (
                  <Badge variant={masterAccount.isActive ? 'default' : 'secondary'}>
                    {masterAccount.isActive ? 'アカウント有効' : 'アカウント無効'}
                  </Badge>
                )}
              </div>
            </CardHeader>
            <CardContent>
              <dl className="divide-y divide-border/50">
                 <DefinitionRow label="ユーザ名">{masterAccount.username || '―'}</DefinitionRow>
                 <DefinitionRow label="氏名">{masterFullName || '―'}</DefinitionRow>
                 <DefinitionRow label="氏名（カナ）">{masterFullNameKana || '―'}</DefinitionRow>
                 <DefinitionRow label="役職">{masterAccount.userPosition || '―'}</DefinitionRow>
                 <DefinitionRow label="メールアドレス">{masterAccount.email || '―'}</DefinitionRow>
                 <DefinitionRow label="電話番号">{masterAccount.phoneNumber || '―'}</DefinitionRow>
                <DefinitionRow label="権限">
                  {masterAccount.role ? (
                    <Badge variant="outline">{formatAdminRole(masterAccount.role)}</Badge>
                  ) : (
                    '―'
                  )}
                </DefinitionRow>
              </dl>
            </CardContent>
          </Card>
        </div>

        {/* ── 関連情報 (Tabs) ──────────────────────────────────────────────── */}
        <Card>
          <CardHeader className="pb-2">
            <CardTitle className="text-lg font-semibold">関連情報</CardTitle>
          </CardHeader>
          <CardContent>
            <Tabs defaultValue="users">
              <TabsList className="justify-start mb-4">
                <TabsTrigger value="users">
                  ユーザー一覧
                  {users?.length != null && (
                    <span className="ml-1.5 rounded-full bg-muted px-1.5 py-0.5 text-xs font-medium text-muted-foreground">
                      {users.length}
                    </span>
                  )}
                </TabsTrigger>
                <TabsTrigger value="sites">
                  拠点一覧
                  {sites?.length != null && (
                    <span className="ml-1.5 rounded-full bg-muted px-1.5 py-0.5 text-xs font-medium text-muted-foreground">
                      {sites.length}
                    </span>
                  )}
                </TabsTrigger>
              </TabsList>

              <TabsContent value="users">
                <div className="overflow-x-auto">
                  <AdminCompanyUsersTable users={users} />
                </div>
              </TabsContent>

              <TabsContent value="sites">
                <div className="overflow-x-auto">
                  <AdminCompanySitesTable sites={sites} />
                </div>
              </TabsContent>
            </Tabs>
          </CardContent>
        </Card>

        {/* ── 登録・更新情報 ───────────────────────────────────────────────── */}
        <Card>
          <CardHeader className="pb-2">
            <div className="flex items-center gap-2">
              <Clock3 className="h-5 w-5 text-muted-foreground" />
              <CardTitle className="text-lg font-semibold">登録・更新情報</CardTitle>
            </div>
          </CardHeader>
          <CardContent>
            <dl className="divide-y divide-border/50">
              <DefinitionRow label="企業登録日時">
                {formatAdminCompanyDateTime(company.createdAt) || '―'}
              </DefinitionRow>
              <DefinitionRow label="企業登録者">{company.createdBy || '―'}</DefinitionRow>
              <DefinitionRow label="企業更新日時">
                {formatAdminCompanyDateTime(company.updatedAt) || '―'}
              </DefinitionRow>
              <DefinitionRow label="企業更新者">{company.updatedBy || '―'}</DefinitionRow>
              <DefinitionRow label="マスター登録日時">
                {formatAdminCompanyDateTime(masterAccount.createdAt) || '―'}
              </DefinitionRow>
              <DefinitionRow label="マスター登録者">{masterAccount.createdBy || '―'}</DefinitionRow>
              <DefinitionRow label="マスター更新日時">
                {formatAdminCompanyDateTime(masterAccount.updatedAt) || '―'}
              </DefinitionRow>
              <DefinitionRow label="マスター更新者">{masterAccount.updatedBy || '―'}</DefinitionRow>
            </dl>
          </CardContent>
        </Card>

        {/* ── Danger Zone ─────────────────────────────────────────────────── */}
        {(company.companyStatus === 'ACTIVE' || company.companyStatus === 'SUSPENDED') && (
          <Card
            className={
              isSuspended
                ? 'border-emerald-200 bg-emerald-50/40'
                : 'border-destructive/40'
            }
          >
            <CardHeader className="pb-2">
              <div className="flex items-center gap-2">
                {isSuspended ? (
                  <ShieldCheck className="h-5 w-5 text-emerald-600" />
                ) : (
                  <AlertTriangle className="h-5 w-5 text-destructive" />
                )}
                <CardTitle
                  className={
                    isSuspended
                      ? 'text-lg font-semibold text-emerald-700'
                      : 'text-lg font-semibold text-destructive'
                  }
                >
                  {dangerZoneTitle}
                </CardTitle>
              </div>
            </CardHeader>
            <CardContent>
              <div className="flex flex-col gap-1.5 sm:flex-row sm:items-center sm:justify-between">
                <div>
                  {company.companyStatus === 'ACTIVE' ? (
                    <>
                      <p className="text-sm font-medium text-foreground">
                        アカウント停止
                      </p>
                      <p className="text-sm text-muted-foreground">
                        この企業のアカウントを停止すると、ログインできなくなります。
                      </p>
                    </>
                  ) : (
                    <>
                      <p className="text-sm font-medium text-foreground">
                        {dangerZoneActionLabel}
                      </p>
                      <p className="text-sm text-muted-foreground">
                        {dangerZoneActionDescription}
                      </p>
                    </>
                  )}
                </div>

                <div className="shrink-0">
                  {company.companyStatus === 'ACTIVE' && (
                    <Button
                      type="button"
                      variant="destructive"
                      size="sm"
                      disabled={isOwnCompany}
                      title={
                        isOwnCompany
                          ? '管理者自身が所属する企業は停止できません。'
                          : undefined
                      }
                      onClick={handleOpenSuspendDialog}
                    >
                      アカウント停止
                    </Button>
                  )}
                  {company.companyStatus === 'SUSPENDED' && (
                    <Button
                      type="button"
                      size="sm"
                      className="bg-emerald-600 text-white hover:bg-emerald-700"
                      onClick={() => void handleOpenResolveDialog()}
                    >
                      {dangerZoneActionLabel}
                    </Button>
                  )}
                </div>
              </div>
            </CardContent>
          </Card>
        )}
      </div>

      {/* ── Danger Confirmation AlertDialog ─────────────────────────────────── */}
      <AlertDialog open={dangerConfirmOpen} onOpenChange={setDangerConfirmOpen}>
        <AlertDialogContent>
          <AlertDialogHeader>
            <AlertDialogTitle>アカウントを停止しますか？</AlertDialogTitle>
            <AlertDialogDescription asChild>
              <div className="space-y-2 text-sm text-muted-foreground">
                <p>
                  <span className="font-medium text-foreground">
                    {company.nameJA}
                  </span>{' '}
                  のアカウントを停止しようとしています。
                </p>
                <p>
                  停止すると、この企業に所属するすべてのユーザーがログインできなくなります。
                  操作は後から解除できます。
                </p>
              </div>
            </AlertDialogDescription>
          </AlertDialogHeader>
          <AlertDialogFooter>
            <AlertDialogCancel>キャンセル</AlertDialogCancel>
            <AlertDialogAction
              className="bg-destructive text-destructive-foreground hover:bg-destructive/90"
              onClick={handleDangerConfirmed}
            >
              停止手続きへ進む
            </AlertDialogAction>
          </AlertDialogFooter>
        </AlertDialogContent>
      </AlertDialog>

      {/* ── Existing dialogs (unchanged) ────────────────────────────────────── */}
      <CompanySuspendDialog
        open={suspendDialogOpen}
        item={actionTarget}
        payload={suspendPayload}
        fieldErrors={fieldErrors}
        isSubmitting={submitting}
        onOpenChange={handleSuspendDialogOpenChange}
        onPayloadChange={setSuspendPayload}
        onConfirm={handleConfirmSuspend}
      />
      <CompanySuspensionResolveDialog
        open={resolveDialogOpen}
        item={actionTarget}
        suspensions={activeSuspensions}
        selectedSuspensionId={selectedSuspensionId}
        resolutionNote={resolutionNote}
        fieldErrors={fieldErrors}
        isLoading={resolveLoading}
        isSubmitting={submitting}
        onOpenChange={handleResolveDialogOpenChange}
        onSelectedSuspensionIdChange={setSelectedSuspensionId}
        onResolutionNoteChange={setResolutionNote}
        onConfirm={handleConfirmResolve}
      />
    </main>
  );
}
