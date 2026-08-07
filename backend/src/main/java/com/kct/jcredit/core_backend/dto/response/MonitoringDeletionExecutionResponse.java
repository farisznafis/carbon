package com.kct.jcredit.core_backend.dto.response;

import com.kct.jcredit.core_backend.dto.response.base.BaseResponse;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class MonitoringDeletionExecutionResponse extends BaseResponse {
    private int deletedCount;
    private int failedCount;
    private LocalDateTime executedAt;
    private String executedBy;
}
