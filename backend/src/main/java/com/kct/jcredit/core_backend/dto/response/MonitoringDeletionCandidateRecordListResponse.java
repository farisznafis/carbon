package com.kct.jcredit.core_backend.dto.response;

import com.kct.jcredit.core_backend.dto.response.base.BaseResponse;
import com.kct.jcredit.core_backend.dto.response.details.MonitoringDeletionCandidateRecordDetailResponse;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class MonitoringDeletionCandidateRecordListResponse extends BaseResponse {
    private Integer projectId;
    private String projectPublicId;
    private Integer siteId;
    private String sitePublicId;
    private long totalRecords;
    private List<MonitoringDeletionCandidateRecordDetailResponse> records;
}
