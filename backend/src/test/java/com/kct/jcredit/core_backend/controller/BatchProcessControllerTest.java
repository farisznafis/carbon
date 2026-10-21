//USE SHANAZ
package com.kct.jcredit.core_backend.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

import com.kct.jcredit.core_backend.repository.postgres.ProjectRepository;
import com.kct.jcredit.core_backend.dto.response.MonitoringRecordSolarResponse;
import com.kct.jcredit.core_backend.dto.response.SiteReductionResultResponse;
import com.kct.jcredit.core_backend.dto.response.ProjectReductionResultResponse;
import com.kct.jcredit.core_backend.dto.response.BatchProcessHistoryResponse;
import com.kct.jcredit.core_backend.service.batch.GetBatchService;
import com.kct.jcredit.core_backend.entity.Project;
import com.kct.jcredit.core_backend.controller.BatchProcessController;

@ExtendWith(MockitoExtension.class)
@DisplayName("BatchController のテスト")
class BatchProcessControllerTest {

    @Mock
    private GetBatchService batchService;

    @Mock
    private ProjectRepository projectRepository;

    @InjectMocks
    private BatchProcessController batchController;

    // ============================================================================
    // batchProcessHistory
    // ============================================================================
    @Nested
    @DisplayName("batchProcessHistory のテスト")
    class BatchProcessHistoryTest {

        @Nested
        @DisplayName("【正常系】")
        class SuccessCase {

            @Test
            @DisplayName("UT-BC-101: デフォルトの page/limit で nextPageToken なしの場合、200OK レスポンスを返す")
            void batchProcessHistory_defaultParamsNoToken_returnsOkResponse() {
                // Arrange
                Integer page = 1;
                Integer limit = 20;
                String nextPageToken = null;

                BatchProcessHistoryResponse mockResponse = new BatchProcessHistoryResponse();
                // TODO: set field mockResponse sesuai struktur asli (list histori, nextPageToken, dll)

                when(batchService.getBatchProcessHistory(page, limit, nextPageToken))
                        .thenReturn(mockResponse);

                // Act
                ResponseEntity<BatchProcessHistoryResponse> response =
                        batchController.BatchProcessHistory(page, limit, nextPageToken);

                // Assert
                assertThat(response).isNotNull();
                assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
                assertThat(response.getBody()).isEqualTo(mockResponse);

                verify(batchService, times(1)).getBatchProcessHistory(page, limit, nextPageToken);
            }

            @Test
            @DisplayName("UT-BC-102: nextPageToken が指定された場合（2ページ目以降）、正しく引き渡される")
            void batchProcessHistory_withNextPageToken_passesTokenCorrectly() {
                // Arrange
                Integer page = 2;
                Integer limit = 10;
                String nextPageToken = "eyJsYXN0SWQiOjEwMH0="; // contoh token pagination

                BatchProcessHistoryResponse mockResponse = new BatchProcessHistoryResponse();

                when(batchService.getBatchProcessHistory(page, limit, nextPageToken))
                        .thenReturn(mockResponse);

                // Act
                ResponseEntity<BatchProcessHistoryResponse> response =
                        batchController.BatchProcessHistory(page, limit, nextPageToken);

                // Assert
                assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
                assertThat(response.getBody()).isEqualTo(mockResponse);

                // Memastikan nextPageToken diteruskan APA ADANYA ke service, bukan di-drop/dimodifikasi
                verify(batchService, times(1)).getBatchProcessHistory(page, limit, nextPageToken);
            }
        }

        @Nested
        @DisplayName("【異常系】")
        class ErrorCase {

            @Test
            @DisplayName("UT-BC-103: サービスが例外をスローする場合、例外が伝播される")
            void batchProcessHistory_serviceThrowsException_propagatesException() {
                // Arrange
                Integer page = 1;
                Integer limit = 20;
                String nextPageToken = null;

                RuntimeException expectedException = new RuntimeException("バッチ履歴の取得に失敗しました");

                when(batchService.getBatchProcessHistory(page, limit, nextPageToken))
                        .thenThrow(expectedException);

                // Act & Assert
                RuntimeException thrown = assertThrows(
                        RuntimeException.class,
                        () -> batchController.BatchProcessHistory(page, limit, nextPageToken)
                );

                assertThat(thrown.getMessage()).isEqualTo("バッチ履歴の取得に失敗しました");
                verify(batchService, times(1)).getBatchProcessHistory(page, limit, nextPageToken);
            }
        }
    }

    // ============================================================================
    // siteReductionResult
    // ============================================================================
    @Nested
    @DisplayName("siteReductionResult のテスト")
    class SiteReductionResultTest {

        @Nested
        @DisplayName("【正常系】")
        class SuccessCase {

            @Test
            @DisplayName("UT-BC-201: projectId が解決できる場合、正しい projDbId で batchService を呼び出し 200OK を返す")
            void siteReductionResult_projectFound_returnsOkResponse() {
                // Arrange
                String siteId = "SITE001";
                String projectId = "PROJ-PUBLIC-001";
                Integer projDbId = 101;

                Project mockProject = mock(Project.class);
                when(mockProject.getId()).thenReturn(projDbId);

                SiteReductionResultResponse mockResponse = new SiteReductionResultResponse();

                when(projectRepository.findByPublicIdWithFallback(projectId))
                        .thenReturn(Optional.of(mockProject));
                when(batchService.getSiteReductionResult(siteId, projDbId))
                        .thenReturn(mockResponse);

                // Act
                ResponseEntity<SiteReductionResultResponse> response =
                        batchController.SiteReductionResult(siteId, projectId);

                // Assert
                assertThat(response).isNotNull();
                assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
                assertThat(response.getBody()).isEqualTo(mockResponse);

                verify(projectRepository, times(1)).findByPublicIdWithFallback(projectId);
                // Poin krusial: pastikan yang diteruskan ke service adalah projDbId hasil resolusi (101),
                // BUKAN projectId string mentah ("PROJ-PUBLIC-001") — ini titik rawan salah kalau ada typo
                // urutan parameter di controller.
                verify(batchService, times(1)).getSiteReductionResult(siteId, projDbId);
            }
        }

        @Nested
        @DisplayName("【異常系】")
        class ErrorCase {

            @Test
            @DisplayName("UT-BC-202: projectId に対応する Project が見つからない場合、例外がスローされ batchService は呼ばれない")
            void siteReductionResult_projectNotFound_throwsExceptionAndSkipsService() {
                // Arrange
                String siteId = "SITE001";
                String projectId = "PROJ-UNKNOWN";

                when(projectRepository.findByPublicIdWithFallback(projectId))
                        .thenReturn(Optional.empty());

                // Act & Assert
                RuntimeException thrown = assertThrows(
                        RuntimeException.class,
                        () -> batchController.SiteReductionResult(siteId, projectId)
                );

                assertThat(thrown.getMessage()).isEqualTo("Project Not Found");

                verify(projectRepository, times(1)).findByPublicIdWithFallback(projectId);
                // Membuktikan short-circuit: karena project gak ketemu, batchService SAMA SEKALI
                // tidak boleh dipanggil — kalau ini gagal, berarti ada bug urutan eksekusi.
                verifyNoInteractions(batchService);
            }

            @Test
            @DisplayName("UT-BC-203: projectId ditemukan tapi batchService melempar exception, exception diteruskan")
            void siteReductionResult_serviceThrows_propagatesException() {
                // Arrange
                String siteId = "SITE001";
                String projectId = "PROJ-PUBLIC-001";
                Integer projDbId = 101;

                Project mockProject = mock(Project.class);
                when(mockProject.getId()).thenReturn(projDbId);

                when(projectRepository.findByPublicIdWithFallback(projectId))
                        .thenReturn(Optional.of(mockProject));
                when(batchService.getSiteReductionResult(siteId, projDbId))
                        .thenThrow(new RuntimeException("削減結果の取得に失敗しました"));

                // Act & Assert
                RuntimeException thrown = assertThrows(
                        RuntimeException.class,
                        () -> batchController.SiteReductionResult(siteId, projectId)
                );

                assertThat(thrown.getMessage()).isEqualTo("削減結果の取得に失敗しました");

                verify(projectRepository, times(1)).findByPublicIdWithFallback(projectId);
                verify(batchService, times(1)).getSiteReductionResult(siteId, projDbId);
            }
        }
    }

    // ============================================================================
    // projectReductionResult
    // ============================================================================
    @Nested
    @DisplayName("projectReductionResult のテスト")
    class ProjectReductionResultTest {

        @Nested
        @DisplayName("【正常系】")
        class SuccessCase {

            @Test
            @DisplayName("UT-BC-301: projectId が解決できる場合、正しい projDbId で batchService を呼び出し 200OK を返す")
            void projectReductionResult_projectFound_returnsOkResponse() {
                // Arrange
                String projectId = "PROJ-PUBLIC-002";
                Integer projDbId = 202;

                Project mockProject = mock(Project.class);
                when(mockProject.getId()).thenReturn(projDbId);

                ProjectReductionResultResponse mockResponse = new ProjectReductionResultResponse();

                when(projectRepository.findByPublicIdWithFallback(projectId))
                        .thenReturn(Optional.of(mockProject));
                when(batchService.getProjectReductionResult(projDbId))
                        .thenReturn(mockResponse);

                // Act
                ResponseEntity<ProjectReductionResultResponse> response =
                        batchController.ProjectReductionResult(projectId);

                // Assert
                assertThat(response).isNotNull();
                assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
                assertThat(response.getBody()).isEqualTo(mockResponse);

                verify(projectRepository, times(1)).findByPublicIdWithFallback(projectId);
                verify(batchService, times(1)).getProjectReductionResult(projDbId);
            }
        }

        @Nested
        @DisplayName("【異常系】")
        class ErrorCase {

            @Test
            @DisplayName("UT-BC-302: projectId に対応する Project が見つからない場合、例外がスローされ batchService は呼ばれない")
            void projectReductionResult_projectNotFound_throwsExceptionAndSkipsService() {
                // Arrange
                String projectId = "PROJ-UNKNOWN";

                when(projectRepository.findByPublicIdWithFallback(projectId))
                        .thenReturn(Optional.empty());

                // Act & Assert
                RuntimeException thrown = assertThrows(
                        RuntimeException.class,
                        () -> batchController.ProjectReductionResult(projectId)
                );

                assertThat(thrown.getMessage()).isEqualTo("Project Not Found");

                verify(projectRepository, times(1)).findByPublicIdWithFallback(projectId);
                verifyNoInteractions(batchService);
            }

            @Test
            @DisplayName("UT-BC-303: projectId ditemukan tapi batchService melempar exception, exception diteruskan")
            void projectReductionResult_serviceThrows_propagatesException() {
                // Arrange
                String projectId = "PROJ-PUBLIC-002";
                Integer projDbId = 202;

                Project mockProject = mock(Project.class);
                when(mockProject.getId()).thenReturn(projDbId);

                when(projectRepository.findByPublicIdWithFallback(projectId))
                        .thenReturn(Optional.of(mockProject));
                when(batchService.getProjectReductionResult(projDbId))
                        .thenThrow(new RuntimeException("排出量合計の取得に失敗しました"));

                // Act & Assert
                RuntimeException thrown = assertThrows(
                        RuntimeException.class,
                        () -> batchController.ProjectReductionResult(projectId)
                );

                assertThat(thrown.getMessage()).isEqualTo("排出量合計の取得に失敗しました");

                verify(projectRepository, times(1)).findByPublicIdWithFallback(projectId);
                verify(batchService, times(1)).getProjectReductionResult(projDbId);
            }
        }
    }
}