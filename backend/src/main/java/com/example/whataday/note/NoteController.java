package com.example.whataday.note;

import com.example.whataday.common.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

/** 手动记录接口。 */
@RestController
@RequestMapping("/api/notes")
@Tag(name = "手动记录", description = "补充活动之外的用户笔记，参与日报生成")
public class NoteController {

    private final NoteService noteService;

    public NoteController(NoteService noteService) {
        this.noteService = noteService;
    }

    @PostMapping
    @Operation(summary = "新增手动记录")
    public ApiResponse<UserNote> create(@Valid @RequestBody CreateNoteRequest request) {
        return ApiResponse.ok(noteService.create(request.noteTime(), request.content(), request.tags()));
    }

    @GetMapping
    @Operation(summary = "按日期查询手动记录", description = "date 缺省为今天")
    public ApiResponse<List<UserNote>> byDate(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ApiResponse.ok(noteService.byDate(date));
    }
}
