package com.kct.jcredit.core_backend.constant;

public class ResponseConstants {
    // Status codes
    public static final String OK = "OK";
    public static final String UNAUTHORIZED = "UNAUTHORIZED";
    public static final String FORBIDDEN = "FORBIDDEN";
    public static final String NOT_FOUND = "NOT_FOUND";
    public static final String INTERNAL_SERVER_ERROR = "INTERNAL_SERVER_ERROR";
    public static final String BAD_REQUEST = "BAD_REQUEST";
    public static final String VALIDATION_ERROR = "VALIDATION_ERROR";
    public static final String INVALID_CREDENTIALS = "INVALID_CREDENTIALS";
    public static final String INVALID_CURRENT_PASSWORD = "INVALID_CURRENT_PASSWORD";
    public static final String SAME_PASSWORD = "SAME_PASSWORD";
    public static final String USED_EMAIL = "USED_EMAIL";
    public static final String USED_USERNAME = "USED_USERNAME";
    public static final String ACCOUNT_LOCKED = "ACCOUNT_LOCKED";
    public static final String FILE_TOO_LARGE = "FILE_TOO_LARGE";
    public static final String UNSUPPORTED_FILE_TYPE = "UNSUPPORTED_FILE_TYPE";
    public static final String ALREADY_SUBMITTED = "ALREADY_SUBMITTED";
    public static final String SUSPENDED = "SUSPENDED";
    public static final String PAYMENT_REQUIRED = "PAYMENT_REQUIRED";
    public static final String CONFLICT = "CONFLICT";
    public static final String EMAIL_ALREADY_USED = "EMAIL_ALREADY_USED";
    public static final String CORP_NUMBER_ALREADY_USED = "CORP_NUMBER_ALREADY_USED";
    public static final String PROJECT_NUMBER_ALREADY_USED = "PROJECT_NUMBER_ALREADY_USED";
    public static final String METHOD_NUMBER_ALREADY_USED = "METHOD_NUMBER_ALREADY_USED";

    // Messages
    public static final String MSG_LOGIN_NEEDED = "ログインが必要です。";
    public static final String MSG_UNAUTHORIZED = "アクセスできません。";
    public static final String MSG_NOT_FOUND = "該当するリソースが見つかりませんでした。";
    public static final String MSG_SITE_NOT_FOUND = "該当するサイトが見つかりませんでした。";
    public static final String MSG_PROJECT_NOT_FOUND = "該当するプロジェクトが見つかりませんでした。";
    public static final String MSG_USER_NOT_FOUND = "該当するユーザーが見つかりませんでした。";
    public static final String MSG_INTERNAL_ERROR = "サイト情報の取得中にエラーが発生しました。";
    public static final String MSG_BAD_REQUEST = "入力値に誤りがあります。";
    public static final String MSG_VALIDATION_ERROR = "入力内容に誤りがあります。";
    public static final String MSG_LOGIN_FAILED = "ユーザー名またはパスワードまたは企業IDが正しくありません。";
    public static final String MSG_UNEXPEXTED_ERROR = "想定外エラーがあります。";
    public static final String MSG_FORBIDDEN = "このリソースにアクセスすることができません。";
    public static final String MSG_INVALID_CURRENT_PASSWORD = "現在のパスワードが正しくありません。";
    public static final String MSG_SAME_PASSWORD = "新しいパスワードは現在のパスワードと異なる必要があります。";
    public static final String MSG_USED_EMAIL = "このメールアドレスは既に使用されています。";
    public static final String MSG_EMAIL_ALREADY_USED = "このメールアドレスは既に使用されています。";
    public static final String MSG_CORP_NUMBER_ALREADY_USED = "この法人番号は既に登録されています。";
    public static final String MSG_DUPLICATE_INPUT_DATA = "入力データに重複があります。";
    public static final String MSG_PROJECT_NUMBER_ALREADY_USED = "このプロジェクト番号は既に登録されています。";
    public static final String MSG_PROJECT_NUMBER_DUPLICATED_IN_REQUEST = "同じリクエスト内でプロジェクト番号が重複しています。";
    public static final String MSG_METHOD_NUMBER_ALREADY_USED = "この方法論番号は既に登録されています。";
    public static final String MSG_METHOD_NUMBER_DUPLICATED_IN_REQUEST = "同じリクエスト内で方法論番号が重複しています。";
    public static final String MSG_USED_USERNAME = "このユーザ名は既に使用されています。";
    public static final String MSG_ACCOUNT_LOCKED = "アカウントがロックされています。しばらくしてから再度お試しください。";
    public static final String MSG_FILE_TOO_LARGE = "アップロードされたファイルは、許可されている最大サイズを超えています。";
    public static final String MSG_UNSUPPORTED_FILE_TYPE = "このファイル形式はアップロードできません。";
    public static final String MSG_ALREADY_SUBMITTED = "こちらのフォームはすでに提出済みのため、内容の変更はできません。";
    public static final String MSG_SUSPENDED = "アカウントは現在停止されています。利用を再開するにはお支払いを確認してください。";
    public static final String MSG_COMPANY_NOT_SUSPENDED = "社は停止されていません。";
    public static final String MSG_REACTIVATION_SUCCESS = "アカウントが再開されました。";
    public static final String MSG_PAYMENT_FAILED = "支払いに失敗しました。";
    public static final String MSG_PAYMENT_DECLINED = "カード情報が無効です。";
    public static final String MSG_SUSPEND_SUCCESS = "会社が停止されました。";
    public static final String MSG_COMPANY_ALREADY_SUSPENDED = "社はすでに停止されています。";
    public static final String MSG_COMPANY_NOT_PENDING = "会社は承認待ちではありません。";
    public static final String MSG_MASTER_USER_EXISTS = "この会社のマスターユーザーは既に存在します。";
    public static final String MSG_APPROVAL_EMAIL_FAILED = "マスターアカウントのメール送信に失敗しました。";
    public static final String MSG_COMPANY_APPROVED = "会社の承認が完了し、マスターユーザーが作成されました。";
    public static final String MSG_COMPANY_REJECTED = "会社の申請は却下され、登録データは削除されました。";
    public static final String MSG_COMPANY_REJECT_FAILED = "会社の却下処理に失敗しました。";
    public static final String MSG_RELOGIN_REQUIRED = "パスワード変更後のトークンは無効です。再度ログインしてください。";
    
    private ResponseConstants() {}
}
