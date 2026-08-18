package com.kct.jcredit.core_backend.service.projectplan;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;

import com.kct.jcredit.core_backend.dto.projectplan.ApplicableConditionsRequest;

import org.junit.jupiter.api.Test;

/**
 * The page registry, and the one thing it deliberately refuses to serve.
 */
class ProjectPlanFormRegistryTest {

    private final ProjectPlanFormRegistry registry = new ProjectPlanFormRegistry();

    @Test
    void everyPageIsReachableBySlugAndBySortKey() {
        List<FormDefinition<?>> all = registry.all();

        assertThat(all).isNotEmpty();
        all.forEach(definition -> {
            assertThat(registry.bySlug(definition.slug())).contains(definition);
            assertThat(registry.bySortKey(definition.skPrefix())).contains(definition);
        });
    }

    @Test
    void slugsAndSortKeysAreUnique() {
        List<FormDefinition<?>> all = registry.all();

        assertThat(all).extracting(FormDefinition::slug).doesNotHaveDuplicates();
        assertThat(all).extracting(FormDefinition::skPrefix).doesNotHaveDuplicates();
    }

    @Test
    void noPageRepeatsPerMethodologyToday() {
        assertThat(registry.all()).allSatisfy(
                definition -> assertThat(definition.perMethodology()).isFalse());
    }

    @Test
    void registeringARepeatingPageIsRefusedWithTheReasonWhy() {
        // The official form repeats sections once per 方法論 ①–⑤ and the key
        // scheme is ready for it, but project stores exactly one 方法論. Failing
        // loudly at startup beats shipping pages that collect ②–⑤ data nothing
        // downstream can hold.
        FormDefinition<ApplicableConditionsRequest> repeating = new FormDefinition<>(
                "repeating-page",
                "REPEATING_PAGE",
                "繰り返しページ",
                99,
                true,
                ApplicableConditionsRequest.class,
                List.of());

        assertThatThrownBy(() -> ProjectPlanFormRegistry.rejectUnsupportedRepetition(repeating))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("repeating-page")
                .hasMessageContaining("方法論");
    }

    @Test
    void theKeySchemeStillDistinguishesMethodologiesForWhenItIsSupported() {
        FormDefinition<ApplicableConditionsRequest> repeating = new FormDefinition<>(
                "repeating-page",
                "REPEATING_PAGE",
                "繰り返しページ",
                99,
                true,
                ApplicableConditionsRequest.class,
                List.of());

        assertThat(repeating.sk(1)).isEqualTo("REPEATING_PAGE#M1");
        assertThat(repeating.sk(2)).isEqualTo("REPEATING_PAGE#M2");
        // Singleton pages keep the bare prefix, so today's stored items are
        // unaffected if repetition is enabled later.
        assertThat(registry.bySlug("application").orElseThrow().sk(null))
                .isEqualTo("PROJECT_APPLICATION");
    }
}
