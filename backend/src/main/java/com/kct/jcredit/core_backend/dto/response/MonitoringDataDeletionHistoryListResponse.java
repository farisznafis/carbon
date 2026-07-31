package com.kct.jcredit.core_backend.dto.response;

import com.kct.jcredit.core_backend.dto.response.base.BaseResponse;
import com.kct.jcredit.core_backend.dto.response.details.MonitoringDataDeletionHistoryDetailResponse;
import com.kct.jcredit.core_backend.entity.base.PaginationDetailResponse;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class MonitoringDataDeletionHistoryListResponse extends BaseResponse {
    private List<MonitoringDataDeletionHistoryDetailResponse> history;
    private PaginationDetailResponse pagination;
}
