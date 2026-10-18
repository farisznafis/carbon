package com.kct.jcredit.core_backend.service;

import com.kct.jcredit.core_backend.dto.request.CancelRequestDecisionRequest;
import com.kct.jcredit.core_backend.dto.request.CreateCancelRequestRequest;
import com.kct.jcredit.core_backend.dto.response.CancelRequestResponse;
import com.kct.jcredit.core_backend.dto.response.CancelRequestDecisionResponse;
import com.kct.jcredit.core_backend.dto.response.CancelRequestListResponse;

public interface CancelRequestService {
    
    /**
     * Create a new cancel request for monitoring record
     */
    CancelRequestResponse createCancelRequest(
            String username,
            String companyId,
            CreateCancelRequestRequest request
    );
    
    CancelRequestListResponse listCancelRequests();

    
    CancelRequestDecisionResponse processDecision(Integer requestId, CancelRequestDecisionRequest request, String operatorUsername);
    
}