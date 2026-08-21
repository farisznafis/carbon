package com.kct.jcredit.core_backend.service.impl;

import java.util.Objects;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.kct.jcredit.core_backend.constant.ResponseConstants;
import com.kct.jcredit.core_backend.dto.request.UpdatePartiCompanyRequest;
import com.kct.jcredit.core_backend.dto.response.UpdatePartiCompanyResponse;
import com.kct.jcredit.core_backend.dto.response.details.CompanyInfoResponse;
import com.kct.jcredit.core_backend.entity.ParticipatingCompany;
import com.kct.jcredit.core_backend.repository.postgres.ParticipatingCompanyRepository;
import com.kct.jcredit.core_backend.service.PartiCompanyService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class PartiCompanyServiceImpl implements PartiCompanyService {
    private final ParticipatingCompanyRepository participatingCompanyRepository;

    @Override
    @Transactional
    public UpdatePartiCompanyResponse updateCompany(String companyId, UpdatePartiCompanyRequest request) {
        UpdatePartiCompanyResponse response = new UpdatePartiCompanyResponse();

        ParticipatingCompany company = participatingCompanyRepository.findByCompanyIdForUpdate(companyId)
                .orElse(null);

        if (Objects.isNull(company)) {
            response.setStatusCode(ResponseConstants.NOT_FOUND);
            response.setMessage(ResponseConstants.MSG_COMPANY_NOT_FOUND);
            return response;
        }

        validateRequiredFields(request, response);
        if (response.getStatusCode() != null) {
            return response;
        }

        Integer currentCompanyId = company.getId();
        if (request.getCorpNumber() != null
                && participatingCompanyRepository.findByCorpNumber(request.getCorpNumber())
                        .filter(existing -> !existing.getId().equals(currentCompanyId))
                        .isPresent()) {
            response.setStatusCode(ResponseConstants.CONFLICT);
            response.setMessage(ResponseConstants.MSG_CORP_NUMBER_ALREADY_USED);
            return response;
        }

        if (request.getNameJa() != null) {
            company.setNameJa(request.getNameJa());
        }
        if (request.getNameFurigana() != null) {
            company.setNameFurigana(request.getNameFurigana());
        }
        if (request.getNameEn() != null) {
            company.setNameEn(request.getNameEn());
        }
        if (request.getPhoneNumber() != null) {
            company.setPhoneNumber(request.getPhoneNumber());
        }
        if (request.getFax() != null) {
            company.setFax(request.getFax());
        }
        if (request.getCorpNumber() != null) {
            company.setCorpNumber(request.getCorpNumber());
        }
        if (request.getPostalCode() != null) {
            company.setPostalCode(request.getPostalCode());
        }
        if (request.getPrefecture() != null) {
            company.setPrefecture(request.getPrefecture());
        }
        if (request.getMunicipality() != null) {
            company.setMunicipality(request.getMunicipality());
        }
        if (request.getBlocknum() != null) {
            company.setBlocknum(request.getBlocknum());
        }
        if (request.getBuildingName() != null) {
            company.setBuildingName(request.getBuildingName());
        }

        try {
            company = participatingCompanyRepository.save(company);

            CompanyInfoResponse companyDetails = CompanyInfoResponse.builder()
                    .id(String.valueOf(company.getId()))
                    .companyId(company.getCompanyId())
                    .nameJA(company.getNameJa())
                    .nameFurigana(company.getNameFurigana())
                    .nameEN(company.getNameEn())
                    .email(company.getEmail())
                    .phoneNumber(company.getPhoneNumber())
                    .fax(company.getFax())
                    .corpNumber(company.getCorpNumber())
                    .postalCode(company.getPostalCode())
                    .prefecture(company.getPrefecture())
                    .municipality(company.getMunicipality())
                    .blocknum(company.getBlocknum())
                    .buildingName(company.getBuildingName())
                    .address(String.format("%s %s %s %s", company.getPrefecture(), company.getMunicipality(), company.getBlocknum(), company.getBuildingName()))
                    .isSuspended(company.isSuspended())
                    .build();

            response.setCompany(companyDetails);
            response.setStatusCode(ResponseConstants.OK);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            response.setStatusCode(ResponseConstants.INTERNAL_SERVER_ERROR);
            response.setMessage(ResponseConstants.MSG_INTERNAL_ERROR);
        }

        return response;
    }

    private void validateRequiredFields(UpdatePartiCompanyRequest request,
                                        UpdatePartiCompanyResponse response) {
        Map<String, String> errors = new LinkedHashMap<>();

        addRequiredError(errors, "nameJa", request.getNameJa(), "法人名を入力してください。");
        addRequiredError(errors, "nameFurigana", request.getNameFurigana(), "法人名カナを入力してください。");
        addRequiredError(errors, "nameEn", request.getNameEn(), "英語法人名を入力してください。");
        addRequiredError(errors, "phoneNumber", request.getPhoneNumber(), "電話番号を入力してください。");
        addRequiredError(errors, "corpNumber", request.getCorpNumber(), "法人番号を入力してください。");
        addRequiredError(errors, "postalCode", request.getPostalCode(), "郵便番号を入力してください。");
        addRequiredError(errors, "prefecture", request.getPrefecture(), "都道府県を入力してください。");
        addRequiredError(errors, "municipality", request.getMunicipality(), "市区町村を入力してください。");
        addRequiredError(errors, "blocknum", request.getBlocknum(), "番地を入力してください。");

        if (!errors.isEmpty()) {
            response.setStatusCode(ResponseConstants.VALIDATION_ERROR);
            response.setMessage(ResponseConstants.MSG_VALIDATION_ERROR);
            response.setErrors(errors);
        }
    }

    private void addRequiredError(Map<String, String> errors, String field, String value, String message) {
        if (value != null && value.isBlank()) {
            errors.put(field, message);
        }
    }
}
