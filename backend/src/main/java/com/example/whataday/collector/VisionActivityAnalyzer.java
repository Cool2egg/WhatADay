package com.example.whataday.collector;

import com.example.whataday.activity.ActivityEvent;
import com.example.whataday.activity.ActivitySource;
import com.example.whataday.activity.ActivityType;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.data.image.Image;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.ImageContent;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.output.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.List;
import java.util.Optional;

/**
 * 基于视觉模型的活动分析：把「窗口信息 + 截图」理解成结构化活动事件。
 *
 * <p>这个类最重要的性质是<b>永不失败</b>。采集是后台长跑行为，不能因为模型限流、
 * 网络抖动、返回格式不对就中断。以下情况一律降级为窗口信息（source = WINDOW_FALLBACK）：
 * 没配置 API Key、本次没有截图、调用超时或报错、返回内容不是合法 JSON。
 *
 * <p>标注 {@link Primary} 的原因：容器里同时存在本类与 {@link WindowFallbackAnalyzer}
 * 两个 {@code ActivityAnalyzer}，采集服务应当默认使用本类，由本类在失败时主动调用降级实现。
 */
@Primary
@Component
public class VisionActivityAnalyzer implements ActivityAnalyzer {

    private static final Logger log = LoggerFactory.getLogger(VisionActivityAnalyzer.class);

    private static final String SYSTEM_PROMPT = """
            你是一个工作活动分析助手。用户会给你当前前台应用的名称、窗口标题、时间，
            有时还会附上一张屏幕截图。请判断用户此刻正在做什么，并输出结构化结果。

            规则：
            - type 必须取以下之一：
              CODING（写代码、调试、看代码）
              LEARNING（看教程、读文档、学习）
              MEETING（会议、语音沟通）
              BROWSING（浏览网页、查资料）
              ENTERTAINMENT（游戏、视频、娱乐）
              COMMUNICATION（聊天、邮件等文字沟通）
              OTHER（无法归类）
            - description：一句中文，说明用户在做什么，不要照抄窗口标题
            - keywords：1 到 5 个关键词
            - confidence：你对判断的把握程度，0 到 1 之间的小数
            - appName 与 windowTitle：原样返回用户给你的值，不要编造

            只输出 JSON 对象本身，不要输出 Markdown 代码块，也不要输出任何解释文字。格式示例：
            {"appName":"IntelliJ IDEA","windowTitle":"CouponService.java","type":"CODING",\
            "description":"正在修改优惠券领取逻辑","keywords":["Java","优惠券"],"confidence":0.93}
            """;

    private static final String USER_TEMPLATE = """
            应用：%s
            窗口标题：%s
            时间：%s
            """;

    /** 模型可能不存在（未配置 API Key），用 Optional 表达这种「可选依赖」。 */
    private final Optional<ChatLanguageModel> model;
    private final WindowFallbackAnalyzer fallbackAnalyzer;
    private final ObjectMapper objectMapper;

    public VisionActivityAnalyzer(Optional<ChatLanguageModel> model,
                                  WindowFallbackAnalyzer fallbackAnalyzer,
                                  ObjectMapper objectMapper) {
        this.model = model;
        this.fallbackAnalyzer = fallbackAnalyzer;
        this.objectMapper = objectMapper;
    }

    @Override
    public ActivityEvent analyze(CaptureObservation observation, Optional<Path> screenshot) {
        if (model.isEmpty()) {
            log.debug("未配置视觉模型，使用窗口降级");
            return fallbackAnalyzer.analyze(observation, screenshot);
        }

        try {
            ActivityAnalysisResult result = requestAnalysis(model.get(), observation, screenshot);
            if (result == null) {
                throw new IllegalStateException("模型返回了空的 JSON");
            }
            return toEvent(observation, result);
        } catch (Exception e) {
            log.warn("视觉分析失败，降级为窗口信息：{}", e.toString());
            return fallbackAnalyzer.analyze(observation, screenshot);
        }
    }

    private ActivityAnalysisResult requestAnalysis(ChatLanguageModel model,
                                                   CaptureObservation observation,
                                                   Optional<Path> screenshot) throws IOException {
        String userText = USER_TEMPLATE.formatted(
                text(observation.appName()),
                text(observation.windowTitle()),
                String.valueOf(observation.observedAt()));

        UserMessage userMessage = screenshot
                .map(path -> UserMessage.from(userText, toImageContent(path)))
                .orElseGet(() -> UserMessage.from(userText));

        List<ChatMessage> messages = List.of(SystemMessage.from(SYSTEM_PROMPT), userMessage);
        Response<AiMessage> response = model.generate(messages);

        String json = stripCodeFence(response.content().text());
        return objectMapper.readValue(json, ActivityAnalysisResult.class);
    }

    private ActivityEvent toEvent(CaptureObservation observation, ActivityAnalysisResult result) {
        String windowTitle = firstNonBlank(result.windowTitle(), observation.windowTitle());
        return new ActivityEvent(
                null,
                observation.id(),
                observation.observedAt(),
                // 零时长起步，后续由合并逻辑推进
                observation.observedAt(),
                firstNonBlank(result.appName(), observation.appName()),
                windowTitle,
                // 未知类型自动落到 OTHER，保证入库数据类型可控
                ActivityType.from(result.type()),
                firstNonBlank(result.description(), windowTitle, observation.appName()),
                result.keywords(),
                // record 的紧凑构造器会把置信度夹紧到 [0,1]
                result.confidence(),
                ActivitySource.VISION,
                null);
    }

    private static ImageContent toImageContent(Path path) {
        Image image = Image.builder()
                .base64Data(readAsBase64(path))
                .mimeType("image/png")
                .build();
        // 截图已经缩放到 1280 宽以内，这里用 HIGH 细节以保证画面中的文字可读；
        // 控制成本靠的是「每 2 分钟最多一张」的频率限制，而不是牺牲分辨率。
        return new ImageContent(image, ImageContent.DetailLevel.HIGH);
    }

    private static String readAsBase64(Path path) {
        try {
            return Base64.getEncoder().encodeToString(Files.readAllBytes(path));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /** 有些模型仍会用 Markdown 代码块包裹 JSON，这里做一次容错剥离。 */
    static String stripCodeFence(String text) {
        if (text == null) {
            return "";
        }
        String trimmed = text.trim();
        if (!trimmed.startsWith("```")) {
            return trimmed;
        }
        int firstLineBreak = trimmed.indexOf('\n');
        int closingFence = trimmed.lastIndexOf("```");
        if (firstLineBreak < 0 || closingFence <= firstLineBreak) {
            return trimmed;
        }
        return trimmed.substring(firstLineBreak + 1, closingFence).trim();
    }

    private static String firstNonBlank(String... candidates) {
        for (String candidate : candidates) {
            if (candidate != null && !candidate.isBlank()) {
                return candidate;
            }
        }
        return null;
    }

    private static String text(String value) {
        return value == null || value.isBlank() ? "（未知）" : value;
    }
}
