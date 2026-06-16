package com.kct.jcredit.core_backend.dto.request;

import com.kct.jcredit.core_backend.enums.EquipmentFileType;

import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.NotBlank;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FileItemSolarRequest {
 
    // Enum — Spring otomatis reject nilai yang tidak ada di EquipmentFileType
    // via @JsonCreator, tidak perlu @NotBlank String lagi
    @NotNull
    private EquipmentFileType fileType;
 
    // S3 key hasil presign — bukan URL langsung
    @NotBlank
    private String s3Key;
 
    @NotBlank
    private String fileName;

    @NotBlank
    private String fileId;
}