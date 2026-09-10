package com.example.whataday;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * 数据层测试基类。
 *
 * <p>所有子类共用同一份 {@code @SpringBootTest} 配置，因此 Spring 只启动一次上下文。
 * 测试库使用 {@code backend/target} 下的文件型 SQLite：SQLite 的 {@code :memory:}
 * 是「一条连接一个库」，无法跨连接复用，用文件才能既共享上下文又不丢表结构。
 * 每个用例前清空数据，保证相互隔离。
 */
@SpringBootTest(properties = "spring.datasource.url=jdbc:sqlite:./target/test-whataday.db")
public abstract class RepositoryTestSupport {

    @Autowired
    protected JdbcTemplate jdbcTemplate;

    @BeforeEach
    void cleanDatabase() {
        // 子表先删，避免外键约束影响
        jdbcTemplate.execute("DELETE FROM daily_report");
        jdbcTemplate.execute("DELETE FROM user_note");
        jdbcTemplate.execute("DELETE FROM activity_event");
        jdbcTemplate.execute("DELETE FROM capture_observation");
    }
}
