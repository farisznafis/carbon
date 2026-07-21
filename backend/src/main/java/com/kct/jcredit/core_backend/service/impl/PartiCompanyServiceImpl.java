package com.kct.jcredit.core_backend.service.impl;

import java.util.Objects;

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

        ParticipatingCompany company = participatingCompanyRepository.findByCompanyId(companyId)
                .orElse(null);

        if (Objects.isNull(company)) {
            response.setStatusCode(ResponseConstants.NOT_FOUND);
            response.setMessage(ResponseConstants.MSG_SITE_NOT_FOUND);
            return response;
        }

        if (request.getPhoneNumber() != null) {
            company.setPhoneNumber(request.getPhoneNumber());
        }
        if (request.getFax() != null) {
            company.setFax(request.getFax());
        }
        if (request.getEmail() != null) {
            company.setEmail(request.getEmail());
        }

        try {
            company = participatingCompanyRepository.save(company);

            CompanyInfoResponse companyDetails = CompanyInfoResponse.builder()
                    .id(String.valueOf(company.getId()))
                    .nameJA(company.getNameJa())
                    .nameFurigana(company.getNameFurigana())
                    .nameEN(company.getNameEn())
                    .email(company.getEmail())
                    .phoneNumber(company.getPhoneNumber())
                    .fax(company.getFax())
                    .corpNumber(company.getCorpNumber())
                    //make a string of the adress from the address fields
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
}
