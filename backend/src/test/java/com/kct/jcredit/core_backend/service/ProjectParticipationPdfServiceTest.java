// package com.kct.jcredit.core_backend.service;

// import com.kct.jcredit.core_backend.service.impl.*;
// import com.kct.jcredit.core_backend.entity.ParticipatingCompany;
// import com.kct.jcredit.core_backend.entity.ParticipatingProject;
// import com.kct.jcredit.core_backend.entity.ProjectParticipationConfirmation;
// import com.kct.jcredit.core_backend.entity.Project;
// import com.kct.jcredit.core_backend.entity.Site;
// import com.kct.jcredit.core_backend.entity.EnR002SustainabilityCommitment;
// import com.kct.jcredit.core_backend.entity.Notification;
// import com.kct.jcredit.core_backend.entity.EnR002ApplicationConfirmation;
// import com.kct.jcredit.core_backend.entity.EnR002Equipment;
// import com.kct.jcredit.core_backend.repository.dynamodb.EnR002ApplicationConfirmationRepository;
// import com.kct.jcredit.core_backend.repository.dynamodb.EnR002SustainabilityCommitmentRepository;
// import com.kct.jcredit.core_backend.repository.dynamodb.ProjectParticipationConfirmationRepository;
// import com.kct.jcredit.core_backend.repository.postgres.NotificationRepository;
// import com.kct.jcredit.core_backend.repository.postgres.ParticipatingProjectRepository;

// import java.math.BigDecimal;
// import java.time.LocalDate;
// import java.time.LocalDateTime;
// import java.nio.file.Path;

// import org.junit.jupiter.api.BeforeEach;
// import org.junit.jupiter.api.Test;
// import org.junit.jupiter.api.io.TempDir;
// import org.mockito.*;
// import software.amazon.awssdk.services.s3.presigner.S3Presigner;
// import software.amazon.awssdk.services.s3.S3Client;

// import static org.junit.jupiter.api.Assertions.*;
// import static org.mockito.ArgumentMatchers.*;
// import static org.mockito.Mockito.*;

// class ProjectParticipationPdfServiceTest {

//     @Mock private S3Client s3Client;
//     @Mock private S3Presigner s3Presigner;
//     @Mock private ParticipatingProjectRepository participatingProjectRepository;
//     @Mock private EnR002ApplicationConfirmationRepository confirmationItemsRepository;
//     @Mock private EnR002SustainabilityCommitmentRepository sustainabilityRepository;
//     @Mock private ProjectParticipationConfirmationRepository reviewRepository;
//     @Mock private NotificationRepository notificationRepository;

//     private ParticipationPdfServiceImpl service;

//     @TempDir
//     Path tempDir; // 生成したPDFを一時的に保存する場所

//     @BeforeEach
//     void setUp() {
//         MockitoAnnotations.openMocks(this);
//         service = new ParticipationPdfServiceImpl(
//                 s3Client, s3Presigner,
//                 participatingProjectRepository,
//                 confirmationItemsRepository,
//                 sustainabilityRepository,
//                 reviewRepository,
//                 notificationRepository
//         );
//         // @Value のフィールドはリフレクションで注入
//         setField(service, "bucketName", "test-bucket");
//         setField(service, "presignExpirySeconds", 900);
//     }

//     @Test
//     void generateAndProcess_실제docx置換からPDF生成まで検証() throws Exception {
//         Integer participationId = 1;

//         // ── ダミーデータ組み立て ─────────────────────────────
//         Project project = mock(Project.class);
//         when(project.getProjectName()).thenReturn("テスト太陽光プログラム");
//         when(project.getMethodName()).thenReturn("再エネ利用促進事業");
//         when(project.getMethodDetails()).thenReturn("会員規約に基づく利用目的テスト");
//         when(project.getId()).thenReturn(100);

//         ParticipatingCompany company = mock(ParticipatingCompany.class);
//         when(company.getNameJa()).thenReturn("テスト株式会社");
//         when(company.getNameFurigana()).thenReturn("テストカブシキガイシャ");
//         when(company.getPostalCode()).thenReturn("123-4567");
//         when(company.getPrefecture()).thenReturn("東京都");
//         when(company.getMunicipality()).thenReturn("千代田区");
//         when(company.getBlocknum()).thenReturn("1-1-1");
//         when(company.getBuildingName()).thenReturn("テストビル5F");
//         when(company.getPhoneNumber()).thenReturn("03-1234-5678");
//         when(company.getFax()).thenReturn("03-1234-5679");
//         when(company.getEmail()).thenReturn("test@example.com");

//         Site site = mock(Site.class);
//         when(site.getCompany()).thenReturn(company);
//         when(site.getId()).thenReturn(200);

//         EnR002Equipment equipment = mock(EnR002Equipment.class);
//         when(equipment.getSite()).thenReturn(site);
//         when(equipment.getEquipmentOpDate()).thenReturn(LocalDate.of(2025, 4, 1));
//         when(equipment.getNominalMaxOutput()).thenReturn(new BigDecimal("100.5"));
//         when(equipment.getPcsManufacturer()).thenReturn("テストメーカー");
//         when(equipment.getPcsModel()).thenReturn("TM-1000");
//         when(equipment.getPcsUnitCount()).thenReturn(3);
//         when(equipment.getDeviceNo()).thenReturn("DVC-0001");
//         when(equipment.getPcsStandby()).thenReturn(new BigDecimal("5.0"));
//         when(equipment.getHasBattery()).thenReturn(true);
//         when(equipment.getBatteryMaker()).thenReturn("バッテリーメーカー");
//         when(equipment.getBatteryModel()).thenReturn("BM-500");
//         when(equipment.getBatteryCapacity()).thenReturn(new BigDecimal("50.0"));
//         when(equipment.getTotalInvestment()).thenReturn(10_000_000);
//         when(equipment.getSubsidyName()).thenReturn("テスト補助金");
//         when(equipment.getSubsidyAmount()).thenReturn(new BigDecimal("2000000"));
//         when(equipment.getPreProjectUnitPrice()).thenReturn(new BigDecimal("15.5"));

//         ParticipatingProject participation = mock(ParticipatingProject.class);
//         when(participation.getEquipment()).thenReturn(equipment);
//         when(participation.getProject()).thenReturn(project);
//         when(participation.getCreatedAt()).thenReturn(LocalDateTime.of(2025, 6, 1, 10, 0));
//         when(participation.getId()).thenReturn(participationId);

//         EnR002ApplicationConfirmation confirmation = mock(EnR002ApplicationConfirmation.class);
//         when(confirmation.getIsAgreeDataUse()).thenReturn(true);
//         when(confirmation.getIsAgreeExtraData()).thenReturn(true);
//         when(confirmation.getIsAgreeTransfer()).thenReturn(false);
//         when(confirmation.getIsNotRegistered()).thenReturn(true);
//         when(confirmation.getIsKeidanrenParticipation()).thenReturn(true);
//         when(confirmation.getIsEmitter()).thenReturn(true);
//         when(confirmation.getIsAdjustedEmissionsAdded()).thenReturn(false);
//         when(confirmation.getEmitterCode()).thenReturn("123456789");
//         when(confirmation.getIsSubjectToPeriodicReport()).thenReturn(true);
//         when(confirmation.getIsPeriodicReportTypeOne()).thenReturn(true);
//         when(confirmation.getEnergyBusinessCode()).thenReturn("1234567");
//         when(confirmation.getFactoryCode()).thenReturn("7654321");

//         EnR002SustainabilityCommitment sustainability = mock(EnR002SustainabilityCommitment.class);
//         when(sustainability.getIsAgreeTerms()).thenReturn(true);
//         when(sustainability.getCompanyName()).thenReturn("テスト株式会社");
//         when(sustainability.getRepRole()).thenReturn("代表取締役");
//         when(sustainability.getRepName()).thenReturn("山田太郎");

//         ProjectParticipationConfirmation review = mock(ProjectParticipationConfirmation.class);
//         when(review.getIsSolarContractDoc()).thenReturn(true);
//         when(review.getIsPcsSpecDoc()).thenReturn(true);
//         when(review.getIsBatterySpecDoc()).thenReturn(true);
//         when(review.getIsOperationStartDoc()).thenReturn(true);
//         when(review.getIsGenerationDataDoc()).thenReturn(true);
//         when(review.getIsReversePowerDoc()).thenReturn(false);
//         when(review.getIsInvestmentProofDoc()).thenReturn(true);
//         when(review.getIsApplicationCompleteCheck()).thenReturn(true);
//         when(review.getIsFormsAttachCheck()).thenReturn(true);
//         when(review.getIsEmissionsReductionActivityListCheck()).thenReturn(true);
//         when(review.getIsOriginalSpecCheck()).thenReturn(true);
//         when(review.getIsSubsidyCheck()).thenReturn(true);
//         when(review.getPrimaryApplReviewerName()).thenReturn("確認者A");
//         when(review.getPrimaryApplReviewerInkan()).thenReturn("印A");
//         when(review.getSecondaryApplReviewerName()).thenReturn("確認者B");
//         when(review.getSecondaryApplReviewerInkan()).thenReturn("印B");

//         // ── Repositoryモック設定 ─────────────────────────────
//         when(participatingProjectRepository.findById(participationId))
//                 .thenReturn(java.util.Optional.of(participation));
//         when(confirmationItemsRepository.findByParticipationId(participationId))
//                 .thenReturn(java.util.Optional.of(confirmation));
//         when(sustainabilityRepository.findByParticipationId(participationId))
//                 .thenReturn(java.util.Optional.of(sustainability));
//         when(reviewRepository.findByParticipationId(participationId))
//                 .thenReturn(java.util.Optional.of(review));

//         // S3アップロードはキャプチャして中身をローカルに保存する
//         ArgumentCaptor<software.amazon.awssdk.core.sync.RequestBody> bodyCaptor =
//                 ArgumentCaptor.forClass(software.amazon.awssdk.core.sync.RequestBody.class);

//         doAnswer(invocation -> null)
//                 .when(s3Client)
//                 .putObject(any(software.amazon.awssdk.services.s3.model.PutObjectRequest.class), bodyCaptor.capture());

//         // ── 実行 ─────────────────────────────────────────
//         assertDoesNotThrow(() -> service.generateAndProcess(participationId));

//         // ── 検証 ─────────────────────────────────────────
//         verify(participatingProjectRepository).updatePdfS3Key(eq(participationId), anyString());
//         verify(notificationRepository).save(any(Notification.class));

//         // 実際に生成されたPDFをファイルに書き出して目視確認可能にする
//         Path outputPdf = tempDir.resolve("output_test.pdf");
//         // bodyCaptorから実際のバイト列を取り出す場合はRequestBody内部APIの制約があるため、
//         // 別途convertToPdf結果を直接確認したい場合はサービス側にpackage-privateメソッドを
//         // 用意してテストから直接呼ぶ方法を推奨（下記の代替案参照）。

//         System.out.println("PDF生成テスト完了。出力先候補: " + outputPdf.toAbsolutePath());
//     }

//     // リフレクションで@Valueフィールドをセットするヘルパー
//     private void setField(Object target, String fieldName, Object value) {
//         try {
//             var field = target.getClass().getDeclaredField(fieldName);
//             field.setAccessible(true);
//             field.set(target, value);
//         } catch (Exception e) {
//             throw new RuntimeException(e);
//         }
//     }
// }
