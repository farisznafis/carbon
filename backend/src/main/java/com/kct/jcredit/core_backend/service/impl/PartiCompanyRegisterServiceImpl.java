package com.kct.jcredit.core_backend.service.impl;

import com.kct.jcredit.core_backend.constant.ResponseConstants;
import com.kct.jcredit.core_backend.dto.request.PartiCompanyRegisterRequest;
import com.kct.jcredit.core_backend.dto.response.details.PartiCompanyResponseDetails;
import com.kct.jcredit.core_backend.entity.ParticipatingCompany;
import com.kct.jcredit.core_backend.exception.DuplicateResourceException;
import com.kct.jcredit.core_backend.repository.postgres.ParticipatingCompanyRegisterRepository;
import com.kct.jcredit.core_backend.service.PartiCompanyRegisterService;
import com.kct.jcredit.core_backend.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class PartiCompanyRegisterServiceImpl implements PartiCompanyRegisterService {

    private final NotificationService notificationService;
    private final ParticipatingCompanyRegisterRepository repository;

    @Override
    @Transactional
    public PartiCompanyResponseDetails register(PartiCompanyRegisterRequest request) {

        // 1. Tiered duplicate validation — corpNumber is the authoritative unique key
        //    corpNumber + PENDING → same company already submitted
        if (repository.findByCorpNumberAndRegStatus(request.getCorporateNumber(), "PENDING").isPresent()) {
            throw new DuplicateResourceException(ResponseConstants.ALREADY_SUBMITTED, ResponseConstants.MSG_ALREADY_SUBMITTED);
        }
        //    corpNumber + APPROVED/other (not PENDING, not REJECTED) → already registered to another party
        if (repository.existsByCorpNumberAndRegStatusNot(request.getCorporateNumber(), "REJECTED")) {
            throw new DuplicateResourceException(ResponseConstants.CORP_NUMBER_ALREADY_USED, ResponseConstants.MSG_CORP_NUMBER_ALREADY_USED);
        }

        // 2. Secondary email checks (different corpNumber → different company, so allow if email+PENDING)
        if (repository.findByEmailAndRegStatus(request.getCorporateEmail(), "APPROVED").isPresent()) {
            throw new DuplicateResourceException(ResponseConstants.EMAIL_ALREADY_USED, ResponseConstants.MSG_EMAIL_ALREADY_USED);
        }
        if (repository.findByRepEmailAndRegStatus(request.getEmail(), "APPROVED").isPresent()) {
            throw new DuplicateResourceException(ResponseConstants.EMAIL_ALREADY_USED, ResponseConstants.MSG_EMAIL_ALREADY_USED);
        }

        // 3. Find REJECTED record for reactivation (corpNumber is authoritative, email as fallback)
        ParticipatingCompany entity = repository.findByCorpNumberAndRegStatus(request.getCorporateNumber(), "REJECTED")
                .or(() -> repository.findByEmailAndRegStatus(request.getCorporateEmail(), "REJECTED"))
                .or(() -> repository.findByRepEmailAndRegStatus(request.getEmail(), "REJECTED"))
                .orElse(null);

        boolean isReactivation = entity != null;
        if (!isReactivation) {
            entity = new ParticipatingCompany();
        }

        // 4. dto -> entity
        entity.setCorpNumber(request.getCorporateNumber());
        entity.setNameJa(request.getCorporateName());
        entity.setNameFurigana(request.getCorporateNameKana());
        entity.setNameEn(request.getCompanyNameEnglish());
        entity.setEmail(request.getCorporateEmail());
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
        entity.setRepEmail(request.getEmail());
        entity.setRepPhoneNumber(request.getPhoneNumber());
        entity.setFax(request.getFaxNumber());

        // 5. Generate new company ID (fresh ID for reactivations too)
        String newCompanyId = generateUniqueCompanyId();
        entity.setCompanyId(newCompanyId);
        entity.setRegStatus("PENDING");
        entity.setIsActive(true);
        entity.setContractEndDate(LocalDate.now().plusYears(1));

        // 6. save to database
        repository.save(entity);

        // Notify all operating users regardless of company (simple)
        notificationService.createNotificationForAllOperatingUsers(
                "新規登録申請のお知らせ",
                "新しい参加企業の登録申請がありました。企業ID: " + newCompanyId
        );

        // Build detailed email body with all registration info
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
                + "メールアドレス: " + request.getCorporateEmail() + "\n"
                + "電話番号: " + request.getCorporatePhoneNumber() + "\n\n"
                + "■ 住所\n"
                + "郵便番号: " + request.getPostalCode() + "\n"
                + "都道府県: " + request.getPrefecture() + "\n"
                + "市区町村: " + request.getMunicipality() + "\n"
                + "番地: " + request.getBlocknum() + "\n"
                + "建物名: " + request.getBuildingName() + "\n\n"
                + "■ 代表者情報\n"
                + "氏名: " + request.getLastName() + " " + request.getFirstName() + "\n"
                + "氏名（カナ）: " + request.getLastNameKana() + " " + request.getFirstNameKana() + "\n"
                + "役職: " + request.getPosition() + "\n"
                + "メールアドレス: " + request.getEmail() + "\n"
                + "電話番号: " + request.getPhoneNumber() + "\n"
                + "FAX番号: " + request.getFaxNumber() + "\n\n"
                + "■ Methodology\n"
                + "選択方法論: " + methodologiesStr + "\n"
                + "補足説明: " + Optional.ofNullable(request.getActivityInformation()).orElse("なし");

        // Send email to operator company contacts (detailed)
        notificationService.sendEmailToAllOperatorCompanies(
                "新規登録申請のお知らせ",
                emailBody
        );


        // 7. Return Response
        return new PartiCompanyResponseDetails(
                "CREATED",
                "登録申請が完了しました。",
                newCompanyId,
                "PENDING"
        );
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
