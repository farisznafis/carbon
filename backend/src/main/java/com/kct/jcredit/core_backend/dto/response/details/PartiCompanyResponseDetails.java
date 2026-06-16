package com.kct.jcredit.core_backend.dto.response.details;
import com.kct.jcredit.core_backend.dto.response.PartiCompanyResponse;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PartiCompanyResponseDetails extends PartiCompanyResponse {
    private String registrationId;
    private String status;

    public PartiCompanyResponseDetails(String statusCode, String message, String registrationId, String status) {
        super(statusCode, message);
        this.registrationId = registrationId;
        this.status = status;
    }
}