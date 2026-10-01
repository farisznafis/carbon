package com.kct.jcredit.core_backend.siteprojectcontrollertest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.net.URL;
import java.time.Duration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.kct.jcredit.core_backend.repository.dynamodb.EnR002ApplicationConfirmationRepository;
import com.kct.jcredit.core_backend.repository.dynamodb.EnR002SustainabilityCommitmentRepository;
import com.kct.jcredit.core_backend.repository.dynamodb.ProjectParticipationConfirmationRepository;
import com.kct.jcredit.core_backend.repository.postgres.ParticipatingProjectRepository;
import com.kct.jcredit.core_backend.service.NotificationService;
import com.kct.jcredit.core_backend.service.impl.ParticipationPdfServiceImpl;

import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;

// Scope note: only generatePresignedUrl is covered here, because that is the single
// method reachable from SiteProjectController (via getMembershipPdfByCompany).
// generateAndProcess and getPdfDownloadUrl are triggered from the approval flow and
// belong to that controller's test round.
@ExtendWith(MockitoExtension.class)
@DisplayName("ParticipationPdfServiceImpl のテスト")
class ParticipationPdfServiceImplTest {

    @Mock private S3Client s3Client;
    @Mock private S3Presigner s3Presigner;
    @Mock private ParticipatingProjectRepository participatingProjectRepository;
    @Mock private EnR002ApplicationConfirmationRepository confirmationItemsRepository;
    @Mock private EnR002SustainabilityCommitmentRepository sustainabilityRepository;
    @Mock private ProjectParticipationConfirmationRepository reviewRepository;
    @Mock private NotificationService notificationService;

    @InjectMocks
    private ParticipationPdfServiceImpl pdfService;

    private static final String BUCKET = "gx-carbon-test";
    private static final int EXPIRY_SECONDS = 900;
    private static final String S3_KEY = "participation/20/10/77/draft.pdf";

    @BeforeEach
    void setUp() {
        // @Value fields are not populated outside the Spring context.
        ReflectionTestUtils.setField(pdfService, "bucketName", BUCKET);
        ReflectionTestUtils.setField(pdfService, "presignExpirySeconds", EXPIRY_SECONDS);
    }

    // ==================================================================
    // 09. generatePresignedUrl
    // ==================================================================

    @Test
    @DisplayName("UT-SPS-09-01: 正常 - 設定されたバケット・キー・有効期限でpresigned URLを生成する")
    void generatePresignedUrl_buildsRequestFromConfigAndKey() throws Exception {
        PresignedGetObjectRequest presigned = mock(PresignedGetObjectRequest.class);
        when(presigned.url()).thenReturn(new URL("https://s3.example.com/signed"));
        when(s3Presigner.presignGetObject(any(GetObjectPresignRequest.class))).thenReturn(presigned);

        String url = pdfService.generatePresignedUrl(S3_KEY);

        assertThat(url).isEqualTo("https://s3.example.com/signed");

        ArgumentCaptor<GetObjectPresignRequest> captor =
                ArgumentCaptor.forClass(GetObjectPresignRequest.class);
        verify(s3Presigner, times(1)).presignGetObject(captor.capture());

        GetObjectPresignRequest request = captor.getValue();
        assertThat(request.signatureDuration()).isEqualTo(Duration.ofSeconds(EXPIRY_SECONDS));
        assertThat(request.getObjectRequest().bucket()).isEqualTo(BUCKET);
        assertThat(request.getObjectRequest().key()).isEqualTo(S3_KEY);
    }

    @Test
    @DisplayName("UT-SPS-09-02: 異常 - presigner が例外をスローする場合そのまま伝播される")
    void generatePresignedUrl_presignerThrows_propagatesException() {
        when(s3Presigner.presignGetObject(any(GetObjectPresignRequest.class)))
                .thenThrow(new RuntimeException("signing failed"));

        assertThrows(RuntimeException.class, () -> pdfService.generatePresignedUrl(S3_KEY));
    }

    @Test
    @DisplayName("UT-SPS-09-03: 準正常 - s3Keyの形式・所有者は検証されずそのまま署名される（要確認）")
    void generatePresignedUrl_doesNotValidateKeyOwnership() throws Exception {
        // NOTE: the key is signed as given, with no prefix or ownership check. Safe as
        // long as every caller supplies a key read from the database (which is the case
        // for getMembershipPdfByCompany). Recorded so that any future caller passing a
        // client-supplied key is recognised as a cross-tenant exposure.
        PresignedGetObjectRequest presigned = mock(PresignedGetObjectRequest.class);
        when(presigned.url()).thenReturn(new URL("https://s3.example.com/signed"));
        when(s3Presigner.presignGetObject(any(GetObjectPresignRequest.class))).thenReturn(presigned);

        String otherCompanyKey = "participation/99/99/99/draft.pdf";
        pdfService.generatePresignedUrl(otherCompanyKey);

        ArgumentCaptor<GetObjectPresignRequest> captor =
                ArgumentCaptor.forClass(GetObjectPresignRequest.class);
        verify(s3Presigner).presignGetObject(captor.capture());
        assertThat(captor.getValue().getObjectRequest().key()).isEqualTo(otherCompanyKey);
    }
}