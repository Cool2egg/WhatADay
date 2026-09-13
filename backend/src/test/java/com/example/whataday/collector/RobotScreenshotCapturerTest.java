package com.example.whataday.collector;

import org.junit.jupiter.api.Test;

import java.awt.image.BufferedImage;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 截图缩放逻辑的测试。
 *
 * <p>真正的 {@code Robot} 抓屏需要图形环境，无法在无头环境测试；
 * 但缩放是纯图像计算，可以完整验证——这是这段逻辑被抽成静态方法的直接原因。
 */
class RobotScreenshotCapturerTest {

    @Test
    void keepsOriginalImageWhenWithinLimit() {
        BufferedImage source = new BufferedImage(800, 600, BufferedImage.TYPE_INT_RGB);

        assertThat(RobotScreenshotCapturer.scaleToMaxWidth(source, 1280)).isSameAs(source);
    }

    @Test
    void scalesDownPreservingAspectRatio() {
        BufferedImage source = new BufferedImage(2560, 1440, BufferedImage.TYPE_INT_RGB);

        BufferedImage scaled = RobotScreenshotCapturer.scaleToMaxWidth(source, 1280);

        assertThat(scaled.getWidth()).isEqualTo(1280);
        assertThat(scaled.getHeight()).isEqualTo(720);
    }

    @Test
    void nonPositiveLimitDisablesScaling() {
        BufferedImage source = new BufferedImage(2560, 1440, BufferedImage.TYPE_INT_RGB);

        assertThat(RobotScreenshotCapturer.scaleToMaxWidth(source, 0)).isSameAs(source);
    }
}
