package com.example.whataday.memory;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/** 工作记忆 Embedding 配置，使用 OpenAI 兼容接口。 */
@Component
@ConfigurationProperties(prefix = "whataday.embedding")
public class EmbeddingProperties {

    private String apiKey = "";
    private String baseUrl = "https://api.openai.com/v1";
    private String modelName = "text-embedding-3-small";
    private int timeoutSeconds = 20;

    public String getApiKey() { return apiKey; }
    public void setApiKey(String apiKey) { this.apiKey = apiKey; }
    public String getBaseUrl() { return baseUrl; }
    public void setBaseUrl(String baseUrl) { this.baseUrl = baseUrl; }
    public String getModelName() { return modelName; }
    public void setModelName(String modelName) { this.modelName = modelName; }
    public int getTimeoutSeconds() { return timeoutSeconds; }
    public void setTimeoutSeconds(int timeoutSeconds) { this.timeoutSeconds = timeoutSeconds; }
}
