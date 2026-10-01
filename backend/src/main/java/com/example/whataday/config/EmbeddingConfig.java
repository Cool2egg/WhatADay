package com.example.whataday.config;

import com.example.whataday.memory.EmbeddingProperties;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.openai.OpenAiEmbeddingModel;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

/** Embedding 模型装配；未配置 Key 时不创建 Bean，索引仍可保存为待向量化状态。 */
@Configuration
public class EmbeddingConfig {

    @Bean
    @ConditionalOnExpression("'${whataday.embedding.api-key:}'.length() > 0")
    public EmbeddingModel embeddingModel(EmbeddingProperties properties) {
        return OpenAiEmbeddingModel.builder()
                .apiKey(properties.getApiKey())
                .baseUrl(properties.getBaseUrl())
                .modelName(properties.getModelName())
                .timeout(Duration.ofSeconds(properties.getTimeoutSeconds()))
                .build();
    }
}
