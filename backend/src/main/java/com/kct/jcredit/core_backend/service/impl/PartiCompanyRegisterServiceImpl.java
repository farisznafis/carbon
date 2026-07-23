package com.kct.jcredit.core_backend.service.impl;

import com.kct.jcredit.core_backend.dto.request.PartiCompanyInfoValidateRequest;
import com.kct.jcredit.core_backend.dto.request.PartiCompanyRegisterRequest;
import com.kct.jcredit.core_backend.dto.response.details.PartiCompanyResponseDetails;
import com.kct.jcredit.core_backend.entity.ParticipatingCompany;
import com.kct.jcredit.core_backend.enums.CompanyStatus;
import com.kct.jcredit.core_backend.repository.postgres.ParticipatingCompanyRegisterRepository;
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

        notificationService.createNotificationForAllOperatingUsers(
                "新規登録申請のお知らせ",
                "新しい参加企業の登録申請がありました。企業ID: " + newCompanyId
        );

        String signupTime = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss"));
        String methodologiesStr = Optional.ofNullable(request.getMethodology())
                .map(list -> String.join(", ", list))
                .orElse("なし");

        String emailBody = "【新規登録申請のお知らせ】\n\n"
                + "登録日時: " + signupTime + "\n"
                + "企業ID: " + newCompanyId + "\n\n"
                + "■ 法人情報\n"
                + "法人番号: " + request.getCorporateNumber() + "\n"
                + "法人名: " + request.getCorporateName() + "\n"
                + "法人名カナ: " + request.getCorporateNameKana() + "\n"
                + "会社名（英語）: " + request.getCompanyNameEnglish() + "\n"
                + "電話番号: " + request.getCorporatePhoneNumber() + "\n\n"
                + "■ 住所\n"
                + "郵便番号: " + request.getPostalCode() + "\n"
                + "都道府県: " + request.getPrefecture() + "\n"
                + "市区町村: " + request.getMunicipality() + "\n"
                + "番地: " + request.getBlocknum() + "\n"
                + "建物名: " + request.getBuildingName() + "\n\n"
                + "■ 担当者情報\n"
                + "氏名: " + request.getLastName() + " " + request.getFirstName() + "\n"
                + "氏名（カナ）: " + request.getLastNameKana() + " " + request.getFirstNameKana() + "\n"
                + "役職: " + request.getPosition() + "\n"
                + "メールアドレス: " + request.getEmail() + "\n"
                + "電話番号: " + request.getPhoneNumber() + "\n"
                + "FAX番号: " + request.getFaxNumber() + "\n\n"
                + "■ Methodology\n"
                + "選択方法論: " + methodologiesStr + "\n"
                + "補足説明: " + Optional.ofNullable(request.getActivityInformation()).orElse("なし");

        notificationService.sendEmailToAllOperatorCompanies(
                "新規登録申請のお知らせ",
                emailBody
        );

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
