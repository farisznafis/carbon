package com.kct.jcredit.core_backend.service.documentdata;

import static com.kct.jcredit.core_backend.service.documentdata.DocumentValues.number;
import static com.kct.jcredit.core_backend.service.documentdata.DocumentValues.section;
import static com.kct.jcredit.core_backend.service.documentdata.DocumentValues.text;
import static com.kct.jcredit.core_backend.service.documentdata.DocumentValues.textColumn;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import com.kct.jcredit.core_backend.constant.PermissionCode;
import com.kct.jcredit.core_backend.dto.response.EnR002IndividualActivityRowResponse;
import com.kct.jcredit.core_backend.dto.response.ProjectDocumentDataResponse.Section;
import com.kct.jcredit.core_backend.dto.response.ProjectDocumentDataResponse.Table;
import com.kct.jcredit.core_backend.entity.Project;
import com.kct.jcredit.core_backend.enums.EligibilityStatus;
import com.kct.jcredit.core_backend.enums.ProjectFileType;
import com.kct.jcredit.core_backend.enums.ReportReadiness;
import com.kct.jcredit.core_backend.service.EnR002IndividualActivityService;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Component;

/**
 * 個別活動実績報告リスト（別紙10-1）: one row per piece of equipment taking part
 * in the project.
 *
 * <p>The rows are the ones the individual-activity screen lists; this only
 * lays them out as a table. EN-R-002 is the one methodology on the platform,
 * so its list is the list.
 */
@Component
@RequiredArgsConstructor
public class ActivityResultListDataProvider implements DocumentDataProvider {

    private final EnR002IndividualActivityService individualActivityService;

    @Override
    public ProjectFileType fileType() {
        return ProjectFileType.ACTIVITY_RESULT_LIST;
    }

    /** The individual-activity endpoints require it; the same rows must not be readable without it here. */
    @Override
    public Optional<PermissionCode> additionalPermission() {
        return Optional.of(PermissionCode.MONITORING_READ);
    }

    @Override
    public List<Section> sections(Project project, String projectId, String username) {
        List<EnR002IndividualActivityRowResponse> activities =
                individualActivityService.getRowsByProject(project.getPublicId(), null, username);

        List<List<String>> rows = activities.stream()
                .map(activity -> Arrays.asList(
                        activity.companyId(),
                        activity.companyName(),
                        activity.siteId(),
                        activity.siteAddress(),
                        activity.hasBattery() ? "有" : "無",
                        eligibilityLabel(activity.eligibilityStatus()),
                        readinessLabel(activity.reportReadiness())))
                .toList();

        return List.of(
                section("project", "プロジェクト概要", List.of(
                        text("projectName", "プロジェクト名称", project.getProjectName()),
                        text("projectNumber", "プロジェクト番号", project.getProjectNumber()))),
                section("activities", "個別活動一覧",
                        List.of(number("activityCount", "設備数", activities.size(), "件")),
                        new Table(
                                List.of(
                                        textColumn("companyId", "会員ID"),
                                        textColumn("companyName", "会員名"),
                                        textColumn("siteId", "拠点ID"),
                                        textColumn("siteAddress", "設置場所"),
                                        textColumn("hasBattery", "蓄電池"),
                                        textColumn("eligibilityStatus", "適格性"),
                                        textColumn("reportReadiness", "報告準備状況")),
                                rows)));
    }

    private static String eligibilityLabel(EligibilityStatus status) {
        if (status == null) {
            return null;
        }
        return switch (status) {
            case ELIGIBLE -> "適格";
            case NOT_ELIGIBLE -> "不適格";
            case PENDING -> "確認中";
        };
    }

    private static String readinessLabel(ReportReadiness readiness) {
        if (readiness == null) {
            return null;
        }
        return switch (readiness) {
            case READY -> "報告可";
            case MISSING_EVIDENCE -> "証憑不足";
            case NO_BATCH_RESULT -> "バッチ結果なし";
        };
    }
}
