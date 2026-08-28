package com.kct.jcredit.core_backend.dto.response;

import com.kct.jcredit.core_backend.dto.response.base.BaseResponse;

import lombok.Getter;
import lombok.Setter;

@Getter 
@Setter
public class DocuSignEnvelopeResult extends BaseResponse{
    private String envelopeId;
    private String status;
}
