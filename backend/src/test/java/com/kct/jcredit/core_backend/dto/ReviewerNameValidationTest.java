package com.kct.jcredit.core_backend.dto;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import com.kct.jcredit.core_backend.dto.projectplan.ProjectApplicationRequest;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;

/**
 * The 審査機関 names are a fixed official list (【削除不可】プルダウン用 sheet of
 * sakuseirei_keikaku_solar_program.xlsx). Six of them contain half-width Latin,
 * ASCII spaces or ASCII parentheses, so a full-width-only rule silently makes
 * half the dropdown unsavable. This pins the whole list against the DTO.
 */
class ReviewerNameValidationTest {

    private static final Validator VALIDATOR =
            Validation.buildDefaultValidatorFactory().getValidator();

    /** All 14 官方 entries, including the two the frontend list is currently missing. */
    static List<String> officialReviewerNames() {
        return List.of(
                "ペリージョンソンレジストラークリーンディベロップメントメカニズム株式会社",
                "一般社団法人 日本能率協会",
                "一般財団法人日本品質保証機構",
                "一般財団法人日本海事協会",
                "ソコテック・サーティフィケーション・ジャパン株式会社",
                "環境省(ダミーの審査機関)",
                "株式会社トーマツ審査評価機構",
                "日本海事検定キューエイ株式会社",
                "株式会社 JACO CDM",
                "ロイド レジスター クオリティ アシュアランス リミテッドジャパン",
                "SGS ジャパン株式会社 認証サービス事業部",
                "ビューローベリタスジャパン株式会社 システム認証事業本部",
                "日本検査キューエイ株式会社",
                "株式会社日本スマートエナジー認証機構");
    }

    @ParameterizedTest
    @MethodSource("officialReviewerNames")
    void everyOfficialAgencyIsAcceptedAsReviewerName(String agency) {
        ProjectApplicationRequest request = new ProjectApplicationRequest();
        request.setReviewerName(agency);

        assertThat(fieldsWithErrors(request))
                .as("reviewerName should accept the official name %s", agency)
                .doesNotContain("reviewerName");
    }

    @ParameterizedTest
    @MethodSource("officialReviewerNames")
    void everyOfficialAgencyIsAcceptedAsValidationCheckReviewerName(String agency) {
        ProjectApplicationRequest request = new ProjectApplicationRequest();
        request.setValidationCheckReviewerName(agency);

        assertThat(fieldsWithErrors(request))
                .as("validationCheckReviewerName should accept the official name %s", agency)
                .doesNotContain("validationCheckReviewerName");
    }

    @Test
    void bothReviewerFieldsStillRejectScriptLikeInput() {
        ProjectApplicationRequest request = new ProjectApplicationRequest();
        request.setReviewerName("<script>alert(1)</script>");
        request.setValidationCheckReviewerName("<script>alert(1)</script>");

        Set<String> errors = fieldsWithErrors(request);
        assertThat(errors).contains("reviewerName", "validationCheckReviewerName");
    }

    private static Set<String> fieldsWithErrors(Object target) {
        Set<ConstraintViolation<Object>> violations = VALIDATOR.validate(target);
        return violations.stream()
                .map(violation -> violation.getPropertyPath().toString())
                .collect(Collectors.toSet());
    }
}
