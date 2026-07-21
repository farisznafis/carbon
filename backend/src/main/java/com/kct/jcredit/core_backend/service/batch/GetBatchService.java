package com.kct.jcredit.core_backend.service.batch;

import com.kct.jcredit.core_backend.dto.response.SiteReductionResultResponse;
import com.kct.jcredit.core_backend.dto.response.ProjectReductionResultResponse;
import com.kct.jcredit.core_backend.dto.response.BatchProcessHistoryResponse;

public interface GetBatchService {

    public BatchProcessHistoryResponse getBatchProcessHistory(Integer page, Integer limit, String nextPageToken);
    public SiteReductionResultResponse getSiteReductionResult(String siteId, Integer projectId);
    public ProjectReductionResultResponse getProjectReductionResult(Integer projectId);

}
