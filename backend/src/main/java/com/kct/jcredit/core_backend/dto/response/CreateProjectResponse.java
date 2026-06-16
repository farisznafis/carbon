package com.kct.jcredit.core_backend.dto.response;

import java.util.List;

import com.kct.jcredit.core_backend.dto.response.base.BaseResponse;
import com.kct.jcredit.core_backend.dto.response.details.ProjectDetailResponse;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateProjectResponse extends BaseResponse{
    private List<ProjectDetailResponse> project;
}