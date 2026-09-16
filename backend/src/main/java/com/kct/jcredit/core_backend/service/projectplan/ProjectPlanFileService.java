package com.kct.jcredit.core_backend.service.projectplan;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.UUID;
import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.kct.jcredit.core_backend.constant.ResponseConstants;
import com.kct.jcredit.core_backend.dto.response.EnR002FileResponse;
import com.kct.jcredit.core_backend.service.storage.S3FileStorage;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

/**
 * Files attached to project-plan pages.
 *
 * <p>Two-phase, to match page atomicity. An upload lands under {@code tmp/} and
 * the client puts the returned key into the page body; only when the page
 * validates and is about to be stored are those keys promoted to their
 * permanent home. A page that never validates leaves nothing behind but a temp
 * object, which the bucket lifecycle rule expires.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProjectPlanFileService {

    private static final String TEMP_KEY_FORMAT = S3FileStorage.TEMP_ROOT + "projectplan/%s/%s_%s";
    private static final String TEMP_KEY_PREFIX = S3FileStorage.TEMP_ROOT + "projectplan/";
    private static final String TEMP_KEY_PREFIX_PATTERN = "^" + TEMP_KEY_PREFIX + "[^/]+/";
    private static final String PERMANENT_KEY_PREFIX_FORMAT = "projects/%s/plan/";
    private static final String PERMANENT_KEY_FORMAT = PERMANENT_KEY_PREFIX_FORMAT + "%s/%s";
    static final Duration DOWNLOAD_URL_VALIDITY = Duration.ofMinutes(15);

    private final S3FileStorage storage;

    /**
     * Stores one file provisionally and hands back the key to put in the page.
     *
     * @param fieldName the page field this file belongs to, e.g. {@code methodCollDescriptionImg}
     */
    public EnR002FileResponse uploadToTemp(
            String projectId,
            String formSlug,
            String fieldName,
            String uploadedBy,
            MultipartFile file) {

        storage.validate(file);

        String fileId = UUID.randomUUID().toString();
        String sanitized = S3FileStorage.sanitizeFileName(file.getOriginalFilename());
        String s3Key = String.format(TEMP_KEY_FORMAT, fileId, sanitizeField(fieldName), sanitized);

        storage.put(s3Key, file, Map.of(
                "fileId", fileId,
                "uploadedBy", uploadedBy,
                "fileType", sanitizeField(fieldName),
                "projectId", projectId,
                "formSlug", formSlug));

        log.info("Project-plan file uploaded to tmp. projectId={}, slug={}, s3Key={}",
                projectId, formSlug, s3Key);

        EnR002FileResponse response = EnR002FileResponse.builder()
                .s3Key(s3Key)
                .fileId(fileId)
                // The sanitised form goes in the key; the client shows the name
                // the operator actually chose, which for a Japanese filename
                // sanitises down to a row of underscores.
                .originalName(originalName(file))
                .build();
        response.setStatusCode(ResponseConstants.OK);
        return response;
    }

    /**
     * Promotes every provisional key in a validated page body, returning the
     * body with those keys rewritten to their permanent form.
     *
     * <p>Keys are found by their {@code tmp/projectplan/} prefix rather than by
     * a per-field declaration, so a page that gains a file field needs no
     * change here. Anything that is not one of our temp keys — an already
     * permanent key, ordinary text — is left exactly as it is.
     *
     * <p>Returns the same node when there is nothing to promote, so the common
     * case costs one walk and no re-parse.
     */
    public JsonNode promoteUploads(JsonNode body, String projectId, String formSlug) {
        if (body == null || !body.isObject()) {
            return body;
        }
        ObjectNode copy = body.deepCopy();
        return rewrite(copy, projectId, formSlug) ? copy : body;
    }

    /**
     * Fields holding a provisional key this service will not promote.
     *
     * <p>Only {@code tmp/projectplan/} keys are ours. Anything else under
     * {@code tmp/} would be stored exactly as sent and then deleted by the
     * bucket's lifecycle rule, leaving a saved page pointing at an object that
     * no longer exists — silently. Better to refuse the save.
     *
     * <p>Checked before promotion, so a page rejected for this moves nothing.
     *
     * @return field name to message, empty when the page is clean
     */
    public Map<String, String> foreignTempKeys(JsonNode body) {
        Map<String, String> offending = new LinkedHashMap<>();
        collectForeignTempKeys(body, offending);
        return offending;
    }

    private static void collectForeignTempKeys(JsonNode node, Map<String, String> offending) {
        if (node instanceof ObjectNode object) {
            var names = object.fieldNames();
            while (names.hasNext()) {
                String name = names.next();
                JsonNode child = object.get(name);
                if (child != null && child.isTextual()
                        && S3FileStorage.isTemporary(child.asText())
                        && !child.asText().startsWith(TEMP_KEY_PREFIX)) {
                    offending.putIfAbsent(name,
                            "アップロードされたファイルの参照が不正です。ファイルを再度アップロードしてください。");
                } else {
                    collectForeignTempKeys(child, offending);
                }
            }
        } else if (node instanceof ArrayNode array) {
            array.forEach(child -> collectForeignTempKeys(child, offending));
        }
    }

    /**
     * Whether a key is a stored attachment of this project's plan.
     *
     * <p>The download endpoint takes the key from the client, so this is what
     * stops it being used to read any object in the bucket: another project's
     * plan, an equipment photo, or a provisional upload nobody has saved.
     */
    public boolean isStoredFileOf(String s3Key, String projectId) {
        if (s3Key == null || projectId == null || projectId.isBlank()) {
            return false;
        }
        String prefix = String.format(PERMANENT_KEY_PREFIX_FORMAT, projectId);
        // {slug}/{file} must follow; the prefix alone names no object.
        return s3Key.startsWith(prefix) && s3Key.substring(prefix.length()).contains("/");
    }

    /** A short-lived URL to view one stored attachment. Check ownership first. */
    public String downloadUrl(String s3Key) {
        return storage.presignGet(s3Key, DOWNLOAD_URL_VALIDITY);
    }

    private boolean rewrite(JsonNode node, String projectId, String formSlug) {
        boolean changed = false;

        if (node instanceof ObjectNode object) {
            var names = object.fieldNames();
            while (names.hasNext()) {
                String name = names.next();
                JsonNode child = object.get(name);
                if (isTempKey(child)) {
                    object.put(name, promote(child.asText(), projectId, formSlug));
                    changed = true;
                } else {
                    changed |= rewrite(child, projectId, formSlug);
                }
            }
        } else if (node instanceof ArrayNode array) {
            for (int i = 0; i < array.size(); i++) {
                JsonNode child = array.get(i);
                if (isTempKey(child)) {
                    array.set(i, array.textNode(promote(child.asText(), projectId, formSlug)));
                    changed = true;
                } else {
                    changed |= rewrite(child, projectId, formSlug);
                }
            }
        }
        return changed;
    }

    private static boolean isTempKey(JsonNode node) {
        return node != null && node.isTextual() && node.asText().startsWith(TEMP_KEY_PREFIX);
    }

    private String promote(String tempKey, String projectId, String formSlug) {
        String permanentKey = permanentKeyFor(tempKey, projectId, formSlug);
        storage.promote(tempKey, permanentKey);
        return permanentKey;
    }

    /** {@code tmp/projectplan/{fileId}/x.png} -> {@code projects/{id}/plan/{slug}/x.png}. */
    String permanentKeyFor(String tempS3Key, String projectId, String formSlug) {
        String fileNamePart = tempS3Key.replaceAll(TEMP_KEY_PREFIX_PATTERN, "");
        return String.format(PERMANENT_KEY_FORMAT, projectId, formSlug, fileNamePart);
    }

    private static String originalName(MultipartFile file) {
        String name = file.getOriginalFilename();
        return name == null || name.isBlank() ? "document" : name;
    }

    private static String sanitizeField(String fieldName) {
        String sanitized = S3FileStorage.sanitizeFileName(fieldName);
        return sanitized.isEmpty() ? "file" : sanitized;
    }
}
