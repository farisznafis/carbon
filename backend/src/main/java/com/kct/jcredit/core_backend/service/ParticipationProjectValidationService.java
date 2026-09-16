package com.kct.jcredit.core_backend.service;

import com.kct.jcredit.core_backend.dto.request.EnR002EquipmentRequest;
import com.kct.jcredit.core_backend.dto.request.EnR002FinalAgreementRequest;
import com.kct.jcredit.core_backend.dto.request.EnR002SustainabilityCommitmentRequest;
import com.kct.jcredit.core_backend.dto.request.EnR002ConfirmationItemRequest;
import com.kct.jcredit.core_backend.dto.response.EnR002SustainabilityCommitmentResponse;

import java.util.List;

public interface ParticipationProjectValidationService {
    void validateEquipment(EnR002EquipmentRequest equipment);
    void validateConfirmationItems(EnR002ConfirmationItemRequest request);
    void validateSustainabilityCommitment(EnR002SustainabilityCommitmentResponse request);
    void validateFinalAgreement(EnR002FinalAgreementRequest request);
    void validateSustainabilityCommitment(EnR002SustainabilityCommitmentRequest request);
}
