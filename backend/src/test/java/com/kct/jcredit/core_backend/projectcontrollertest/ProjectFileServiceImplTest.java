package com.kct.jcredit.core_backend.projectcontrollertest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URL;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.multipart.MultipartFile;

import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;

import com.kct.jcredit.core_backend.constant.ResponseConstants;
import com.kct.jcredit.core_backend.dto.response.ProjectFileDownloadResponse;
import com.kct.jcredit.core_backend.dto.response.ProjectFileListResponse;
import com.kct.jcredit.core_backend.dto.response.ProjectFileResponse;
import com.kct.jcredit.core_backend.dto.response.base.BaseResponse;
import com.kct.jcredit.core_backend.exception.BusinessRuleViolationException;
import com.kct.jcredit.core_backend.exception.FileTooLargeException;
import com.kct.jcredit.core_backend.exception.ResourceNotFoundException;
import com.kct.jcredit.core_backend.exception.UnsupportedFileTypeException;
import com.kct.jcredit.core_backend.repository.postgres.ProjectFilesRepository;
import com.kct.jcredit.core_backend.repository.postgres.ProjectRepository;
import com.kct.jcredit.core_backend.service.impl.ProjectFileServiceImpl;
import com.kct.jcredit.core_backend.entity.Project;
import com.kct.jcredit.core_backend.entity.ProjectFiles;
import com.kct.jcredit.core_backend.enums.ProjectFileType;
// Scope note: all four methods of this service are reachable from ProjectController,
// so the whole class is covered.
// NOTE: errors are reported by throwing here, not by a statusCode on the response, so the
// controller's ResponseUtil mapping only ever runs for the success path on these blocks.
@ExtendWith(MockitoExtension.class)
@DisplayName("ProjectFileServiceImpl のテスト")
class ProjectFileServiceImplTest {

    @Mock private ProjectRepository projectRepository;
    @Mock private ProjectFilesRepository projectFilesRepository;
    @Mock private S3Presigner s3Presigner;
    @Mock private S3Client s3Client;

    @InjectMocks
    private ProjectFileServiceImpl fileService;

    private static final String BUCKET = "gx-carbon-test";
    private static final String PROJECT_ID = "PUB-PJ-001";
    private static final Integer PROJECT_DB_ID = 7;
    private static final Integer OTHER_PROJECT_DB_ID = 99;
    private static final Integer FILE_ID = 77;
    private static final String USERNAME = "operator001";
    private static final String PDF = "application/pdf";
    private static final String XLSX =
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
    private static final ProjectFileType FILE_TYPE = ProjectFileType.values()[0];

    @BeforeEach
    void setUp() {
        // @Value fields are not populated outside the Spring context.
        ReflectionTestUtils.setField(fileService, "bucketName", BUCKET);
    }

    // ─────────────────────────────────────────────────────────────
    // Fixtures
    // ─────────────────────────────────────────────────────────────

    private void stubProjectLookup() {
        Project project = mock(Project.class);
        lenient().when(project.getId()).thenReturn(PROJECT_DB_ID);
        when(projectRepository.findByPublicIdWithFallback(PROJECT_ID))
                .thenReturn(Optional.of(project));
    }

    private MultipartFile validFile(String originalName) throws IOException {
        MultipartFile file = mock(MultipartFile.class);
        lenient().when(file.isEmpty()).thenReturn(false);
        lenient().when(file.getSize()).thenReturn(1024L);
        lenient().when(file.getContentType()).thenReturn(PDF);
        lenient().when(file.getOriginalFilename()).thenReturn(originalName);
        lenient().when(file.getInputStream())
                .thenReturn(new ByteArrayInputStream("content".getBytes()));
        return file;
    }

    private ProjectFiles fileEntity(Integer projectDbId, boolean active) {
        ProjectFiles entity = mock(ProjectFiles.class);
        lenient().when(entity.getId()).thenReturn(FILE_ID);
        lenient().when(entity.getProjectId()).thenReturn(projectDbId);
        lenient().when(entity.getIsActive()).thenReturn(active);
        lenient().when(entity.getS3Key()).thenReturn("project/7/files/type/file_abc_doc.pdf");
        lenient().when(entity.getOriginalFileName()).thenReturn("プロジェクト計画書.pdf");
        lenient().when(entity.getFileType()).thenReturn(FILE_TYPE);
        return entity;
    }

    // ==================================================================
    // 05. uploadFile
    // ==================================================================

    @Test
    @DisplayName("UT-PS-05-01: 正常 - ファイルがS3に保存され、DBに記録されOKを返す")
    void uploadFile_validFile_storesAndRecords() throws IOException {
        stubProjectLookup();
        MultipartFile file = validFile("plan.pdf");
        ProjectFiles savedEntity = fileEntity(PROJECT_DB_ID, true);
        when(projectFilesRepository.save(any(ProjectFiles.class))).thenReturn(savedEntity);

        ProjectFileResponse response =
                fileService.uploadFile(PROJECT_ID, FILE_TYPE, file, USERNAME);

        assertThat(response.getStatusCode()).isEqualTo(ResponseConstants.OK);
        verify(s3Client, times(1))
                .putObject(any(PutObjectRequest.class), any(RequestBody.class));
        verify(projectFilesRepository, times(1)).save(any(ProjectFiles.class));
    }

    @Test
    @DisplayName("UT-PS-05-02: 異常 - 空のファイルは拒否され、S3にもDBにも書き込まれない")
    void uploadFile_emptyFile_rejected() throws IOException {
        stubProjectLookup();
        MultipartFile file = mock(MultipartFile.class);
        when(file.isEmpty()).thenReturn(true);

        assertThrows(BusinessRuleViolationException.class,
                () -> fileService.uploadFile(PROJECT_ID, FILE_TYPE, file, USERNAME));

        verifyNoInteractions(s3Client);
        verify(projectFilesRepository, never()).save(any());
    }

    @Test
    @DisplayName("UT-PS-05-03: 異常 - 10MB超のファイルは拒否される（境界値）")
    void uploadFile_tooLarge_rejected() throws IOException {
        stubProjectLookup();
        MultipartFile file = validFile("plan.pdf");
        // The check is >, so exactly 10MB is accepted and one byte more is not.
        when(file.getSize()).thenReturn(10L * 1024 * 1024 + 1);

        assertThrows(FileTooLargeException.class,
                () -> fileService.uploadFile(PROJECT_ID, FILE_TYPE, file, USERNAME));

        verifyNoInteractions(s3Client);
    }

    @Test
    @DisplayName("UT-PS-05-04: 異常 - 許可されていない形式のファイルは拒否される")
    void uploadFile_unsupportedContentType_rejected() throws IOException {
        stubProjectLookup();
        MultipartFile file = validFile("plan.docx");
        when(file.getContentType())
                .thenReturn("application/vnd.openxmlformats-officedocument.wordprocessingml.document");

        assertThrows(UnsupportedFileTypeException.class,
                () -> fileService.uploadFile(PROJECT_ID, FILE_TYPE, file, USERNAME));

        verifyNoInteractions(s3Client);
    }

    @Test
    @DisplayName("UT-PS-05-05: 正常 - xlsxも許可形式として受け付けられる")
    void uploadFile_xlsxIsAccepted() throws IOException {
        // The J-Credit application packages (project plan, monitoring report) are .xlsx,
        // so the allow list is not PDF-only.
        stubProjectLookup();
        MultipartFile file = validFile("monitoring.xlsx");
        when(file.getContentType()).thenReturn(XLSX);
        ProjectFiles savedEntity = fileEntity(PROJECT_DB_ID, true);
        when(projectFilesRepository.save(any(ProjectFiles.class))).thenReturn(savedEntity);

        ProjectFileResponse response =
                fileService.uploadFile(PROJECT_ID, FILE_TYPE, file, USERNAME);

        assertThat(response.getStatusCode()).isEqualTo(ResponseConstants.OK);
    }

    @Test
    @DisplayName("UT-PS-05-06: 正常 - 日本語のファイル名は表示名として保持され、S3キーのみ英数字化される")
    void uploadFile_japaneseFileName_keptForDisplayAndSanitizedForKey() throws IOException {
        // The stored original name is what the user sees in the list and on download, so it
        // must stay readable; only the key needs an ASCII-safe segment.
        stubProjectLookup();
        MultipartFile file = validFile("プロジェクト計画書.pdf");
        ProjectFiles savedEntity = fileEntity(PROJECT_DB_ID, true);
        when(projectFilesRepository.save(any(ProjectFiles.class))).thenReturn(savedEntity);

        fileService.uploadFile(PROJECT_ID, FILE_TYPE, file, USERNAME);

        ArgumentCaptor<ProjectFiles> entityCaptor =
                ArgumentCaptor.forClass(ProjectFiles.class);
        verify(projectFilesRepository).save(entityCaptor.capture());
        assertThat(entityCaptor.getValue().getOriginalFileName())
                .isEqualTo("プロジェクト計画書.pdf");

        ArgumentCaptor<PutObjectRequest> s3Captor =
                ArgumentCaptor.forClass(PutObjectRequest.class);
        verify(s3Client).putObject(s3Captor.capture(), any(RequestBody.class));
        assertThat(s3Captor.getValue().key()).matches("[\\x20-\\x7E]+");
    }

    @Test
    @DisplayName("UT-PS-05-07: 異常 - DB保存に失敗した場合、S3に孤立したオブジェクトを残さないべき【要修正】")
    void uploadFile_dbSaveFails_shouldLeaveNoOrphanInS3() {
        // Pattern B: the S3 write happens before the row is persisted, and @Transactional
        // rolls back only the database. A failed save therefore leaves an object in S3 that
        // nothing points at - invisible, never cleaned up, and still billed.
        // EnR002FileUploadServiceImpl already solves this in the same codebase by writing to
        // a tmp key and promoting it in afterCommit; this service does not use that pattern.
        try {
            stubProjectLookup();
            MultipartFile file = validFile("plan.pdf");
            when(projectFilesRepository.save(any(ProjectFiles.class)))
                    .thenThrow(new RuntimeException("db down"));

            assertThrows(RuntimeException.class,
                    () -> fileService.uploadFile(PROJECT_ID, FILE_TYPE, file, USERNAME));

            verify(s3Client, never())
                    .putObject(any(PutObjectRequest.class), any(RequestBody.class));
        } catch (IOException e) {
            throw new AssertionError(e);
        }
    }

    @Test
    @DisplayName("UT-PS-05-08: 異常 - プロジェクトが存在しない場合、内部コードではなく日本語メッセージを返す")
    void uploadFile_projectNotFound_messageIsNotAStatusCode() throws IOException {
        when(projectRepository.findByPublicIdWithFallback(PROJECT_ID))
                .thenReturn(Optional.empty());
        MultipartFile file = validFile("plan.pdf");

        ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class,
                () -> fileService.uploadFile(PROJECT_ID, FILE_TYPE, file, USERNAME));

        // The message reaches the user, so it must not be the literal "NOT_FOUND".
        assertThat(ex.getMessage()).isEqualTo(ResponseConstants.MSG_PROJECT_NOT_FOUND);
        assertThat(ex.getMessage()).isNotEqualTo(ResponseConstants.NOT_FOUND);
    }

    // ==================================================================
    // 06. listFiles
    // ==================================================================

    @Test
    @DisplayName("UT-PS-06-01: 正常 - fileType指定ありの場合、種別で絞り込んだ検索が使われる")
    void listFiles_withFilter_usesFilteredQuery() {
        stubProjectLookup();
        ProjectFiles listed = fileEntity(PROJECT_DB_ID, true);
        when(projectFilesRepository
                .findByProjectIdAndFileTypeAndIsActiveTrueOrderByCreatedAtDesc(
                        PROJECT_DB_ID, FILE_TYPE))
                .thenReturn(List.of(listed));

        ProjectFileListResponse response = fileService.listFiles(PROJECT_ID, FILE_TYPE);

        assertThat(response.getStatusCode()).isEqualTo(ResponseConstants.OK);
        assertThat(response.getFiles()).hasSize(1);
        verify(projectFilesRepository, never())
                .findByProjectIdAndIsActiveTrueOrderByCreatedAtDesc(any());
    }

    @Test
    @DisplayName("UT-PS-06-02: 正常 - fileType未指定の場合、絞り込みなしの検索が使われる")
    void listFiles_withoutFilter_usesUnfilteredQuery() {
        stubProjectLookup();
        when(projectFilesRepository
                .findByProjectIdAndIsActiveTrueOrderByCreatedAtDesc(PROJECT_DB_ID))
                .thenReturn(List.of());

        ProjectFileListResponse response = fileService.listFiles(PROJECT_ID, null);

        assertThat(response.getStatusCode()).isEqualTo(ResponseConstants.OK);
        assertThat(response.getFiles()).isEmpty();
        verify(projectFilesRepository, never())
                .findByProjectIdAndFileTypeAndIsActiveTrueOrderByCreatedAtDesc(any(), any());
    }

    @Test
    @DisplayName("UT-PS-06-03: 異常 - プロジェクトが存在しない場合、ファイル検索は行われない")
    void listFiles_projectNotFound_doesNotQueryFiles() {
        when(projectRepository.findByPublicIdWithFallback(PROJECT_ID))
                .thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> fileService.listFiles(PROJECT_ID, null));

        verifyNoInteractions(projectFilesRepository);
    }

    // ==================================================================
    // 07. getDownloadUrl
    // ==================================================================

    @Test
    @DisplayName("UT-PS-07-01: 正常 - 設定されたバケット・キー・15分の有効期限でpresigned URLを生成する")
    void getDownloadUrl_buildsPresignRequestFromConfigAndKey() throws Exception {
        stubProjectLookup();
        ProjectFiles entity = fileEntity(PROJECT_DB_ID, true);
        when(projectFilesRepository.findById(FILE_ID)).thenReturn(Optional.of(entity));

        PresignedGetObjectRequest presigned = mock(PresignedGetObjectRequest.class);
        when(presigned.url()).thenReturn(new URL("https://s3.example.com/signed"));
        when(s3Presigner.presignGetObject(any(GetObjectPresignRequest.class)))
                .thenReturn(presigned);

        ProjectFileDownloadResponse response =
                fileService.getDownloadUrl(PROJECT_ID, FILE_ID);

        assertThat(response.getStatusCode()).isEqualTo(ResponseConstants.OK);
        assertThat(response.getDownloadUrl()).isEqualTo("https://s3.example.com/signed");
        assertThat(response.getOriginalFileName()).isEqualTo("プロジェクト計画書.pdf");

        ArgumentCaptor<GetObjectPresignRequest> captor =
                ArgumentCaptor.forClass(GetObjectPresignRequest.class);
        verify(s3Presigner).presignGetObject(captor.capture());
        assertThat(captor.getValue().signatureDuration()).isEqualTo(Duration.ofMinutes(15));
        assertThat(captor.getValue().getObjectRequest().bucket()).isEqualTo(BUCKET);
    }

    @Test
    @DisplayName("UT-PS-07-02: 異常 - ファイルが存在しない場合NOT_FOUNDをスローする")
    void getDownloadUrl_fileNotFound_throws() {
        stubProjectLookup();
        when(projectFilesRepository.findById(FILE_ID)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> fileService.getDownloadUrl(PROJECT_ID, FILE_ID));

        verifyNoInteractions(s3Presigner);
    }

    @Test
    @DisplayName("UT-PS-07-03: 異常 - 他プロジェクトのファイルIDを指定した場合NOT_FOUNDをスローする")
    void getDownloadUrl_fileFromAnotherProject_throws() {
        // The file id in the path is a plain integer, so without this check any project
        // could sign any other project's review documents.
        stubProjectLookup();
        ProjectFiles foreign = fileEntity(OTHER_PROJECT_DB_ID, true);
        when(projectFilesRepository.findById(FILE_ID)).thenReturn(Optional.of(foreign));

        assertThrows(ResourceNotFoundException.class,
                () -> fileService.getDownloadUrl(PROJECT_ID, FILE_ID));

        verifyNoInteractions(s3Presigner);
    }

    @Test
    @DisplayName("UT-PS-07-04: 異常 - 削除済みファイルはダウンロードURLを発行しない")
    void getDownloadUrl_softDeletedFile_throws() {
        stubProjectLookup();
        ProjectFiles deleted = fileEntity(PROJECT_DB_ID, false);
        when(projectFilesRepository.findById(FILE_ID)).thenReturn(Optional.of(deleted));

        assertThrows(ResourceNotFoundException.class,
                () -> fileService.getDownloadUrl(PROJECT_ID, FILE_ID));

        verifyNoInteractions(s3Presigner);
    }

    // ==================================================================
    // 08. deleteFile
    // ==================================================================

    @Test
    @DisplayName("UT-PS-08-01: 正常 - 論理削除され、削除者と削除日時が記録される")
    void deleteFile_valid_softDeletesWithActorAndTimestamp() {
        stubProjectLookup();
        ProjectFiles entity = fileEntity(PROJECT_DB_ID, true);
        when(projectFilesRepository.findById(FILE_ID)).thenReturn(Optional.of(entity));

        BaseResponse response = fileService.deleteFile(PROJECT_ID, FILE_ID, USERNAME);

        assertThat(response.getStatusCode()).isEqualTo(ResponseConstants.OK);
        assertThat(response.getMessage()).isEqualTo(ResponseConstants.MSG_FILE_DELETED);
        verify(entity).setIsActive(false);
        verify(entity).setDeletedBy(USERNAME);
        verify(entity).setDeletedAt(any(LocalDateTime.class));
        verify(projectFilesRepository).save(entity);
    }

    @Test
    @DisplayName("UT-PS-08-02: 異常 - ファイルが存在しない場合NOT_FOUNDをスローし保存しない")
    void deleteFile_notFound_throws() {
        stubProjectLookup();
        when(projectFilesRepository.findById(FILE_ID)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> fileService.deleteFile(PROJECT_ID, FILE_ID, USERNAME));

        verify(projectFilesRepository, never()).save(any());
    }

    @Test
    @DisplayName("UT-PS-08-03: 異常 - 他プロジェクトのファイルは削除できない")
    void deleteFile_fileFromAnotherProject_throws() {
        stubProjectLookup();
        ProjectFiles foreign = fileEntity(OTHER_PROJECT_DB_ID, true);
        when(projectFilesRepository.findById(FILE_ID)).thenReturn(Optional.of(foreign));

        assertThrows(ResourceNotFoundException.class,
                () -> fileService.deleteFile(PROJECT_ID, FILE_ID, USERNAME));

        verify(projectFilesRepository, never()).save(any());
    }

    @Test
    @DisplayName("UT-PS-08-04: 準正常 - S3のオブジェクトは削除されず残る（要確認）")
    void deleteFile_leavesS3ObjectInPlace() {
        // NOTE: deletion is logical only. For review documents that is defensible - the
        // audit trail outlives the row - but nothing ever removes the object, so S3 grows
        // without bound and there is no retention rule written down anywhere.
        stubProjectLookup();
        ProjectFiles active = fileEntity(PROJECT_DB_ID, true);
        when(projectFilesRepository.findById(FILE_ID)).thenReturn(Optional.of(active));

        fileService.deleteFile(PROJECT_ID, FILE_ID, USERNAME);

        verifyNoInteractions(s3Client);
    }
}