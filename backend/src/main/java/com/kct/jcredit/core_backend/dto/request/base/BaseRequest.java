package com.kct.jcredit.core_backend.dto.request.base;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BaseRequest {
    private String id;
    private String processedBy;
    private String companyId;
}
