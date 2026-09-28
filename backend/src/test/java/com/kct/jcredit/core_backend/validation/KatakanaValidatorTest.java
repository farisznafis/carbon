package com.kct.jcredit.core_backend.validation;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * フリガナ fields.
 *
 * <p>Japanese names are written 姓 名 with a space between the halves, and the
 * SQL columns store them that way — so a pattern with no space rejected values
 * the system had autofilled from its own database.
 */
class KatakanaValidatorTest {

    private KatakanaValidator validator;

    @BeforeEach
    void setUp() {
        validator = new KatakanaValidator();
    }

    private boolean accepts(String value) {
        return validator.isValid(value, null);
    }

    @Test
    void plainKatakanaIsAccepted() {
        assertThat(accepts("ヤマダタロウ")).isTrue();
        assertThat(accepts("カブシキガイシャテスト")).isTrue();
    }

    @Test
    void aNameSplitBySpaceIsAccepted() {
        assertThat(accepts("ヤマダ タロウ")).as("ASCII space").isTrue();
        assertThat(accepts("ヤマダ　タロウ")).as("full-width space").isTrue();
    }

    @Test
    void theLongVowelMarkAndMiddleDotStillPass() {
        assertThat(accepts("コーヒー")).isTrue();
        assertThat(accepts("ジョン・スミス")).isTrue();
    }

    @Test
    void nonKatakanaIsStillRejected() {
        assertThat(accepts("山田タロウ")).as("kanji").isFalse();
        assertThat(accepts("やまだ")).as("hiragana").isFalse();
        assertThat(accepts("Yamada")).as("latin").isFalse();
        assertThat(accepts("ﾔﾏﾀﾞ")).as("half-width kana").isFalse();
    }

    @Test
    void nullAndEmptyAreLeftToNotBlank() {
        assertThat(accepts(null)).isTrue();
        assertThat(accepts("")).isTrue();
    }
}
