package com.kct.jcredit.core_backend.service;

import com.kct.jcredit.core_backend.dto.request.EnR002EquipmentRequest;
import com.kct.jcredit.core_backend.dto.request.EnR002FinalAgreementRequest;
import com.kct.jcredit.core_backend.dto.request.EnR002SustainabilityComitmentRequest;
import com.kct.jcredit.core_backend.dto.request.EnR002ConfirmationItemRequest;
import com.kct.jcredit.core_backend.dto.request.FinalAgreementValidateRequest;
import com.kct.jcredit.core_backend.dto.response.EnR002SustainabilityCommitmentResponse;

import java.util.List;

public interface ParticipationProjectValidationService {
    void validateEquipment(EnR002EquipmentRequest equipment);
    void validateConfirmationItems(EnR002ConfirmationItemRequest request);
    void validateSustainabilityCommitment(EnR002SustainabilityCommitmentResponse request);
    void validateFinalAgreement(FinalAgreementValidateRequest request);
    void validateSustainabilityCommitment(EnR002SustainabilityComitmentRequest request);
    void validateFinalAgreement(EnR002FinalAgreementRequest request);
}
