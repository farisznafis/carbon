package com.kct.jcredit.core_backend.dto.response;

import com.kct.jcredit.core_backend.entity.base.PaginationDetailResponse;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BatchProcessHistoryResponse {
    private List<BatchProcessHistoryItemDto> items;
    private PaginationDetailResponse pagination;
}
