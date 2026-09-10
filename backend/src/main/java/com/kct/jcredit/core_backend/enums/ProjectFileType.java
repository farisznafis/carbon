package com.kct.jcredit.core_backend.enums;

/**
 * Every document exchanged with the VVB (審査機関) or the scheme
 * secretariat/authentication committee (制度事務局／認証委員会),
 * across both J-Credit steps (project registration and
 * monitoring/verification/certification).
 *
 * Each constant carries its own fixed owner, so ownership is never
 * taken from client input - it is always resolved from the type
 * being uploaded. See ProjectFiles#ownedByKanrisha.
 */
public enum ProjectFileType {

    // --- STEP 1: Project registration ---

    /** プロジェクト計画書. Confirmed against the actual Ver.1.4.0 template:
     *  the registration application, plan change notice and pledge sheet
     *  are all sheets inside this same workbook, not separate documents. */
    PROJECT_PLAN(true),

    /** 妥当性確認報告書 - validation result issued by the VVB. */
    VALIDATION_REPORT(false),

    /** 登録通知 - registration decision from the secretariat/committee. */
    REGISTRATION_RESULT(false),

    // --- STEP 2: Monitoring / verification / certification ---

    /** モニタリング報告書. Confirmed against the actual Ver.1.5.0 template:
     *  the certification application is a sheet inside this same workbook. */
    MONITORING_REPORT(true),

    /** 個別活動実績報告リスト（別紙10-1）. Format differs per methodology;
     *  the methodology itself is looked up via the parent project, not
     *  stored again here (a project has exactly one methodology). */
    ACTIVITY_RESULT_LIST(true),

    /** 検証報告書 - verification result issued by the VVB. */
    VERIFICATION_REPORT(false),

    /** 認証通知 - certification/issuance decision from the committee. */
    CERTIFICATION_RESULT(false),

    // --- Cross-cutting, can occur during either step ---

    /** 質問状／指摘事項シート - clarification questions from the VVB
     *  during their review, ahead of their final report. */
    VVB_QUERY_SHEET(false),

    /** 追加資料 - extra material the VVB asked for mid-review. */
    ADDITIONAL_MATERIAL(true);

    private final boolean ownedByKanrisha;

    ProjectFileType(boolean ownedByKanrisha) {
        this.ownedByKanrisha = ownedByKanrisha;
    }

    /** true = authored by kanrisha (us); false = authored by an
     *  external body (審査機関 or 制度事務局／認証委員会). */
    public boolean isOwnedByKanrisha() {
        return ownedByKanrisha;
    }
}