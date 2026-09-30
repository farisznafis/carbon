package com.kct.jcredit.core_backend.service.impl;

import com.kct.jcredit.core_backend.dto.request.PartiCompanyInfoValidateRequest;
import com.kct.jcredit.core_backend.dto.request.PartiCompanyRegisterRequest;
import com.kct.jcredit.core_backend.dto.response.details.PartiCompanyResponseDetails;
import com.kct.jcredit.core_backend.entity.ParticipatingCompany;
import com.kct.jcredit.core_backend.enums.CompanyStatus;
import com.kct.jcredit.core_backend.repository.postgres.ParticipatingCompanyRegisterRepository;
import com.kct.jcredit.core_backend.service.NotificationMessages;
import com.kct.jcredit.core_backend.service.NotificationService;
import com.kct.jcredit.core_backend.service.PartiCompanyRegisterService;
import com.kct.jcredit.core_backend.service.PartiCompanySignupValidator;
import com.kct.jcredit.core_backend.service.OtpChallengeService;
import com.kct.jcredit.core_backend.config.SecurityFlowProperties;
import com.kct.jcredit.core_backend.enums.OtpPurpose;
import com.kct.jcredit.core_backend.constant.ResponseConstants;
import com.kct.jcredit.core_backend.exception.CustomException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import java.util.UUID;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Service
@RequiredArgsConstructor
public class PartiCompanyRegisterServiceImpl implements PartiCompanyRegisterService {

    private final NotificationService notificationService;
    private final ParticipatingCompanyRegisterRepository repository;
    private final PartiCompanySignupValidator signupValidator;
    private final OtpChallengeService otpChallengeService;
    private final SecurityFlowProperties securityFlowProperties;
    private final NotificationMessages messages;

    @Override
    @Transactional(readOnly = true)
    public void validateCompanyInfo(PartiCompanyInfoValidateRequest request) {
        signupValidator.validateCompanyInfo(request);
    }

    @Override
    @Transactional
    public PartiCompanyResponseDetails register(PartiCompanyRegisterRequest request) {

        signupValidator.validateCompanyInfo(request);

        if (securityFlowProperties.getFeatures().isRejectedSignupRevision()
                && (repository.findByCorpNumberAndCompanyStatus(
                        request.getCorporateNumber(), CompanyStatus.REJECTED).isPresent()
                    || repository.findByEmailAndCompanyStatus(
                        request.getEmail(), CompanyStatus.REJECTED).isPresent())) {
            throw new CustomException(
                    ResponseConstants.CONFLICT,
                    "Rejected registrations must be revised through the emailed action link.");
        }

        if (securityFlowProperties.getFeatures().isSignupEmailVerification()) {
            if (request.getEmailVerificationChallengeId() == null) {
                throw new CustomException(ResponseConstants.OTP_REQUIRED, ResponseConstants.MSG_OTP_REQUIRED);
            }
            otpChallengeService.requireVerified(
                    request.getEmailVerificationChallengeId(),
                    OtpPurpose.SIGNUP_EMAIL_VERIFICATION,
                    request.getEmail());
        }

        ParticipatingCompany entity = securityFlowProperties.getFeatures().isRejectedSignupRevision()
                ? null
                : repository.findByCorpNumberAndCompanyStatus(request.getCorporateNumber(), CompanyStatus.REJECTED)
                    .or(() -> repository.findByEmailAndCompanyStatus(request.getEmail(), CompanyStatus.REJECTED))
                    .orElse(null);

        boolean isReactivation = entity != null;
        if (!isReactivation) {
            entity = new ParticipatingCompany();
        }

        entity.setCorpNumber(request.getCorporateNumber());
        entity.setNameJa(request.getCorporateName());
        entity.setNameFurigana(request.getCorporateNameKana());
        entity.setNameEn(request.getCompanyNameEnglish());
        entity.setEmail(request.getEmail());
        entity.setPhoneNumber(request.getCorporatePhoneNumber());

        entity.setPostalCode(request.getPostalCode());
        entity.setPrefecture(request.getPrefecture());
        entity.setMunicipality(request.getMunicipality());
        entity.setBlocknum(request.getBlocknum());
        entity.setBuildingName(request.getBuildingName());

        entity.setRepFirstName(request.getFirstName());
        entity.setRepLastName(request.getLastName());
        entity.setRepFirstNameKana(request.getFirstNameKana());
        entity.setRepLastNameKana(request.getLastNameKana());
        entity.setRepPosition(request.getPosition());
        entity.setRepPhoneNumber(request.getPhoneNumber());
        entity.setFax(request.getFaxNumber());
        entity.setRequestedMethodologies(request.getMethodology());
        entity.setActivityInformation(request.getActivityInformation());

        String newCompanyId = generateUniqueCompanyId();
        entity.setCompanyId(newCompanyId);
        entity.setCompanyStatus(CompanyStatus.PENDING);
        entity.setIsActive(false);
        if (securityFlowProperties.getFeatures().isSignupEmailVerification()) {
            entity.setEmailVerifiedAt(LocalDateTime.now());
        }

        repository.save(entity);

        NotificationMessages.Content operatorNotice = messages.of("signup.operator", newCompanyId);
        notificationService.createNotificationForAllOperatingUsers(
                operatorNotice.subject(), operatorNotice.body());

        String signupTime = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss"));
        String emptyValue = messages.text("signup.operator.mail.empty-value");
        String methodologiesStr = Optional.ofNullable(request.getMethodology())
                .map(list -> String.join(", ", list))
                .orElse(emptyValue);

        NotificationMessages.Content operatorMail = messages.of("signup.operator.mail",
                signupTime,
                newCompanyId,
                request.getCorporateNumber(),
                request.getCorporateName(),
                request.getCorporateNameKana(),
                request.getCompanyNameEnglish(),
                request.getCorporatePhoneNumber(),
                request.getPostalCode(),
                request.getPrefecture(),
                request.getMunicipality(),
                request.getBlocknum(),
                request.getBuildingName(),
                request.getLastName(),
                request.getFirstName(),
                request.getLastNameKana(),
                request.getFirstNameKana(),
                request.getPosition(),
                request.getEmail(),
                request.getPhoneNumber(),
                request.getFaxNumber(),
                methodologiesStr,
                Optional.ofNullable(request.getActivityInformation()).orElse(emptyValue));

        notificationService.sendEmailToAllOperatorCompanies(
                operatorMail.subject(), operatorMail.body());

        if (securityFlowProperties.getFeatures().isSignupEmailVerification()) {
            UUID challengeId = request.getEmailVerificationChallengeId();
            afterCommit(() -> otpChallengeService.consumeVerified(
                    challengeId, OtpPurpose.SIGNUP_EMAIL_VERIFICATION));
        }

        return new PartiCompanyResponseDetails(
                "CREATED",
                "登録申請が完了しました。",
                newCompanyId,
                "PENDING"
        );
    }

    private void afterCommit(Runnable runnable) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    runnable.run();
                }
            });
        } else {
            runnable.run();
        }
    }

    private String generateUniqueCompanyId() {
        SecureRandom random = new SecureRandom();
        String companyId;
        do {
            int randomNumber = 10000000 + random.nextInt(90000000);
            companyId = "COMP" + randomNumber;
        } while (repository.existsByCompanyId(companyId));
        return companyId;
    }
}
