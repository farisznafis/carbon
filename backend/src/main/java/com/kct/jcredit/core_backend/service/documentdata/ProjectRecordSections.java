package com.kct.jcredit.core_backend.service.documentdata;

import static com.kct.jcredit.core_backend.service.documentdata.DocumentValues.date;
import static com.kct.jcredit.core_backend.service.documentdata.DocumentValues.japanDate;
import static com.kct.jcredit.core_backend.service.documentdata.DocumentValues.join;
import static com.kct.jcredit.core_backend.service.documentdata.DocumentValues.number;
import static com.kct.jcredit.core_backend.service.documentdata.DocumentValues.section;
import static com.kct.jcredit.core_backend.service.documentdata.DocumentValues.text;

import java.util.List;

import com.kct.jcredit.core_backend.dto.response.ProjectDocumentDataResponse.Section;
import com.kct.jcredit.core_backend.entity.OperatorCompany;
import com.kct.jcredit.core_backend.entity.Project;

/**
 * Sections read from the project record. The documents of a project repeat
 * these, so they are written once here and each provider picks the ones its
 * document has.
 *
 * <p>認証対象期間 and the member counts (planned and actual) are not here:
 * they are filled in by hand on the Excel forms and the system does not keep
 * them.
 */
final class ProjectRecordSections {

    private static final String UNIT_EMISSION = "t-CO2";

    private ProjectRecordSections() {
    }

    static Section overview(Project project) {
        return section("project", "プロジェクト概要", List.of(
                text("projectName", "プロジェクト名称", project.getProjectName()),
                text("projectNumber", "プロジェクト番号", project.getProjectNumber()),
                text("regionalCategory", "地域区分", project.getRegionalCategory()),
                date("projectStartedAt", "プロジェクト開始日", japanDate(project.getStartedAt())),
                text("usePurpose", "利用目的", project.getUsePurpose())));
    }

    static Section methodology(Project project) {
        return section("methodology", "方法論", List.of(
                text("methodCategory", "方法論区分", project.getMethodCategory()),
                // The forms write the number with its 区分, e.g. EN-R-002.
                text("methodNumber", "方法論番号",
                        join("-", project.getMethodCategory(), project.getMethodNumber())),
                text("methodName", "方法論名称", project.getMethodName()),
                text("methodologyVersion", "方法論バージョン", project.getMethodologyVersion()),
                text("methodMainEmission", "方法論排出源／活動", project.getMethodMainEmission()),
                text("methodDetails", "方法論説明", project.getMethodDetails())));
    }

    static Section emissions(Project project) {
        return section("emissions", "排出削減量", List.of(
                number("emissionReduction", "排出削減量", project.getEmissionReduction(), UNIT_EMISSION),
                number("baselineEmission", "ベースライン排出量", project.getBaselineEmission(), UNIT_EMISSION),
                number("postProjectEmission", "プロジェクト実施後排出量",
                        project.getPostProjectEmission(), UNIT_EMISSION)));
    }

    /** The operator company that runs the project. */
    static Section operator(Project project) {
        OperatorCompany company = project.getOperatorCompany();
        return section("operator", "プロジェクト実施者", List.of(
                text("operatorName", "事業者名", company.getNameJa()),
                text("operatorNameKana", "事業者名（フリガナ）", company.getNameFurigana()),
                text("operatorNameEn", "事業者名（英語）", company.getNameEn()),
                text("operatorCorpNumber", "法人番号", company.getCorpNumber()),
                text("operatorPostalCode", "郵便番号", company.getPostalCode()),
                text("operatorAddress", "住所", join("",
                        company.getPrefecture(), company.getMunicipality(),
                        company.getBlocknum(), company.getBuildingName())),
                text("operatorPhoneNumber", "電話番号", company.getPhoneNumber()),
                text("operatorEmail", "メールアドレス", company.getEmail())));
    }

    static Section representative(Project project) {
        return section("representative", "代表者", List.of(
                text("repPosition", "役職", project.getRepPosition()),
                text("repName", "氏名", join(" ", project.getRepLastName(), project.getRepFirstName())),
                text("repNameKana", "氏名（フリガナ）",
                        join(" ", project.getRepLastNameKana(), project.getRepFirstNameKana())),
                text("repPhoneNumber", "電話番号", project.getRepPhoneNumber()),
                text("repEmail", "メールアドレス", project.getRepEmail())));
    }

    static Section personInCharge(Project project) {
        return section("personInCharge", "責任者", List.of(
                text("picPosition", "役職", project.getPicPosition()),
                text("picName", "氏名", join(" ", project.getPicLastName(), project.getPicFirstName())),
                text("picNameKana", "氏名（フリガナ）",
                        join(" ", project.getPicLastNameKana(), project.getPicFirstNameKana())),
                text("picPhoneNumber", "電話番号", project.getPicPhoneNumber()),
                text("picEmail", "メールアドレス", project.getPicEmail()),
                text("dataManagementPic", "データ管理責任者", project.getDataManagementPic())));
    }
}
