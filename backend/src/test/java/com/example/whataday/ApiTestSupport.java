package com.example.whataday;

import com.example.whataday.collector.CollectorService;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

/**
 * 接口层测试基类。
 *
 * <p>用与数据层测试相同的文件型测试库，且 Spring 上下文在所有测试类之间共享，因此每个用例前
 * 必须把状态重置干净，否则会互相干扰。要重置的有两处：
 * <ul>
 *   <li><b>数据库</b>——{@code MockDataSeeder} 会在应用启动时灌入当天示例数据。</li>
 *   <li><b>采集器运行状态</b>——它保存在内存里、随上下文共享，若上一用例把采集器开着，
 *       下一用例的 {@code start()} 会因为「已在运行」而跳过，导致拿不到示例数据。</li>
 * </ul>
 */
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = "spring.datasource.url=jdbc:sqlite:./target/test-whataday.db")
public abstract class ApiTestSupport {

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected JdbcTemplate jdbcTemplate;

    @Autowired
    private CollectorService collectorService;

    @BeforeEach
    void resetState() {
        collectorService.stop();
        jdbcTemplate.execute("DELETE FROM daily_report");
        jdbcTemplate.execute("DELETE FROM user_note");
        jdbcTemplate.execute("DELETE FROM activity_event");
        jdbcTemplate.execute("DELETE FROM capture_observation");
    }
}
