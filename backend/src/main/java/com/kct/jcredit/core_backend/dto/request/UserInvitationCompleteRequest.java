package com.kct.jcredit.core_backend.dto.request;

import com.kct.jcredit.core_backend.validation.Katakana;
import com.kct.jcredit.core_backend.validation.PhoneNumber;
import com.kct.jcredit.core_backend.validation.ValidSecurityPassword;
import com.kct.jcredit.core_backend.validation.ValidUsername;
import com.kct.jcredit.core_backend.validation.Zenkaku;
import com.kct.jcredit.core_backend.validation.ZenkakuWithSymbols;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * Profile the invitee fills in after opening their invitation link.
 *
 * <p>Deliberately carries no email field: the address comes from the invitation the master issued,
 * so opening the link is what proves the invitee owns it.
 */
@Data
public class UserInvitationCompleteRequest {

    // ユーザー名
    @NotBlank(message = "ユーザー名を入力してください。")
    @Size(min = 3, max = 50, message = "ユーザー名は3文字以上50文字以内で入力してください。")
    @ValidUsername
    private String username;

    // 名前（名）
    @NotBlank(message = "名前（名）を入力してください。")
    @Size(max = 50, message = "名前（名）は50文字以内で入力してください。")
    @ZenkakuWithSymbols
    private String firstName;

    // 名前カナ（名）
    @NotBlank(message = "名前カナ（名）を入力してください。")
    @Size(max = 50, message = "名前カナ（名）は50文字以内で入力してください。")
    @Katakana
    private String firstNameKana;

    // 名前（姓）
    @NotBlank(message = "名前（姓）を入力してください。")
    @Size(max = 50, message = "名前（姓）は50文字以内で入力してください。")
    @ZenkakuWithSymbols
    private String lastName;

    // 名前カナ（姓）
    @NotBlank(message = "名前カナ（姓）を入力してください。")
    @Size(max = 50, message = "名前カナ（姓）は50文字以内で入力してください。")
    @Katakana
    private String lastNameKana;

    // 電話番号
    @NotBlank(message = "電話番号を入力してください。")
    @PhoneNumber
    @Size(min = 7, max = 16, message = "電話番号は7文字以上16文字以内で入力してください。")
    private String phoneNumber;

    // 役職
    @NotBlank(message = "担当者の役職を入力してください。")
    @Size(max = 50, message = "役職は50文字以内で入力してください。")
    @Zenkaku
    private String userPosition;

    // パスワード（他のリンク完了フローと同じ強度ルール）
    @NotBlank(message = "パスワードを入力してください。")
    @ValidSecurityPassword
    private String password;
}
