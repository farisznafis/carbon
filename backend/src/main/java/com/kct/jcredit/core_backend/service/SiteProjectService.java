package com.kct.jcredit.core_backend.service;

import com.kct.jcredit.core_backend.dto.request.EnR002EquipmentRequest;
import com.kct.jcredit.core_backend.dto.request.TncValidateRequest;
import com.kct.jcredit.core_backend.dto.request.JigyoushaValidateRequest;
import com.kct.jcredit.core_backend.dto.request.FinalAgreementValidateRequest;
import com.kct.jcredit.core_backend.dto.request.EnR002ConfirmationItemRequest;
import com.kct.jcredit.core_backend.dto.response.EquipmentSolarRegisterResponse;
import com.kct.jcredit.core_backend.dto.response.EnR002SustainabilityCommitmentResponse;

import java.util.List;
public interface SiteProjectService {
    EquipmentSolarRegisterResponse registerEquipments(
            Integer siteId, Integer projectId, String companyId,
            EnR002ConfirmationItemRequest request);

    EnR002SustainabilityCommitmentResponse getJigyoushaDefault(Integer siteId, Integer projectId);

    void validateEquipmentsNotEmpty(List<EnR002EquipmentRequest> equipments);
    void validateAllEquipments(List<EnR002EquipmentRequest> equipments);
    void validateTncConsent(EnR002ConfirmationItemRequest request);
    void validateTncFields(EnR002ConfirmationItemRequest request);
    void validateJigyousha(EnR002ConfirmationItemRequest request);
    void validateFinalAgreement(EnR002ConfirmationItemRequest request);

    void validateTnc(TncValidateRequest request);
    void validateJigyousha(JigyoushaValidateRequest request);
    void validateFinalAgreement(FinalAgreementValidateRequest request);
}