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
    public static final String CONFLICT = "CONFLICT";
    public static final String EMAIL_ALREADY_USED = "EMAIL_ALREADY_USED";
    public static final String CORP_NUMBER_ALREADY_USED = "CORP_NUMBER_ALREADY_USED";
    public static final String PROJECT_NUMBER_ALREADY_USED = "PROJECT_NUMBER_ALREADY_USED";
    public static final String METHOD_NUMBER_ALREADY_USED = "METHOD_NUMBER_ALREADY_USED";
    public static final String LIMIT_EXCEEDED = "LIMIT_EXCEEDED";
    public static final String SERVICE_UNAVAILABLE = "SERVICE_UNAVAILABLE";
    public static final String OTP_REQUIRED = "OTP_REQUIRED";
    public static final String INVALID_OR_EXPIRED_TOKEN = "INVALID_OR_EXPIRED_TOKEN";
    public static final String TOO_MANY_REQUESTS = "TOO_MANY_REQUESTS";

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
    public static final String MSG_VALIDATION_SUCCESS = "入力内容に問題ありません。";
    public static final String MSG_ELPV_EVIDENCE_REQUIRED = "発電量（ELPV）の証憑ファイルをアップロードしてください。";
    public static final String MSG_ELPVR_EVIDENCE_REQUIRED = "自家消費量（ELPVR）の証憑ファイルをアップロードしてください。";
    public static final String MSG_ELPJ_BATTERY_EVIDENCE_REQUIRED = "蓄電池仕様書の証憑ファイルをアップロードしてください。";
    public static final String MSG_ELPJ_CONTROL_EVIDENCE_REQUIRED = "制御機器稼働時間（ELPJ）の証憑ファイルをアップロードしてください。";
    public static final String MSG_PROJECT_ID_REQUIRED = "プロジェクトIDを入力してください。";
    public static final String MSG_SITE_ID_REQUIRED = "拠点IDを入力してください。";
    public static final String MSG_EQUIPMENT_ID_REQUIRED = "設備IDを入力してください。";
    public static final String MSG_EQUIPMENT_NOT_FOUND = "該当する設備が見つかりませんでした。";
    public static final String MSG_PROJECT_TYPE_REQUIRED = "プロジェクトタイプを入力してください。";
    public static final String MSG_DATE_RECORDED_REQUIRED = "記録日を入力してください。";
    public static final String MSG_DATE_RECORDED_RANGE = "記録日は活動開始日以降、かつ本日以前の日付を入力してください。";
    public static final String MSG_DATE_RECORDED_DUPLICATE = "同一プロジェクト、同一拠点、同一記録日はすでに登録されています。";
    public static final String MSG_ELPJ_BATTERY_VALUE_REQUIRED = "バッテリーの値を入力してください。";
    public static final String MSG_BATTERY_NOT_APPLICABLE = "この設備には蓄電池が登録されていないため、バッテリー関連項目は入力できません。";
    public static final String MSG_ELPV_VALUE_MIN = "ELpv（電力使用量）は0以上で入力してください。";
    public static final String MSG_ELPVR_VALUE_MIN = "ELpvr（電力供給量）は0以上で入力してください。";
    public static final String MSG_ELPVR_EXCEEDS_ELPV = "自家消費量（ELPVR）は発電量（ELPV）を超えることはできません。";
    public static final String MSG_ELPJ_CONTROL_VALUE_MIN = "ELPJ制御値は0以上で入力してください。";
    public static final String MSG_ELPJ_BATTERY_VALUE_MIN = "ELPJバッテリー値は0以上で入力してください。";
    public static final String MSG_ELPJ_ACCURACY_REQUIRED = "測定精度を入力してください。";
    public static final String MSG_ELPJ_ACCURACY_MIN = "ELPJ計測精度は0以上で入力してください。";
    public static final String MSG_ELPJ_ACCURACY_MAX = "ELPJ計測精度は100以下で入力してください。";
    public static final String MSG_ELPJ_BATTERY_VALUE_REQUIRED_WHEN_CHANGED =
            "バッテリー交換がある場合、ELPJバッテリー値を入力してください。";
    public static final String MSG_USER_NOT_IN_COMPANY = "ユーザーはこの企業に所属していません。";
    public static final String MSG_SITE_NOT_IN_COMPANY = "サイトはこの企業に所属していません。";
    public static final String MSG_SITE_HAS_NO_COMPANY = "サイトに企業情報が設定されていません。";
    public static final String MSG_SITE_PROJECT_RELATION_NOT_FOUND = "サイトとプロジェクトの関連が見つかりませんでした。";
    public static final String MSG_MONITORING_RECORD_NOT_FOUND = "監視レコードが見つかりませんでした。";
    public static final String MSG_MONITORING_RECORD_REGISTERED = "監視レコードが正常に登録されました。";
    public static final String MSG_LOGIN_FAILED = "ユーザー名またはパスワードまたは企業IDが正しくありません。";
    public static final String MSG_USER_LOGIN_FAILED = "企業IDまたはメールアドレスまたはユーザー名またはパスワードが正しくありません。";
    public static final String MSG_OPERATOR_LOGIN_FAILED = "メールアドレスまたはユーザー名またはパスワードが正しくありません。";
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
    public static final String MSG_SUSPENDED = "アカウントは現在停止されています。管理者へお問い合わせください。";
    public static final String MSG_COMPANY_NOT_SUSPENDED = "会社は停止されていません。";
    public static final String MSG_REACTIVATION_SUCCESS = "アカウントが再開されました。";
    public static final String MSG_SUSPEND_SUCCESS = "会社が停止されました。";
    public static final String MSG_COMPANY_ALREADY_SUSPENDED = "会社はすでに停止されています。";
    public static final String MSG_COMPANY_NOT_PENDING = "会社は承認待ちではありません。";
    public static final String MSG_MASTER_USER_EXISTS = "この会社のマスターユーザーは既に存在します。";
    public static final String MSG_APPROVAL_EMAIL_FAILED = "マスターアカウントのメール送信に失敗しました。";
    public static final String MSG_COMPANY_APPROVED = "会社の承認が完了し、マスターユーザーが作成されました。";
    public static final String MSG_COMPANY_REJECTED = "会社の申請は却下され、登録データは削除されました。";
    public static final String MSG_COMPANY_REJECT_FAILED = "会社の却下処理に失敗しました。";
    public static final String MSG_RELOGIN_REQUIRED = "パスワード変更後のトークンは無効です。再度ログインしてください。";
    public static final String MSG_DELETE_SUCCESS = "会社が削除されました。";
    public static final String MSG_COMPANY_ALREADY_DELETED = "この会社は既に削除されています。";
    public static final String MSG_DELETION_REQUEST_SUBMITTED = "削除申請が提出されました。";
    public static final String MSG_DELETION_REQUEST_NOT_FOUND = "削除申請が見つかりませんでした。";
    public static final String MSG_DELETION_ALREADY_REQUESTED = "この会社の削除申請は既に提出されています。";
    public static final String MSG_DELETION_REQUEST_APPROVED = "削除申請が承認され、会社が削除されました。";
    public static final String MSG_DELETION_REQUEST_REJECTED = "削除申請が却下されました。";
    public static final String MSG_CANNOT_APPROVE_OWN_REQUEST = "自分の削除申請は承認できません。";
    public static final String MSG_DELETION_ALREADY_PROCESSED = "この削除申請は既に処理されています。";
    public static final String MSG_LIMIT_EXCEEDED = "上限に達しています。";
    
    // Company lifecycle messages
    public static final String MSG_COMPANY_LIST_RETRIEVED = "企業一覧を取得しました。";
    public static final String MSG_COMPANY_DETAIL_RETRIEVED = "企業詳細を取得しました。";
    public static final String MSG_COMPANY_NOT_FOUND = "対象の企業が見つかりませんでした。";
    public static final String MSG_MASTER_ACCOUNT_NOT_FOUND = "対象の企業にマスターアカウントが存在しません。";
    public static final String MSG_COMPANY_STATUS_CONFLICT = "現在の企業ステータスではこの操作を実行できません。";
    public static final String MSG_SUSPENSION_ALREADY_ACTIVE = "同じ理由の停止がすでに有効です。";
    public static final String MSG_SUSPENSION_CREATED = "企業の利用停止を登録しました。";
    public static final String MSG_SUSPENSION_NOT_FOUND = "対象の停止情報が見つかりませんでした。";
    public static final String MSG_SUSPENSION_ALREADY_RESOLVED = "この停止情報はすでに解除されています。";
    public static final String MSG_SUSPENSION_LIST_RETRIEVED = "企業の停止履歴を取得しました。";
    public static final String MSG_SUSPENSION_RESOLVED = "企業の停止理由を解除しました。";
    public static final String MSG_SUSPENSION_RESOLVED_COMPANY_STILL_SUSPENDED =
            "停止理由を解除しましたが、他の停止理由が残っているため企業は停止中です。";
    public static final String MSG_COMPANY_ACCESS_SUSPENDED = "企業は現在利用停止中です。";

    public static final String MSG_SERVICE_UNAVAILABLE = "セキュリティ確認サービスが一時的に利用できません。";
    public static final String MSG_OTP_REQUIRED = "メール認証コードの入力が必要です。";
    public static final String MSG_INVALID_OR_EXPIRED_TOKEN = "セキュリティトークンが無効か、有効期限が切れています。";
    public static final String MSG_TOO_MANY_REQUESTS = "リクエストが多すぎます。しばらくしてから再度お試しください。";

    // Security flow messages (action links, password reset, email OTP)
    public static final String MSG_ACTION_LINK_OPENED = "リンクを開きました。";
    public static final String MSG_MASTER_ACCOUNT_SETUP_COMPLETED = "マスターアカウントを作成しました。企業が有効化されました。";
    public static final String MSG_PASSWORD_RESET_EMAIL_SENT = "該当するアカウントが存在する場合、パスワードリセット用のメールを送信しました。";
    public static final String MSG_PASSWORD_RESET_LINK_QUEUED = "パスワードリセット用のリンクを送信しました。";
    public static final String MSG_PASSWORD_RESET_COMPLETED = "パスワードのリセットが完了しました。再度ログインしてください。";
    public static final String MSG_EMAIL_OTP_SETTING_UPDATED = "メールログイン認証設定を更新しました。";
    public static final String MSG_OTP_CODE_SENT = "認証コードを送信しました。";

    public static final String MSG_PAYMENT_FAILED = "支払いに失敗しました。";
    public static final String MSG_PAYMENT_DECLINED = "カード情報が無効です。";

    // Monitoring data deletion messages
    public static final String MSG_MONITORING_DELETION_CANDIDATES_RETRIEVED = "モニタリングデータの削除候補を取得しました。";
    public static final String MSG_MONITORING_DELETION_NO_CANDIDATES = "削除対象のモニタリングデータはありません。";
    public static final String MSG_MONITORING_DELETION_COMPLETED = "モニタリングデータの削除が完了しました。";
    public static final String MSG_MONITORING_DELETION_HISTORY_RETRIEVED = "モニタリングデータの削除履歴を取得しました。";
    public static final String MSG_MONITORING_DELETION_CANDIDATE_RECORDS_RETRIEVED = "モニタリングデータの削除候補明細を取得しました。";
    public static final String MSG_MONITORING_DELETION_PROJECT_NOT_CANDIDATE = "対象のプロジェクトは削除候補ではありません。";
    public static final String OPEN = "OPEN";
    public static final String APPROVED = "APPROVED";
    public static final String REJECTED = "REJECTED";

    // --- Project plan (プロジェクト計画書) ---
    public static final String MSG_FORM_NOT_DEFINED = "指定されたフォームは存在しません。";
    public static final String MSG_FORM_RETRIEVED = "フォーム情報を取得しました。";
    public static final String MSG_FORM_SAVED = "フォームを保存しました。";
    public static final String MSG_PROGRESS_RETRIEVED = "入力状況を取得しました。";

    private ResponseConstants() {}
}
