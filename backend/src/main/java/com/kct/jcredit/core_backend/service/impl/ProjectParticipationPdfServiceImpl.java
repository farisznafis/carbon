package com.kct.jcredit.core_backend.service.impl;

import com.kct.jcredit.core_backend.dto.response.details.ProjectDetailResponse;
import com.kct.jcredit.core_backend.dto.response.details.SiteResponse;
import com.kct.jcredit.core_backend.dto.response.details.ValidationErrorDetail;
import com.kct.jcredit.core_backend.constant.ResponseConstants;
import com.kct.jcredit.core_backend.dto.request.EnR002EquipmentRequest;
import com.kct.jcredit.core_backend.dto.request.EnR002FileItemRequest;
import com.kct.jcredit.core_backend.dto.request.EnR002FinalAgreementRequest;
import com.kct.jcredit.core_backend.dto.request.EnR002ConfirmationItemRequest;
import com.kct.jcredit.core_backend.dto.request.EnR002JoinProjectRequest;
import com.kct.jcredit.core_backend.dto.request.EnR002SustainabilityCommitmentRequest;
import com.kct.jcredit.core_backend.dto.response.EnR002SustainabilityCommitmentResponse;
import com.kct.jcredit.core_backend.dto.response.EnR002EquipmentResponse;
import com.kct.jcredit.core_backend.dto.response.ProjectJoinResponse;
import com.kct.jcredit.core_backend.dto.response.ProjectResponse;
import com.kct.jcredit.core_backend.dto.response.ProjectInfoResponse;
import com.kct.jcredit.core_backend.dto.response.ProjectListInfoResponse;
import com.kct.jcredit.core_backend.dto.response.ProjectPdfDownloadResponse;
import com.kct.jcredit.core_backend.dto.response.SiteListResponse;
import com.kct.jcredit.core_backend.entity.ParticipatingCompany;
import com.kct.jcredit.core_backend.entity.ParticipatingProject;
import com.kct.jcredit.core_backend.entity.ProjectParticipationConfirmation;
import com.kct.jcredit.core_backend.entity.ParticipatingUser;
import com.kct.jcredit.core_backend.entity.Project;
import com.kct.jcredit.core_backend.entity.Site;
import com.kct.jcredit.core_backend.entity.SiteProjectRelation;
import com.kct.jcredit.core_backend.entity.EnR002EquipmentFile;
import com.kct.jcredit.core_backend.entity.EnR002SustainabilityCommitment;
import com.kct.jcredit.core_backend.entity.Notification;
import com.kct.jcredit.core_backend.entity.ConfirmationItems;
import com.kct.jcredit.core_backend.entity.EnR002ApplicationConfirmation;
import com.kct.jcredit.core_backend.entity.EnR002Equipment;
import com.kct.jcredit.core_backend.exception.BulkValidationException;
import com.kct.jcredit.core_backend.exception.CustomException;
import com.kct.jcredit.core_backend.enums.EquipmentFileType;
import com.kct.jcredit.core_backend.service.ParticipationProjectService;
import com.kct.jcredit.core_backend.service.ProjectService;
import com.kct.jcredit.core_backend.service.SiteService;

import fr.opensagres.poi.xwpf.converter.pdf.PdfConverter;
import fr.opensagres.poi.xwpf.converter.pdf.PdfOptions;

import com.kct.jcredit.core_backend.service.ParticipationProjectValidationService;
import com.kct.jcredit.core_backend.service.ProjectParticipationPdfService;
import com.kct.jcredit.core_backend.service.EnR002FileUploadService;
import com.kct.jcredit.core_backend.repository.postgres.ProjectRepository;
import com.kct.jcredit.core_backend.repository.dynamodb.EnR002ApplicationConfirmationRepository;
import com.kct.jcredit.core_backend.repository.dynamodb.EnR002SustainabilityCommitmentRepository;
import com.kct.jcredit.core_backend.repository.dynamodb.ProjectParticipationConfirmationRepository;
import com.kct.jcredit.core_backend.repository.postgres.SiteProjectRelationRepository;
import com.kct.jcredit.core_backend.repository.postgres.EnR002EquipmentFileRepository;
import com.kct.jcredit.core_backend.repository.postgres.EnR002EquipmentRepository;
import com.kct.jcredit.core_backend.repository.postgres.SiteRepository;
import com.kct.jcredit.core_backend.repository.postgres.NotificationRepository;
import com.kct.jcredit.core_backend.repository.postgres.ProjectRepository;
import com.kct.jcredit.core_backend.repository.postgres.ParticipatingCompanyRepository;
import com.kct.jcredit.core_backend.repository.postgres.ParticipatingUserRepository;
import com.kct.jcredit.core_backend.repository.postgres.ParticipatingProjectRepository;

import lombok.RequiredArgsConstructor;

import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.springframework.beans.factory.annotation.Value;
import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.Example;
import org.springframework.data.domain.Sort;

import java.time.ZoneId;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProjectParticipationPdfServiceImpl implements ProjectParticipationPdfService {

    @Value("${app.s3.bucket:}")
    private String bucketName;

    @Value("${app.s3.presign.expiry-seconds:900}")
    private int presignExpirySeconds;

    private final S3Client    s3Client;
    private final S3Presigner s3Presigner;

    private final ParticipatingProjectRepository participatingProjectRepository;
    private final EnR002ApplicationConfirmationRepository confirmationItemsRepository;
    private final EnR002SustainabilityCommitmentRepository sustainabilityRepository;
    private final ProjectParticipationConfirmationRepository reviewRepository;

    // ⚠️ Ganti dengan nama class/repository notifikasi yang ASLI di codebase-mu —
    //    aku asumsikan pola sederhana sesuai tabel `notification` yang sudah ada
    //    (title, message, read, operating_user_id/participating_user_id)
    private final NotificationRepository notificationRepository;

    private static final String TEMPLATE_PATH = "/templates/betsushi9_template.docx";
    private static final DateTimeFormatter JP_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy年MM月dd日");

    // ─────────────────────────────────────────────────────────────
    // ORCHESTRATOR
    // ─────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public void generateAndProcess(Integer participationId) {

        ParticipatingProject participation = participatingProjectRepository
                .findById(participationId)
                .orElseThrow(() -> new CustomException(
                        ResponseConstants.NOT_FOUND, "参加情報が見つかりません。"));

        EnR002Equipment equipment = participation.getEquipment();
        Site site = equipment.getSite();
        ParticipatingCompany company = site.getCompany();
        Project project = participation.getProject();

        EnR002ApplicationConfirmation confirmationItems = confirmationItemsRepository
                .findByParticipationId(participationId)
                .orElseThrow(() -> new CustomException("DATA_MISSING", "確認事項データが見つかりません。"));

        EnR002SustainabilityCommitment sustainability = sustainabilityRepository
                .findByParticipationId(participationId)
                .orElseThrow(() -> new CustomException("DATA_MISSING", "事業者情報データが見つかりません。"));

        ProjectParticipationConfirmation review = reviewRepository
                .findByParticipationId(participationId)
                .orElseThrow(() -> new CustomException("DATA_MISSING", "レビューデータが見つかりません。"));

        try {
            XWPFDocument doc = loadTemplate();

            Map<String, String> placeholders = buildPlaceholders(
                    project, company, equipment,
                    confirmationItems, sustainability, review, participation);

            replacePlaceholdersInParagraphs(doc, placeholders);
            replacePlaceholdersInTables(doc, placeholders);

            byte[] pdfBytes = convertToPdf(doc);

            String s3Key = uploadDraftToS3(pdfBytes, project.getId(), site.getId(), participationId);

            participatingProjectRepository.updatePdfS3Key(participationId, s3Key);

            // ─────────────────────────────────────────────────
            // TODO: DocuSignアカウント準備後、ここでenvelope作成・送信を実装
            //   1. createEnvelope(pdfBytes, participation.getUser().getEmail())
            //   2. Webhook受信ハンドラー追加 → onEnvelopeCompleted()
            //   3. 署名済みPDFをDocuSignから取得 → S3再アップロード
            //   4. updatePdfS3Key() を署名済みキーで再実行
            // 現状: 下書きPDFのみS3に保存、署名フローは未実装
            // ─────────────────────────────────────────────────
            log.warn("[TODO] DocuSign envelope belum dikirim — akun belum siap. participationId={}",
                    participationId);

            notifyDraftReady(participation);

        } catch (IOException e) {
            log.error("PDF generation failed. participationId={}, error={}",
                    participationId, e.getMessage(), e);
            throw new RuntimeException("PDF生成に失敗しました", e);
        }
    }

    // ─────────────────────────────────────────────────────────────
    // PRESIGNED URL
    // ─────────────────────────────────────────────────────────────

    @Override
    public String generatePresignedUrl(String s3Key) {
        GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                .signatureDuration(Duration.ofSeconds(presignExpirySeconds))
                .getObjectRequest(GetObjectRequest.builder()
                        .bucket(bucketName)
                        .key(s3Key)
                        .build())
                .build();

        return s3Presigner.presignGetObject(presignRequest).url().toString();
    }

    // ─────────────────────────────────────────────────────────────
    // PRIVATE — Template & Placeholder
    // ─────────────────────────────────────────────────────────────

    private XWPFDocument loadTemplate() throws IOException {
        InputStream stream = getClass().getResourceAsStream(TEMPLATE_PATH);
        if (stream == null) {
            throw new RuntimeException("Template not found: " + TEMPLATE_PATH);
        }
        return new XWPFDocument(stream);
    }

    private Map<String, String> buildPlaceholders(
            Project project,
            ParticipatingCompany company,
            EnR002Equipment equipment,
            EnR002ApplicationConfirmation confirmationItems,
            EnR002SustainabilityCommitment sustainability,
            ProjectParticipationConfirmation review,
            ParticipatingProject participation) {

        Map<String, String> map = new LinkedHashMap<>();

        // ── Halaman 1-2: プロジェクト・企業情報 ────────────────
        map.put("${プログラム名}", safe(project.getProjectName()));
        map.put("${companyname}", safe(company.getNameJa()));
        map.put("${プロジェクト概要}", safe(project.getMethodName()));
        map.put("${applyDate}", formatDate(participation.getCreatedAt()));
        map.put("${h1}", safe(company.getNameFurigana()));
        map.put("${h2}", safe(company.getNameJa()));
        // ⚠️ h3 (inkan online) — DocuSign belum siap, tidak ada data tanda tangan.
        //     Dikosongkan dulu sampai flow signing diimplementasikan.
        map.put("${h3}", "");
        map.put("${h4}", safe(company.getPostalCode()));
        map.put("${h5}", buildAddress(company));
        map.put("${h6}", safe(company.getPhoneNumber()));
        map.put("${h7}", safe(company.getFax()));
        map.put("${h8}", safe(company.getEmail()));

        // ⚠️ プログラム型運営／管理者名 (operator company info) — perlu akses
        //     project.getOperatorCompany(). Kalau relasi ini belum ada di
        //     entity Project, tambahkan dulu (FK operator_company_id sudah
        //     ada di tabel).
        // map.put("${プログラム型運営}", safe(project.getOperatorCompany().getNameJa()));
        // map.put("${管理者名}", operatorRepName);

        // ── Confirmation Items (a1-a16) ────────────────────────
        // ⚠️ a1 "agree_terms" di dokumen pemetaan mengarah ke
        //     en_r002_application_confirmation, TAPI di entity kita,
        //     isAgreeTerms disimpan di SustainabilityCommitment (final
        //     agreement), bukan ConfirmationItems. Aku ambil dari sana —
        //     tolong konfirmasi apakah ini resolusi yang benar.
        map.put("${a1}", boolToMark(sustainability.getIsAgreeTerms()));
        map.put("${a2}", boolToMark(confirmationItems.getIsAgreeDataUse()));
        map.put("${a3}", boolToMark(confirmationItems.getIsAgreeExtraData()));
        map.put("${a4}", boolToMark(confirmationItems.getIsAgreeTransfer()));
        map.put("${a5}", boolToMark(confirmationItems.getIsNotRegistered()));
        map.put("${a6}", Boolean.TRUE.equals(confirmationItems.getIsKeidanrenParticipation())
                ? "参加しています" : "参加していません");
        map.put("${a7}", Boolean.TRUE.equals(confirmationItems.getIsEmitter())
                ? "該当します" : "該当しません");
        map.put("${a13}", safe(confirmationItems.getEmitterCode()));
        map.put("${a8}", Boolean.TRUE.equals(confirmationItems.getIsSubjectToPeriodicReport())
                ? "対象者に該当します" : "対象者に該当しません");
        map.put("${a9}", confirmationItems.getIsPeriodicReportTypeOne() == null ? ""
                : Boolean.TRUE.equals(confirmationItems.getIsPeriodicReportTypeOne()) ? "一種" : "二種");
        map.put("${a14}", safe(confirmationItems.getEnergyBusinessCode()));
        map.put("${a15}", safe(confirmationItems.getFactoryCode()));

        // ⚠️ a10-a12 (sustainability_commitment_filled / equipment_info_filled /
        //     equipment_info_attached) — sesuai keputusan sebelumnya, selalu
        //     "true" karena validasi backend sudah mewajibkan ini terisi
        //     sebelum submit berhasil.
        map.put("${a10}", "○");
        map.put("${a11}", "○");
        map.put("${a12}", "○");

        // ── Halaman 3: Sustainability Commitment ───────────────
        map.put("${事業者名}", safe(sustainability.getCompanyName()));
        map.put("${代表者役職}", safe(sustainability.getRepRole()));
        map.put("${代表者名}", safe(sustainability.getRepName()));

        // ── Halaman 4: Equipment ────────────────────────────────
        map.put("${f1}", formatDate(equipment.getEquipmentOpDate()));
        map.put("${f2}", decimal(equipment.getNominalMaxOutput()));
        map.put("${f3}", safe(equipment.getPcsManufacturer()));
        map.put("${f4}", safe(equipment.getPcsModel()));
        map.put("${f5}", equipment.getPcsUnitCount() != null ? String.valueOf(equipment.getPcsUnitCount()) : "");
        map.put("${f6}", safe(equipment.getDeviceNo()));
        map.put("${f7}", decimal(equipment.getPcsStandby()));
        map.put("${f8}", Boolean.TRUE.equals(equipment.getHasBattery()) ? "有" : "無");
        map.put("${f9}", safe(equipment.getBatteryMaker()));
        map.put("${f10}", safe(equipment.getBatteryModel()));
        map.put("${f11}", decimal(equipment.getBatteryCapacity()));
        map.put("${f12}", equipment.getTotalInvestment() != null
                ? String.format("%,d", equipment.getTotalInvestment()) : "");
        map.put("${f13}", safe(equipment.getSubsidyName()));
        map.put("${f14}", equipment.getSubsidyAmount() != null
                ? String.format("%,.0f", equipment.getSubsidyAmount()) : "");
        map.put("${f15}", decimal(equipment.getPreProjectUnitPrice()));

        // ── Halaman 5: 添付資料確認欄（Review — auto dari file check operator）
        map.put("${b1}", boolToMark(review.getIsSolarContractDoc()));
        map.put("${b2}", boolToMark(review.getIsPcsSpecDoc()));
        map.put("${b3}", boolToMark(review.getIsBatterySpecDoc()));
        map.put("${b4}", boolToMark(review.getIsBatterySpecDoc()));
        map.put("${b5}", boolToMark(review.getIsInvestmentProofDoc()));

        // ⚠️ 送付先アドレス／送付先／FAX番号 — sumber data masih belum
        //     dikonfirmasi (kemungkinan preferensi operator, bukan data
        //     per-participation). Dikosongkan sampai dikonfirmasi.
        map.put("${送付先アドレス}", "");
        map.put("${送付先}", "");
        map.put("${FAX番号}", "");

        // ── Halaman 6-8: プロジェクト概要（繰り返し）────────────
        map.put("${利用目的}", safe(project.getMethodDetails()));
        map.put("${YYYY年MM月DD}", formatDate(participation.getCreatedAt()));

        // ── Halaman 9-10: 確認者記名・押印欄 ─────────────────────
        map.put("${c1}", boolToMark(review.getIsSolarContractDoc()));
        map.put("${c2}", boolToMark(review.getIsPcsSpecDoc()));
        map.put("${c3}", boolToMark(review.getIsBatterySpecDoc()));
        map.put("${c4}", boolToMark(review.getIsOperationStartDoc()));
        map.put("${c5}", boolToMark(review.getIsGenerationDataDoc()));
        map.put("${c6}", boolToMark(review.getIsReversePowerDoc()));
        map.put("${c7}", boolToMark(review.getIsInvestmentProofDoc()));

        map.put("${d1}", boolToMark(review.getIsApplicationCompleteCheck()));
        map.put("${d2}", boolToMark(review.getIsFormsAttachCheck()));
        map.put("${d3}", boolToMark(review.getIsEmissionsReductionActivityListCheck()));
        map.put("${d4}", boolToMark(review.getIsOriginalSpecCheck()));
        map.put("${d5}", boolToMark(review.getIsSubsidyCheck()));

        map.put("${管理者_1次}", safe(review.getPrimaryApplReviewerName()));
        map.put("${㊞1}", safe(review.getPrimaryApplReviewerInkan()));
        map.put("${管理者_2次}", safe(review.getSecondaryApplReviewerName()));
        map.put("${㊞2}", safe(review.getSecondaryApplReviewerInkan()));

        return map;
    }

    private void replacePlaceholdersInParagraphs(XWPFDocument doc, Map<String, String> placeholders) {
        doc.getParagraphs().forEach(p -> replaceSingleParagraph(p, placeholders));
    }

    private void replacePlaceholdersInTables(XWPFDocument doc, Map<String, String> placeholders) {
        doc.getTables().forEach(table ->
                table.getRows().forEach(row ->
                        row.getTableCells().forEach(cell ->
                                cell.getParagraphs().forEach(p ->
                                        replaceSingleParagraph(p, placeholders)))));
    }

    private void replaceSingleParagraph(XWPFParagraph paragraph, Map<String, String> placeholders) {
        List<XWPFRun> runs = paragraph.getRuns();
        if (runs.isEmpty()) return;

        String fullText = runs.stream()
                .map(r -> r.getText(0) != null ? r.getText(0) : "")
                .collect(Collectors.joining());

        boolean hasPlaceholder = placeholders.keySet().stream().anyMatch(fullText::contains);
        if (!hasPlaceholder) return;

        String replaced = fullText;
        for (Map.Entry<String, String> entry : placeholders.entrySet()) {
            replaced = replaced.replace(entry.getKey(), entry.getValue());
        }

        runs.get(0).setText(replaced, 0);
        for (int i = 1; i < runs.size(); i++) {
            runs.get(i).setText("", 0);
        }
    }

    private byte[] convertToPdf(XWPFDocument doc) throws IOException {
        ByteArrayOutputStream pdfOut = new ByteArrayOutputStream();
        PdfOptions options = PdfOptions.create();
        PdfConverter.getInstance().convert(doc, pdfOut, options);
        return pdfOut.toByteArray();
    }

    private String uploadDraftToS3(byte[] pdfBytes, Integer projectId, Integer siteId, Integer participationId) {
        String s3Key = String.format(
                "projects/%d/sites/%d/participation/%d/draft_membership.pdf",
                projectId, siteId, participationId);

        s3Client.putObject(
                PutObjectRequest.builder()
                        .bucket(bucketName)
                        .key(s3Key)
                        .contentType("application/pdf")
                        .contentDisposition("inline; filename=\"membership.pdf\"")
                        .build(),
                RequestBody.fromBytes(pdfBytes));

        log.info("Draft PDF uploaded. participationId={}, key={}", participationId, s3Key);
        return s3Key;
    }

    // ─────────────────────────────────────────────────────────────
    // PRIVATE — Notifikasi (sistem existing)
    // ─────────────────────────────────────────────────────────────

    private void notifyDraftReady(ParticipatingProject participation) {
        // ⚠️ Sesuaikan dengan interface NotificationRepository/Service ASLI
        //     di codebase-mu — ini asumsi pola sederhana dari tabel `notification`
        try {
            notificationRepository.save(Notification.builder()
                    .title("入会届レビュー完了")
                    .message("入会届のレビューが完了し、書類が準備されました。")
                    .read(false)
                    // .participatingUserId(participation.getUser().getId())
                    .build());
        } catch (Exception e) {
            log.error("Notification failed. participationId={}, error={}",
                    participation.getId(), e.getMessage());
            // TODO: retry table
        }
    }

    // ─────────────────────────────────────────────────────────────
    // PRIVATE — Helpers
    // ─────────────────────────────────────────────────────────────

    private String safe(String v) { return v != null ? v : ""; }
    private String decimal(BigDecimal v) { return v != null ? v.toPlainString() : ""; }
    private String boolToMark(Boolean v) { return Boolean.TRUE.equals(v) ? "○" : "×"; }

    private String formatDate(LocalDate date) {
        return date != null ? date.format(JP_DATE_FORMAT) : "";
    }

    private String formatDate(LocalDateTime dateTime) {
        return dateTime != null ? dateTime.toLocalDate().format(JP_DATE_FORMAT) : "";
    }

    private String buildAddress(ParticipatingCompany company) {
        return String.format("%s%s%s%s",
                safe(company.getPrefecture()),
                safe(company.getMunicipality()),
                safe(company.getBlocknum()),
                safe(company.getBuildingName()));
    }
}