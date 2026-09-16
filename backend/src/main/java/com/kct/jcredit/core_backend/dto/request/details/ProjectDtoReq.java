package com.kct.jcredit.core_backend.dto.request.details;

import com.kct.jcredit.core_backend.validation.ValidEmail;

import java.time.Instant;

import com.kct.jcredit.core_backend.validation.AnyCharacter;
import com.kct.jcredit.core_backend.validation.JapaneseText;
import com.kct.jcredit.core_backend.validation.Katakana;
import com.kct.jcredit.core_backend.validation.PhoneNumber;
import com.kct.jcredit.core_backend.validation.Zenkaku;
import com.kct.jcredit.core_backend.validation.ZenkakuWithSymbols;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ProjectDtoReq {

    @NotBlank(message = "プロジェクト名は必須です。")
    @Size(max = 100, message = "プロジェクト名は100文字以内で入力してください。")
    // Not @AnyCharacter: 計画書 page 2 copies this name read-only and only
    // accepts Japanese, Latin letters, digits and a few symbols.
    @JapaneseText(allowAlphanumeric = true,
            message = "プロジェクト名は日本語・英数字と許可された記号で入力してください。")
    private String projectName;

    @NotBlank(message = "プロジェクト番号は必須です。")
    @Size(max = 10, message = "プロジェクト番号は10文字以内で入力してください。")
    @Pattern(regexp = "^P[0-9]+$", message = "プロジェクト番号はPで始まる数字で入力してください。")
    private String projectNumber;

    @NotBlank(message = "プロジェクトステータスは必須です。")
    @Size(max = 10, message = "プロジェクトステータスは10文字以内で入力してください。")
    @Zenkaku(message = "プロジェクトステータスは全角文字で入力してください。")
    private String projectStatus;

    @NotNull(message = "開始日時は必須です。")
    @PastOrPresent(message = "開始日時は未来の日時を指定できません。")
    private Instant startedAt;

    @NotNull(message = "ベースライン排出量は必須です。")
    @DecimalMin(value = "0", message = "ベースライン排出量は0以上で入力してください。")
    private Double baselineEmissions;

    @NotNull(message = "プロジェクト実施後排出量は必須です。")
    @DecimalMin(value = "0", message = "プロジェクト実施後排出量は0以上で入力してください。")
    private Double postProjectEmission;

    @NotBlank(message = "代表者名（名）は必須です。")
    @Size(max = 50, message = "代表者名（名）は50文字以内で入力してください。")
    @ZenkakuWithSymbols(message = "代表者名（名）は全角文字と許可された記号で入力してください。")
    private String repFirstName;

    @NotBlank(message = "代表者名カナ（名）は必須です。")
    @Size(max = 50, message = "代表者名カナ（名）は50文字以内で入力してください。")
    @Katakana(message = "代表者名カナ（名）は全角カタカナで入力してください。")
    private String repFirstNameKana;

    @NotBlank(message = "代表者名（姓）は必須です。")
    @Size(max = 50, message = "代表者名（姓）は50文字以内で入力してください。")
    @ZenkakuWithSymbols(message = "代表者名（姓）は全角文字と許可された記号で入力してください。")
    private String repLastName;

    @NotBlank(message = "代表者名カナ（姓）は必須です。")
    @Size(max = 50, message = "代表者名カナ（姓）は50文字以内で入力してください。")
    @Katakana(message = "代表者名カナ（姓）は全角カタカナで入力してください。")
    private String repLastNameKana;

    @ValidEmail(message = "代表者メールアドレスの形式が正しくありません。")
    @NotBlank(message = "代表者メールアドレスは必須です。")
    @Size(max = 254, message = "代表者メールアドレスは254文字以内で入力してください。")
    private String repEmail;

    @NotBlank(message = "代表者電話番号は必須です。")
    @Size(max = 20, message = "代表者電話番号は20文字以内で入力してください。")
    @PhoneNumber(message = "代表者電話番号の形式が正しくありません。（例: 0312345678）")
    private String repPhoneNumber;

    @NotBlank(message = "代表者役職は必須です。")
    @Size(max = 100, message = "代表者役職は100文字以内で入力してください。")
    @Zenkaku(message = "代表者役職は全角文字で入力してください。")
    private String repPosition;

    @NotBlank(message = "担当者名（名）は必須です。")
    @Size(max = 50, message = "担当者名（名）は50文字以内で入力してください。")
    @ZenkakuWithSymbols(message = "担当者名（名）は全角文字と許可された記号で入力してください。")
    private String picFirstName;

    @NotBlank(message = "担当者名カナ（名）は必須です。")
    @Size(max = 50, message = "担当者名カナ（名）は50文字以内で入力してください。")
    @Katakana(message = "担当者名カナ（名）は全角カタカナで入力してください。")
    private String picFirstNameKana;

    @NotBlank(message = "担当者名（姓）は必須です。")
    @Size(max = 50, message = "担当者名（姓）は50文字以内で入力してください。")
    @ZenkakuWithSymbols(message = "担当者名（姓）は全角文字と許可された記号で入力してください。")
    private String picLastName;

    @NotBlank(message = "担当者名カナ（姓）は必須です。")
    @Size(max = 50, message = "担当者名カナ（姓）は50文字以内で入力してください。")
    @Katakana(message = "担当者名カナ（姓）は全角カタカナで入力してください。")
    private String picLastNameKana;

    @ValidEmail(message = "担当者メールアドレスの形式が正しくありません。")
    @NotBlank(message = "担当者メールアドレスは必須です。")
    @Size(max = 254, message = "担当者メールアドレスは254文字以内で入力してください。")
    private String picEmail;

    @NotBlank(message = "担当者電話番号は必須です。")
    @Size(max = 20, message = "担当者電話番号は20文字以内で入力してください。")
    @PhoneNumber(message = "担当者電話番号の形式が正しくありません。（例: 0312345678）")
    private String picPhoneNumber;

    @NotBlank(message = "担当者役職は必須です。")
    @Size(max = 100, message = "担当者役職は100文字以内で入力してください。")
    @Zenkaku(message = "担当者役職は全角文字で入力してください。")
    private String picPosition;

    @NotBlank(message = "データ管理責任者は必須です。")
    @Size(max = 50, message = "データ管理責任者は50文字以内で入力してください。")
    @ZenkakuWithSymbols(message = "データ管理責任者は全角文字と許可された記号で入力してください。")
    private String dataManagementPic;

    @NotBlank(message = "地域区分は必須です。")
    @Size(max = 10, message = "地域区分は10文字以内で入力してください。")
    @Zenkaku(message = "地域区分は全角文字で入力してください。")
    private String regionalCategory;

    @NotNull(message = "新規会員数は必須です。")
    @Min(value = 0, message = "新規会員数は0以上で入力してください。")
    private Integer newMembersTotal;

    @NotNull(message = "脱退会員数は必須です。")
    @Min(value = 0, message = "脱退会員数は0以上で入力してください。")
    private Integer withdrawnMembers;

    @NotNull(message = "累計会員数は必須です。")
    @Min(value = 0, message = "累計会員数は0以上で入力してください。")
    private Integer cumulativeMembers;

    @NotNull(message = "会員数は必須です。")
    @Min(value = 0, message = "会員数は0以上で入力してください。")
    private Integer membersTotal;

    @NotBlank(message = "方法論バージョンは必須です。")
    @Pattern(regexp = "^Ver\\.[0-9]+(\\.[0-9]+)?$", message = "方法論バージョンはVer.1またはVer.1.2の形式で入力してください。")
    @Size(max = 10, message = "方法論バージョンは10文字以内で入力してください。")
    private String methodologyVersion;

    @NotBlank(message = "方法論分類は必須です。")
    @Pattern(regexp = "^(EN-S|EN-R|IN|AG|WA)$", message = "方法論分類はEN-S、EN-R、IN、AG、WAのいずれかを入力してください。")
    @Size(max = 10, message = "方法論分類は10文字以内で入力してください。")
    private String methodCategory;

    @NotBlank(message = "方法論番号は必須です。")
    @Size(min = 3, max = 3, message = "方法論番号は3桁で入力してください。")
    @Pattern(regexp = "^[0-9]{3}$", message = "方法論番号は3桁の数字で入力してください。")
    private String methodNumber;

    @NotBlank(message = "方法論名は必須です。")
    @Size(max = 255, message = "方法論名は255文字以内で入力してください。")
    @ZenkakuWithSymbols(message = "方法論名は全角文字と許可された記号で入力してください。")
    private String methodName;

    @NotBlank(message = "方法論主対象ガスは必須です。")
    @Size(max = 255, message = "方法論主対象ガスは255文字以内で入力してください。")
    private String methodMainEmission;

    @NotNull(message = "方法論併用可否は必須です。")
    private Boolean methodCombiFeasibility;

    @NotBlank(message = "方法論詳細は必須です。")
    @Size(max = 1000, message = "方法論詳細は1000文字以内で入力してください。")
    private String methodDetails;

    @NotBlank(message = "利用目的は必須です。")
    @Size(max = 1000, message = "利用目的は1000文字以内で入力してください。")
    private String usePurpose;

    @NotBlank(message = "運営会社IDは必須です。")
    private String operatorCompanyId;
}
