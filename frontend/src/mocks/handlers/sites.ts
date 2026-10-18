import { http } from 'msw';
import type { SiteProjectEquipmentApiItem } from '@/api/site/types';
import {
  COMPANY,
  equipments,
  findProject,
  findSite,
  findUser,
  participations,
  projectEquipment,
  projects,
  sites,
  users,
  type MockSite,
} from '../db';
import { mockFileUrl, nextId, notFound, nowIso, ok, raw, readJson } from '../utils';

const COMPANY_SITE_IDS = () => new Set(sites.map((site) => site.siteId));

function siteListItem(site: MockSite) {
  const pic = findUser(site.picUserId) ?? users[0];
  return {
    siteId: site.siteId,
    siteName: site.siteName,
    postalCode: site.postalCode,
    prefecture: site.prefecture,
    municipality: site.municipality,
    blocknum: site.blocknum,
    buildingName: site.buildingName,
    picFirstName: pic.firstName,
    picLastName: site.picUserId ? pic.lastName : 'マスター',
    projects: site.projects,
  };
}

function siteProjectItem(projectId: string, siteId: string) {
  const project = findProject(projectId)!;
  const companySiteIds = COMPANY_SITE_IDS();
  const siteParticipation = participations.find(
    (item) => item.projectId === projectId && item.siteId === siteId,
  );
  const isCompanyJoined = participations.some(
    (item) => item.projectId === projectId && companySiteIds.has(item.siteId),
  );

  return {
    ...project,
    baselineEmission: project.baselineEmission,
    participationId: siteParticipation?.participationId ?? null,
    participationStatus: siteParticipation?.status ?? null,
    pdfStatus: siteParticipation?.pdfStatus ?? null,
    isCompanyJoined,
    hasSiteRelation: Boolean(siteParticipation),
  };
}

function equipmentById(id: number) {
  return equipments.find((item) => item.id === id);
}

type SiteBody = {
  siteName?: string;
  postalCode?: string;
  prefecture?: string;
  prefectureName?: string;
  municipality?: string;
  cityName?: string;
  blocknum?: string;
  blockName?: string;
  buildingName?: string;
  addressDetail?: string;
  userId?: string | number | null;
  picuserId?: string | number | null;
};

function applySiteBody(site: MockSite, body: SiteBody) {
  site.siteName = body.siteName ?? site.siteName;
  site.postalCode = body.postalCode ?? site.postalCode;
  site.prefecture = body.prefecture ?? body.prefectureName ?? site.prefecture;
  site.municipality = body.municipality ?? body.cityName ?? site.municipality;
  site.blocknum = body.blocknum ?? body.blockName ?? site.blocknum;
  site.buildingName = body.buildingName ?? body.addressDetail ?? site.buildingName;
  const picId = body.picuserId ?? body.userId;
  if (picId !== undefined) site.picUserId = picId === null || picId === '' ? null : String(picId);
}

export const siteHandlers = [
  // --- Project participation flow (more specific than /site/:siteId) ---
  http.get('/api/site/project/presigned-url', ({ request }) => {
    const s3Key = new URL(request.url).searchParams.get('s3Key') ?? 'evidence.pdf';
    return raw(mockFileUrl(s3Key.split('/').pop() || 'evidence.pdf'));
  }),
  http.get('/api/site/project/:projectId/my-participations', ({ params }) => {
    const companySiteIds = COMPANY_SITE_IDS();
    return raw(
      participations
        .filter((item) => item.projectId === params.projectId && companySiteIds.has(item.siteId))
        .map((item) => ({
          participationId: item.participationId,
          siteId: item.siteId,
          siteName: findSite(item.siteId)?.siteName ?? item.siteId,
          status: item.status,
          pdfStatus: item.pdfStatus,
        })),
    );
  }),
  http.get('/api/site/project/:projectId/membership-pdf', ({ params }) =>
    ok({ pdfUrl: mockFileUrl(`入会届_${params.projectId}.pdf`), expiresIn: 900 }),
  ),
  http.post('/api/site/project/sites/:siteId/files/upload', ({ request, params }) => {
    const fileType = new URL(request.url).searchParams.get('fileType') ?? 'file';
    const id = nextId();
    return ok({ s3Key: `demo/${params.siteId}/${fileType}-${id}.pdf`, fileId: `F${id}`, originalName: `${fileType}.pdf` });
  }),
  http.get('/api/site/project/sites/:siteId/sustainability-commitment/default', () =>
    ok({ companyName: COMPANY.nameJA, repName: '山田 太郎', repRole: '代表取締役', hasConsent: false, isAgreeTerms: false }),
  ),
  http.post('/api/site/project/equipment/validate', () => ok({})),
  http.post('/api/site/project/confirmation-items/validate', () => ok({})),
  http.post('/api/site/project/sustainability-commitment/validate', () => ok({})),
  http.post('/api/site/project/final/validate', () => ok({})),
  http.post('/api/site/project/projects/:projectId/join-company', async ({ request, params }) => {
    const body = await readJson<{ equipment: SiteProjectEquipmentApiItem }>(request);
    const siteId = String(body.equipment?.siteId ?? 'S003');
    const equipmentId = nextId();
    const participationId = nextId();
    equipments.push({ ...body.equipment, id: equipmentId, equipmentId, siteId, files: [] });
    participations.push({
      participationId,
      projectId: String(params.projectId),
      siteId,
      status: 'OPEN',
      pdfStatus: 'PENDING',
      submittedAt: nowIso(),
      equipmentId,
      companyName: COMPANY.nameJA,
    });
    projectEquipment[`${params.projectId}:${siteId}`] = [equipmentId];
    return ok({ message: '参加申請を受け付けました（デモ）', participationId, equipmentId, siteProjectRelationId: nextId() });
  }),
  http.post('/api/site/project/projects/:projectId/:siteId/join-site', ({ params }) => {
    const participationId = nextId();
    participations.push({
      participationId,
      projectId: String(params.projectId),
      siteId: String(params.siteId),
      status: 'OPEN',
      pdfStatus: 'PENDING',
      submittedAt: nowIso(),
      equipmentId: 0,
      companyName: COMPANY.nameJA,
    });
    return ok({ participationId, siteProjectRelationId: nextId() });
  }),

  // --- Sites ---
  http.get('/api/site', () => ok({ siteList: sites.map(siteListItem) })),
  http.post('/api/site/validate-create', () => ok({})),
  http.post('/api/site/validate-update/:siteId', () => ok({})),
  http.post('/api/site', async ({ request }) => {
    const body = await readJson<SiteBody>(request);
    const site: MockSite = {
      siteId: `S${String(nextId()).padStart(3, '0')}`,
      siteName: '',
      postalCode: '',
      prefecture: '',
      municipality: '',
      blocknum: '',
      buildingName: '',
      picUserId: null,
      projects: [],
      createdAt: nowIso(),
      metrics: { sumEmissionReduction: '0', sumEmission: '0', jcredit: '0' },
    };
    applySiteBody(site, body);
    sites.push(site);
    return ok({ siteId: site.siteId });
  }),
  http.get('/api/site/:siteId', ({ params }) => {
    const site = findSite(String(params.siteId));
    if (!site) return notFound();
    return ok({ site: { ...siteListItem(site), data: { ...site.metrics, siteId: site.siteId, siteName: site.siteName } } });
  }),
  http.patch('/api/site/:siteId', async ({ request, params }) => {
    const site = findSite(String(params.siteId));
    if (!site) return notFound();
    applySiteBody(site, await readJson<SiteBody>(request));
    return ok({});
  }),
  http.delete('/api/site/:siteId', ({ params }) => {
    const index = sites.findIndex((site) => site.siteId === params.siteId);
    if (index >= 0) sites.splice(index, 1);
    return ok({});
  }),
  http.get('/api/site/:siteId/project', ({ params }) =>
    ok({
      projects: projects.map((project) => siteProjectItem(project.id, String(params.siteId))),
      pagination: { page: 1, limit: 50, totalItems: projects.length, totalPages: 1, nextPageToken: null, hasMore: false },
    }),
  ),

  // --- Equipment ---
  http.get('/api/site/:siteId/equipment', ({ params }) =>
    ok({ equipments: equipments.filter((item) => item.siteId === params.siteId) }),
  ),
  http.get('/api/site/:siteId/equipment/:equipmentId/history', ({ params }) => {
    const equipment = equipmentById(Number(params.equipmentId));
    if (!equipment) return raw([]);
    return raw([
      {
        id: nextId(),
        changeDate: equipment.equipmentOpDate,
        previousEquipmentId: null,
        monitoringStartDate: equipment.equipmentOpDate,
        monitoringEndDate: null,
        isActive: true,
        registeredBy: '佐藤 花子',
        registeredAt: '2025-04-15T09:00:00Z',
        equipment,
      },
    ]);
  }),
  http.post('/api/site/:siteId/equipment/:equipmentId/change', async ({ request, params }) => {
    const previous = equipmentById(Number(params.equipmentId));
    const body = await readJson<SiteProjectEquipmentApiItem>(request);
    const id = nextId();
    const updated: SiteProjectEquipmentApiItem = {
      ...previous,
      ...body,
      id,
      equipmentId: id,
      siteId: String(params.siteId),
      previousEquipmentId: previous?.id ?? null,
    };
    equipments.push(updated);
    return ok(updated);
  }),
  http.get('/api/site/:siteId/equipment/:equipmentId', ({ params }) => {
    const equipment = equipmentById(Number(params.equipmentId));
    return equipment ? ok({ ...equipment, siteId: String(params.siteId) }) : notFound();
  }),
  http.get('/api/site/:siteId/:projectId/equipment', ({ params }) => {
    const ids = projectEquipment[`${params.projectId}:${params.siteId}`] ?? [];
    return ok({ equipments: ids.map(equipmentById).filter(Boolean) });
  }),
  http.post('/api/site/:siteId/:projectId/equipment', async ({ request, params }) => {
    const body = await readJson<SiteProjectEquipmentApiItem>(request);
    const id = nextId();
    const created = { ...body, id, equipmentId: id, siteId: String(params.siteId) };
    equipments.push(created);
    const key = `${params.projectId}:${params.siteId}`;
    projectEquipment[key] = [...(projectEquipment[key] ?? []), id];
    return ok(created);
  }),

  // --- Company users ---
  http.get('/api/user', () =>
    ok({
      users: users.map((user) => ({
        ...user,
        siteName: findSite(user.siteId)?.siteName,
      })),
    }),
  ),
  http.post('/api/user/validate-update', () => ok({})),
  http.post('/api/user/invitations', () => ok({ message: '招待メールを送信しました（デモ）' })),
  http.patch('/api/user', async ({ request }) => {
    const body = await readJson<{
      userId: number;
      firstName: string;
      firstNameKana: string;
      lastName: string;
      lastNameKana: string;
      email: string;
      phoneNumber: string;
      userPosition: string;
      siteId: string | null;
    }>(request);
    const user = findUser(String(body.userId));
    if (!user) return notFound();
    Object.assign(user, {
      firstName: body.firstName ?? user.firstName,
      firstNameKana: body.firstNameKana ?? user.firstNameKana,
      lastName: body.lastName ?? user.lastName,
      lastNameKana: body.lastNameKana ?? user.lastNameKana,
      email: body.email ?? user.email,
      phoneNumber: body.phoneNumber ?? user.phoneNumber,
      position: body.userPosition ?? user.position,
      siteId: body.siteId === undefined ? user.siteId : body.siteId,
    });
    return ok({ user: { ...user, siteName: findSite(user.siteId)?.siteName } });
  }),
  http.delete('/api/user/:userId', ({ params }) => {
    const index = users.findIndex((user) => user.id === params.userId);
    if (index >= 0) users.splice(index, 1);
    return ok({});
  }),
];
