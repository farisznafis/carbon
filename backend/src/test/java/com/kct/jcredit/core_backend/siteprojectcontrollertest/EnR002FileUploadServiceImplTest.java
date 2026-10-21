package com.kct.jcredit.core_backend.siteprojectcontrollertest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.multipart.MultipartFile;

import com.kct.jcredit.core_backend.constant.ResponseConstants;
import com.kct.jcredit.core_backend.dto.response.EnR002FileResponse;
import com.kct.jcredit.core_backend.enums.EquipmentFileType;
import com.kct.jcredit.core_backend.exception.CustomException;
import com.kct.jcredit.core_backend.service.impl.EnR002FileUploadServiceImpl;
import com.kct.jcredit.core_backend.service.storage.S3FileStorage;

@ExtendWith(MockitoExtension.class)
@DisplayName("EnR002FileUploadServiceImpl のテスト")
class EnR002FileUploadServiceImplTest {

    @Mock
    private S3FileStorage storage;
    @Mock
    private MultipartFile multipartFile;

    @InjectMocks
    private EnR002FileUploadServiceImpl fileUploadService;

    private static final String SITE_ID = "PUB-SITE-001";
    private static final String UPLOADED_BY = "master001";
    private static final Integer EQUIPMENT_ID = 500;

    // ==================================================================
    // 01. uploadFile
    // ==================================================================

    @Test
    @DisplayName("UT-SPS-01-01: 正常 - ファイルがtmp領域に保存されOKとキー情報を返す")
    void uploadFile_validFile_storesToTempAndReturnsOk() {
        when(multipartFile.getOriginalFilename()).thenReturn("契約書.pdf");

        try (MockedStatic<S3FileStorage> mockedStatic = mockStatic(S3FileStorage.class)) {
            mockedStatic.when(() -> S3FileStorage.sanitizeFileName("契約書.pdf"))
                    .thenReturn("keiyakusho.pdf");

            EnR002FileResponse response = fileUploadService.uploadFile(
                    SITE_ID, EquipmentFileType.SOLAR_CONTRACT, UPLOADED_BY, multipartFile);

            assertThat(response.getStatusCode()).isEqualTo(ResponseConstants.OK);
            assertThat(response.getFileId()).isNotBlank();
            assertThat(response.getOriginalName()).isEqualTo("keiyakusho.pdf");
            // The key must land under the tmp root so that changeEquipment's
            // "tmp/equipments/" guard accepts it later.
            assertThat(response.getS3Key())
                    .contains("equipments/")
                    .endsWith("solar_contract_keiyakusho.pdf");

            // Validation must happen before anything is written.
            InOrder inOrder = inOrder(storage);
            inOrder.verify(storage).validate(multipartFile);
            inOrder.verify(storage).put(anyString(), eq(multipartFile), anyMap());
        }
    }

    @Test
    @DisplayName("UT-SPS-01-02: 正常 - S3メタデータにfileId・uploadedBy・fileType・siteIdが記録される")
    void uploadFile_recordsMetadata() {
        when(multipartFile.getOriginalFilename()).thenReturn("spec.pdf");

        try (MockedStatic<S3FileStorage> mockedStatic = mockStatic(S3FileStorage.class)) {
            mockedStatic.when(() -> S3FileStorage.sanitizeFileName("spec.pdf")).thenReturn("spec.pdf");

            EnR002FileResponse response = fileUploadService.uploadFile(
                    SITE_ID, EquipmentFileType.PCS_SPEC, UPLOADED_BY, multipartFile);

            @SuppressWarnings("unchecked")
            ArgumentCaptor<Map<String, String>> metaCaptor = ArgumentCaptor.forClass(Map.class);
            verify(storage).put(anyString(), eq(multipartFile), metaCaptor.capture());

            Map<String, String> meta = metaCaptor.getValue();
            assertThat(meta).containsEntry("uploadedBy", UPLOADED_BY)
                    .containsEntry("fileType", EquipmentFileType.PCS_SPEC.getValue())
                    .containsEntry("siteId", SITE_ID)
                    .containsEntry("fileId", response.getFileId());
        }
    }

    @Test
    @DisplayName("UT-SPS-01-03: 正常 - 呼び出しごとに異なるfileIdが払い出される")
    void uploadFile_generatesUniqueFileIdPerCall() {
        when(multipartFile.getOriginalFilename()).thenReturn("a.pdf");

        try (MockedStatic<S3FileStorage> mockedStatic = mockStatic(S3FileStorage.class)) {
            mockedStatic.when(() -> S3FileStorage.sanitizeFileName("a.pdf")).thenReturn("a.pdf");

            EnR002FileResponse first = fileUploadService.uploadFile(
                    SITE_ID, EquipmentFileType.PANEL_CATALOG, UPLOADED_BY, multipartFile);
            EnR002FileResponse second = fileUploadService.uploadFile(
                    SITE_ID, EquipmentFileType.PANEL_CATALOG, UPLOADED_BY, multipartFile);

            assertThat(first.getFileId()).isNotEqualTo(second.getFileId());
            assertThat(first.getS3Key()).isNotEqualTo(second.getS3Key());
        }
    }

    @Test
    @DisplayName("UT-SPS-01-04: 異常 - 検証で弾かれた場合S3への書き込みは行われない")
    void uploadFile_validationFails_doesNotWriteToS3() {
        doThrow(new CustomException(ResponseConstants.FILE_TOO_LARGE,
                ResponseConstants.MSG_FILE_TOO_LARGE))
                .when(storage).validate(multipartFile);

        assertThrows(CustomException.class, () -> fileUploadService.uploadFile(
                SITE_ID, EquipmentFileType.SOLAR_CONTRACT, UPLOADED_BY, multipartFile));

        verify(storage, never()).put(anyString(), any(), anyMap());
    }

    @Test
    @DisplayName("UT-SPS-01-05: 異常 - S3書き込みが失敗した場合、例外がそのまま伝播される")
    void uploadFile_storagePutThrows_propagatesException() {
        when(multipartFile.getOriginalFilename()).thenReturn("a.pdf");

        try (MockedStatic<S3FileStorage> mockedStatic = mockStatic(S3FileStorage.class)) {
            mockedStatic.when(() -> S3FileStorage.sanitizeFileName("a.pdf")).thenReturn("a.pdf");
            doThrow(new RuntimeException("S3 unavailable"))
                    .when(storage).put(anyString(), eq(multipartFile), anyMap());

            assertThrows(RuntimeException.class, () -> fileUploadService.uploadFile(
                    SITE_ID, EquipmentFileType.SOLAR_CONTRACT, UPLOADED_BY, multipartFile));
        }
    }

    @Test
    @DisplayName("UT-SPS-01-06: 準正常 - siteIdの所有者検証が行われない（アーキテクチャ上の注記）")
    void uploadFile_noSiteOwnershipCheck() {
        // NOTE: siteId is only written into the S3 metadata and the log; it does not
        // affect the tmp key (tmp/equipments/{uuid}/...) and is never checked against
        // the uploader's company. Any master/user can upload against any siteId.
        // Recorded as the current design; enforcement would have to be added here
        // or at the controller (which currently extracts only the username).
        when(multipartFile.getOriginalFilename()).thenReturn("a.pdf");

        try (MockedStatic<S3FileStorage> mockedStatic = mockStatic(S3FileStorage.class)) {
            mockedStatic.when(() -> S3FileStorage.sanitizeFileName("a.pdf")).thenReturn("a.pdf");

            EnR002FileResponse response = fileUploadService.uploadFile(
                    "PUB-SITE-OTHER-COMPANY", EquipmentFileType.SOLAR_CONTRACT,
                    UPLOADED_BY, multipartFile);

            assertThat(response.getStatusCode()).isEqualTo(ResponseConstants.OK);
            assertThat(response.getS3Key()).doesNotContain("PUB-SITE-OTHER-COMPANY");
        }
    }

    // ==================================================================
    // 10. buildPermanentKey / moveToPermanent / deletePermanentFile
    // ==================================================================

    @Test
    @DisplayName("UT-SPS-10-01: 正常 - tmpキーから拠点・設備配下の恒久キーが組み立てられる")
    void buildPermanentKey_fromTempKey_buildsSitePath() {
        String tempKey = S3FileStorage.TEMP_ROOT + "equipments/abc-123/solar_contract_doc.pdf";

        String permanentKey = fileUploadService.buildPermanentKey(tempKey, SITE_ID, EQUIPMENT_ID);

        assertThat(permanentKey)
                .isEqualTo("sites/" + SITE_ID + "/equipments/" + EQUIPMENT_ID
                        + "/solar_contract_doc.pdf");
    }

    @Test
    @DisplayName("UT-SPS-10-02: 準正常 - tmp形式でないキーを渡すと元のキーが丸ごと埋め込まれる（要確認）")
    void buildPermanentKey_nonTempKey_embedsWholeKey() {
        // NOTE: replaceAll only strips the tmp prefix pattern. A key that is already
        // permanent (or arbitrary) passes through untouched and is nested inside the
        // new path. submitParticipation calls this with client-supplied s3Key values
        // without checking they are tmp keys, unlike changeEquipment which rejects them
        // with INVALID_FILE_REFERENCE.
        String alreadyPermanent = "sites/OTHER/equipments/9/doc.pdf";

        String permanentKey = fileUploadService.buildPermanentKey(
                alreadyPermanent, SITE_ID, EQUIPMENT_ID);

        assertThat(permanentKey)
                .isEqualTo("sites/" + SITE_ID + "/equipments/" + EQUIPMENT_ID
                        + "/sites/OTHER/equipments/9/doc.pdf");
    }

    @Test
    @DisplayName("UT-SPS-10-03: 正常 - moveToPermanentはストレージのpromoteに委譲される")
    void moveToPermanent_delegatesToStoragePromote() {
        fileUploadService.moveToPermanent("tmp/key", "permanent/key");

        verify(storage, times(1)).promote("tmp/key", "permanent/key");
    }

    @Test
    @DisplayName("UT-SPS-10-04: 正常 - deletePermanentFileは有効なキーのみ削除する")
    void deletePermanentFile_deletesOnlyForValidKey() {
        fileUploadService.deletePermanentFile("sites/S/equipments/1/doc.pdf", EQUIPMENT_ID);
        verify(storage, times(1)).delete("sites/S/equipments/1/doc.pdf");
    }

    @Test
    @DisplayName("UT-SPS-10-05: 準正常 - deletePermanentFileはnull・空白キーでは削除を行わない")
    void deletePermanentFile_skipsNullOrBlankKey() {
        fileUploadService.deletePermanentFile(null, EQUIPMENT_ID);
        fileUploadService.deletePermanentFile("   ", EQUIPMENT_ID);

        verify(storage, never()).delete(anyString());
    }
}
