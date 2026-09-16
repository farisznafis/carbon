package com.kct.jcredit.core_backend.dto.response;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.LocalDateTime;
import com.kct.jcredit.core_backend.dto.response.base.BaseResponse;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UpdateProjectStatusResponse extends BaseResponse {

    private LocalDateTime updatedAt;
    private String projectName;
    private String oldStatus;
    private String newStatus;  
}
