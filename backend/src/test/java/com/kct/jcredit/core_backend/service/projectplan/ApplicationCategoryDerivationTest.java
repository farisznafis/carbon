package com.kct.jcredit.core_backend.service.projectplan;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import com.kct.jcredit.core_backend.dto.projectplan.OtherOperatorProjectsRequest;
import com.kct.jcredit.core_backend.dto.projectplan.ProjectApplicationRequest;

class ApplicationCategoryDerivationTest {

    @Test
    void everyPlanIsARegistrationWhateverTheClientSent() {
        ProjectApplicationRequest form = new ProjectApplicationRequest();
        form.setApplicationCategory("計画変更");

        ApplicationCategoryDerivation.application().derive(form, null);

        assertThat(form.getApplicationCategory()).isEqualTo("登録");
    }

    @Test
    void aRegistrationKeepsNoChangeFlagsOrChangeDate() {
        ProjectApplicationRequest form = new ProjectApplicationRequest();
        form.setIsChangePicName(true);
        form.setIsChangeOtherContactsPicEmail(false);
        form.setApplicationChangeDate("2026-09-01");
        form.setRegistrationDate("2026-09-01");

        ApplicationCategoryDerivation.application().derive(form, null);

        assertThat(form.getIsChangePicName()).isNull();
        assertThat(form.getIsChangeOtherContactsPicEmail()).isNull();
        assertThat(form.getApplicationChangeDate()).isNull();
        assertThat(form.getRegistrationDate()).isEqualTo("2026-09-01");
    }

    @Test
    void theCategoryReachesTheClientReadOnly() {
        assertThat(ApplicationCategoryDerivation.application().derivedFields())
                .containsExactly("applicationCategory");
    }

    @Test
    void otherOperatorsLoseTheirChangeFlagsToo() {
        OtherOperatorProjectsRequest form = new OtherOperatorProjectsRequest();
        form.setIsChangePicPhoneNumber(true);

        ApplicationCategoryDerivation.otherOperators().derive(form, null);

        assertThat(form.getIsChangePicPhoneNumber()).isNull();
    }
}
