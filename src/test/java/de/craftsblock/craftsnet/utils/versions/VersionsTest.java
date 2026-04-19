package de.craftsblock.craftsnet.utils.versions;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class VersionsTest {

    @Test
    void shouldReturnTrueWhenVersionsAreEqualWithoutOperator() {
        boolean result = Versions.suitable("1.2.3", "1.2.3");
        assertThat(result).isTrue();
    }

    @Test
    void shouldReturnTrueWhenCurrentIsGreaterThanExpected() {
        boolean result = Versions.suitable("1.3.0", ">1.2.0");
        assertThat(result).isTrue();
    }

    @Test
    void shouldReturnFalseWhenCurrentIsLessThanExpected() {
        boolean result = Versions.suitable("1.1.9", ">1.2.0");
        assertThat(result).isFalse();
    }

    @Test
    void shouldSupportGreaterOrEqualOperator() {
        assertThat(Versions.suitable("1.2.0", ">=1.2.0")).isTrue();
        assertThat(Versions.suitable("1.3.0", ">=1.2.0")).isTrue();
        assertThat(Versions.suitable("1.1.9", ">=1.2.0")).isFalse();
    }

    @Test
    void shouldSupportLessOrEqualOperator() {
        assertThat(Versions.suitable("1.2.0", "<=1.2.0")).isTrue();
        assertThat(Versions.suitable("1.1.9", "<=1.2.0")).isTrue();
        assertThat(Versions.suitable("1.3.0", "<=1.2.0")).isFalse();
    }

    @Test
    void shouldSupportLessThanOperator() {
        assertThat(Versions.suitable("1.1.9", "<1.2.0")).isTrue();
        assertThat(Versions.suitable("1.2.0", "<1.2.0")).isFalse();
    }

    @Test
    void shouldCleanVersionStrings() {
        boolean result = Versions.suitable("1.2.3-SNAPSHOT", "1.2.3");
        assertThat(result).isTrue();
    }

    @Test
    void shouldHandleWeirdCharactersInVersion() {
        boolean result = Versions.suitable("v1.2.3", "=1.2.3");
        assertThat(result).isTrue();
    }

    @Test
    void shouldDefaultToEqualsWhenNoOperatorProvided() {
        assertThat(Versions.suitable("1.2.3", "1.2.3")).isTrue();
        assertThat(Versions.suitable("1.2.4", "1.2.3")).isFalse();
    }

    @Test
    void shouldHandleDashAsDotReplacement() {
        boolean result = Versions.suitable("1-2-3", "1.2.3");
        assertThat(result).isTrue();
    }

    @Test
    void shouldCompareMultiSegmentVersionsCorrectly() {
        assertThat(Versions.suitable("1.10.0", ">1.2.0")).isTrue();
        assertThat(Versions.suitable("1.2.0", ">1.10.0")).isFalse();
    }

    @Test
    void shouldHandleShortVersions() {
        assertThat(Versions.suitable("1.2", "1.2.0")).isTrue();
        assertThat(Versions.suitable("1.2.0", "1.2")).isTrue();
    }

    @Test
    void shouldHandleTrailingDotsOrEmptyParts() {
        assertThat(Versions.suitable("1.2.", "1.2.0")).isTrue();
    }

    @Test
    void shouldHandleOnlyOperatorAsExpected() {
        assertThat(Versions.suitable("1.2.3", ">")).isTrue();
    }

}
