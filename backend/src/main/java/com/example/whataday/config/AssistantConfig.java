package com.example.whataday.config;

import com.example.whataday.assistant.WorkAssistant;
import com.example.whataday.assistant.WorkAssistantTools;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.service.AiServices;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** 工作助手装配；没有聊天模型时不创建，接口由 Controller 返回明确提示。 */
@Configuration
public class AssistantConfig {

    @Bean
    @ConditionalOnBean(ChatLanguageModel.class)
    public WorkAssistant workAssistant(ChatLanguageModel chatLanguageModel,
                                       WorkAssistantTools tools) {
        return AiServices.builder(WorkAssistant.class)
                .chatLanguageModel(chatLanguageModel)
                .tools(tools)
                .build();
    }
}
