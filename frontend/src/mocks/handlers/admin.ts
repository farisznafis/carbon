import { http } from 'msw';
import type { AdminOperatorApiItem, CreateAdminOperatorPayload } from '@/api/admin/operators/types';
import type { SuspendCompanyPayload } from '@/api/admin/types';
import { participationReviewChecklistFields } from '@/api/admin/participationReviewTypes';
import {
  COMPANY,
  batchHistory,
  companies,
  equipments,
  findProject,
  operators,
  participations,
  siteName,
  sites,
  suspensions,
  users,
  type MockParticipation,
} from '../db';
import { nextId, notFound, nowIso, numberParam, ok, paginate, raw, readJson } from '../utils';

const reviewers: Record<number, { primary: string; secondary: string }> = {
  501: { primary: '中村 翔', secondary: '管理 次郎' },
  502: { primary: '中村 翔', secondary: '管理 次郎' },
  505: { primary: '中村 翔', secondary: '' },
};

function reviewDetail(item: MockParticipation) {
  const equipment = equipments.find((entry) => entry.id === item.equipmentId) ?? equipments[0];
  const decided = item.status !== 'OPEN';
  const checklist = Object.fromEntries(participationReviewChecklistFields.map((field) => [field, decided]));
  const reviewer = reviewers[item.participationId] ?? { primary: '', secondary: '' };

  return {
    participationId: item.participationId,
    siteId: Number(item.siteId.replace(/\D/g, '')),
    projectId: Number(item.projectId.replace(/\D/g, '')),
    projectName: findProject(item.projectId)?.projectName ?? '',
    siteName: siteName(item.siteId),
    companyName: item.companyName,
    submittedAt: item.submittedAt,
    equipment,
    confirmationItems: {
      isAgreeDataUse: true,
      isAgreeExtraData: true,
      isAgreeTransfer: true,
      isNotRegistered: true,
      isKeidanrenParticipation: false,
      isEmitter: false,
      emitterCode: '',
      isAdjustedEmissionsAdded: false,
      isSubjectToPeriodicReport: false,
      isPeriodicReportTypeOne: false,
      energyBusinessCode: '',
      factoryCode: '',
    },
    sustainabilityCommitment: {
      companyName: item.companyName,
      repName: '山田 太郎',
      repRole: '代表取締役',
      hasConsent: true,
      isAgreeTerms: true,
    },
    review: {
      ...checklist,
      primaryApplReviewerName: reviewer.primary,
      secondaryApplReviewerName: reviewer.secondary,
    },
    pdfStatus: item.pdfStatus,
  };
}

function companyDetail(companyId: string) {
  const company = companies.find((item) => item.companyId === companyId);
  if (!company) return null;
  const isDemoCompany = companyId === COMPANY.companyId;
  const master = users[0];

  return {
    timestamp: nowIso(),
    masterAccount: {
      username: isDemoCompany ? master.username : `master_${companyId.toLowerCase()}`,
      fullName: isDemoCompany ? `${master.lastName} ${master.firstName}` : `${company.representativeLastName} ${company.representativeFirstName}`,
      fullNameKana: isDemoCompany ? `${master.lastNameKana} ${master.firstNameKana}` : 'タナカ ダイヒョウ',
      firstName: isDemoCompany ? master.firstName : company.representativeFirstName,
      lastName: isDemoCompany ? master.lastName : company.representativeLastName,
      firstNameKana: isDemoCompany ? master.firstNameKana : company.representativeFirstNameKana,
      lastNameKana: isDemoCompany ? master.lastNameKana : company.representativeLastNameKana,
      phoneNumber: company.representativePhoneNumber,
      email: isDemoCompany ? master.email : company.email,
      userPosition: company.representativePosition,
      role: 'master',
      isActive: company.isActive,
      createdAt: company.createdAt,
      updatedAt: company.updatedAt ?? company.createdAt,
      createdBy: 'system',
      updatedBy: 'system',
    },
    company: {
      companyId: company.companyId,
      corpNumber: company.corpNumber,
      nameJA: company.nameJA,
      nameFurigana: company.nameFurigana,
      nameEN: company.nameEN,
      email: company.email,
      phoneNumber: company.phoneNumber,
      faxNumber: company.fax ?? '',
      address: company.address,
      postalCode: company.postalCode,
      prefecture: company.prefecture,
      municipality: company.municipality,
      blocknum: company.blocknum,
      buildingName: company.buildingName ?? '',
      companyStatus: company.status,
      subscriptionStatus: company.status === 'ACTIVE' ? 'ACTIVE' : 'INACTIVE',
      isActive: company.isActive,
      createdAt: company.createdAt,
      updatedAt: company.updatedAt ?? company.createdAt,
      createdBy: 'system',
      updatedBy: 'system',
    },
    users: isDemoCompany
      ? users.map((user) => ({
          username: user.username,
          fullName: `${user.lastName} ${user.firstName}`,
          email: user.email,
          role: user.role,
          siteName: user.siteId ? siteName(user.siteId) : '-',
        }))
      : [],
    sites: isDemoCompany
      ? sites.map((site) => ({
          siteName: site.siteName,
          picEmail: users.find((user) => user.id === site.picUserId)?.email ?? master.email,
          createdAt: site.createdAt,
          address: `${site.prefecture}${site.municipality}${site.blocknum}${site.buildingName}`,
          postalCode: site.postalCode,
          prefecture: site.prefecture,
          municipality: site.municipality,
          blocknum: site.blocknum,
          buildingName: site.buildingName,
          projectNames: site.projects,
        }))
      : [],
  };
}

function setCompanyStatus(companyId: string, status: (typeof companies)[number]['status']) {
  const company = companies.find((item) => item.companyId === companyId);
  if (company) {
    company.status = status;
    company.isActive = status === 'ACTIVE';
    company.updatedAt = nowIso();
  }
  return company;
}

export const adminHandlers = [
  // --- Operators ---
  http.get('/api/admin/companies/operator-user', () => ok({ users: operators })),
  http.post('/api/admin/companies/operator-user', async ({ request }) => {
    const body = await readJson<CreateAdminOperatorPayload>(request);
    const created: AdminOperatorApiItem = {
      ...body,
      id: String(nextId()),
      role: 'operator',
      companyId: '9001',
      permissions: body.permissionCodes ?? [],
    };
    operators.push(created);
    return ok({ user: created });
  }),
  http.get('/api/admin/companies/operator-user/:userId', ({ params }) => {
    const operator = operators.find((item) => String(item.id) === params.userId);
    return operator ? ok({ profile: operator }) : notFound('運用者が見つかりません（デモ）');
  }),
  http.patch('/api/admin/companies/operator-user/:userId', async ({ request, params }) => {
    const operator = operators.find((item) => String(item.id) === params.userId);
    if (!operator) return notFound();
    const body = await readJson<CreateAdminOperatorPayload>(request);
    const { permissionCodes, ...profile } = body;
    Object.assign(operator, profile, permissionCodes ? { permissions: permissionCodes } : {});
    return ok({ user: operator });
  }),

  // --- Membership reviews ---
  http.get('/api/admin/companies/participate/open', () =>
    raw(participations.filter((item) => item.status === 'OPEN').map(reviewDetail)),
  ),
  http.get('/api/admin/companies/participations/approved', () =>
    raw(participations.filter((item) => item.status === 'APPROVED').map(reviewDetail)),
  ),
  http.get('/api/admin/companies/participations/rejected', () =>
    raw(participations.filter((item) => item.status === 'REJECTED').map(reviewDetail)),
  ),
  http.get('/api/admin/companies/participate/:participationId', ({ params }) => {
    const item = participations.find((entry) => entry.participationId === Number(params.participationId));
    return item ? raw(reviewDetail(item)) : notFound('入会届が見つかりません（デモ）');
  }),
  http.post('/api/admin/companies/participation/:participationId/approve', ({ params }) => {
    const item = participations.find((entry) => entry.participationId === Number(params.participationId));
    if (item) {
      item.status = 'APPROVED';
      item.pdfStatus = 'COMPLETED';
    }
    return ok({});
  }),
  http.post('/api/admin/companies/participation/:participationId/reject', ({ params }) => {
    const item = participations.find((entry) => entry.participationId === Number(params.participationId));
    if (item) {
      item.status = 'REJECTED';
      item.pdfStatus = 'REJECTED';
    }
    return ok({});
  }),

  // --- Companies ---
  http.get('/api/admin/companies', ({ request }) => {
    const url = new URL(request.url);
    const statuses = url.searchParams.get('status')?.split(',').filter(Boolean);
    const search = url.searchParams.get('search')?.toLowerCase();
    const prefecture = url.searchParams.get('prefecture');
    const sortOrder = url.searchParams.get('sortOrder') === 'asc' ? 1 : -1;
    const filtered = companies
      .filter((company) => !statuses?.length || statuses.includes(company.status))
      .filter((company) => !prefecture || company.prefecture === prefecture)
      .filter(
        (company) =>
          !search ||
          [company.nameJA, company.nameEN, company.email, company.companyId].some((value) =>
            value.toLowerCase().includes(search),
          ),
      )
      .sort((left, right) => left.createdAt.localeCompare(right.createdAt) * sortOrder);
    const { items, pagination } = paginate(filtered, numberParam(url, 'page', 1), numberParam(url, 'limit', 20));
    return ok({ companies: items, pagination });
  }),
  http.get('/api/admin/companies/:companyId', ({ params }) => {
    const detail = companyDetail(String(params.companyId));
    return detail ? ok(detail) : notFound('企業が見つかりません（デモ）');
  }),
  http.post('/api/admin/companies/:companyId/password-reset-link', () =>
    ok({ message: 'パスワードリセットリンクを送信しました（デモ）', timestamp: nowIso() }),
  ),
  http.post('/api/admin/companies/:companyId/approve', ({ params }) => {
    setCompanyStatus(String(params.companyId), 'ACTIVE');
    return ok({ companyStatus: 'ACTIVE' });
  }),
  http.post('/api/admin/companies/:companyId/reject', ({ params }) => {
    setCompanyStatus(String(params.companyId), 'REJECTED');
    return ok({ companyStatus: 'REJECTED' });
  }),
  http.get('/api/admin/companies/:companyId/suspensions', ({ request, params }) => {
    const url = new URL(request.url);
    const activeOnly = url.searchParams.get('activeOnly') === 'true';
    const list = (suspensions[String(params.companyId)] ?? []).filter((item) => !activeOnly || item.active);
    const { items, pagination } = paginate(list, numberParam(url, 'page', 1), numberParam(url, 'limit', 20));
    return ok({ suspensions: items, pagination });
  }),
  http.post('/api/admin/companies/:companyId/suspensions', async ({ request, params }) => {
    const companyId = String(params.companyId);
    const body = await readJson<SuspendCompanyPayload>(request);
    (suspensions[companyId] ??= []).unshift({
      id: nextId(),
      reasonCode: body.reasonCode ?? 'OTHER',
      reasonDetail: body.reasonDetail ?? '',
      createdAt: nowIso(),
      createdBy: '管理 次郎',
      resolvedAt: null,
      resolvedBy: null,
      resolutionNote: null,
      active: true,
    });
    setCompanyStatus(companyId, 'SUSPENDED');
    return ok({ companyStatus: 'SUSPENDED', activeSuspensionCount: suspensions[companyId].filter((item) => item.active).length });
  }),
  http.post('/api/admin/companies/:companyId/suspensions/:suspensionId/resolve', async ({ request, params }) => {
    const companyId = String(params.companyId);
    const body = await readJson<{ resolutionNote: string }>(request);
    const target = (suspensions[companyId] ?? []).find((item) => item.id === Number(params.suspensionId));
    if (target) {
      target.active = false;
      target.resolvedAt = nowIso();
      target.resolvedBy = '管理 次郎';
      target.resolutionNote = body.resolutionNote ?? '';
    }
    const activeSuspensionCount = (suspensions[companyId] ?? []).filter((item) => item.active).length;
    if (activeSuspensionCount === 0) setCompanyStatus(companyId, 'ACTIVE');
    return ok({ companyStatus: activeSuspensionCount ? 'SUSPENDED' : 'ACTIVE', activeSuspensionCount });
  }),

  // --- Batch ---
  http.get('/api/batch/history', ({ request }) => {
    const url = new URL(request.url);
    return ok(paginate(batchHistory, numberParam(url, 'page', 1), numberParam(url, 'limit', 10)));
  }),
];
