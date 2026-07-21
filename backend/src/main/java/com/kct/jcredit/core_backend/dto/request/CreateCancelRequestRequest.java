package com.kct.jcredit.core_backend.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;



@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateCancelRequestRequest {

    @NotNull(message = "siteId is required")
    private String siteId;
    
    @NotBlank(message = "recordPK is required")
    private String recordPK;

    @NotBlank(message = "recordSK is required")
    private String recordSK;
  
    @NotBlank(message = "reasonText is required")
    private String reasonText;
}