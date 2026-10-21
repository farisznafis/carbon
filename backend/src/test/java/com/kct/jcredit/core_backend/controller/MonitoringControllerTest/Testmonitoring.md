# MonitoringController API Map

## API-to-Service Map

| # | Method | API | Controller method | Service/dependency | Request/JWT/header notes |
|---:|---|---|---|---|---|
| 1 | POST | `/monitoring` | `RegisterMonitoringRecordSolar` | `MonitoringService.registerMonitoringRecordSolar` + `JwtUtil.extractUsername` + `JwtUtil.extractCompanyId` | `RegisterMonitoringRecordSolarRequest`; JWT credentials provide username/companyId |
| 2 | POST | `/monitoring/validate` | `validateMonitoringRecordSolar` | `MonitoringService.validateMonitoringRecordSolar` + both JWT extractors | Same validated request as registration; does not save a record |
| 3 | GET | `/monitoring?projectId=...&siteId=...` | `ReadLastMonitoringRecordSolar` | `MonitoringService.readLastMonitoringRecordSolar` + both JWT extractors | Required `projectId` and `siteId` query parameters |
| 4 | GET | `/monitoring/{projectId}/list` | `listMonitoringRecords` | `MonitoringService.readAllMonitoringRecordsSolar` + `JwtUtil.extractUsername` | `page` default 1, `limit` default 50, optional `nextPageToken`; companyId is not passed |
| 5 | GET | `/monitoring/{siteId}/{projectId}/list` | `listMonitoringRecordsbySite` | `MonitoringService.readSiteMonitoringRecordsSolar` + both JWT extractors | Path IDs, `page` default 1, `limit` default 50, optional `nextPageToken` |
| 6 | POST | `/monitoring/upload-elpv-evidence` | `uploadElpvEvidence` | `MonitoringService.uploadElpvEvidence` + both JWT extractors | Multipart `projectId`, `siteId`, required `file`, optional `description` |
| 7 | POST | `/monitoring/upload-elpvr-evidence` | `uploadElpvrEvidence` | `MonitoringService.uploadElpvrEvidence` + both JWT extractors | Multipart `projectId`, `siteId`, required `file`, optional `description` |
| 8 | POST | `/monitoring/upload-elpj-battery-specification-evidence` | `uploadElpjBatterySpecificationEvidence` | `MonitoringService.uploadElpjBatterySpecificationEvidence` + both JWT extractors | Multipart `projectId`, `siteId`, required `file`, optional `description` |
| 9 | POST | `/monitoring/upload-elpj-control-evidence` | `uploadElpjControlEvidence` | `MonitoringService.uploadElpjControlEvidence` + both JWT extractors | Multipart `projectId`, `siteId`, required `file`, optional `description` |
| 10 | POST | `/monitoring/cancel-request` | `createCancelRequest` | `CancelRequestService.createCancelRequest` + both JWT extractors | `CreateCancelRequestRequest`; `master` or `user` role |
| 11 | GET | `/monitoring/cancel-requests` | `getCancelRequests` | `CancelRequestService.listCancelRequests` | `Authentication` parameter is not read; `admin` or `operator` with `MONITORING_CANCEL` permission |
| 12 | POST | `/monitoring/{cancelRequestId}/cancel-decision` | `makeDecision` | `CancelRequestService.processDecision` + `JwtUtil.extractUsername` | Path ID, `CancelRequestDecisionRequest`; `admin` or `operator` with `MONITORING_CANCEL` permission |
| 13 | GET | `/monitoring/{siteId}/{projectId}/equipment/{equipmentId}/evidence-item` | `readMonitoringEvidenceItems` | `MonitoringService.readMonitoringEvidenceItems` + both JWT extractors | Path IDs; controller returns HTTP 200 directly with the service body |

## Service Index

1. `MonitoringService`
   - `POST /monitoring` (`registerMonitoringRecordSolar`)
   - `POST /monitoring/validate` (`validateMonitoringRecordSolar`)
   - `GET /monitoring` (`readLastMonitoringRecordSolar`)
   - `GET /monitoring/{projectId}/list` (`readAllMonitoringRecordsSolar`)
   - `GET /monitoring/{siteId}/{projectId}/list` (`readSiteMonitoringRecordsSolar`)
   - Four `/monitoring/upload-*-evidence` endpoints
   - `GET /monitoring/{siteId}/{projectId}/equipment/{equipmentId}/evidence-item` (`readMonitoringEvidenceItems`)
2. `CancelRequestService`
   - `POST /monitoring/cancel-request` (`createCancelRequest`)
   - `GET /monitoring/cancel-requests` (`listCancelRequests`)
   - `POST /monitoring/{cancelRequestId}/cancel-decision` (`processDecision`)
3. `JwtUtil`
   - `extractUsername` and/or `extractCompanyId` on authenticated service-delegating operations, as listed above

## HTTP, Validation, and Authorization Notes

- Most endpoints convert the returned `statusCode` with `ResponseUtil.getHttpStatus`; `readMonitoringEvidenceItems` is the exception and always returns HTTP 200.
- `RegisterMonitoringRecordSolarRequest` has bean constraints for required identifiers/measurements/evidence, numeric bounds/precision, and date format. Equipment-dependent rules are service validation and should be distinguished from MVC validation.
- `CreateCancelRequestRequest` requires `siteId`, `recordPK`, `recordSK`, and `reasonText`; `CancelRequestDecisionRequest.decision` must be `APPROVE` or `REJECT`.
- `page` and `limit` have defaults but no controller-level `@Min`/`@Max` annotations. Test parameter forwarding/defaults here; do not claim MVC rejects out-of-range values unless implemented elsewhere.
- Authorization annotations exist on project-wide listing, cancel-request creation/listing, and cancel decisions. Other methods only declare bearer authentication in this controller. Use Spring Security tests if verifying these rules at the HTTP layer.
- Use MockMvc for request binding, bean validation, multipart media type/required parameters, and method-security behavior. Direct method invocation does not execute these MVC/security layers.

## Legacy Test Coverage and Gaps

The root-level `MonitoringControllerTestOld.java` has many direct-call tests for record registration, latest record, site-specific pagination, uploads, cancel-request creation/listing, and approve/reject decisions. It does not cover `POST /monitoring/validate`, project-wide `GET /{projectId}/list`, or the evidence-item GET route. The old suite also has overlapping ELPV upload cases that should be separated by route; validation expectations should be moved to MockMvc instead of simulated service exceptions.
