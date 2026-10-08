package com.kct.jcredit.core_backend.enums;

import java.util.List;

public enum ProjectFileType {

    // --- STEP 1: Project registration ---

    /** プロジェクト計画書. Confirmed against the actual Ver.1.4.0 template:
     *  the registration application, plan change notice and pledge sheet
     *  are all sheets inside this same workbook, not separate documents. */
    PROJECT_PLAN(true, 10, FileFormat.EXCEL),

    /** 妥当性確認報告書 - validation result issued by the VVB. */
    VALIDATION_REPORT(false, 10, FileFormat.PDF),

    /** 登録結果通知 - registration decision from the secretariat/committee. */
    REGISTRATION_RESULT(false, 10, FileFormat.PDF),

    // --- STEP 2: Monitoring / verification / certification ---

    /** モニタリング報告書. Confirmed against the actual Ver.1.5.0 template:
     *  the certification application is a sheet inside this same workbook. */
    MONITORING_REPORT(true, 10, FileFormat.EXCEL),

    /** 個別活動実績報告リスト（別紙10-1）. Format differs per methodology;
     *  the methodology itself is looked up via the parent project, not
     *  stored again here (a project has exactly one methodology). */
    ACTIVITY_RESULT_LIST(true, 10, FileFormat.EXCEL),

    /** 検証報告書 - verification result issued by the VVB. */
    VERIFICATION_REPORT(false, 10, FileFormat.PDF),

    /** 認証結果通知 - certification/issuance decision from the committee. */
    CERTIFICATION_RESULT(false, 10, FileFormat.PDF),

    // --- Cross-cutting, can occur during either step ---

    /** 質問状／指摘事項シート. Mostly Excel, but the layout is the VVB's own,
     *  so PDF and Word are accepted as well. */
    VVB_QUERY_SHEET(false, 10, FileFormat.EXCEL, FileFormat.PDF, FileFormat.WORD),

    /** 追加資料 - anything that doesn't fit the types above. */
    ADDITIONAL_MATERIAL(true, 10, FileFormat.PDF, FileFormat.EXCEL, FileFormat.WORD, FileFormat.IMAGE);

    /** Upload formats, each defined by its file extensions and MIME types. */
    public enum FileFormat {
        PDF(List.of("pdf"),
            List.of("application/pdf")),
        EXCEL(List.of("xlsx", "xls"),
              List.of("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                      "application/vnd.ms-excel")),
        WORD(List.of("docx", "doc"),
             List.of("application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                     "application/msword")),
        IMAGE(List.of("jpg", "jpeg", "png"),
              List.of("image/jpeg", "image/png"));

        private final List<String> extensions;
        private final List<String> contentTypes;

        FileFormat(List<String> extensions, List<String> contentTypes) {
            this.extensions = extensions;
            this.contentTypes = contentTypes;
        }

        public List<String> getExtensions() { return extensions; }
        public List<String> getContentTypes() { return contentTypes; }
    }

    private final boolean ownedByKanrisha;
    private final int versionLimit;
    private final List<FileFormat> allowedFormats;

    ProjectFileType(boolean ownedByKanrisha, int versionLimit, FileFormat... allowedFormats) {
        this.ownedByKanrisha = ownedByKanrisha;
        this.versionLimit = versionLimit;
        this.allowedFormats = List.of(allowedFormats);
    }

    public boolean isOwnedByKanrisha() { return ownedByKanrisha; }
    public int getVersionLimit() { return versionLimit; }
    public List<FileFormat> getAllowedFormats() { return allowedFormats; }

    /** Flat list of accepted extensions, e.g. ["xlsx", "xls"] - for error messages and the UI. */
    public List<String> getAllowedExtensions() {
        return allowedFormats.stream()
                .flatMap(f -> f.getExtensions().stream())
                .toList();
    }

    /**
     * The extension must belong to an allowed format, and the MIME type must match that same format.
     * "application/octet-stream" is tolerated because some browsers send it for Office files
     * when Office isn't installed on the client machine.
     */
    public boolean accepts(String extension, String contentType) {
        for (FileFormat format : allowedFormats) {
            if (format.getExtensions().contains(extension)) {
                return contentType == null
                        || "application/octet-stream".equals(contentType)
                        || format.getContentTypes().contains(contentType);
            }
        }
        return false;
    }
}