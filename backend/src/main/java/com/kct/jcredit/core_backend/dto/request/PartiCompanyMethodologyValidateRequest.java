package com.kct.jcredit.core_backend.dto.request;

import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import jakarta.validation.constraints.NotBlank;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PartiCompanyMethodologyValidateRequest {

    @NotEmpty(message = "方法論を1つ以上選択してください。")
    private List<String> methodology;

    @NotBlank(message = "活動内容を入力してください。もし活動がない場合は「なし」を選んでください。")
    private String activityInformation;
}
