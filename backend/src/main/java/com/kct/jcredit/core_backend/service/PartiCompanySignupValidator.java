package com.kct.jcredit.core_backend.service;

import com.kct.jcredit.core_backend.constant.ResponseConstants;
import com.kct.jcredit.core_backend.dto.request.PartiCompanyInfoValidateRequest;
import com.kct.jcredit.core_backend.dto.request.PartiCompanyRegisterRequest;
import com.kct.jcredit.core_backend.exception.CustomException;
import com.kct.jcredit.core_backend.repository.postgres.ParticipatingCompanyRegisterRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class PartiCompanySignupValidator {

    private final ParticipatingCompanyRegisterRepository repository;

    public void validateCompanyInfo(PartiCompanyInfoValidateRequest request) {
        validateDuplicates(
                request.getCorporateNumber(),
                request.getCorporateEmail(),
                request.getEmail()
        );
    }

    public void validateCompanyInfo(PartiCompanyRegisterRequest request) {
        validateDuplicates(
                request.getCorporateNumber(),
                request.getCorporateEmail(),
                request.getEmail()
        );
    }

    private void validateDuplicates(String corporateNumber, String corporateEmail, String representativeEmail) {
        Map<String, String> errors = new LinkedHashMap<>();

        if (repository.findByCorpNumberAndRegStatus(corporateNumber, "PENDING").isPresent()) {
            errors.put("corporateNumber", ResponseConstants.MSG_ALREADY_SUBMITTED);
        } else if (repository.existsByCorpNumberAndRegStatusNot(corporateNumber, "REJECTED")) {
            errors.put("corporateNumber", ResponseConstants.MSG_CORP_NUMBER_ALREADY_USED);
        }

        if (repository.findByEmailAndRegStatus(corporateEmail, "APPROVED").isPresent()) {
            errors.put("corporateEmail", ResponseConstants.MSG_EMAIL_ALREADY_USED);
        }

        if (repository.findByRepEmailAndRegStatus(representativeEmail, "APPROVED").isPresent()) {
            errors.put("email", ResponseConstants.MSG_EMAIL_ALREADY_USED);
        }

        if (!errors.isEmpty()) {
            throw new CustomException(
                    ResponseConstants.VALIDATION_ERROR,
                    ResponseConstants.MSG_VALIDATION_ERROR,
                    errors,
                    null
            );
        }
    }
}
