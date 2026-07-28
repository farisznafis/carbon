package com.kct.jcredit.core_backend.service.project;

import com.kct.jcredit.core_backend.dto.request.ProjectUploadDocumentRequest;
import com.kct.jcredit.core_backend.dto.response.ProjectUploadDocumentResponse;
import com.kct.jcredit.core_backend.entity.Project;
import com.kct.jcredit.core_backend.entity.ProjectDocumentStatus;
import com.kct.jcredit.core_backend.entity.ProjectFiles;
import com.kct.jcredit.core_backend.repository.dynamodb.ProjectDocumentStatusRepository;
import com.kct.jcredit.core_backend.repository.postgres.ProjectFilesRepository;
import com.kct.jcredit.core_backend.repository.postgres.ProjectRepository;
import com.kct.jcredit.core_backend.service.impl.ProjectUploadDocumentServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("ProjectUploadDocumentService - Unit Tests")
class ProjectUploadDocumentTest {

    @Mock
    private ProjectRepository projectRepository;

    @Mock
    private ProjectFilesRepository projectFilesRepository;

    @Mock
    private ProjectDocumentStatusRepository projectDocumentStatusRepository;

    @Mock
    private S3Client s3Client;

    @InjectMocks
    private ProjectUploadDocumentServiceImpl uploadService;

    private final Integer PROJECT_ID = 123;
    private final String PROJECT_ID_STR = "123";
    private Project dummyProject;
    private ProjectFiles dummyProjectFiles;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(uploadService, "BUCKET_NAME", "dummy-s3-bucket");

        dummyProject = new Project();
        dummyProject.setId(PROJECT_ID);

        dummyProjectFiles = new ProjectFiles();
        dummyProjectFiles.setId(PROJECT_ID);
        dummyProjectFiles.setCertificateRegistration("s3://dummy/cert.pdf");
        dummyProjectFiles.setReportValidation("s3://dummy/val.pdf");
        dummyProjectFiles.setReportVerification("s3://dummy/ver.pdf");
    }

    // ==========================================================
    // 1. Test: Process Upload (S3)
    // ==========================================================
    @Test
    @DisplayName("TC-UPLOAD-01: Success upload all 3 documents to S3")
    void processUpload_success() {
        // Given
        when(projectRepository.findById(PROJECT_ID)).thenReturn(Optional.of(dummyProject));
        when(projectFilesRepository.findById(PROJECT_ID)).thenReturn(Optional.of(dummyProjectFiles));

        // Mock AWS S3 client agar tidak beneran upload ke internet
        when(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
                .thenReturn(PutObjectResponse.builder().build());

        // Buat file dummy PDF
        MultipartFile certFile = new MockMultipartFile("fileRegistrationCert", "cert.pdf", "application/pdf", "dummy data 1".getBytes());
        MultipartFile valFile = new MockMultipartFile("fileValidationReport", "val.pdf", "application/pdf", "dummy data 2".getBytes());
        MultipartFile verFile = new MockMultipartFile("fileVerificationReport", "ver.pdf", "application/pdf", "dummy data 3".getBytes());

        // When
        ProjectUploadDocumentResponse response = uploadService.processUpload(PROJECT_ID_STR, certFile, valFile, verFile, any());

        // Then
        assertNotNull(response);
        assertEquals("200", response.getStatusCode());
        assertEquals("ファイルのアップロードが完了しました (Upload File Selesai)", response.getMessage());

        // Verifikasi bahwa S3Client dipanggil persis 3 kali (karena ada 3 file)
        verify(s3Client, times(3)).putObject(any(PutObjectRequest.class), any(RequestBody.class));
        verify(projectFilesRepository, times(1)).save(any(ProjectFiles.class));
    }

    @Test
    @DisplayName("TC-UPLOAD-02: Throw exception when project not found")
    void processUpload_projectNotFound() {
        // Given
        when(projectRepository.findById(PROJECT_ID)).thenReturn(Optional.empty());

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            uploadService.processUpload(PROJECT_ID_STR, null, null, null, any());
        });

        assertEquals("エラー: プロジェクトが見つかりません (Project Not Found)", exception.getMessage());
        
        verify(s3Client, never()).putObject(any(PutObjectRequest.class), any(RequestBody.class));
    }

    // ==========================================================
    // 2. Test: Get Documents
    // ==========================================================
    @Test
    @DisplayName("TC-GETDOC-01: Success get document URLs")
    void getDocumentsByProjectId_success() {
        // Given
        when(projectFilesRepository.findById(PROJECT_ID)).thenReturn(Optional.of(dummyProjectFiles));

        // When
        ProjectUploadDocumentResponse response = uploadService.getDocumentsByProjectId(PROJECT_ID_STR);

        // Then
        assertNotNull(response);
        assertEquals("200", response.getStatusCode());
        assertEquals("s3://dummy/cert.pdf", response.getRegistrationCertUrl());
        assertEquals("s3://dummy/val.pdf", response.getValidationReportUrl());
        assertEquals("s3://dummy/ver.pdf", response.getVerificationReportUrl());
    }

    // ==========================================================
    // 3. Test: Update Status
    // ==========================================================
    @Test
    @DisplayName("TC-STATUS-01: Success update document receive status")
    void updateStatus_success() {
        // Given
        ProjectDocumentStatus dummyStatus = new ProjectDocumentStatus();
        dummyStatus.setProjectId(PROJECT_ID_STR);
        
        when(projectDocumentStatusRepository.findById(PROJECT_ID_STR)).thenReturn(Optional.of(dummyStatus));

        ProjectUploadDocumentRequest request = new ProjectUploadDocumentRequest();
        request.setIsRegistrationCertReceived(true);
        request.setIsValidationReportReceived(false);

        // When
        ProjectUploadDocumentResponse response = uploadService.updateStatus(PROJECT_ID_STR, request);

        // Then
        assertNotNull(response);
        assertEquals("200", response.getStatusCode());

        verify(projectDocumentStatusRepository, times(1)).save(argThat(status -> 
            Boolean.TRUE.equals(status.getIsRegistrationCertReceived()) &&
            Boolean.FALSE.equals(status.getIsValidationReportReceived())
        ));
    }

    @Test
    @DisplayName("TC-UPLOAD-03: Partial upload (only 1 file) success")
    void processUpload_partialUpload_success() {
        // Given: Anggap projectFiles sudah ada
        when(projectRepository.findById(PROJECT_ID)).thenReturn(Optional.of(dummyProject));
        when(projectFilesRepository.findById(PROJECT_ID)).thenReturn(Optional.of(dummyProjectFiles));
        
        when(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
                .thenReturn(PutObjectResponse.builder().build());

        MultipartFile certFile = new MockMultipartFile("fileRegistrationCert", "cert.pdf", "application/pdf", "dummy".getBytes());

        // When
        ProjectUploadDocumentResponse response = uploadService.processUpload(PROJECT_ID_STR, certFile, null, null, any());

        // Then
        assertEquals("200", response.getStatusCode());
        
        // verification just 1 file uploaded
        verify(s3Client, times(1)).putObject(any(PutObjectRequest.class), any(RequestBody.class));
    }

    @Test
    @DisplayName("TC-UPLOAD-04: S3 throws IOException (simulate error)")
    void processUpload_s3IOException_throwsRuntimeException() {
        when(projectRepository.findById(PROJECT_ID)).thenReturn(Optional.of(dummyProject));
        when(projectFilesRepository.findById(PROJECT_ID)).thenReturn(Optional.of(dummyProjectFiles));

        // simulation: AWS S3 client throws IOException 
        when(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
                .thenThrow(new RuntimeException(new java.io.IOException("Simulated S3 Network Error")));

        MultipartFile certFile = new MockMultipartFile("fileRegistrationCert", "cert.pdf", "application/pdf", "dummy".getBytes());

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            uploadService.processUpload(PROJECT_ID_STR, certFile, null, null, any());
        });

        assertEquals("S3アップロードに失敗しました", exception.getMessage());
    }

    // ==========================================================
    // Get Documents
    // ==========================================================

    @Test
    @DisplayName("TC-GETDOC-02: Returns 200 with null URLs when files not exist")
    void getDocumentsByProjectId_filesNotExist() {
        // Given
        when(projectFilesRepository.findById(PROJECT_ID)).thenReturn(Optional.empty());

        // When
        ProjectUploadDocumentResponse response = uploadService.getDocumentsByProjectId(PROJECT_ID_STR);

        // Then
        assertEquals("200", response.getStatusCode());
        assertNull(response.getRegistrationCertUrl());
        assertNull(response.getValidationReportUrl());
        assertNull(response.getVerificationReportUrl());
    }

    @Test
    @DisplayName("TC-STATUS-02: Creates new status when not exist in DB")
    void updateStatus_createsNew_whenNotExist() {
        // Given
        when(projectDocumentStatusRepository.findById(PROJECT_ID_STR)).thenReturn(Optional.empty());

        ProjectUploadDocumentRequest request = new ProjectUploadDocumentRequest();
        request.setIsRegistrationCertReceived(true);

        // When
        ProjectUploadDocumentResponse response = uploadService.updateStatus(PROJECT_ID_STR, request);

        // Then
        assertEquals("200", response.getStatusCode());

        verify(projectDocumentStatusRepository, times(1)).save(argThat(status -> 
            status.getProjectId().equals(PROJECT_ID_STR) &&
            Boolean.TRUE.equals(status.getIsRegistrationCertReceived())
        ));
    }
}
