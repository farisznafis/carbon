package com.kct.jcredit.core_backend.service;

import com.kct.jcredit.core_backend.dto.request.EquipmentItemSolarRequest;
import com.kct.jcredit.core_backend.dto.request.TncValidateRequest;
import com.kct.jcredit.core_backend.dto.request.JigyoushaValidateRequest;
import com.kct.jcredit.core_backend.dto.request.FinalAgreementValidateRequest;
import com.kct.jcredit.core_backend.dto.request.EquipmentRegisterSolarRequest;
import com.kct.jcredit.core_backend.dto.response.EquipmentSolarRegisterResponse;
import com.kct.jcredit.core_backend.dto.response.JigyoushaDefaultResponse;

import java.util.List;
public interface SiteProjectService {
    EquipmentSolarRegisterResponse registerEquipments(
            Integer siteId, Integer projectId, String companyId,
            EquipmentRegisterSolarRequest request);

    JigyoushaDefaultResponse getJigyoushaDefault(Integer siteId, Integer projectId);

    void validateEquipmentsNotEmpty(List<EquipmentItemSolarRequest> equipments);
    void validateAllEquipments(List<EquipmentItemSolarRequest> equipments);
    void validateTncConsent(EquipmentRegisterSolarRequest request);
    void validateTncFields(EquipmentRegisterSolarRequest request);
    void validateJigyousha(EquipmentRegisterSolarRequest request);
    void validateFinalAgreement(EquipmentRegisterSolarRequest request);

    void validateTnc(TncValidateRequest request);
    void validateJigyousha(JigyoushaValidateRequest request);
    void validateFinalAgreement(FinalAgreementValidateRequest request);
}