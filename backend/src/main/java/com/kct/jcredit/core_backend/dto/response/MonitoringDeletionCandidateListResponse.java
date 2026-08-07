package com.kct.jcredit.core_backend.dto.response;

import com.kct.jcredit.core_backend.dto.response.base.BaseResponse;
import com.kct.jcredit.core_backend.dto.response.details.MonitoringDeletionCandidateDetailResponse;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class MonitoringDeletionCandidateListResponse extends BaseResponse {
    private long totalProjects;
    private long totalRecords;
    private List<MonitoringDeletionCandidateDetailResponse> candidates;
}
