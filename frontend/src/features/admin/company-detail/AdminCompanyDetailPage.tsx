import { useMemo, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import {
  Building2,
  Clock3,
  MapPin,
  ShieldCheck,
  Users,
  Warehouse,
} from 'lucide-react';
import type {
  AdminCompanyAccountActionTarget,
  CompanyStatus,
  CompanySuspension,
  SuspendCompanyPayload,
} from '@/api/admin';
import {
  getCompanySuspensions,
  resolveCompanySuspension,
  suspendAdminCompany,
} from '@/api/admin';
import { ApiError } from '@/api/_shared/errors';
import { PageBackButton } from '@/components/navigation/PageBackButton';
import { Badge } from '@/components/ui/badge';
import { Button } from '@/components/ui/button';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { useAuth } from '@/contexts/AuthContext';
import { CompanySuspendDialog } from '@/features/admin/dashboard/tables/CompanySuspendDialog';
import { CompanySuspensionResolveDialog } from '@/features/admin/dashboard/tables/CompanySuspensionResolveDialog';
import { useAdminCompanyDetailQuery } from '@/hooks/admin/useAdminCompanyDetailQuery';
import { useToast } from '@/hooks/use-toast';
import { paths } from '@/routes/paths';
import { AdminCompanyDetailSection } from './components/AdminCompanyDetailSection';
import { AdminCompanySitesTable } from './components/AdminCompanySitesTable';
import { AdminCompanyUsersTable } from './components/AdminCompanyUsersTable';
import {
  formatAdminCompanyAddress,
  formatAdminCompanyDateTime,
  formatAdminRole,
  formatCompanyStatus,
  formatSubscriptionStatus,
} from './companyDetailDisplay';

const DEFAULT_SUSPEND_PAYLOAD: SuspendCompanyPayload = {
  reasonCode: 'BILLING',
  reasonDetail: '',
};

const companyStatusClassNames: Record<CompanyStatus, string> = {
  PENDING: 'border-transparent bg-amber-100 text-amber-800 hover:bg-amber-100',
  REJECTED: 'border-transparent bg-red-100 text-red-800 hover:bg-red-100',
  ACTIVE:
    'border-transparent bg-emerald-100 text-emerald-800 hover:bg-emerald-100',
  SUSPENDED:
    'border-transparent bg-slate-100 text-slate-700 hover:bg-slate-100',
};

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

function StatusBadge({ status }: { status: CompanyStatus }) {
  return (
    <Badge className={companyStatusClassNames[status]}>
      {formatCompanyStatus(status)}
    </Badge>
  );
}

function DetailLoadingState() {
  return (
    <div className="flex min-h-[60vh] items-center justify-center text-small text-muted-foreground">
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
      <div className="mx-auto w-full max-w-[1200px]">
        <Card className="mx-auto max-w-lg rounded-xl">
          <CardHeader>
            <CardTitle className="text-large font-bold text-foreground">
              企業情報を取得できませんでした
            </CardTitle>
          </CardHeader>
          <CardContent className="space-y-6">
            <p className="text-body text-muted-foreground">{message}</p>
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

export default function AdminCompanyDetailPage() {
  const { companyId } = useParams();
  const navigate = useNavigate();
  const { user } = useAuth();
  const { toast } = useToast();
  const companyDetailQuery = useAdminCompanyDetailQuery(companyId);
  const [submitting, setSubmitting] = useState(false);
  const [fieldErrors, setFieldErrors] = useState<Record<string, string>>({});
  const [suspendDialogOpen, setSuspendDialogOpen] = useState(false);
  const [suspendPayload, setSuspendPayload] = useState<SuspendCompanyPayload>(
    DEFAULT_SUSPEND_PAYLOAD,
  );
  const [resolveDialogOpen, setResolveDialogOpen] = useState(false);
  const [resolveLoading, setResolveLoading] = useState(false);
  const [activeSuspensions, setActiveSuspensions] = useState<
    CompanySuspension[]
  >([]);
  const [selectedSuspensionId, setSelectedSuspensionId] = useState<
    number | null
  >(null);
  const [resolutionNote, setResolutionNote] = useState('');

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
    setSuspendDialogOpen(true);
  };

  const handleConfirmSuspend = async () => {
    if (!actionTarget) return;

    setSubmitting(true);
    setFieldErrors({});
    try {
      await suspendAdminCompany(actionTarget.companyId, {
        reasonCode: 'BILLING',
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

    const suspension = activeSuspensions.find(
      (item) => item.id === selectedSuspensionId,
    );
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
        description: getErrorMessage(
          error,
          'アカウントの有効化に失敗しました。',
        ),
        variant: 'destructive',
      });
    } finally {
      setSubmitting(false);
    }
  };

  const handleBack = () => navigate(paths.adminDashboard);

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

  const { company, masterAccount, users, sites } = detail;
  const masterFullName =
    masterAccount.fullName ||
    [masterAccount.lastName, masterAccount.firstName].filter(Boolean).join(' ');
  const masterFullNameKana =
    masterAccount.fullNameKana ||
    [masterAccount.lastNameKana, masterAccount.firstNameKana]
      .filter(Boolean)
      .join(' ');

  return (
    <main className="min-h-screen bg-background px-4 py-10 sm:px-6">
      <div className="mx-auto w-full max-w-[1200px] space-y-8">
        <header className="flex items-center gap-4">
          <PageBackButton
            label=""
            onClick={handleBack}
            aria-label="戻る"
            className="h-12 w-12 rounded-full p-0"
          />
          <div className="min-w-0">
            <h1 className="text-foreground">企業情報の詳細</h1>
            <p className="mt-1 break-words text-small text-muted-foreground">
              {company.nameJA || company.companyId}
            </p>
          </div>
        </header>

        <div className="grid grid-cols-1 gap-6 lg:grid-cols-2">
          <AdminCompanyDetailSection
            title="企業情報"
            icon={<Building2 className="h-5 w-5" />}
            fields={[
              { label: '企業名', value: company.nameJA },
              { label: '企業名（カナ）', value: company.nameFurigana },
              { label: '企業名（英語）', value: company.nameEN },
              { label: '法人番号', value: company.corpNumber },
              { label: 'メールアドレス', value: company.email },
              { label: '電話番号', value: company.phoneNumber },
              { label: 'FAX番号', value: company.faxNumber },
              {
                label: '企業ステータス',
                content: <StatusBadge status={company.companyStatus} />,
              },
              {
                label: '契約ステータス',
                value: formatSubscriptionStatus(company.subscriptionStatus),
              },
              {
                label: 'アカウント状態',
                content: (
                  <Badge variant={company.isActive ? 'default' : 'secondary'}>
                    {company.isActive ? '有効' : '無効'}
                  </Badge>
                ),
              },
            ]}
          />
          <AdminCompanyDetailSection
            title="所在地"
            icon={<MapPin className="h-5 w-5" />}
            fields={[
              { label: '郵便番号', value: company.postalCode },
              { label: '都道府県', value: company.prefecture },
              { label: '市区町村', value: company.municipality },
              { label: '番地', value: company.blocknum },
              { label: '建物名', value: company.buildingName },
              {
                label: '住所',
                value: formatAdminCompanyAddress(company),
                className: 'md:col-span-2',
              },
            ]}
          />
          <AdminCompanyDetailSection
            title="マスターアカウント"
            icon={<ShieldCheck className="h-5 w-5" />}
            className="lg:col-span-2"
            fields={[
              { label: 'ユーザ名', value: masterAccount.username },
              { label: '氏名', value: masterFullName },
              { label: '氏名（カナ）', value: masterFullNameKana },
              { label: '役職', value: masterAccount.userPosition },
              { label: 'メールアドレス', value: masterAccount.email },
              { label: '電話番号', value: masterAccount.phoneNumber },
              { label: '権限', value: formatAdminRole(masterAccount.role) },
              {
                label: 'アカウント状態',
                content: (
                  <Badge
                    variant={masterAccount.isActive ? 'default' : 'secondary'}
                  >
                    {masterAccount.isActive ? '有効' : '無効'}
                  </Badge>
                ),
              },
            ]}
          />
        </div>

        <section className="space-y-4">
          <div className="flex items-center gap-3">
            <Users className="h-5 w-5 text-primary" />
            <h2 className="text-foreground">ユーザ一覧</h2>
          </div>
          <AdminCompanyUsersTable users={users} />
        </section>

        <section className="space-y-4">
          <div className="flex items-center gap-3">
            <Warehouse className="h-5 w-5 text-primary" />
            <h2 className="text-foreground">拠点一覧</h2>
          </div>
          <AdminCompanySitesTable sites={sites} />
        </section>

        <AdminCompanyDetailSection
          title="登録・更新情報"
          icon={<Clock3 className="h-5 w-5" />}
          fields={[
            {
              label: '企業登録日時',
              value: formatAdminCompanyDateTime(company.createdAt),
            },
            {
              label: '企業更新日時',
              value: formatAdminCompanyDateTime(company.updatedAt),
            },
            { label: '企業登録者', value: company.createdBy },
            { label: '企業更新者', value: company.updatedBy },
            {
              label: 'マスター登録日時',
              value: formatAdminCompanyDateTime(masterAccount.createdAt),
            },
            {
              label: 'マスター更新日時',
              value: formatAdminCompanyDateTime(masterAccount.updatedAt),
            },
            { label: 'マスター登録者', value: masterAccount.createdBy },
            { label: 'マスター更新者', value: masterAccount.updatedBy },
          ]}
        />

        <div className="flex justify-end pb-4">
          {company.companyStatus === 'ACTIVE' && (
            <Button
              type="button"
              variant="destructive"
              size="md"
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
              size="md"
              onClick={() => void handleOpenResolveDialog()}
            >
              アカウント有効化
            </Button>
          )}
        </div>
      </div>

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
