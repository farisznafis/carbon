package com.kct.jcredit.core_backend.service.impl;

import com.kct.jcredit.core_backend.constant.ResponseConstants;
import com.kct.jcredit.core_backend.dto.request.CancelRequestDecisionRequest;
import com.kct.jcredit.core_backend.dto.request.CreateCancelRequestRequest;
import com.kct.jcredit.core_backend.dto.response.CancelRequestResponse;
import com.kct.jcredit.core_backend.entity.RecordCancelRequest;
import com.kct.jcredit.core_backend.dto.response.CancelRequestDecisionResponse;
import com.kct.jcredit.core_backend.dto.response.CancelRequestItemDto;
import com.kct.jcredit.core_backend.dto.response.CancelRequestListResponse;
import com.kct.jcredit.core_backend.entity.Site;
import com.kct.jcredit.core_backend.entity.EnR002MonitoringRecord;
import com.kct.jcredit.core_backend.repository.postgres.RecordCancelRequestRepository;
import com.kct.jcredit.core_backend.repository.postgres.SiteRepository;
import com.kct.jcredit.core_backend.repository.dynamodb.MonitoringTableRecordSolarRepository;
import com.kct.jcredit.core_backend.service.CancelRequestService;
import com.kct.jcredit.core_backend.service.NotificationMessages;
import com.kct.jcredit.core_backend.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class CancelRequestServiceImpl implements CancelRequestService {
    
    private final RecordCancelRequestRepository cancelRequestRepository;
    private final SiteRepository siteRepository;
    private final MonitoringTableRecordSolarRepository monitoringRecordSolarRepository;
    private final NotificationService notificationService;
    private final NotificationMessages messages;
    
    private static final DateTimeFormatter ISO_FORMATTER = 
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'").withZone(ZoneOffset.UTC);
    
    @Override
    @Transactional
    public CancelRequestResponse createCancelRequest(
            String username,
            String companyId,
            CreateCancelRequestRequest request) {
        
        try {
            CancelRequestResponse response = new CancelRequestResponse();

            // 1. Validate site exists and user has access
            Optional<Site> siteOpt = siteRepository.findByPublicIdWithFallback(request.getSiteId());
            if (siteOpt.isEmpty()) {
                response.setStatusCode(ResponseConstants.NOT_FOUND);
                response.setMessage(ResponseConstants.MSG_SITE_NOT_FOUND);
                return response;
            }
            
            Site site = siteOpt.get();
            
            // Check if user has access to this site's company
            if (site.getCompany() == null || 
                !site.getCompany().getCompanyId().equals(companyId)) {
                response.setStatusCode(ResponseConstants.FORBIDDEN);
                response.setMessage(ResponseConstants.MSG_FORBIDDEN);
                return response;
            }
            
            // 2. Validate monitoring record exists in NoSQL
            Optional<EnR002MonitoringRecord> recordOpt = 
                    monitoringRecordSolarRepository.findByPKandSK(request.getRecordPK(), request.getRecordSK());
            
            if (recordOpt.isEmpty()) {
                response.setStatusCode(ResponseConstants.NOT_FOUND);
                response.setMessage(ResponseConstants.MSG_NOT_FOUND);
                return response;

            }
            
            EnR002MonitoringRecord monitoringRecord = recordOpt.get();
            
            // 3. Check if record is already cancelled
            if (!monitoringRecord.getIsActive()) {
                response.setStatusCode(ResponseConstants.BAD_REQUEST);
                response.setMessage("このレコードは既に取り消されています。");
                return response;
            }
            
            // 4. Check for existing pending cancel requests. convert string to local date time
            boolean hasPendingRequest = cancelRequestRepository.existsPendingRequestForRecord(
                request.getRecordPK(), 
                request.getRecordSK()
            );
            
            if (hasPendingRequest) {
                response.setStatusCode(ResponseConstants.BAD_REQUEST);
                response.setMessage("このレコードの取消申請が既に存在します。");
                return response;
            }
            
            // 5. Create cancel request entity
            RecordCancelRequest cancelRequest = RecordCancelRequest.builder()
                    .site(site)
                    .status("PENDING")
                    .recordPk(request.getRecordPK())
                    .recordSk(request.getRecordSK())
                    .reason(request.getReasonText())
                    .requestedBy(username)
                    .requestedAt(Instant.now())
                    .build();
            
            // Set base entity fields
            cancelRequest.setCreatedBy(username);
            cancelRequest.setUpdatedBy(username);
            cancelRequest.setIsActive(true);
            
            // 6. Save to database
            RecordCancelRequest savedRequest = cancelRequestRepository.save(cancelRequest);
            
            // Notify all operators about the new cancel request
            try {
                NotificationMessages.Content created = messages.of("cancel.created",
                    String.valueOf(savedRequest.getSite().getPublicId()),
                    savedRequest.getRequestedBy(),
                    String.valueOf(savedRequest.getId()));
                notificationService.createNotificationForAllOperatingUsers(
                    created.subject(), created.body());
                notificationService.sendEmailToAllOperatingUsers(
                    created.subject(), created.body());
            } catch (Exception e) {
                log.warn("Operator notification failed for cancelRequestId={}: {}", savedRequest.getId(), e.getMessage());
            }
            
            // 7. Build success response

            response = CancelRequestResponse.builder()
                    .cancelRequestId(savedRequest.getId())
                    .recordPk(savedRequest.getRecordPk())
                    .recordSk(request.getRecordSK())
                    .status(savedRequest.getStatus())
                    .requestedBy(savedRequest.getRequestedBy())
                    .requestedAt(ISO_FORMATTER.format(savedRequest.getRequestedAt()))
                    .siteId(savedRequest.getSite().getPublicId())
                    .reason(savedRequest.getReason())
                    .build();
            response.setStatusCode(ResponseConstants.OK);
            response.setMessage("取消申請が正常に作成されました。");
            return response;
            
        } catch (Exception e) {
                CancelRequestResponse response = new CancelRequestResponse();
                response.setStatusCode(ResponseConstants.INTERNAL_SERVER_ERROR);
                response.setMessage(ResponseConstants.MSG_INTERNAL_ERROR);
                return response;
        }
    }
    @Override
    public CancelRequestListResponse listCancelRequests() {
        // Implementation for listing cancel requests
            CancelRequestListResponse response = new CancelRequestListResponse();
            // Fetch cancel requests from database, convert to response DTOs, and set in response
            // This is a placeholder implementation
            List<RecordCancelRequest> cancelRequests = cancelRequestRepository.findAll();
            response.setItems(cancelRequests.stream().map(this::toListItemDto).collect(Collectors.toList()));
            response.setStatusCode(ResponseConstants.OK);
            response.setMessage("取消申請のリストが正常に取得されました。");
            return response;

    }

     public CancelRequestItemDto toListItemDto(RecordCancelRequest entity) {
        return CancelRequestItemDto.builder()
            .id(entity.getId())
            .requestedAt(entity.getRequestedAt().toString())
            .reason(entity.getReason())
            .recordPk(entity.getRecordPk())
            .status(entity.getStatus().toLowerCase()) // "PENDING" → "pending"
            .requestedBy(entity.getRequestedBy())
            .siteName(entity.getSite() != null ? entity.getSite().getSiteName() : null)
            .decidedAt(entity.getDecidedAt() != null ? entity.getDecidedAt().toString() : null)
            .decidedBy(entity.getDecidedBy())
            .build();
    }

    @Override
    @Transactional
    public CancelRequestDecisionResponse processDecision(Integer requestId, CancelRequestDecisionRequest request, String operatorUsername) {

        CancelRequestDecisionResponse response = new CancelRequestDecisionResponse();
    
        // 1. 取消申請を取得
        RecordCancelRequest cancelRequest = cancelRequestRepository
            .findById(requestId)
            .orElseThrow(() -> new IllegalStateException("取消申請が見つかりません: " + requestId));
        
        // 2. バリデーション 
        if (!"PENDING".equals(cancelRequest.getStatus())) {
            response.setStatusCode(ResponseConstants.BAD_REQUEST);
            response.setMessage("PENDING以外は処理できません。現在の状態: " + cancelRequest.getStatus());
            return response;
        }

        // 相殺レコードが既に存在する場合
        if (cancelRequest.getOffsetRecordPk() != null) {
            response.setStatusCode(ResponseConstants.BAD_REQUEST);
            response.setMessage("相殺レコードが既に作成済みです");
            return response;
        }
        
        // 3. 承認 or 却下 
        if ("APPROVE".equals(request.getDecision())) {
            return approveRequest(cancelRequest, request, operatorUsername);
        } else {
            return rejectRequest(cancelRequest, request, operatorUsername);
        }
    }


    private CancelRequestDecisionResponse approveRequest(RecordCancelRequest cancelRequest, CancelRequestDecisionRequest request, String operatorUsername) {
        CancelRequestDecisionResponse response = new CancelRequestDecisionResponse();
        // Implementation for approving cancel request
          log.info("承認処理開始: cancelRequestId={}", cancelRequest.getId()); 
        // 1. 元のレコードをNoSQLから取得
       Optional<EnR002MonitoringRecord> originalRecord = 
                    monitoringRecordSolarRepository.findByPKandSK(cancelRequest.getRecordPk(), cancelRequest.getRecordSk());
        if (!originalRecord.isPresent()) {
            response.setStatusCode(ResponseConstants.NOT_FOUND);
            response.setMessage(ResponseConstants.MSG_NOT_FOUND);
            return response;
        } 
        try{
            // 2. 相殺レコードを作成（値を反転）
            EnR002MonitoringRecord offsetRecord = createOffsetRecord(originalRecord.get());

            // 3. NoSQLに相殺レコードを保存
            monitoringRecordSolarRepository.save(offsetRecord);
            
            // 4. RDBの取消申請を更新
            Instant now = Instant.now();
            cancelRequest.setStatus("APPROVED");
            cancelRequest.setDecidedBy(operatorUsername); 
            cancelRequest.setDecidedAt(now);
            cancelRequest.setOffsetRecordPk(offsetRecord.getId());
            cancelRequest.setOffsetRecordSk(offsetRecord.getTimeUpdated());
            
            RecordCancelRequest saved = cancelRequestRepository.save(cancelRequest);
            
            // 5. レスポンス構築
            response = CancelRequestDecisionResponse.builder()
                .cancelRequestId(saved.getId())
                .status("APPROVED")
                .decidedBy(saved.getDecidedBy())
                .decidedAt(saved.getDecidedAt().toString())
                .originalRecordPk(saved.getRecordPk())
                .originalRecordSk(saved.getRecordSk().toString())
                .offsetRecordPk(saved.getOffsetRecordPk())
                .offsetRecordSk(saved.getOffsetRecordSk().toString())
                .managerRemarks(request.getManagerRemarks())
                .build();
            response.setStatusCode(ResponseConstants.OK);
            // Notify participating user about approval
            try {
                Integer participantId = originalRecord.get().getParticipatingUserID();
                if (participantId != null) {
                    NotificationMessages.Content approved = messages.of(
                        "cancel.approved", String.valueOf(saved.getId()));
                    notificationService.createNotification(
                        approved.subject(), approved.body(), participantId, null);
                }
            } catch (Exception e) {
                log.warn("Participant notification failed for cancelRequestId={}: {}", saved.getId(), e.getMessage());
            }

            return response;
            
        } catch (Exception e) {
            log.error("承認処理中にエラーが発生: cancelRequestId={}, error={}", cancelRequest.getId(), e.getMessage());
            response.setStatusCode(ResponseConstants.INTERNAL_SERVER_ERROR);
            response.setMessage(ResponseConstants.MSG_INTERNAL_ERROR);
            return response;
        }
    }

    private CancelRequestDecisionResponse rejectRequest(RecordCancelRequest cancelRequest, CancelRequestDecisionRequest request, String operatorUsername) {
         log.info("却下処理開始: cancelRequestId={}", cancelRequest.getId());
        // Fetch original monitoring record to get participating user id for notification
        Optional<EnR002MonitoringRecord> originalRecord = monitoringRecordSolarRepository
            .findByPKandSK(cancelRequest.getRecordPk(), cancelRequest.getRecordSk());
        // RDBの取消申請を更新
        Instant now = Instant.now();
        cancelRequest.setStatus("REJECTED");
        cancelRequest.setDecidedBy(operatorUsername); 
        cancelRequest.setDecidedAt(now);
        
        RecordCancelRequest saved = cancelRequestRepository.save(cancelRequest);
        
        log.info("却下処理完了");
        CancelRequestDecisionResponse response = CancelRequestDecisionResponse.builder()
            .cancelRequestId(saved.getId())
            .status("REJECTED")         
            .decidedBy(saved.getDecidedBy())
            .decidedAt(saved.getDecidedAt().toString())
            .originalRecordPk(saved.getRecordPk())
            .originalRecordSk(saved.getRecordSk().toString())
            .managerRemarks(request.getManagerRemarks())
            .build();
        response.setStatusCode(ResponseConstants.OK);
        // Notify participating user about rejection
        try {
            if (originalRecord.isPresent() && originalRecord.get().getParticipatingUserID() != null) {
                Integer participantId = originalRecord.get().getParticipatingUserID();
                NotificationMessages.Content rejected = messages.of("cancel.rejected",
                    String.valueOf(saved.getId()), request.getManagerRemarks());
                notificationService.createNotification(
                    rejected.subject(), rejected.body(), participantId, null);
            }
        } catch (Exception e) {
            log.warn("Participant rejection notification failed for cancelRequestId={}: {}", saved.getId(), e.getMessage());
        }

        // レスポンス構築（offsetRecordはnull）
        return response;
    }

    private EnR002MonitoringRecord createOffsetRecord(EnR002MonitoringRecord original) {
        
        String currentTimestamp = Instant.now().toString();
        
        EnR002MonitoringRecord offsetRecord = new EnR002MonitoringRecord();
        
        // ===== 同じ値を保持 =====
        offsetRecord.setId(original.getId());  // 同じPK（拠点+センサー識別）
        offsetRecord.setProjectID(original.getProjectID());
        offsetRecord.setSiteID(original.getSiteID());
        offsetRecord.setParticipatingUserID(original.getParticipatingUserID());
        offsetRecord.setDateRecorded(original.getDateRecorded());

        // ===== 新しいタイムスタンプ =====
        offsetRecord.setTimeUpdated(currentTimestamp);  // SK: 新しい時刻
        
        // ===== 数値を反転（マイナス値にする） =====
        offsetRecord.setElpjControlValue(
            original.getElpjControlValue() != null 
                ? original.getElpjControlValue().negate() 
                : null
        );
        
        offsetRecord.setElpjBatteryValue(
            original.getElpjBatteryValue() != null 
                ? original.getElpjBatteryValue().negate() 
                : null
        );
        
        offsetRecord.setElpjAccuracy(
            original.getElpjAccuracy() != null 
                ? original.getElpjAccuracy().negate() 
                : null
        );
        
        offsetRecord.setElPv(
            original.getElPv() != null 
                ? original.getElPv().negate() 
                : null
        );
        
        offsetRecord.setElPvr(
            original.getElPvr() != null 
                ? original.getElPvr().negate() 
                : null
        );
        
        // ===== 証憑（エビデンス）はクリア =====
        // 相殺レコードは元のレコードを参照するため、証憑は不要
        offsetRecord.setElpjControlEquipmentSpecFiles(null);
        offsetRecord.setElpjBatteryEvidence(null);
        offsetRecord.setElPvEvidence(null);
        offsetRecord.setElPvrEvidence(null);
        offsetRecord.setElPvEvidenceUrl(null);
        offsetRecord.setElPvrEvidenceUrl(null);
        offsetRecord.setElpjBatterySpecificationUrl(null);
        offsetRecord.setElpjControlEvidenceUrl(null);
        
        // ===== 相殺レコード特有の設定 =====
       // offsetRecord.setChangedBattery(false);  // 相殺レコードではバッテリー交換なし
        offsetRecord.setIsActive(true);    
        
        return offsetRecord;
    }

}