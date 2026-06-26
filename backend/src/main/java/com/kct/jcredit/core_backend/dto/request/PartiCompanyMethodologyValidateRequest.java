package com.kct.jcredit.core_backend.dto.request;

import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PartiCompanyMethodologyValidateRequest {

    @NotEmpty(message = "方法論を1つ以上選択してください。")
    private List<String> methodology;

    private String activityInformation;
}
