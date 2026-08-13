package com.kct.jcredit.core_backend.dto.projectplan;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbAttribute;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;

/**
 * A file uploaded against a project-plan page.
 *
 * Clients send the {@code s3Key} returned by the temp upload endpoint. On a
 * successful page save the key is promoted from {@code tmp/} to its permanent
 * location and the permanent key is what gets stored here.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@DynamoDbBean
public class FormAttachment {

    @JsonProperty("fileId")
    private String fileId;

    @JsonProperty("s3Key")
    private String s3Key;

    @JsonProperty("originalName")
    private String originalName;

    /** Which slot on the page this file belongs to (e.g. an evidence field name). */
    @JsonProperty("fileType")
    private String fileType;

    @DynamoDbAttribute("fileId")
    public String getFileId() {
        return fileId;
    }

    @DynamoDbAttribute("s3Key")
    public String getS3Key() {
        return s3Key;
    }

    @DynamoDbAttribute("originalName")
    public String getOriginalName() {
        return originalName;
    }

    @DynamoDbAttribute("fileType")
    public String getFileType() {
        return fileType;
    }
}
