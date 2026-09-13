package com.example.whataday.config;

import com.example.whataday.collector.VisionModelProperties;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

/** 视觉模型装配。 */
@Configuration
public class VisionModelConfig {

    /**
     * 创建语言/视觉模型；<b>未配置 API Key 时不创建这个 Bean</b>，
     * 活动分析会自动退回窗口降级、日报生成会退回启发式实现，因此无 Key 也能完整运行应用。
     */
    @Bean
    @ConditionalOnExpression(ModelConditions.API_KEY_PRESENT)
    public ChatLanguageModel visionChatModel(VisionModelProperties properties) {
        OpenAiChatModel.OpenAiChatModelBuilder builder = OpenAiChatModel.builder()
                .apiKey(properties.getApiKey())
                .baseUrl(properties.getBaseUrl())
                .modelName(properties.getModelName())
                .timeout(Duration.ofSeconds(properties.getTimeoutSeconds()))
                .maxTokens(properties.getMaxTokens())
                .temperature(properties.getTemperature())
                .logRequests(properties.isLogRequests())
                .logResponses(properties.isLogResponses());

        if (properties.isUseJsonResponseFormat()) {
            builder.responseFormat("json_object");
        }
        return builder.build();
    }
}
