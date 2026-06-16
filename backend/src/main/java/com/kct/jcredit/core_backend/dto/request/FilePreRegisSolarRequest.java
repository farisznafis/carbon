package com.kct.jcredit.core_backend.dto.request;

import com.kct.jcredit.core_backend.enums.EquipmentFileType;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FilePreRegisSolarRequest {
    @NotNull
    private EquipmentFileType fileType;
}