// package com.kct.jcredit.core_backend.service.project;

// import com.kct.jcredit.core_backend.constant.ResponseConstants;
// import com.kct.jcredit.core_backend.dto.response.FileUploadSolarResponse;
// import com.kct.jcredit.core_backend.entity.SiteR002EquipmentFile;
// import com.kct.jcredit.core_backend.exception.CustomException;
// import com.kct.jcredit.core_backend.service.impl.FileUploadSolarServiceImpl;
// import com.kct.jcredit.core_backend.repository.postgres.SiteR002EquipmentFileRepository;
// import org.junit.jupiter.api.BeforeEach;
// import org.junit.jupiter.api.DisplayName;
// import org.junit.jupiter.api.Test;
// import org.junit.jupiter.api.extension.ExtendWith;
// import org.mockito.InjectMocks;
// import org.mockito.Mock;
// import org.mockito.junit.jupiter.MockitoExtension;
// import org.springframework.mock.web.MockMultipartFile;
// import org.springframework.test.util.ReflectionTestUtils;
// import org.springframework.web.multipart.MultipartFile;
// import software.amazon.awssdk.core.sync.RequestBody;
// import software.amazon.awssdk.services.s3.S3Client;
// import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
// import software.amazon.awssdk.services.s3.model.PutObjectRequest;

// import java.io.IOException;
// import java.util.Optional;

// import static org.junit.jupiter.api.Assertions.*;
// import static org.mockito.ArgumentMatchers.any;
// import static org.mockito.ArgumentMatchers.anyString;
// import static org.mockito.ArgumentMatchers.eq;
// import static org.mockito.Mockito.*;

// @ExtendWith(MockitoExtension.class)
// @DisplayName("FileUploadSolarService - Unit Test")
// class FileUploadSolarTest {

//     @Mock
//     private S3Client s3Client;

//     @Mock
//     private SiteR002EquipmentFileRepository siteR002EquipmentFileRepository; 

//     @InjectMocks
//     private FileUploadSolarServiceImpl fileUploadService;

//     private final Integer SITE_ID = 123;
//     private final String USERNAME = "test_user";
//     private MockMultipartFile validPdfFile;
//     private SiteR002EquipmentFile mockEquipmentFile;

//     @BeforeEach
//     void setUp() {
//         ReflectionTestUtils.setField(fileUploadService, "bucketName", "test-bucket");

//         validPdfFile = new MockMultipartFile(
//                 "file",
//                 "document.pdf",
//                 "application/pdf",
//                 "dummy content".getBytes()
//         );

//         mockEquipmentFile = new SiteR002EquipmentFile();
//         mockEquipmentFile.setId(1);
//         mockEquipmentFile.setFieldReference("equipmentOpDate");
//         mockEquipmentFile.sets3Key("/projects/123/solar/cert_old.pdf"); 
//     }

//     // ==========================================================
//     // 1. SKENARIO NORMAL (HAPPY PATH)
//     // ==========================================================

//     @Test
//     @DisplayName("TC-FUS-01: Sukses upload file PDF (Update record yang sudah ada)")
//     void uploadEquipmentOpDate_success_existingRecord() {
//         // Given: 
//         when(siteR002EquipmentFileRepository.findByMemberDataIdAndFieldReference(eq(SITE_ID), anyString()))
//                 .thenReturn(Optional.of(mockEquipmentFile));

//         // When
//         FileUploadSolarResponse response = fileUploadService.uploadEquipmentOpDate(SITE_ID, validPdfFile, "Bukti Tanggal", USERNAME);

//         // Then
//         assertNotNull(response);
//         assertEquals(ResponseConstants.OK, response.getStatusCode());
//         assertEquals("EQUIPMENT_OP_DATE_EVIDENCE", response.getFile().getFileType());
        
//         // Memastikan S3 Client dipanggil untuk MENGHAPUS file lama, lalu UPLOAD file baru
//         verify(s3Client, times(1)).deleteObject(any(DeleteObjectRequest.class));
//         verify(s3Client, times(1)).putObject(any(PutObjectRequest.class), any(RequestBody.class));
//         verify(siteR002EquipmentFileRepository, times(1)).save(any(SiteR002EquipmentFile.class));
//     }

//     @Test
//     @DisplayName("TC-FUS-02: Sukses upload file PDF (Data benar-benar baru, belum ada di DB)")
//     void uploadEquipmentOpDate_success_newRecord() {
//         // Given: Optional.empty() 
//         when(siteR002EquipmentFileRepository.findByMemberDataIdAndFieldReference(eq(SITE_ID), anyString()))
//                 .thenReturn(Optional.empty());

//         // When
//         FileUploadSolarResponse response = fileUploadService.uploadEquipmentOpDate(SITE_ID, validPdfFile, "Bukti Tanggal Baru", USERNAME);

//         // Then
//         assertNotNull(response);
        
//         // check response content
//         verify(s3Client, never()).deleteObject(any(DeleteObjectRequest.class));
        
//         // check upload to S3 and save to DB
//         verify(s3Client, times(1)).putObject(any(PutObjectRequest.class), any(RequestBody.class));
//         verify(siteR002EquipmentFileRepository, times(1)).save(any(SiteR002EquipmentFile.class));
//     }

//     @Test
//     @DisplayName("TC-FUS-03: Memastikan semua method wrapper berjalan sukses")
//     void testAllWrapperMethods_success() {
//         when(siteR002EquipmentFileRepository.findByMemberDataIdAndFieldReference(eq(SITE_ID), anyString()))
//                 .thenReturn(Optional.of(mockEquipmentFile));

//         fileUploadService.uploadNominalMaxOutput(SITE_ID, validPdfFile, "Desc 1", USERNAME);
//         fileUploadService.uploadPowerCondStandby(SITE_ID, validPdfFile, "Desc 2", USERNAME);
//         fileUploadService.uploadBatteryInfo(SITE_ID, validPdfFile, "Desc 3", USERNAME);
//         fileUploadService.uploadTotalInvestment(SITE_ID, validPdfFile, "Desc 4", USERNAME);
//         fileUploadService.uploadSubsidyAmount(SITE_ID, validPdfFile, "Desc 5", USERNAME);
//         fileUploadService.uploadPreProjectUnitPrice(SITE_ID, validPdfFile, "Desc 6", USERNAME);
        
//         verify(s3Client, times(6)).putObject(any(PutObjectRequest.class), any(RequestBody.class));
//         verify(siteR002EquipmentFileRepository, times(6)).save(any(SiteR002EquipmentFile.class));
//     }

//     // ==========================================================
//     // 2. SKENARIO ERROR VALIDASI (UNHAPPY PATH)
//     // ==========================================================

//     @Test
//     @DisplayName("TC-FUS-04: Gagal karena file kosong atau null")
//     void validateFile_fails_whenFileIsEmpty() {
//         MockMultipartFile emptyFile = new MockMultipartFile("file", "empty.pdf", "application/pdf", new byte[0]);

//         CustomException exception = assertThrows(CustomException.class, () -> {
//             fileUploadService.uploadEquipmentOpDate(SITE_ID, emptyFile, "Desc", USERNAME);
//         });

//         assertEquals(ResponseConstants.VALIDATION_ERROR, exception.getErrorCode());
//     }

//     @Test
//     @DisplayName("TC-FUS-05: Gagal karena ukuran file melebihi 10MB")
//     void validateFile_fails_whenFileTooLarge() {
//         MultipartFile largeFile = mock(MultipartFile.class);
//         when(largeFile.isEmpty()).thenReturn(false);
//         when(largeFile.getSize()).thenReturn(11L * 1024 * 1024); 

//         CustomException exception = assertThrows(CustomException.class, () -> {
//             fileUploadService.uploadEquipmentOpDate(SITE_ID, largeFile, "Desc", USERNAME);
//         });

//         assertEquals(ResponseConstants.FILE_TOO_LARGE, exception.getErrorCode());
//     }

//     @Test
//     @DisplayName("TC-FUS-06: Gagal karena format file tidak diizinkan")
//     void validateFile_fails_whenContentTypeNotAllowed() {
//         MockMultipartFile txtFile = new MockMultipartFile(
//                 "file", "catatan.txt", "text/plain", "dummy text".getBytes()
//         );

//         CustomException exception = assertThrows(CustomException.class, () -> {
//             fileUploadService.uploadEquipmentOpDate(SITE_ID, txtFile, "Desc", USERNAME);
//         });

//         assertEquals(ResponseConstants.UNSUPPORTED_FILE_TYPE, exception.getErrorCode());
//     }

//     // ==========================================================
//     // 3. SKENARIO EDGE CASES & EXCEPTION HANDLING
//     // ==========================================================

//     @Test
//     @DisplayName("TC-FUS-07: Menggunakan fallback 'document.pdf' jika OriginalFilename Null")
//     void uploadFile_usesFallbackFilename_whenOriginalIsNull() {
//         MockMultipartFile noNameFile = new MockMultipartFile(
//                 "file",
//                 null, 
//                 "application/pdf",
//                 "dummy content".getBytes() 
//         );

//         when(siteR002EquipmentFileRepository.findByMemberDataIdAndFieldReference(eq(SITE_ID), anyString()))
//                 .thenReturn(Optional.of(mockEquipmentFile));

//         // When
//         FileUploadSolarResponse response = fileUploadService.uploadEquipmentOpDate(SITE_ID, noNameFile, "Desc", USERNAME);

//         // Then
//         assertNotNull(response);
//         assertTrue(response.getFile().getUrl().endsWith("_document.pdf"), "Nama file fallback tidak sesuai"); 
//     }

//     @Test
//     @DisplayName("TC-FUS-08: Sistem tidak crash meskipun gagal menghapus S3 lama (Warning Logs)")
//     void uploadFile_continues_evenIfDeleteOldS3Fails() {
//         // Given
//         when(siteR002EquipmentFileRepository.findByMemberDataIdAndFieldReference(eq(SITE_ID), anyString()))
//                 .thenReturn(Optional.of(mockEquipmentFile));

//         // aws simultion: deleteObject melempar exception
//         doThrow(new RuntimeException("AWS Error Simulasi"))
//                 .when(s3Client).deleteObject(any(DeleteObjectRequest.class));

//         // When: Proses harus tetap berlanjut dan mengembalikan HTTP 200 OK
//         FileUploadSolarResponse response = fileUploadService.uploadEquipmentOpDate(SITE_ID, validPdfFile, "Desc", USERNAME);

//         // Then
//         assertNotNull(response);
//         assertEquals(ResponseConstants.OK, response.getStatusCode());
//         verify(s3Client, times(1)).putObject(any(PutObjectRequest.class), any(RequestBody.class));
//     }

//     @Test
//     @DisplayName("TC-FUS-09: Gagal membaca isi file (IOException melempar CustomException 500)")
//     void uploadToS3_fails_throwsIOException() throws IOException {
//         // Given
//         MultipartFile ioErrorFile = mock(MultipartFile.class);
//         when(ioErrorFile.isEmpty()).thenReturn(false);
//         when(ioErrorFile.getSize()).thenReturn(1024L);
//         when(ioErrorFile.getContentType()).thenReturn("image/jpeg");
//         when(ioErrorFile.getOriginalFilename()).thenReturn("image.jpg");
//         when(ioErrorFile.getInputStream()).thenThrow(new IOException("Simulasi Gagal Membaca File"));

//         when(siteR002EquipmentFileRepository.findByMemberDataIdAndFieldReference(eq(SITE_ID), anyString()))
//                 .thenReturn(Optional.of(mockEquipmentFile));

//         // When & Then
//         CustomException exception = assertThrows(CustomException.class, () -> {
//             fileUploadService.uploadEquipmentOpDate(SITE_ID, ioErrorFile, "Desc", USERNAME);
//         });

//         assertEquals(ResponseConstants.INTERNAL_SERVER_ERROR, exception.getErrorCode());
//     }
// }