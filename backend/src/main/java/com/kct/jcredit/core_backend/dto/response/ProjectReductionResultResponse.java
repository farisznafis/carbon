package com.kct.jcredit.core_backend.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProjectReductionResultResponse {
    
    @JsonProperty("statusCode")
    private String statusCode;
    
    private ProjectReductionResultDataDto data;
}
