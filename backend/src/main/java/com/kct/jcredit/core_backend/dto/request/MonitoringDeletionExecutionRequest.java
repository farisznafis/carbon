package com.kct.jcredit.core_backend.dto.request;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

@Data
public class MonitoringDeletionExecutionRequest {

    @NotEmpty(message = "削除対象のプロジェクトを指定してください。")
    private List<String> projectPublicIds;
}
