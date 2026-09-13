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
     * 创建视觉模型；<b>未配置 API Key 时不创建这个 Bean</b>，
     * 分析器会自动退回窗口降级，因此无 Key 也能完整运行整个应用。
     *
     * <p>这里用 {@link ConditionalOnExpression} 而不是 {@code @ConditionalOnProperty}：
     * Key 未设置时属性值是空字符串，而 {@code @ConditionalOnProperty} 会把
     * 「属性存在但为空」判为匹配，结果就是拿着空 Key 去构建模型并在启动时报错。
     */
    @Bean
    @ConditionalOnExpression("'${whataday.vision.api-key:}'.length() > 0")
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
