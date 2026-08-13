package com.kct.jcredit.core_backend.util;

import java.lang.reflect.Field;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Maps a Bean Validation property path onto the JSON name the client sees.
 *
 * <p>Extracted from {@code GlobalErrorHandler} so that programmatic validation
 * (the project-plan form service, whose DTO type is only known at runtime and so
 * cannot use {@code @Valid}) produces byte-identical {@code errors} keys to the
 * {@code @Valid} path. Without a shared resolver the two silently diverge the
 * first time a DTO's {@code @JsonProperty} differs from its field name.
 */
public final class JsonFieldNameResolver {

    private static final String REQUEST_DTO_PACKAGE = "com.kct.jcredit.core_backend.dto.request.";

    private JsonFieldNameResolver() {}

    /** Resolves against a known root type, e.g. {@code "picName"} or {@code "rows[0].picName"}. */
    public static String resolve(Class<?> rootType, String propertyPath) {
        if (rootType == null || propertyPath == null || propertyPath.isEmpty()) {
            return propertyPath;
        }

        String[] pathParts = propertyPath.split("\\.");
        Class<?> currentClass = rootType;
        StringBuilder jsonPath = new StringBuilder();

        for (String pathPart : pathParts) {
            String collectionSuffix = "";
            String fieldName = pathPart;
            int collectionStart = pathPart.indexOf('[');
            if (collectionStart >= 0) {
                collectionSuffix = pathPart.substring(collectionStart);
                fieldName = pathPart.substring(0, collectionStart);
            }

            if (jsonPath.length() > 0) {
                jsonPath.append(".");
            }
            jsonPath.append(jsonFieldName(currentClass, fieldName)).append(collectionSuffix);

            Field nestedField = findField(currentClass, fieldName);
            if (nestedField != null) {
                currentClass = nestedField.getType();
            }
        }

        return jsonPath.toString();
    }

    /** Resolves using Spring's binding object name, which names a request DTO. */
    public static String resolveByObjectName(String propertyPath, String objectName) {
        Class<?> rootType = classFromObjectName(objectName);
        return rootType == null ? propertyPath : resolve(rootType, propertyPath);
    }

    private static Class<?> classFromObjectName(String objectName) {
        if (objectName == null || objectName.isEmpty()) {
            return null;
        }
        String className = Character.toUpperCase(objectName.charAt(0)) + objectName.substring(1);
        try {
            return Class.forName(REQUEST_DTO_PACKAGE + className);
        } catch (ClassNotFoundException ex) {
            return null;
        }
    }

    private static String jsonFieldName(Class<?> targetClass, String fieldName) {
        Field field = findField(targetClass, fieldName);
        if (field == null) {
            return fieldName;
        }

        JsonProperty jsonProperty = field.getAnnotation(JsonProperty.class);
        if (jsonProperty != null && !jsonProperty.value().isBlank()) {
            return jsonProperty.value();
        }

        return fieldName;
    }

    private static Field findField(Class<?> targetClass, String fieldName) {
        Class<?> currentClass = targetClass;
        while (currentClass != null && currentClass != Object.class) {
            try {
                return currentClass.getDeclaredField(fieldName);
            } catch (NoSuchFieldException ex) {
                currentClass = currentClass.getSuperclass();
            }
        }
        return null;
    }
}
