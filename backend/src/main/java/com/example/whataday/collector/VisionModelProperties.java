package com.example.whataday.collector;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 视觉模型配置（对应 {@code whataday.vision.*}）。
 *
 * <p>API Key 通过环境变量注入（见 {@code application.yml} 里的 {@code ${WHATADAY_VISION_API_KEY:}}），
 * 不写进任何配置文件，避免密钥随代码入库。
 */
@Component
@ConfigurationProperties(prefix = "whataday.vision")
public class VisionModelProperties {

    /** API Key；为空表示未配置模型，分析时直接走窗口降级。 */
    private String apiKey = "";

    /** 兼容 OpenAI 协议的端点地址；换用其它厂商模型时改这里。 */
    private String baseUrl = "https://api.openai.com/v1";

    /** 模型名。 */
    private String modelName = "gpt-4o-mini";

    /** 单次调用超时（秒）。 */
    private int timeoutSeconds = 30;

    /** 单次回复的最大 token 数。 */
    private int maxTokens = 500;

    /** 温度：分析任务需要稳定输出，默认取 0。 */
    private double temperature = 0.0D;

    /**
     * 是否要求模型以 JSON 对象格式回复。
     *
     * <p>默认开启，能显著提高返回可解析的概率；但部分 OpenAI 兼容端点不支持该参数，
     * 遇到调用报错时可以关掉，改由提示词约束输出格式。
     */
    private boolean useJsonResponseFormat = true;

    /** 是否打印请求体。默认关闭——请求体里包含截图内容。 */
    private boolean logRequests = false;

    /** 是否打印响应体。 */
    private boolean logResponses = false;

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public String getModelName() {
        return modelName;
    }

    public void setModelName(String modelName) {
        this.modelName = modelName;
    }

    public int getTimeoutSeconds() {
        return timeoutSeconds;
    }

    public void setTimeoutSeconds(int timeoutSeconds) {
        this.timeoutSeconds = timeoutSeconds;
    }

    public int getMaxTokens() {
        return maxTokens;
    }

    public void setMaxTokens(int maxTokens) {
        this.maxTokens = maxTokens;
    }

    public double getTemperature() {
        return temperature;
    }

    public void setTemperature(double temperature) {
        this.temperature = temperature;
    }

    public boolean isUseJsonResponseFormat() {
        return useJsonResponseFormat;
    }

    public void setUseJsonResponseFormat(boolean useJsonResponseFormat) {
        this.useJsonResponseFormat = useJsonResponseFormat;
    }

    public boolean isLogRequests() {
        return logRequests;
    }

    public void setLogRequests(boolean logRequests) {
        this.logRequests = logRequests;
    }

    public boolean isLogResponses() {
        return logResponses;
    }

    public void setLogResponses(boolean logResponses) {
        this.logResponses = logResponses;
    }
}
