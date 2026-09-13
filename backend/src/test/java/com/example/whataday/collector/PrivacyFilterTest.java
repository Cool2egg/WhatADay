package com.example.whataday.collector;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class PrivacyFilterTest {

    private static PrivacyFilter filterOf(String... processNames) {
        CollectorProperties properties = new CollectorProperties();
        properties.setExcludedProcesses(List.of(processNames));
        return new PrivacyFilter(properties);
    }

    @Test
    void matchesIgnoringCaseAndSurroundingWhitespace() {
        PrivacyFilter filter = filterOf("WeChat.exe", "1Password.exe");

        assertThat(filter.isExcluded("WeChat.exe")).isTrue();
        assertThat(filter.isExcluded("wechat.exe")).isTrue();
        assertThat(filter.isExcluded("  WECHAT.EXE  ")).isTrue();
        assertThat(filter.isExcluded("idea64.exe")).isFalse();
    }

    @Test
    void nullAndBlankAreNeverExcluded() {
        PrivacyFilter filter = filterOf("WeChat.exe");

        assertThat(filter.isExcluded(null)).isFalse();
        assertThat(filter.isExcluded("   ")).isFalse();
    }

    @Test
    void emptyConfigurationExcludesNothing() {
        assertThat(filterOf().isExcluded("WeChat.exe")).isFalse();
    }

    @Test
    void matchingIsExactNotSubstring() {
        // 不能用包含匹配：否则 "NotWeChat.exe" 会被误伤，"WeChat"（无后缀）会被漏掉
        PrivacyFilter filter = filterOf("WeChat.exe");

        assertThat(filter.isExcluded("NotWeChat.exe")).isFalse();
        assertThat(filter.isExcluded("WeChat")).isFalse();
    }
}
