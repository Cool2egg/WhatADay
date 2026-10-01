package com.example.whataday.assistant;

import com.example.whataday.common.ApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import jakarta.validation.Valid;

import java.util.Optional;

/** 工作状态助手接口。 */
@RestController
@RequestMapping("/api/assistant")
public class AssistantController {

    private final Optional<WorkAssistant> assistant;

    public AssistantController(Optional<WorkAssistant> assistant) {
        this.assistant = assistant;
    }

    @PostMapping("/chat")
    public ApiResponse<String> chat(@Valid @RequestBody AssistantChatRequest request) {
        WorkAssistant current = assistant.orElseThrow(() -> new ResponseStatusException(
                HttpStatus.SERVICE_UNAVAILABLE, "未配置聊天模型，工作助手暂不可用"));
        return ApiResponse.ok(current.chat(request.message()));
    }
}
