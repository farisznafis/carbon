package com.kct.jcredit.core_backend.service.projectplan;

import java.util.List;
import java.util.function.Consumer;

import com.kct.jcredit.core_backend.dto.projectplan.OtherOperatorProjectsRequest;
import com.kct.jcredit.core_backend.dto.projectplan.ProjectApplicationRequest;
import com.kct.jcredit.core_backend.dto.projectplan.ProjectPlanFormBase;

/**
 * 申請区分 and what hangs off it, owned by the server.
 *
 * <p>The official 登録申請書 serves three filings — プロジェクト登録,
 * プロジェクト計画変更 and 認証権限保有主体変更 — and which one a plan is follows
 * from where the project stands, not from what an operator picks. Until plans
 * are versioned, every plan on this platform is the first filing, so the
 * category is always {@link #REGISTRATION}.
 *
 * <p>The 変更有無 flags and 計画変更日 only mean something against an earlier
 * approved plan. A registration has none, so they are cleared rather than
 * stored as answers to a question nobody should have been asked.
 */
final class ApplicationCategoryDerivation<T extends ProjectPlanFormBase>
        implements FormDerivation<T> {

    static final String REGISTRATION = "登録";

    private final List<String> derivedFields;
    private final Consumer<T> derive;

    private ApplicationCategoryDerivation(List<String> derivedFields, Consumer<T> derive) {
        this.derivedFields = derivedFields;
        this.derive = derive;
    }

    /** 登録申請書: the category itself, plus the fields a registration has no use for. */
    static ApplicationCategoryDerivation<ProjectApplicationRequest> application() {
        return new ApplicationCategoryDerivation<>(
                List.of("applicationCategory"),
                form -> {
                    form.setApplicationCategory(REGISTRATION);
                    form.setApplicationChangeDate(null);
                    form.setIsChangeRepresentativePosition(null);
                    form.setIsChangeRepresentativeName(null);
                    form.setIsChangePicPosition(null);
                    form.setIsChangePicName(null);
                    form.setIsChangePicEmail(null);
                    form.setIsChangePicPhoneNumber(null);
                    form.setIsChangeOtherContactBusinessName(null);
                    form.setIsChangeOtherContactPicPosition(null);
                    form.setIsChangeOtherContactsPicName(null);
                    form.setIsChangeOtherContactsPicEmail(null);
                    form.setIsChangeOtherContactsPicPhoneNumber(null);
                });
    }

    /** 代表以外のプログラム型運営・管理者 carries the same 変更有無 flags. */
    static ApplicationCategoryDerivation<OtherOperatorProjectsRequest> otherOperators() {
        return new ApplicationCategoryDerivation<>(
                List.of(),
                form -> {
                    form.setIsChangeRepresentativePosition(null);
                    form.setIsChangeRepresentativeName(null);
                    form.setIsChangePicPosition(null);
                    form.setIsChangePicName(null);
                    form.setIsChangePicEmail(null);
                    form.setIsChangePicPhoneNumber(null);
                });
    }

    @Override
    public List<String> derivedFields() {
        return derivedFields;
    }

    @Override
    public void derive(T form, ProjectPlanSqlContext context) {
        derive.accept(form);
    }
}
