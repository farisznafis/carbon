package com.kct.jcredit.core_backend.dto.response;
import com.kct.jcredit.core_backend.dto.response.base.BaseResponse;
import com.kct.jcredit.core_backend.dto.response.details.ProjectDetailMonitoringResponse;

import lombok.Data;

@Data
public class ProjectMonitoringResponse extends BaseResponse{
    private ProjectDetailMonitoringResponse detail;
    
}
