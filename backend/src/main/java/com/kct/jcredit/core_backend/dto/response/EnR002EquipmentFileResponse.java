package com.kct.jcredit.core_backend.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import com.kct.jcredit.core_backend.dto.response.base.BaseResponse;

import lombok.Builder;
import lombok.Getter;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Getter 
public class EnR002EquipmentFileResponse extends BaseResponse {
    private Integer id;
    private String  fileType;
    private String  fileId;
    private String  s3Key;
}
 
