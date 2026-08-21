package com.kct.jcredit.core_backend.dto.projectplan;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Stream;

import com.kct.jcredit.core_backend.validation.Hankaku;
import com.kct.jcredit.core_backend.validation.HankakuWithSymbols;
import com.kct.jcredit.core_backend.validation.Katakana;
import com.kct.jcredit.core_backend.validation.NumericOnly;

import org.junit.jupiter.api.Test;

/**
 * Which plan fields still restrict what characters may be typed.
 *
 * <p>Nearly every bug found in manual testing was a character-set rule refusing
 * a legitimate value: 全角 rules rejecting an ESG case number, half-width rules
 * rejecting t-CO2/年 and モニタリング項目, kana rejecting the space between 姓
 * and 名. The rules were swept, and what survives is the short list below —
 * codes, identifiers and furigana, where the format genuinely matters.
 *
 * <p>This test exists so that list stays deliberate. Adding a strict rule to a
 * field that holds prose fails here, which is the moment to ask whether the
 * operator can actually type what it demands.
 */
class CharacterSetRuleTest {

    private static final Path DTO_DIR =
            Path.of("src/main/java/com/kct/jcredit/core_backend/dto/projectplan");

    /** Half-width only. Codes and identifiers, never prose. */
    private static final Set<String> EXPECTED_HANKAKU = Set.of(
            "ApplicableMethodologyRequest.methodologyCategory",
            "ApplicableMethodologyRequest.methodologyNumber",
            "ApplicableMethodologyRequest.methodologyVersion",
            "OtherOperatorProjectsRequest.energyManageFactoryNumber",
            "ProjectApplicationRequest.energyManageFactoryNumber",
            "ProjectOverviewRequest.projectLocationClassificationEn",
            "ProjectPlanCoverRequest.version",
            "ProjectPlanCoverRequest.projectNumber");

    /** Digits only: 法人番号 and 特定排出者コード. */
    private static final Set<String> EXPECTED_NUMERIC_ONLY = Set.of(
            "OtherOperatorProjectsRequest.emitterCode",
            "ProjectApplicationRequest.emitterCode",
            "ProjectApplicationRequest.methodologyNumber",
            "ProjectOverviewRequest.corpNumber",
            "ProjectOverviewRequest.holderCorpNumber");

    private static Stream<Class<?>> planDtos() throws IOException {
        try (Stream<Path> files = Files.list(DTO_DIR)) {
            return files.map(Path::getFileName)
                    .map(Path::toString)
                    .filter(name -> name.endsWith("Request.java"))
                    .map(name -> name.replace(".java", ""))
                    .<Class<?>>map(name -> {
                        try {
                            return Class.forName(
                                    "com.kct.jcredit.core_backend.dto.projectplan." + name);
                        } catch (ClassNotFoundException e) {
                            throw new IllegalStateException(e);
                        }
                    })
                    .toList()
                    .stream();
        }
    }

    private static Set<String> fieldsAnnotatedWith(Class<? extends Annotation> annotation)
            throws IOException {
        Set<String> found = new TreeSet<>();
        planDtos().forEach(dto -> {
            for (Field field : dto.getDeclaredFields()) {
                if (field.isAnnotationPresent(annotation)) {
                    found.add(dto.getSimpleName() + "." + field.getName());
                }
            }
        });
        return found;
    }

    @Test
    void onlyCodesAndIdentifiersAreStillHalfWidthOnly() throws IOException {
        assertThat(fieldsAnnotatedWith(Hankaku.class))
                .containsExactlyInAnyOrderElementsOf(EXPECTED_HANKAKU);
    }

    @Test
    void onlyCorporateAndEmitterNumbersAreDigitsOnly() throws IOException {
        assertThat(fieldsAnnotatedWith(NumericOnly.class))
                .containsExactlyInAnyOrderElementsOf(EXPECTED_NUMERIC_ONLY);
    }

    @Test
    void furiganaFieldsAreTheOnlyKanaOnlyOnes() throws IOException {
        assertThat(fieldsAnnotatedWith(Katakana.class))
                .allSatisfy(name -> assertThat(name.toLowerCase())
                        .as("%s is kana-only but is not a furigana field", name)
                        .containsAnyOf("kana", "furigana"));
    }

    @Test
    void everyHankakuWithSymbolsFieldDeclaresWhatItAllows() throws IOException {
        List<String> withoutSymbols = planDtos()
                .flatMap(dto -> Stream.of(dto.getDeclaredFields())
                        .filter(f -> f.isAnnotationPresent(HankakuWithSymbols.class))
                        .filter(f -> f.getAnnotation(HankakuWithSymbols.class).allowedSymbols().isEmpty())
                        .map(f -> dto.getSimpleName() + "." + f.getName()))
                .toList();

        // These carry e-mail addresses and 方法論コード. A blank symbol set would
        // reject the @ or the hyphen those always contain.
        assertThat(withoutSymbols).isEmpty();
    }
}
