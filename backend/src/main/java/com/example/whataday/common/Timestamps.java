package com.example.whataday.common;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 数据库时间字段与 {@link LocalDateTime} 的转换工具。
 *
 * <p>SQLite 没有原生日期类型，统一以<b>固定格式</b>的 ISO-8601 文本存储，
 * 格式定为 {@code yyyy-MM-dd'T'HH:mm:ss}。之所以不用 {@link LocalDateTime#toString()}，
 * 是因为它在秒为 0 时会省略秒（{@code 2026-09-10T21:30}），导致同一列中格式不一致、
 * 字符串比较排序出错。固定格式可保证「按 TEXT 范围查询 = 按时间范围查询」。
 */
public final class Timestamps {

    private static final DateTimeFormatter FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    private Timestamps() {
    }

    /** 格式化为数据库文本；{@code null} 透传。 */
    public static String format(LocalDateTime time) {
        return time == null ? null : FORMATTER.format(time);
    }

    /** 解析数据库文本；{@code null} 透传。 */
    public static LocalDateTime parse(String text) {
        return text == null ? null : LocalDateTime.parse(text, FORMATTER);
    }

    /** 某日 00:00:00 的文本，作为当天范围查询的下界（含）。 */
    public static String dayStart(LocalDate date) {
        return FORMATTER.format(date.atStartOfDay());
    }

    /** 次日 00:00:00 的文本，作为当天范围查询的上界（不含）。 */
    public static String dayEnd(LocalDate date) {
        return FORMATTER.format(date.plusDays(1).atStartOfDay());
    }
}
