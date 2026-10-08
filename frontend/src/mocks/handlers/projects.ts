import { http } from 'msw';
import type { ProjectFileType } from '@/api/admin/projectFiles';
import {
  COMPANY,
  companies,
  findProject,
  monitoringRecords,
  participations,
  planForms,
  projectFiles,
  projects,
  sites,
  type MockProject,
} from '../db';
import { mockFileUrl, nextId, notFound, nowIso, ok, readJson } from '../utils';

const PROJECT_FILE_LIMIT = 10;

function projectListItem(project: MockProject) {
  return {
    id: project.id,
    companyId: COMPANY.companyId,
    operatorCompanyId: project.operatorCompanyId,
    companyName: project.companyName,
    siteName: sites.find((site) => site.projects.includes(project.projectName))?.siteName ?? null,
    projectName: project.projectName,
    projectStatus: project.projectStatus,
    emissionReduction: project.emissionReduction,
    updatedAt: project.updatedAt,
    createdAt: project.createdAt,
    methodologyCategory: project.methodologyCategory,
    methodologyNumber: project.methodologyNumber,
    methodologyName: project.methodologyName,
    methodologyMainEmission: project.methodologyMainEmission,
    methodologyVersion: project.methodologyVersion,
    methodCombiFeasibility: project.methodCombiFeasibility,
  };
}

function projectDetail(project: MockProject) {
  return {
    ...project,
    siteId: null,
    baselineEmissions: project.baselineEmission,
    postProjectEmissions: project.postProjectEmission,
    isCompanyJoined: participations.some((item) => item.projectId === project.id),
    hasSiteRelation: participations.some((item) => item.projectId === project.id),
  };
}

/** Yearly emission history derived from the project's start year. */
function emissionChart(project: MockProject) {
  if (!project.emissionReduction) return [];
  return [2025, 2026].map((year, index) => {
    const baseline = Math.round((project.baselineEmission / 2) * (index ? 1.05 : 1) * 10) / 10;
    const actual = Math.round((project.postProjectEmission / 2) * (index ? 0.97 : 1) * 10) / 10;
    return { year: String(year), baseline, actualEmission: actual, reduction: Math.round((baseline - actual) * 10) / 10 };
  });
}

function versionSummary(projectId: string) {
  const counts = new Map<string, number>();
  (projectFiles[projectId] ?? []).forEach((file) => {
    const type = String(file.fileType);
    counts.set(type, (counts.get(type) ?? 0) + 1);
  });
  return [...counts.entries()].map(([fileType, activeCount]) => ({
    fileType,
    activeCount,
    limit: PROJECT_FILE_LIMIT,
    canUpload: activeCount < PROJECT_FILE_LIMIT,
  }));
}

const DOCUMENT_DATA: Partial<Record<ProjectFileType, (project: MockProject) => unknown[]>> = {
  PROJECT_PLAN: (project) => [
    {
      key: 'overview',
      title: '1. プロジェクト概要',
      items: [
        { key: 'projectName', label: 'プロジェクト名', value: project.projectName, unit: null, type: 'TEXT' },
        { key: 'methodology', label: '方法論', value: `${project.methodologyNumber} ${project.methodologyName}`, unit: null, type: 'TEXT' },
        { key: 'start', label: '認証対象期間（開始）', value: '2025-04-01', unit: null, type: 'DATE' },
        { key: 'end', label: '認証対象期間（終了）', value: '2033-03-31', unit: null, type: 'DATE' },
      ],
      table: null,
    },
    {
      key: 'plan',
      title: '4. 排出削減計画',
      items: [
        { key: 'baseline', label: 'ベースライン排出量', value: String(project.baselineEmission), unit: 't-CO2', type: 'NUMBER' },
        { key: 'post', label: 'プロジェクト実施後排出量', value: String(project.postProjectEmission), unit: 't-CO2', type: 'NUMBER' },
      ],
      table: {
        columns: [
          { key: 'year', label: '年度', unit: null, type: 'TEXT' },
          { key: 'newMembers', label: '新規会員数', unit: '件', type: 'NUMBER' },
          { key: 'cumulative', label: '累計会員数', unit: '件', type: 'NUMBER' },
          { key: 'reduction', label: '削減見込量', unit: 't-CO2', type: 'NUMBER' },
        ],
        rows: [2025, 2026, 2027, 2028].map((year, index) => [
          `${year}年度`,
          String(index < 2 ? 4 : 2),
          String(Math.min(4 * (index + 1), 12)),
          String(Math.round(project.emissionReduction / 2 + index * 15)),
        ]),
      },
    },
  ],
  MONITORING_REPORT: (project) => [
    {
      key: 'result',
      title: 'モニタリング結果',
      items: [
        { key: 'period', label: 'モニタリング期間', value: '2025-04-01 〜 2026-03-31', unit: null, type: 'TEXT' },
        { key: 'reduction', label: '排出削減量', value: String(project.emissionReduction), unit: 't-CO2', type: 'NUMBER' },
      ],
      table: {
        columns: [
          { key: 'site', label: '会員拠点', unit: null, type: 'TEXT' },
          { key: 'generation', label: '発電量', unit: 'kWh', type: 'NUMBER' },
          { key: 'reduction', label: '削減量', unit: 't-CO2', type: 'NUMBER' },
        ],
        rows: [
          ['東京本社', '63,412', '28.4'],
          ['大阪工場', '141,906', '63.6'],
          ['仙台事業所', '52,118', '23.3'],
        ],
      },
    },
  ],
  ACTIVITY_RESULT_LIST: () => [
    {
      key: 'activities',
      title: '個別活動実績',
      items: [],
      table: {
        columns: [
          { key: 'no', label: 'No.', unit: null, type: 'TEXT' },
          { key: 'company', label: '会員名', unit: null, type: 'TEXT' },
          { key: 'start', label: '稼働開始日', unit: null, type: 'DATE' },
          { key: 'output', label: '定格出力', unit: 'kW', type: 'NUMBER' },
        ],
        rows: [
          ['1', COMPANY.nameJA, '2025-04-15', '49.5'],
          ['2', COMPANY.nameJA, '2025-06-01', '120.0'],
          ['3', '東日本ソーラー株式会社', '2025-08-20', '35.0'],
        ],
      },
    },
  ],
};

export const projectHandlers = [
  http.get('/api/project', () =>
    ok({
      errors: null,
      projects: projects.map(projectListItem),
      pagination: { page: 1, limit: 50, totalItems: projects.length, totalPages: 1, nextPageToken: null, hasMore: false },
    }),
  ),
  http.post('/api/project', async ({ request }) => {
    const body = await readJson<{ project: Array<Record<string, unknown>> }>(request);
    const input = body.project?.[0] ?? {};
    const created: MockProject = {
      ...projects[0],
      id: `P${String(nextId()).padStart(3, '0')}`,
      projectName: String(input.projectName ?? '新規プロジェクト'),
      projectNumber: String(input.projectNumber ?? `JCL-2026-${nextId()}`),
      projectStatus: String(input.projectStatus ?? '作成済み'),
      methodologyNumber: String(input.methodNumber ?? projects[0].methodologyNumber),
      emissionReduction: 0,
      baselineEmission: Number(input.baselineEmissions ?? 0),
      postProjectEmission: Number(input.postProjectEmission ?? 0),
      newMembersTotal: 0,
      withdrawnMembers: 0,
      cumulativeMembers: 0,
      membersTotal: 0,
      createdAt: nowIso(),
      updatedAt: nowIso(),
    };
    projects.push(created);
    return ok({ errors: null, project: [projectDetail(created)] });
  }),
  http.get('/api/project/:projectId', ({ params }) => {
    const project = findProject(String(params.projectId));
    return project ? ok({ errors: null, project: projectDetail(project) }) : notFound('プロジェクトが見つかりません（デモ）');
  }),
  http.patch('/api/project/:projectId', async ({ request, params }) => {
    const project = findProject(String(params.projectId));
    if (!project) return notFound();
    const body = await readJson<Record<string, unknown>>(request);
    Object.assign(project, {
      ...body,
      baselineEmission: Number(body.baselineEmissions ?? project.baselineEmission),
      methodologyNumber: body.methodNumber ?? project.methodologyNumber,
      methodologyName: body.methodName ?? project.methodologyName,
      updatedAt: nowIso(),
    });
    return ok({});
  }),
  http.patch('/api/project/:projectId/status', async ({ request, params }) => {
    const project = findProject(String(params.projectId));
    const body = await readJson<{ projectStatus: string; processedBy: string; companyId: string }>(request);
    if (project && body.projectStatus) project.projectStatus = body.projectStatus;
    return ok({ id: String(params.projectId), ...body });
  }),
  http.get('/api/project/:projectId/member-terms-info', ({ params }) => {
    const project = findProject(String(params.projectId)) ?? projects[0];
    return ok({
      projectName: project.projectName,
      operatorNameJa: project.companyName,
      adminRepName: `${project.repLastName} ${project.repFirstName}`,
      projectOverview: project.methodDetails,
      usePurpose: project.usePurpose,
      startedAt: project.startedAt,
    });
  }),
  http.get('/api/project/:projectId/monitoring', ({ params }) => {
    const project = findProject(String(params.projectId));
    return ok({ detail: { chart: { chartData: project ? emissionChart(project) : [] } } });
  }),
  http.get('/api/project/:projectId/companies', ({ params }) => {
    const byCompany = new Map<string, (typeof participations)[number]>();
    participations
      .filter((item) => item.projectId === params.projectId && item.status === 'APPROVED')
      .forEach((item) => byCompany.set(item.companyName, item));
    return ok({
      companies: [...byCompany.values()].map((item) => ({
        companyId: companies.find((company) => company.nameJA === item.companyName)?.companyId ?? '',
        companyName: item.companyName,
        participationId: item.participationId,
        pdfStatus: item.pdfStatus,
        pdfUrl: mockFileUrl(`入会届_${item.participationId}.pdf`),
      })),
    });
  }),
  http.get('/api/project/:projectId/participations/:participationId/membership-pdf', ({ params }) =>
    ok({ pdfUrl: mockFileUrl(`入会届_${params.participationId}.pdf`), expiresIn: 900 }),
  ),

  // --- Project files (admin) ---
  http.get('/api/project/:projectId/files', ({ request, params }) => {
    const projectId = String(params.projectId);
    const fileType = new URL(request.url).searchParams.get('fileType');
    const files = (projectFiles[projectId] ?? []).filter((file) => !fileType || file.fileType === fileType);
    return ok({ files, versionSummary: versionSummary(projectId) });
  }),
  http.post('/api/project/:projectId/files', async ({ request, params }) => {
    const projectId = String(params.projectId);
    const fileType = new URL(request.url).searchParams.get('fileType') ?? 'ADDITIONAL_MATERIAL';
    const form = await request.formData().catch(() => null);
    const file = form?.get('file');
    const list = (projectFiles[projectId] ??= []);
    const created = {
      id: nextId(),
      fileType,
      ownedByKanrisha: true,
      originalFileName: file instanceof File ? file.name : 'uploaded.pdf',
      uploadedBy: '管理 次郎',
      uploadedAt: nowIso(),
      versionNo: list.filter((item) => item.fileType === fileType).length + 1,
    };
    list.unshift(created);
    return ok(created);
  }),
  http.get('/api/project/:projectId/files/:fileId/download', ({ params }) => {
    const file = (projectFiles[String(params.projectId)] ?? []).find((item) => item.id === Number(params.fileId));
    return ok({ downloadUrl: mockFileUrl(file?.originalFileName ?? 'document.pdf') });
  }),
  http.delete('/api/project/:projectId/files/:fileId', ({ params }) => {
    const list = projectFiles[String(params.projectId)] ?? [];
    const index = list.findIndex((item) => item.id === Number(params.fileId));
    if (index >= 0) list.splice(index, 1);
    return ok({});
  }),
  http.get('/api/project/:projectId/document-data/:fileType', ({ params }) => {
    const project = findProject(String(params.projectId)) ?? projects[0];
    const fileType = String(params.fileType) as ProjectFileType;
    const hasFile = (projectFiles[project.id] ?? []).some((file) => file.fileType === fileType);
    const sections = hasFile ? (DOCUMENT_DATA[fileType]?.(project) ?? []) : [];
    return ok({ projectId: project.id, fileType, sections });
  }),

  // --- Project plan pages ---
  http.get('/api/projectplan/:projectId/forms/:formSlug', ({ params }) => {
    const data = planForms[String(params.projectId)];
    return ok({
      formSlug: String(params.formSlug),
      displayName: '4.排出削減計画',
      status: data ? 'COMPLETE' : 'NOT_STARTED',
      updatedAt: data ? '2026-05-10T04:00:00Z' : null,
      updatedBy: data ? '伊藤 美咲' : null,
      readOnlyFields: [],
      conditionalFields: [],
      data: data ?? {},
    });
  }),
  http.put('/api/projectplan/:projectId/forms/:formSlug', async ({ request, params }) => {
    const data = await readJson<Record<string, unknown>>(request);
    planForms[String(params.projectId)] = data;
    return ok({ formSlug: String(params.formSlug), status: 'COMPLETE', updatedAt: nowIso(), data });
  }),

  // --- Batch / emission results ---
  http.get('/api/batch/:siteId/:projectId/reduction-result', ({ params }) => {
    const records = monitoringRecords.filter(
      (record) => record.siteId === params.siteId && record.projectId === params.projectId,
    );
    return ok({
      data: records
        .map((record) => {
          const empj = Math.round(record.elPvr * 0.000441 * 1000) / 1000;
          return {
            processing_date: record.dateRecorded,
            embl: Math.round(record.elPv * 0.000441 * 1000) / 1000,
            empj,
            empj_m: Math.round(empj * 0.8 * 1000) / 1000,
            empj_s_control: Math.round(empj * 0.15 * 1000) / 1000,
            empj_s_battery: record.elpjBatteryValue ? Math.round(empj * 0.05 * 1000) / 1000 : 0,
          };
        })
        .reverse(),
    });
  }),
  http.get('/api/batch/:projectId/reduction-result-total', ({ params }) => {
    const project = findProject(String(params.projectId));
    if (!project || !project.emissionReduction) return ok({ data: null });
    const empj = project.postProjectEmission;
    return ok({
      data: {
        processing_date: '2026-09-30',
        embl: project.baselineEmission,
        empj,
        empj_m: Math.round(empj * 0.82 * 10) / 10,
        empj_s_control: Math.round(empj * 0.12 * 10) / 10,
        empj_s_battery: Math.round(empj * 0.06 * 10) / 10,
      },
    });
  }),
];
