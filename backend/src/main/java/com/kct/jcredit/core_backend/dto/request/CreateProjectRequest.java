package com.kct.jcredit.core_backend.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

import com.kct.jcredit.core_backend.dto.request.details.ProjectDtoReq;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateProjectRequest {

    @Valid
    @NotEmpty
    private List<ProjectDtoReq> project;

    private String operatorCompanyId;

    private String createdBy;
}