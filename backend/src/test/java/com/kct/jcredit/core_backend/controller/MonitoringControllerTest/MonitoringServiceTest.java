package com.kct.jcredit.core_backend.controller.MonitoringControllerTest;
/**
 * MonitoringServiceImpl business-logic test outline.
 *
 * Mock all repositories (userRepository, siteRepository, projectRepository,
 * siteProjectRelationRepository, equipmentRepository,
 * monitoringRecordSolarRepository, cancelRequestRepository, s3クライアント等)。
 * Controllerを経由しないため@Valid等のBean Validationは対象外。
 * Service内部のnullチェック・業務ルール・権限判定・例外処理を直接検証する。
 */
class MonitoringServiceTest {

    @Nested
    @DisplayName("POST /monitoring のテスト")
    class RegisterMonitoringRecordSolarTest {

        @Test
        @DisplayName("UT-MS-001: requestがnullの場合はBAD_REQUESTエラーを返す")
        void registerMonitoringRecordSolar_nullRequest_returnsBadRequest() {
            // Input: request=null
            // Output: errors={request: MSG_BAD_REQUEST}が返却される
            //         collectMonitoringFieldErrorsのnullガードが機能すること
        }

        @Test
        @DisplayName("UT-MS-002: 必須項目(projectId/siteId/equipmentId/projectType/dateRecorded)が未設定の場合に個別エラーを返す")
        void registerMonitoringRecordSolar_missingRequiredFields_returnsFieldErrors() {
            // Input: 各項目を順にnullまたは空文字にしたrequest
            // Output: 各項目に対応するMSG_xxx_REQUIREDがerrorsに設定される
            //         collectMonitoringFieldErrorsの必須チェックが項目ごとに機能すること
        }

        @Test
        @DisplayName("UT-MS-003: dateRecordedの形式不正・未来日の場合は日付レンジエラーを返す")
        void registerMonitoringRecordSolar_invalidOrFutureDateRecorded_returnsDateRangeError() {
            // Input: dateRecorded=不正形式の文字列、または翌日以降の日付
            // Output: errors={dateRecorded: MSG_DATE_RECORDED_RANGE}が返却される
            //         parseDateRecordedとcollectMonitoringPayloadDateErrorsの未来日判定が機能すること
        }

        @Test
        @DisplayName("UT-MS-004: 必須証憑(ELpv/ELpvr)のURLが未提出の場合はエラーを返す")
        void registerMonitoringRecordSolar_missingMandatoryEvidenceUrl_returnsEvidenceError() {
            // Input: elPvEvidenceUrl=null、elPvrEvidenceUrl=null
            // Output: errors={elpv_evidence,…}/{elpvr_evidence,…}が返却される
            //         collectMandatoryEvidenceErrorsが必須2項目のみチェックすること
        }

        @Test
        @DisplayName("UT-MS-005: ELPJcontrol証憑は未提出でもバリデーションエラーにならない（任意項目）")
        void registerMonitoringRecordSolar_missingElpjControlEvidence_noValidationError() {
            // Input: elpjControlEvidenceUrl=null、他項目は有効
            // Output: elpj_control_evidenceに関するエラーが発生しない
            //         コメントアウトにより証憑任意化されていることの確認
        }

        @Test
        @DisplayName("UT-MS-006: ELpv/ELpvrがnullまたは負数の場合は最小値エラーを返す")
        void registerMonitoringRecordSolar_nullOrNegativeElpvElpvr_returnsMinValueError() {
            // Input: elPv=null/-1、elPvr=null/-1
            // Output: errors={ELpv_value: MSG_ELPV_VALUE_MIN}/{ELpvr_value: MSG_ELPVR_VALUE_MIN}が返却される
            //         collectMonitoringDataErrorsの最小値チェックが機能すること
        }

        @Test
        @DisplayName("UT-MS-007: ELpvrがELpvを超過した場合のみエラー、同値は許容される")
        void registerMonitoringRecordSolar_elpvrExceedsElpv_returnsErrorOnlyWhenExceeding() {
            // Input: elPv=100、elPvr=150（異常）／elPvr=100（正常）
            // Output: 超過時はerrors={ELpvr_value: MSG_ELPVR_EXCEEDS_ELPV}、同値時はエラーなし
            //         ELpvr>ELpvの比較ロジックの境界値が正しいこと
        }

        @Test
        @DisplayName("UT-MS-008: ELPJcontrol/ELPJbatteryValueが負数の場合は最小値エラーを返す")
        void registerMonitoringRecordSolar_negativeElpjControlOrBatteryValue_returnsMinValueError() {
            // Input: elpjControl=-1、elpjBatteryValue=-1
            // Output: errors={ELPJ_control_value,…}/{ELPJ_battery_value: MSG_ELPJ_BATTERY_VALUE_MIN}が返却される
            //         負数チェックが各項目で機能すること
        }

        @Test
        @DisplayName("UT-MS-009: ELPJaccuracyがnull・負数・最大値超過でエラー、最大値と同値は許容される")
        void registerMonitoringRecordSolar_elpjAccuracyOutOfRange_returnsErrorExceptBoundary() {
            // Input: elpjAccuracy=null/-1/MAX+1（異常）／MAX_PERCENT_VALUE（正常）
            // Output: 異常時はMSG_ELPJ_ACCURACY_REQUIRED/MIN/MAXが返却、境界値は許容される
            //         ELPJaccuracyの必須・範囲チェックと境界値が正しいこと
        }

        @Test
        @DisplayName("UT-MS-010: ユーザーが存在しない場合は内部エラーとなる")
        void registerMonitoringRecordSolar_userNotFound_returnsInternalError() {
            // Input: username=DBに存在しない値
            // Output: RuntimeExceptionが発生しINTERNAL_SERVER_ERROR相当となる
            //         userRepository.findByUsernameのorElseThrowが機能すること
        }

        @Test
        @DisplayName("UT-MS-011: ユーザーが指定会社に未所属の場合はFORBIDDENを返す")
        void registerMonitoringRecordSolar_userNotInCompany_returnsForbidden() {
            // Input: user.company=null、またはcompanyId不一致
            // Output: FORBIDDEN、MSG_USER_NOT_IN_COMPANYが返却される
            //         会社所属チェックが機能すること
        }

        @Test
        @DisplayName("UT-MS-012: 拠点またはプロジェクトが存在しない、または所属会社が不一致の場合はエラーを返す")
        void registerMonitoringRecordSolar_siteOrProjectInvalid_returnsNotFoundOrForbidden() {
            // Input: siteId不正／site.company不一致／projectId不正
            // Output: NOT_FOUND(MSG_SITE_NOT_FOUND・MSG_PROJECT_NOT_FOUND)またはFORBIDDEN(MSG_SITE_NOT_IN_COMPANY)が返却される
            //         拠点・プロジェクトの存在確認と会社整合性チェックが機能すること
        }

        @Test
        @DisplayName("UT-MS-013: 会社不一致・USER権限で未割当拠点・不明roleの場合はFORBIDDENを返す")
        void registerMonitoringRecordSolar_resourceOwnershipOrRoleInvalid_returnsForbidden() {
            // Input: 各条件を満たすuser/site組合せ
            // Output: FORBIDDEN、MSG_FORBIDDENが返却される
            //         validateResourceOwnership/hasAccessToSiteの各分岐が機能すること
        }

        @Test
        @DisplayName("UT-MS-014: USER権限で割当拠点、MASTER権限で会社内任意拠点にアクセスできる")
        void registerMonitoringRecordSolar_validRoleAccess_proceedsToNextValidation() {
            // Input: user.role=user(site一致)／user.role=master
            // Output: アクセス許可され後続の検証に進む
            //         role別アクセス制御が正しく許可されること
        }

        @Test
        @DisplayName("UT-MS-015: 拠点とプロジェクトの関連が存在しない場合はNOT_FOUNDを返す")
        void registerMonitoringRecordSolar_siteProjectRelationNotFound_returnsNotFound() {
            // Input: existsByProjectAndSite=false
            // Output: NOT_FOUND、MSG_NOT_FOUNDが返却される
            //         Site-Project関連チェックが機能すること
        }

        @Test
        @DisplayName("UT-MS-016: 設備が存在しない、または別拠点に属する場合はNOT_FOUNDを返す")
        void registerMonitoringRecordSolar_equipmentNotFoundOrWrongSite_returnsNotFound() {
            // Input: equipmentId不正／equipment.site!=site
            // Output: NOT_FOUND、MSG_EQUIPMENT_NOT_FOUNDが返却される
            //         設備の存在確認と拠点整合性チェックが機能すること
        }

        @Test
        @DisplayName("UT-MS-017: dateRecordedが活動開始日より前、または重複登録の場合はエラーを返す")
        void registerMonitoringRecordSolar_dateBeforeActivityStartOrDuplicate_returnsBusinessError() {
            // Input: dateRecorded<activityStartDate／既存アクティブレコードと同一日付
            // Output: errors={dateRecorded: MSG_DATE_RECORDED_RANGE}または{dateRecorded: MSG_DATE_RECORDED_DUPLICATE}が返却される
            //         collectMonitoringBusinessErrorsの日付範囲・重複チェックが機能すること
        }

        @Test
        @DisplayName("UT-MS-018: 設備hasBattery=trueの場合に実測値または証憑URLが未入力だとエラーを返す")
        void registerMonitoringRecordSolar_hasBatteryTrueMissingValueOrEvidence_returnsRequiredError() {
            // Input: equipment.hasBattery=true、elpjBatteryValue=nullまたはelpjBatterySpecificationUrl=null
            // Output: MSG_ELPJ_BATTERY_VALUE_REQUIREDまたはMSG_ELPJ_BATTERY_EVIDENCE_REQUIREDが返却される
            //         collectBatteryEligibilityErrorsの蓄電池あり分岐が機能すること
        }

        @Test
        @DisplayName("UT-MS-019: 設備hasBattery=falseなのに値または証憑を送信した場合はエラーを返す")
        void registerMonitoringRecordSolar_hasBatteryFalseWithValueOrEvidence_returnsNotApplicableError() {
            // Input: equipment.hasBattery=false、elpjBatteryValueまたはelpjBatterySpecificationUrlに値あり
            // Output: errors={ELPJ_battery_value: MSG_BATTERY_NOT_APPLICABLE}が返却される
            //         蓄電池なし設備への送信拒否ロジックが機能すること
        }

        @Test
        @DisplayName("UT-MS-020: 設備のhasBatteryに応じて適切な入力の場合はエラーにならない")
        void registerMonitoringRecordSolar_hasBatteryConsistentInput_noValidationError() {
            // Input: hasBattery=trueで値・証憑とも有効／hasBattery=falseで値・証憑とも未入力
            // Output: 蓄電池関連のエラーが発生しない
            //         hasBattery判定ロジックの正常系が機能すること
        }

        @Test
        @DisplayName("UT-MS-021: 全条件を満たすrequestで登録が成功する")
        void registerMonitoringRecordSolar_allValid_returnsOk() {
            // Input: 必須項目・証跡・業務ルールを全て満たす有効なrequest
            // Output: OKレスポンスで登録済みレコードが返却される
            //         登録処理全体の正常系が機能すること
        }

        @Test
        @DisplayName("UT-MS-022: 処理中に予期せぬ例外が発生した場合は内部エラーを返す")
        void registerMonitoringRecordSolar_unexpectedException_returnsInternalError() {
            // Input: DB接続エラー等をモックで発生させる
            // Output: INTERNAL_SERVER_ERROR、MSG_INTERNAL_ERRORが返却される
            //         try-catchによる例外ハンドリングが機能すること
        }
    }

    @Nested
    @DisplayName("GET /monitoring/{projectId}/list のテスト")
    class ReadLastMonitoringRecordSolarTest {

        @Test
        @DisplayName("UT-MS-023: ユーザー・拠点・プロジェクトいずれかが不正または権限不足の場合はエラーを返す")
        void readLastMonitoringRecordSolar_invalidUserSiteOrProject_returnsError() {
            // Input: username不正／company不一致／siteId不正／site.company不一致／projectId不正
            // Output: 各ケースに対応するエラー(INTERNAL_SERVER_ERROR・MSG_USER_NOT_IN_COMPANY・MSG_SITE_NOT_FOUND・MSG_SITE_NOT_IN_COMPANY・MSG_PROJECT_NOT_FOUND)が返却される
            //         registerと共通の検証ロジックが同様に機能すること
        }

        @Test
        @DisplayName("UT-MS-024: 権限がない、またはSite-Project関連が存在しない場合はエラーを返す")
        void readLastMonitoringRecordSolar_ownershipOrRelationInvalid_returnsError() {
            // Input: validateResourceOwnership=false／relation不存在
            // Output: FORBIDDEN(MSG_FORBIDDEN)またはNOT_FOUND(MSG_NOT_FOUND)が返却される
            //         権限・関連性チェックが機能すること
        }

        @Test
        @DisplayName("UT-MS-025: 最新レコードが存在しない場合はNOT_FOUNDを返す")
        void readLastMonitoringRecordSolar_recordNotFound_returnsNotFound() {
            // Input: 該当データなし
            // Output: NOT_FOUND、MSG_MONITORING_RECORD_NOT_FOUNDが返却される
            //         データ有無チェックが機能すること
        }

        @Test
        @DisplayName("UT-MS-026: 最新の1件を正常に取得できる")
        void readLastMonitoringRecordSolar_dataExists_returnsOk() {
            // Input: 対象データが1件以上存在
            // Output: OK、最新レコード1件が返却される
            //         最新レコード取得ロジックが機能すること
        }

        @Test
        @DisplayName("UT-MS-027: 処理中に例外が発生した場合は内部エラーを返す")
        void readLastMonitoringRecordSolar_unexpectedException_returnsInternalError() {
            // Input: DB例外等をモックで発生させる
            // Output: INTERNAL_SERVER_ERROR、MSG_INTERNAL_ERRORが返却される
            //         例外ハンドリングが機能すること
        }
    }

    @Nested
    @DisplayName("GET /monitoring のテスト")
    class ReadAllMonitoringRecordsSolarTest {

        @Test
        @DisplayName("UT-MS-028: 各検証条件が不正の場合は対応するエラーを返す")
        void readAllMonitoringRecordsSolar_invalidValidationConditions_returnsError() {
            // Input: username不正／company不一致／siteId不正／projectId不正／権限不足／relation不存在
            // Output: 各ケースに対応するエラーコード・メッセージが返却される
            //         registerと共通の検証ロジックが同様に機能すること
        }

        @Test
        @DisplayName("UT-MS-029: pageとlimitの指定値に応じて適切に補正される")
        void readAllMonitoringRecordsSolar_pageAndLimit_correctsAsExpected() {
            // Input: page=null/0、limit=null/0/101/100
            // Output: page=1に補正、limitはnull/0時50に補正、101以上は100に補正、100はそのまま
            //         ページング補正ロジック(デフォルト値・上限)が機能すること
        }

        @Test
        @DisplayName("UT-MS-030: 該当データが0件の場合はNOT_FOUNDを返す")
        void readAllMonitoringRecordsSolar_noData_returnsNotFound() {
            // Input: データなし
            // Output: NOT_FOUND、MSG_MONITORING_RECORD_NOT_FOUNDが返却される
            //         0件判定が機能すること
        }

        @Test
        @DisplayName("UT-MS-031: 複数件のレコードをページ単位で正常に取得できる")
        void readAllMonitoringRecordsSolar_dataExists_returnsOkWithPagination() {
            // Input: データ複数件あり、page=1、limit=50
            // Output: OK、items一覧とpagination情報が返却される
            //         一覧取得・ページング処理全体が機能すること
        }

        @Test
        @DisplayName("UT-MS-032: 処理中に例外が発生した場合は内部エラーを返す")
        void readAllMonitoringRecordsSolar_unexpectedException_returnsInternalError() {
            // Input: DB例外等をモックで発生させる
            // Output: INTERNAL_SERVER_ERROR、MSG_INTERNAL_ERRORが返却される
            //         例外ハンドリングが機能すること
        }
    }

    @Nested
    @DisplayName("GET /monitoring/{siteId}/{projectId}/list のテスト")
    class ReadSiteMonitoringRecordsSolarTest {

        @Test
        @DisplayName("UT-MS-033: ユーザーが存在しない場合の例外伝播範囲を確認する（要レビュー）")
        void readSiteMonitoringRecordsSolar_userNotFound_checksExceptionPropagationScope() {
            // Input: username不正
            // Output: 他APIと異なりRuntimeExceptionがcatchされず呼び出し元に伝播する可能性がある
            //         orElseThrowのcatch範囲の実装差異を確認すること（要レビュー）
        }

        @Test
        @DisplayName("UT-MS-034: 各検証条件が不正の場合は対応するエラーを返す")
        void readSiteMonitoringRecordsSolar_invalidValidationConditions_returnsError() {
            // Input: company不一致／siteId不正／site.company不一致／projectId不正／権限不足／relation不存在
            // Output: 各ケースに対応するエラーコード・メッセージが返却される
            //         readAllと共通の検証ロジックが同様に機能すること
        }

        @Test
        @DisplayName("UT-MS-035: pageとlimitの指定値に応じて適切に補正される")
        void readSiteMonitoringRecordsSolar_pageAndLimit_correctsAsExpected() {
            // Input: page=null/0、limit=null/0/101
            // Output: page=1、limitは50または100に補正される
            //         readAllと同様のページング補正ロジックが機能すること
        }

        @Test
        @DisplayName("UT-MS-036: データが存在する場合は一覧を返し、0件の場合はNOT_FOUNDを返す")
        void readSiteMonitoringRecordsSolar_dataExistsOrNoData_returnsOkOrNotFound() {
            // Input: データ複数件あり、またはデータなし
            // Output: OK(items一覧とpagination)、またはNOT_FOUND(MSG_MONITORING_RECORD_NOT_FOUND)が返却される
            //         指定サイトの日次データ取得ロジックが機能すること
        }

        @Test
        @DisplayName("UT-MS-037: 処理中に例外が発生した場合は内部エラーを返す")
        void readSiteMonitoringRecordsSolar_unexpectedException_returnsInternalError() {
            // Input: DB例外等をモックで発生させる
            // Output: INTERNAL_SERVER_ERROR、MSG_INTERNAL_ERRORが返却される
            //         例外ハンドリングが機能すること
        }
    }

    @Nested
    @DisplayName("POST /monitoring/upload-elpv-evidence, upload-elpvr-evidence, upload-elpj-battery-specification-evidence, upload-elpj-control-evidence のテスト")
    class UploadEvidenceTest {

        @Test
        @DisplayName("UT-MS-038: ファイルサイズが上限を超過する場合はエラーを返す")
        void uploadEvidence_fileSizeExceedsLimit_throwsFileTooLargeException() {
            // Input: file.size > MAX_FILE_SIZE_BYTES
            // Output: FileTooLargeExceptionが発生し、details(maxSizeMB・actualSizeMB)が設定される
            //         validateFileOrThrowのサイズチェックが機能すること
        }

        @Test
        @DisplayName("UT-MS-039: 許可されていない拡張子の場合はエラーを返す")
        void uploadEvidence_disallowedExtension_throwsUnsupportedFileTypeException() {
            // Input: 拡張子=exe等（fileTypeごとのallowedExtensionsに非含有）
            // Output: UnsupportedFileTypeExceptionが発生し、details(allowedTypes・receivedType)が設定される
            //         validateFileOrThrowの拡張子チェックが機能すること
        }

        @Test
        @DisplayName("UT-MS-040: 許可された拡張子(pdf/png/jpg/jpeg/csv等)の場合は検証を通過する")
        void uploadEvidence_allowedExtension_passesValidation() {
            // Input: 許可拡張子のファイル
            // Output: バリデーションを通過し後続処理に進む
            //         拡張子許可リストの境界値が正しいこと
        }

        @Test
        @DisplayName("UT-MS-041: 各検証条件が不正の場合は対応するエラーを返す")
        void uploadEvidence_invalidValidationConditions_returnsError() {
            // Input: username不正／company不一致／siteId不正／site.company=null or 不一致／projectId不正／権限不足／relation不存在
            // Output: 各ケースに対応するエラーコード・メッセージ(MSG_USER_NOT_IN_COMPANY・MSG_SITE_NOT_FOUND・MSG_SITE_HAS_NO_COMPANY・MSG_SITE_NOT_IN_COMPANY・MSG_PROJECT_NOT_FOUND・MSG_FORBIDDEN・MSG_SITE_PROJECT_RELATION_NOT_FOUND)が返却される
            //         アップロード系共通のアクセス検証ロジックが機能すること
        }

        @Test
        @DisplayName("UT-MS-042: クライアント指定のContent-Typeを信用せず拡張子ベースで安全な値を決定する")
        void getSafeContentType_ignoresClientSpoofedContentType_resolvesByExtension() {
            // Input: 拡張子=pdf、クライアント指定Content-Typeは別の偽装値
            // Output: safeContentType=application/pdfが決定され、クライアント指定値は無視される
            //         MIME Type Spoofing対策ロジックが機能すること
        }

        @Test
        @DisplayName("UT-MS-043: 未知拡張子の場合は安全なデフォルト値にフォールバックする")
        void getSafeContentType_unknownExtension_fallsBackToOctetStream() {
            // Input: 拡張子=未定義の値
            // Output: application/octet-streamが返却される
            //         フォールバック処理が機能すること
        }

        @Test
        @DisplayName("UT-MS-044: ファイル名に不正文字やnullが含まれる場合は安全な名前に変換される")
        void sanitizeFileName_invalidCharactersOrNull_returnsSafeFileName() {
            // Input: originalFilenameに不正文字列を含む、またはnull
            // Output: 不正文字が_に置換される、またはnull時はUUIDによるランダム名が生成される
            //         パストラバーサル・XSS対策ロジックが機能すること
        }

        @Test
        @DisplayName("UT-MS-045: 各APIが正しいfileTypeで共通処理に委譲し証憑アップロードが成功する")
        void uploadEvidence_eachApiDelegatesWithCorrectFileType_returnsOk() {
            // Input: 4つのAPIをそれぞれ有効なfile・パラメータで呼び出す
            // Output: 各々ELPV_EVIDENCE/ELPVR_EVIDENCE/ELPJ_BATTERY_EVIDENCE/ELPJ_CONTROL_EVIDENCEとして処理され、OKとFileInfoResponseが返却される
            //         uploadMonitoringFileSolarへのfileType委譲と正常系全体が機能すること
        }

        @Test
        @DisplayName("UT-MS-046: 処理中に予期せぬ例外(S3接続エラー等)が発生した場合は内部エラーを返す")
        void uploadEvidence_unexpectedException_returnsInternalError() {
            // Input: S3接続エラー等をモックで発生させる
            // Output: INTERNAL_SERVER_ERROR、MSG_INTERNAL_ERRORが返却される
            //         例外ハンドリングが機能すること
        }
    }

    @Nested
    @DisplayName("GET /monitoring/{siteId}/{projectId}/equipment/{equipmentId}/evidence-item のテスト")
    class ReadMonitoringEvidenceItemsTest {

        @Test
        @DisplayName("UT-MS-047: 各検証条件が不正の場合は対応するエラーを返す")
        void readMonitoringEvidenceItems_invalidValidationConditions_returnsError() {
            // Input: username不正／company不一致／siteId不正／site.company不一致／projectId不正／権限不足／relation不存在
            // Output: 各ケースに対応するエラーコード・メッセージが返却される
            //         他APIと共通のアクセス検証ロジックが機能すること
        }

        @Test
        @DisplayName("UT-MS-048: 設備が存在しない、または別拠点に属する場合はNOT_FOUNDを返す")
        void readMonitoringEvidenceItems_equipmentNotFoundOrWrongSite_returnsNotFound() {
            // Input: equipmentId不正／equipment.site!=site
            // Output: NOT_FOUND、MSG_EQUIPMENT_NOT_FOUNDが返却される
            //         設備の存在確認と拠点整合性チェックが機能すること
        }

        @Test
        @DisplayName("UT-MS-049: 当該設備の最新レコードが存在しない場合でもエラーにせず未提出として扱う")
        void readMonitoringEvidenceItems_noLatestRecord_returnsOkWithNotSubmitted() {
            // Input: 最新レコードなし
            // Output: OK、各証憑項目がstatus=STATUS_NOT_SUBMITTEDとして返却される
            //         レコード無しの場合のフォールバック処理が機能すること
        }

        @Test
        @DisplayName("UT-MS-050: ELpv/ELpvrは常にREQUIRED、ELPJcontrolは常にOPTIONALと判定される")
        void resolveEvidenceRequirement_elPvElPvrAndElpjControl_returnsFixedRequirement() {
            // Input: hasBatteryの値を問わずfield=EL_PV/EL_PVR/EL_PJ_CONTROL
            // Output: EL_PV/EL_PVRはREQUIRED、EL_PJ_CONTROLはOPTIONAL(fallbackNote付与)が返却される
            //         方法論に基づく必須区分の固定ロジックが機能すること
        }

        @Test
        @DisplayName("UT-MS-051: ELPJbatteryは設備のhasBatteryに応じてREQUIREDまたはNOT_APPLICABLEと判定される")
        void resolveEvidenceRequirement_elpjBattery_dependsOnHasBattery() {
            // Input: equipment.hasBattery=true／false、field=EL_PJ_BATTERY
            // Output: true時はREQUIRED、false時はNOT_APPLICABLE(notApplicableReason=REASON_NO_BATTERY付与)が返却される
            //         設備情報を唯一の判定根拠とする蓄電池要否ロジックが機能すること
        }

        @Test
        @DisplayName("UT-MS-052: NOT_APPLICABLE項目はevidenceUrlとacceptedTypesが空で返却される")
        void buildEvidenceItem_notApplicable_returnsEmptyUrlAndTypes() {
            // Input: requirement=NOT_APPLICABLE
            // Output: evidenceUrl=null、acceptedTypes=空リスト、status=nullが設定される
            //         対象外項目のレスポンス構築ロジックが機能すること
        }

        @Test
        @DisplayName("UT-MS-053: 証憑URLの有無に応じてstatusが未提出または提出済みとなる")
        void buildEvidenceItem_evidenceUrlPresence_determinesSubmissionStatus() {
            // Input: recordのEvidenceUrlが空、または有効値
            // Output: 空の場合はSTATUS_NOT_SUBMITTED、有効値の場合はSTATUS_SUBMITTEDとevidenceUrlが設定される
            //         提出状況判定ロジックが機能すること
        }

        @Test
        @DisplayName("UT-MS-054: 設備と最新レコードが有効な場合に4項目全ての証憑情報が正しく構築される")
        void readMonitoringEvidenceItems_validEquipmentAndRecord_returnsOkWithAllItems() {
            // Input: equipmentId有効、record有効
            // Output: OK、methodologyName/Number/VersionとevidenceItems4件が返却される
            //         証憑項目一覧取得処理全体が機能すること
        }

        @Test
        @DisplayName("UT-MS-055: 処理中に例外が発生した場合は内部エラーを返す")
        void readMonitoringEvidenceItems_unexpectedException_returnsInternalError() {
            // Input: DB例外等をモックで発生させる
            // Output: INTERNAL_SERVER_ERROR、MSG_INTERNAL_ERRORが返却される
            //         例外ハンドリングが機能すること
        }
    }

    @Nested
    @DisplayName("共通ロジック（parseDateRecorded / isBlank / mergeErrors）のテスト")
    class CommonHelperLogicTest {

        @Test
        @DisplayName("UT-MS-056: 不正な形式またはnullの場合は例外を捕捉してnullを返す")
        void parseDateRecorded_invalidFormatOrNull_returnsNull() {
            // Input: dateRecorded=不正文字列、またはnull
            // Output: nullが返却される(DateTimeParseException／NullPointerExceptionを捕捉)
            //         日付解析の例外安全性が機能すること
        }

        @Test
        @DisplayName("UT-MS-057: null・空文字・空白のみはtrue、有効文字列はfalseを返す")
        void isBlank_nullEmptyBlankOrValidString_returnsExpectedBoolean() {
            // Input: value=null／空文字／空白文字／有効文字列
            // Output: null・空文字・空白時はtrue、有効文字列時はfalseが返却される
            //         文字列ブランク判定ロジックが機能すること
        }

        @Test
        @DisplayName("UT-MS-058: 既存キーは上書きせず新規キーのみ追加する(putIfAbsent)")
        void mergeErrors_existingKeyNotOverwritten_onlyNewKeyAdded() {
            // Input: targetに既存キーaあり、sourceにキーa・bあり
            // Output: targetの既存キーaの値は維持され、新規キーbのみ追加される
            //         エラーマージのキー重複排除ロジックが機能すること
        }
    }
}