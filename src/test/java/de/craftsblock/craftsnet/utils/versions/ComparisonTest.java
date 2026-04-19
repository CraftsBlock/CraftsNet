package de.craftsblock.craftsnet.utils.versions;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ComparisonTest {

    @Test
    void shouldDetectEqualVersions() {
        assertThat(Comparison.EQUAL.suitable("1.2.3", "1.2.3")).isTrue();
        assertThat(Comparison.EQUAL.suitable("1.2.3", "1.2.4")).isFalse();
    }

    @Test
    void shouldDetectGreaterVersions() {
        assertThat(Comparison.GREATER.suitable("1.3.0", "1.2.0")).isTrue();
        assertThat(Comparison.GREATER.suitable("1.2.0", "1.2.0")).isFalse();
    }

    @Test
    void shouldDetectLessVersions() {
        assertThat(Comparison.LESS.suitable("1.1.9", "1.2.0")).isTrue();
        assertThat(Comparison.LESS.suitable("1.2.0", "1.2.0")).isFalse();
    }

    @Test
    void shouldSupportGreaterOrEqual() {
        assertThat(Comparison.GREATER_OR_EQUAL.suitable("1.2.0", "1.2.0")).isTrue();
        assertThat(Comparison.GREATER_OR_EQUAL.suitable("1.3.0", "1.2.0")).isTrue();
        assertThat(Comparison.GREATER_OR_EQUAL.suitable("1.1.9", "1.2.0")).isFalse();
    }

    @Test
    void shouldSupportLessOrEqual() {
        assertThat(Comparison.LESS_OR_EQUAL.suitable("1.2.0", "1.2.0")).isTrue();
        assertThat(Comparison.LESS_OR_EQUAL.suitable("1.1.9", "1.2.0")).isTrue();
        assertThat(Comparison.LESS_OR_EQUAL.suitable("1.3.0", "1.2.0")).isFalse();
    }

    @Test
    void shouldHandleDifferentLengths() {
        assertThat(Comparison.EQUAL.suitable("1.2", "1.2.0")).isTrue();
        assertThat(Comparison.GREATER.suitable("1.2.1", "1.2")).isTrue();
        assertThat(Comparison.LESS.suitable("1.2", "1.2.1")).isTrue();
    }

    @Test
    void shouldHandleNullAndEmptyVersions() {
        assertThat(Comparison.EQUAL.suitable(null, "0")).isTrue();
        assertThat(Comparison.EQUAL.suitable("", "0")).isTrue();
    }

    @Test
    void shouldTrimWhitespace() {
        assertThat(Comparison.EQUAL.suitable(" 1.2.3 ", "1.2.3")).isTrue();
    }

    @Test
    void shouldTreatInvalidPartsAsZero() {
        assertThat(Comparison.EQUAL.suitable("1.a.3", "1.0.3")).isTrue();
    }

    @Test
    void shouldIgnoreWildcardInExpected() {
        assertThat(Comparison.EQUAL.suitable("1.2.3", "1.*.3")).isTrue();
        assertThat(Comparison.GREATER_OR_EQUAL.suitable("1.2.3", "1.*.3")).isTrue();
    }

    @Test
    void shouldApplyComparisonCorrectly() {
        assertThat(Comparison.LESS.apply(-1)).isTrue();
        assertThat(Comparison.LESS.apply(0)).isFalse();

        assertThat(Comparison.GREATER.apply(1)).isTrue();
        assertThat(Comparison.GREATER.apply(0)).isFalse();

        assertThat(Comparison.EQUAL.apply(0)).isTrue();
    }

    @Test
    void shouldResolveFromSymbol() {
        assertThat(Comparison.from("<")).isEqualTo(Comparison.LESS);
        assertThat(Comparison.from(">")).isEqualTo(Comparison.GREATER);
        assertThat(Comparison.from("<=")).isEqualTo(Comparison.LESS_OR_EQUAL);
        assertThat(Comparison.from(">=")).isEqualTo(Comparison.GREATER_OR_EQUAL);
        assertThat(Comparison.from("=")).isEqualTo(Comparison.EQUAL);
    }

    @Test
    void shouldDefaultToEqualForUnknownSymbol() {
        assertThat(Comparison.from("??")).isEqualTo(Comparison.EQUAL);
        assertThat(Comparison.from("")).isEqualTo(Comparison.EQUAL);
    }

}