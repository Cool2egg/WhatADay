package com.example.whataday.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * SQLite 运行时配置。
 *
 * <p>PRAGMA 已在数据源 URL 中配置（见 {@code application.yml}），保证每条新连接都带上；
 * 这里再显式执行一次并打印生效值，一是防御连接池行为差异，二是让启动日志能自证
 * 「WAL 已开启」，便于演示与排查。
 */
@Configuration
public class SqliteConfig {

    private static final Logger log = LoggerFactory.getLogger(SqliteConfig.class);

    @Bean
    public ApplicationRunner sqlitePragmaInitializer(JdbcTemplate jdbcTemplate) {
        return args -> {
            String journalMode = jdbcTemplate.queryForObject("PRAGMA journal_mode=WAL", String.class);
            jdbcTemplate.execute("PRAGMA busy_timeout=5000");
            jdbcTemplate.execute("PRAGMA foreign_keys=ON");
            log.info("SQLite 已就绪：journal_mode={}，busy_timeout=5000，foreign_keys=ON", journalMode);
        };
    }
}
