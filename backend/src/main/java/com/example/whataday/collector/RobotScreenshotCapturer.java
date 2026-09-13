package com.example.whataday.collector;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.awt.AWTException;
import java.awt.Graphics2D;
import java.awt.HeadlessException;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Robot;
import java.awt.Toolkit;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import javax.imageio.ImageIO;

/**
 * 基于 AWT Robot 的截屏实现。
 *
 * <p>两点控制成本与隐私的设计：截图先按最大宽度缩放再落盘，既减少体积也减少送进模型的信息量；
 * 截图只写系统临时目录，且由调用方保证分析后立即删除。
 */
@Component
@ConditionalOnProperty(name = "whataday.collector.mode", havingValue = "desktop")
public class RobotScreenshotCapturer implements ScreenshotCapturer {

    private static final Logger log = LoggerFactory.getLogger(RobotScreenshotCapturer.class);

    private final CollectorProperties properties;

    public RobotScreenshotCapturer(CollectorProperties properties) {
        this.properties = properties;
    }

    @Override
    public Optional<Path> capture() {
        Path target = null;
        try {
            Rectangle screen = new Rectangle(Toolkit.getDefaultToolkit().getScreenSize());
            BufferedImage raw = new Robot().createScreenCapture(screen);
            BufferedImage scaled = scaleToMaxWidth(raw, properties.getScreenshotMaxWidth());

            target = Files.createTempFile("whataday-shot-", ".png");
            ImageIO.write(scaled, "png", target.toFile());
            return Optional.of(target);
        } catch (HeadlessException | AWTException | IOException | SecurityException e) {
            // 无图形界面、屏幕访问被拒等情况下静默降级：没有截图也能继续采集窗口信息
            log.debug("截屏失败，跳过本次截图：{}", e.toString());
            deleteQuietly(target);
            return Optional.empty();
        }
    }

    /**
     * 按最大宽度等比缩放；未超过上限时原样返回。
     *
     * <p>抽成静态方法是为了能被单独测试——它不依赖图形环境。
     */
    static BufferedImage scaleToMaxWidth(BufferedImage source, int maxWidth) {
        if (maxWidth <= 0 || source.getWidth() <= maxWidth) {
            return source;
        }
        int targetWidth = maxWidth;
        int targetHeight = Math.max(1, source.getHeight() * maxWidth / source.getWidth());

        BufferedImage scaled = new BufferedImage(targetWidth, targetHeight, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = scaled.createGraphics();
        try {
            graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            graphics.drawImage(source, 0, 0, targetWidth, targetHeight, null);
        } finally {
            graphics.dispose();
        }
        return scaled;
    }

    private static void deleteQuietly(Path path) {
        if (path == null) {
            return;
        }
        try {
            Files.deleteIfExists(path);
        } catch (IOException ignored) {
            // 临时文件清理失败不影响主流程
        }
    }
}
