package com.kct.jcredit.core_backend.dto.projectplan;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.kct.jcredit.core_backend.validation.DateFormat;
import com.kct.jcredit.core_backend.validation.JapaneseText;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;

/**
 * 認証権限保有主体変更届.
 *
 * <p>Filed only when the project's owner changes — the account allowed to apply
 * for certification. The sheet is hidden in the blank workbook and the
 * instructions say to unhide it, fill it, and then pick 認証権限保有主体変更 as
 * the 申請区分 on 登録申請書 (はじめに, rows 44–51). So the page is registered
 * {@code optional}: reachable, but not counted towards completion and never
 * offered as 続きから.
 *
 * <p>Two blocks, matching the sheet:
 *
 * <ul>
 *   <li>１．プロジェクト登録情報 — the project being transferred</li>
 *   <li>２．所有者変更内容 — the incoming owner</li>
 * </ul>
 *
 * <p>Not in db/projectplan.xlsx at all; taken from
 * {@code 別紙7.プロジェクト計画書フォーマット.xlsx}, sheet
 * 認証権限保有主体変更届（所有者を変更する方のみ提出）.
 */
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@DynamoDbBean
public class AuthorityHolderChangeRequest extends ProjectPlanFormBase {

    /** 届出日 (西暦年月日) */
    @NotBlank
    @DateFormat
    @JsonProperty("submissionDate")
    private String submissionDate;

    // --- １．プロジェクト登録情報 ---

    /** プロジェクト番号 */
    @NotBlank
    @Size(max = 50)
    @JsonProperty("projectNumber")
    private String projectNumber;

    /** プロジェクト実施者名（プログラム型運営・管理者名） */
    @NotBlank
    @Size(max = 255)
    @JapaneseText(allowAlphanumeric = true)
    @JsonProperty("projectOperatorName")
    private String projectOperatorName;

    // --- ２．所有者変更内容 ---

    /**
     * 変更後の口座番号.
     *
     * <p>The sheet prints the fixed part of the number around the input —
     * {@code JP-100-20000-00001-} … {@code -00} — so what the operator enters
     * is one segment, not the whole thing. Stored whole, with no character-set
     * rule: it is an account number, not prose.
     */
    @NotBlank
    @Size(max = 50)
    @JsonProperty("newAccountNumber")
    private String newAccountNumber;

    /** 変更後の法人名 */
    @NotBlank
    @Size(max = 255)
    @JapaneseText(allowAlphanumeric = true)
    @JsonProperty("newCorpName")
    private String newCorpName;

    /** 変更後の法人の担当者所属・氏名 */
    @NotBlank
    @Size(max = 255)
    @JapaneseText(allowAlphanumeric = true)
    @JsonProperty("newPicPositionAndName")
    private String newPicPositionAndName;

    /** 変更後の法人の担当者連絡先（メールアドレス） */
    @NotBlank
    @Email
    @Size(max = 255)
    @JsonProperty("newPicEmail")
    private String newPicEmail;
}
