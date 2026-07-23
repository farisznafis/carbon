package com.kct.jcredit.core_backend.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import com.kct.jcredit.core_backend.dto.response.base.BaseResponse;
import com.kct.jcredit.core_backend.dto.response.details.MonitoringDataSolarResponse;
import com.kct.jcredit.core_backend.entity.EnR002MonitoringRecord;
import com.kct.jcredit.core_backend.entity.base.PaginationDetailResponse;

import java.util.List;

@Data
@EqualsAndHashCode(callSuper = false)
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MonitoringRecordSolarListResponse extends BaseResponse{
    private List<MonitoringDataSolarResponse> items;
    private PaginationDetailResponse pagination;
}
