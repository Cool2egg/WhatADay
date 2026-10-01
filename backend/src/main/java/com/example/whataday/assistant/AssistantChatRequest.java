package com.example.whataday.assistant;

import jakarta.validation.constraints.NotBlank;

public record AssistantChatRequest(@NotBlank String message) {
}
