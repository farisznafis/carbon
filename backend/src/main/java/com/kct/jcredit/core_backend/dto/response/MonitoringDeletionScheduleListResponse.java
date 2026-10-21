package com.kct.jcredit.core_backend.dto.response;

import com.kct.jcredit.core_backend.dto.response.base.BaseResponse;
import com.kct.jcredit.core_backend.dto.response.details.MonitoringDeletionScheduleDetailResponse;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class MonitoringDeletionScheduleListResponse extends BaseResponse {
    private boolean autoDeletionEnabled;
    private long totalProjects;
    private List<MonitoringDeletionScheduleDetailResponse> schedule;
}
