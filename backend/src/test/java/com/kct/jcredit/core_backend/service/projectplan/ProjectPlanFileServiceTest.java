package com.kct.jcredit.core_backend.service.projectplan;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.kct.jcredit.core_backend.service.storage.S3FileStorage;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Promotion of provisional uploads when a page is saved.
 *
 * <p>Keys are recognised by their {@code tmp/projectplan/} prefix rather than
 * declared per field, so these tests are what pin that a page gaining a file
 * field needs no change in the service.
 */
@ExtendWith(MockitoExtension.class)
class ProjectPlanFileServiceTest {

    private static final String PROJECT_ID = "PROJ1";
    private static final String SLUG = "monitoring-plan";
    private static final String TEMP_KEY = "tmp/projectplan/abc-123/methodCollDescriptionImg_plan.png";
    private static final String PERMANENT_KEY =
            "projects/PROJ1/plan/monitoring-plan/methodCollDescriptionImg_plan.png";

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Mock private S3FileStorage storage;

    private ProjectPlanFileService service() {
        return new ProjectPlanFileService(storage);
    }

    private ObjectNode page() {
        return objectMapper.createObjectNode();
    }

    @Test
    void aTempKeyIsPromotedAndRewrittenInPlace() {
        ObjectNode body = page().put("methodCollDescriptionImg", TEMP_KEY);

        JsonNode result = service().promoteUploads(body, PROJECT_ID, SLUG);

        verify(storage).promote(TEMP_KEY, PERMANENT_KEY);
        assertThat(result.get("methodCollDescriptionImg").asText()).isEqualTo(PERMANENT_KEY);
    }

    @Test
    void theOriginalBodyIsLeftAloneSoNothingElseSeesTheRewrite() {
        ObjectNode body = page().put("methodCollDescriptionImg", TEMP_KEY);

        service().promoteUploads(body, PROJECT_ID, SLUG);

        assertThat(body.get("methodCollDescriptionImg").asText()).isEqualTo(TEMP_KEY);
    }

    @Test
    void ordinaryTextAndAlreadyPermanentKeysAreUntouched() {
        ObjectNode body = page()
                .put("methodCollDescription", "モニタリングデータは月次で収集します。")
                .put("methodCollDescriptionImg", PERMANENT_KEY)
                .put("somethingElse", "tmp/equipments/other/photo.png");

        JsonNode result = service().promoteUploads(body, PROJECT_ID, SLUG);

        // Same node back: nothing to promote means no re-parse for the common case.
        assertThat(result).isSameAs(body);
        verify(storage, never()).promote(org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.anyString());
    }

    @Test
    void nestedObjectsAndArraysAreWalkedTooSoAttachmentsAreCovered() {
        ObjectNode body = page();
        ObjectNode nested = body.putObject("evidence");
        nested.put("s3Key", TEMP_KEY);
        body.putArray("attachments").add(TEMP_KEY);

        JsonNode result = service().promoteUploads(body, PROJECT_ID, SLUG);

        assertThat(result.get("evidence").get("s3Key").asText()).isEqualTo(PERMANENT_KEY);
        assertThat(result.get("attachments").get(0).asText()).isEqualTo(PERMANENT_KEY);
        verify(storage, org.mockito.Mockito.times(2)).promote(TEMP_KEY, PERMANENT_KEY);
    }

    @Test
    void theKeyMovesOutOfTheProvisionalAreaEntirely() {
        String permanent = service().permanentKeyFor(TEMP_KEY, PROJECT_ID, SLUG);

        assertThat(permanent).isEqualTo(PERMANENT_KEY);
        // The whole point of promotion: nothing stored may still sit under a
        // prefix the bucket lifecycle rule is free to delete.
        assertThat(S3FileStorage.isTemporary(permanent)).isFalse();
        assertThat(S3FileStorage.isTemporary(TEMP_KEY)).isTrue();
    }

    @Test
    void aNonObjectBodyIsHandedBackUnchanged() {
        assertThat(service().promoteUploads(null, PROJECT_ID, SLUG)).isNull();

        JsonNode array = objectMapper.createArrayNode();
        assertThat(service().promoteUploads(array, PROJECT_ID, SLUG)).isSameAs(array);
    }
}
