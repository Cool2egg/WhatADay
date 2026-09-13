package com.example.whataday.collector;

import com.example.whataday.activity.ActivityEvent;
import com.example.whataday.activity.ActivitySource;
import com.example.whataday.activity.ActivityType;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.ImageContent;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.output.Response;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 视觉分析器测试。
 *
 * <p>这个类最需要被证明的性质不是「模型正常时能解析」，而是
 * <b>模型出任何问题时都不会让采集链路断掉</b>——所以下面一半的用例都在验证降级。
 */
class VisionActivityAnalyzerTest {

    private static final LocalDateTime OBSERVED_AT = LocalDateTime.of(2026, 9, 13, 10, 0);

    private static final String VALID_JSON = """
            {"appName":"IntelliJ IDEA","windowTitle":"CouponService.java","type":"CODING",
             "description":"正在修改优惠券领取逻辑","keywords":["Java","优惠券"],"confidence":0.93}
            """;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final WindowFallbackAnalyzer fallback = new WindowFallbackAnalyzer();

    private VisionActivityAnalyzer analyzerWith(ChatLanguageModel model) {
        return new VisionActivityAnalyzer(Optional.ofNullable(model), fallback, objectMapper);
    }

    @Test
    void parsesModelOutputIntoVisionSourcedActivity() {
        ActivityEvent event = analyzerWith(new StubModel(VALID_JSON)).analyze(observation(), Optional.empty());

        assertThat(event.type()).isEqualTo(ActivityType.CODING);
        assertThat(event.description()).isEqualTo("正在修改优惠券领取逻辑");
        assertThat(event.keywords()).containsExactly("Java", "优惠券");
        assertThat(event.confidence()).isEqualTo(0.93);
        assertThat(event.source()).isEqualTo(ActivitySource.VISION);
        assertThat(event.observationId()).isEqualTo(7L);
        assertThat(event.startTime()).isEqualTo(OBSERVED_AT);
    }

    @Test
    void fallsBackWhenModelThrows() {
        ChatLanguageModel failing = new StubModel(null, new IllegalStateException("模型限流"));

        ActivityEvent event = analyzerWith(failing).analyze(observation(), Optional.empty());

        assertThat(event.source()).isEqualTo(ActivitySource.WINDOW_FALLBACK);
        assertThat(event.type()).isEqualTo(ActivityType.OTHER);
        assertThat(event.description()).isEqualTo("CouponService.java");
    }

    @Test
    void fallsBackWhenModelReturnsNonJson() {
        ActivityEvent event = analyzerWith(new StubModel("抱歉，我无法完成这个请求。"))
                .analyze(observation(), Optional.empty());

        assertThat(event.source()).isEqualTo(ActivitySource.WINDOW_FALLBACK);
    }

    @Test
    void fallsBackWhenNoModelIsConfigured() {
        // 没配 API Key 的情形：模型不存在，照样要能产出事件
        ActivityEvent event = analyzerWith(null).analyze(observation(), Optional.empty());

        assertThat(event.source()).isEqualTo(ActivitySource.WINDOW_FALLBACK);
        assertThat(event.appName()).isEqualTo("idea64");
        assertThat(event.windowTitle()).isEqualTo("CouponService.java");
    }

    @Test
    void unknownTypeFallsBackToOther() {
        String json = """
                {"type":"EXERCISING","description":"骑车上班","keywords":[],"confidence":0.8}
                """;

        ActivityEvent event = analyzerWith(new StubModel(json)).analyze(observation(), Optional.empty());

        assertThat(event.type()).isEqualTo(ActivityType.OTHER);
    }

    @Test
    void confidenceOutsideRangeIsClamped() {
        String tooHigh = """
                {"type":"CODING","description":"写代码","keywords":[],"confidence":1.7}
                """;

        ActivityEvent event = analyzerWith(new StubModel(tooHigh)).analyze(observation(), Optional.empty());

        assertThat(event.confidence()).isEqualTo(1.0);
    }

    @Test
    void missingFieldsAreFilledFromWindowInfo() {
        String minimal = """
                {"type":"CODING"}
                """;

        ActivityEvent event = analyzerWith(new StubModel(minimal)).analyze(observation(), Optional.empty());

        assertThat(event.appName()).isEqualTo("idea64");
        assertThat(event.windowTitle()).isEqualTo("CouponService.java");
        assertThat(event.description()).isEqualTo("CouponService.java");

        assertThat(event.keywords()).isEmpty();
    }

    @Test
    void attachesScreenshotWhenAvailable() throws IOException {
        StubModel model = new StubModel(VALID_JSON);
        Path screenshot = Files.createTempFile("whataday-analyzer-test-", ".png");
        Files.write(screenshot, new byte[]{1, 2, 3, 4});
        try {
            analyzerWith(model).analyze(observation(), Optional.of(screenshot));

            UserMessage sent = (UserMessage) model.captured().get(1);
            assertThat(sent.contents()).anyMatch(content -> content instanceof ImageContent);
        } finally {
            Files.deleteIfExists(screenshot);
        }
    }

    @Test
    void sendsTextOnlyWhenNoScreenshot() {
        StubModel model = new StubModel(VALID_JSON);

        analyzerWith(model).analyze(observation(), Optional.empty());

        UserMessage sent = (UserMessage) model.captured().get(1);
        assertThat(sent.contents()).noneMatch(content -> content instanceof ImageContent);
    }

    @Test
    void stripsMarkdownCodeFenceAroundJson() {
        String fenced = "```json\n" + VALID_JSON.trim() + "\n```";

        ActivityEvent event = analyzerWith(new StubModel(fenced)).analyze(observation(), Optional.empty());

        assertThat(event.source()).isEqualTo(ActivitySource.VISION);
        assertThat(event.type()).isEqualTo(ActivityType.CODING);
    }

    @Test
    void stripCodeFenceLeavesPlainTextUntouched() {
        assertThat(VisionActivityAnalyzer.stripCodeFence("  {\"a\":1}  ")).isEqualTo("{\"a\":1}");
        assertThat(VisionActivityAnalyzer.stripCodeFence(null)).isEmpty();
    }

    private static CaptureObservation observation() {
        return new CaptureObservation(7L, OBSERVED_AT, "idea64", "CouponService.java",
                AnalysisStatus.PENDING, null, null);
    }

    /** 模型桩：要么返回固定文本，要么按配置抛错。 */
    private static final class StubModel implements ChatLanguageModel {

        private final String reply;
        private final RuntimeException failure;
        private List<ChatMessage> captured;

        StubModel(String reply) {
            this(reply, null);
        }

        StubModel(String reply, RuntimeException failure) {
            this.reply = reply;
            this.failure = failure;
        }

        List<ChatMessage> captured() {
            return captured;
        }

        @Override
        public Response<AiMessage> generate(List<ChatMessage> messages) {
            if (failure != null) {
                throw failure;
            }
            this.captured = messages;
            return Response.from(AiMessage.from(reply));
        }
    }
}
