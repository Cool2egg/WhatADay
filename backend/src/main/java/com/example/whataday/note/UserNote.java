package com.example.whataday.note;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 用户手动记录。活动之外的信息（如会议结论、临时想法）由用户自己补录，参与日报生成。
 *
 * @param id        主键，插入前为 {@code null}
 * @param noteTime  记录时间
 * @param content   记录内容
 * @param tags      标签，不会为 {@code null}
 * @param createdAt 入库时间
 */
public record UserNote(
        Long id,
        LocalDateTime noteTime,
        String content,
        List<String> tags,
        LocalDateTime createdAt
) {

    public UserNote {
        tags = tags == null ? List.of() : List.copyOf(tags);
    }
}
