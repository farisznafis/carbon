import { http } from 'msw';
import type { MonitoringData } from '@/api/monitoring/types';
import { cancelRequests, monitoringRecords, notifications, siteName, type MockMonitoringRecord } from '../db';
import { mockFileUrl, nextId, nowIso, numberParam, ok, paginate, readJson, readMockSession } from '../utils';

const newestFirst = (left: MockMonitoringRecord, right: MockMonitoringRecord) =>
  right.dateRecorded.localeCompare(left.dateRecorded);

function recentItem(record: MockMonitoringRecord) {
  return {
    projectId: record.projectId,
    siteId: record.siteId,
    equipmentId: record.equipmentId,
    userId: record.participatingUserId,
    projectType: 'solar',
    dateRecorded: record.dateRecorded,
    timeUpdated: record.timeUpdated,
    changedBattery: record.changedBattery,
    ELpv_value: record.elPv,
    ELpvr_value: record.elPvr,
    ELPJ_battery_value: record.elpjBatteryValue,
    ELPJ_control_value: record.elpjControlValue,
    ELPJ_accuracy: record.elpjAccuracy,
    elpv_evidence: 'demo/elpv.pdf',
    elpvr_evidence: 'demo/elpvr.pdf',
    elpj_battery_evidence: record.elpjBatteryValue ? 'demo/battery.pdf' : null,
    elpj_control_evidence: 'demo/control.pdf',
  };
}

function listItem(record: MockMonitoringRecord) {
  return {
    ...record,
    elpjControlEquipmentSpecFiles: 'demo/control.pdf',
    elpjBatteryEvidence: record.elpjBatteryValue ? 'demo/battery.pdf' : null,
    elPvEvidence: 'demo/elpv.pdf',
    elPvrEvidence: 'demo/elpvr.pdf',
    elPvEvidenceUrl: mockFileUrl('発電量エビデンス.pdf'),
    elPvrEvidenceUrl: mockFileUrl('逆潮流量エビデンス.pdf'),
    elpjBatterySpecificationUrl: record.elpjBatteryValue ? mockFileUrl('蓄電池仕様書.pdf') : null,
    elpjControlEvidenceUrl: mockFileUrl('制御機器エビデンス.pdf'),
    processedAt: record.timeUpdated,
    methodologyType: record.elpjBatteryValue ? 'EN-S-019' : 'EN-R-002',
  };
}

function pagedRecords(request: Request, records: MockMonitoringRecord[]) {
  const url = new URL(request.url);
  const { items, pagination } = paginate(
    [...records].sort(newestFirst),
    numberParam(url, 'page', 1),
    numberParam(url, 'limit', 10),
  );
  return ok({ items: items.map(listItem), pagination });
}

const UPLOAD_KINDS = [
  'upload-elpv-evidence',
  'upload-elpvr-evidence',
  'upload-elpj-battery-specification-evidence',
  'upload-elpj-control-evidence',
];

export const monitoringHandlers = [
  ...UPLOAD_KINDS.map((kind) =>
    http.post(`/api/monitoring/${kind}`, () => {
      const id = nextId();
      return ok({ file: { id: String(id), fileType: kind, fileName: `${kind}.pdf`, s3Key: `demo/${kind}-${id}.pdf`, url: mockFileUrl(`${kind}.pdf`) } });
    }),
  ),
  http.get('/api/monitoring', ({ request }) => {
    const url = new URL(request.url);
    const projectId = url.searchParams.get('projectId');
    const siteId = url.searchParams.get('siteId');
    const limit = numberParam(url, 'limit', 12);
    const records = monitoringRecords
      .filter((record) => record.isActive && record.projectId === projectId && (!siteId || record.siteId === siteId))
      .sort(newestFirst)
      .slice(0, limit);
    return ok({ data: records.map(recentItem) });
  }),
  http.post('/api/monitoring/validate', () => ok({})),
  http.post('/api/monitoring', async ({ request }) => {
    const body = await readJson<MonitoringData>(request);
    const id = `M-${nextId()}`;
    monitoringRecords.push({
      id,
      projectId: String(body.projectId),
      siteId: String(body.siteId ?? ''),
      equipmentId: Number(body.equipmentId ?? 0),
      participatingUserId: Number(readMockSession(request)?.userId ?? 0),
      dateRecorded: body.dateRecorded ?? nowIso().slice(0, 10),
      timeUpdated: nowIso(),
      elPv: Number(body.ELpv_value ?? 0),
      elPvr: Number(body.ELpvr_value ?? 0),
      elpjBatteryValue: body.ELPJ_battery_value ?? null,
      elpjControlValue: Number(body.ELPJ_control_value ?? 0),
      elpjAccuracy: Number(body.ELPJ_accuracy ?? 0),
      isActive: true,
      changedBattery: false,
    });
    return ok({ success: true, message: 'モニタリングデータを登録しました（デモ）', dataId: id });
  }),
  http.get('/api/monitoring/cancel-requests', () => ok({ items: cancelRequests })),
  http.post('/api/monitoring/cancel-request', async ({ request }) => {
    const body = await readJson<{ siteId: string; recordPK: string; recordSK: string; reasonText: string }>(request);
    const created = {
      id: nextId(),
      requestedAt: nowIso(),
      reason: body.reasonText ?? '',
      recordPk: body.recordPK ?? '',
      status: 'PENDING',
      requestedBy: '佐藤 花子',
      siteName: siteName(String(body.siteId ?? '')),
      decidedAt: null,
      decidedBy: null,
    };
    cancelRequests.unshift(created);
    return ok({ cancelRequestId: created.id, recordPk: created.recordPk, recordSk: body.recordSK, status: created.status, requestedAt: created.requestedAt });
  }),
  http.post('/api/monitoring/:cancelRequestId/cancel-decision', async ({ request, params }) => {
    const body = await readJson<{ decision: 'APPROVE' | 'REJECTED'; managerRemarks?: string }>(request);
    const target = cancelRequests.find((item) => item.id === Number(params.cancelRequestId));
    const status = body.decision === 'APPROVE' ? 'APPROVED' : 'REJECTED';
    if (target) {
      target.status = status;
      target.decidedAt = nowIso();
      target.decidedBy = '管理 次郎';
      const record = monitoringRecords.find((item) => item.id === target.recordPk);
      if (record && status === 'APPROVED') record.isActive = false;
    }
    return ok({ cancelRequestId: Number(params.cancelRequestId), status, decidedBy: '管理 次郎', decidedAt: nowIso(), managerRemarks: body.managerRemarks });
  }),
  http.get('/api/monitoring/:siteId/:projectId/list', ({ request, params }) =>
    pagedRecords(
      request,
      monitoringRecords.filter((record) => record.siteId === params.siteId && record.projectId === params.projectId),
    ),
  ),
  http.get('/api/monitoring/:projectId/list', ({ request, params }) =>
    pagedRecords(request, monitoringRecords.filter((record) => record.projectId === params.projectId)),
  ),

  // --- Notifications ---
  http.get('/api/notification/list', ({ request }) => {
    const url = new URL(request.url);
    const unreadOnly = url.searchParams.get('unreadOnly') === 'true';
    const source = notifications.filter((item) => !unreadOnly || !item.read);
    const { items, pagination } = paginate(source, numberParam(url, 'page', 0), numberParam(url, 'size', 10), 0);
    return ok({ notifications: items, pagination });
  }),
  http.get('/api/notification/count', () => ok({ unreadCount: notifications.filter((item) => !item.read).length })),
  http.patch('/api/notification/:notificationId/read', ({ params }) => {
    const target = notifications.find((item) => item.id === Number(params.notificationId));
    if (target) target.read = true;
    return ok({});
  }),
  http.patch('/api/notification/:notificationId/delete', ({ params }) => {
    const index = notifications.findIndex((item) => item.id === Number(params.notificationId));
    if (index >= 0) notifications.splice(index, 1);
    return ok({});
  }),
  http.post('/api/notification/inquiry', () => ok({ message: 'お問い合わせを受け付けました（デモ）' })),
];
