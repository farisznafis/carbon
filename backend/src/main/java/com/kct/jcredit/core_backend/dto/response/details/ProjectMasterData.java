package com.kct.jcredit.core_backend.dto.response.details;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProjectMasterData {
    private Integer projectId;
    private String projectName;
    private String methodologyCategory;
    private String methodologyNumber;
    private String methodology;
    private String location;
    private String regionalCategory;
}
