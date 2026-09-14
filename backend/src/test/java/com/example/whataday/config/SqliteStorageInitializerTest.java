package com.example.whataday.config;

import org.junit.jupiter.api.Test;

import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 数据库路径解析的测试。
 *
 * <p>这段逻辑决定「从哪个目录启动会失败」，所以把边界情况都固定下来：
 * 内存库、file: 形式的 URL、非 SQLite 数据源都不该去建目录。
 */
class SqliteStorageInitializerTest {

    @Test
    void resolvesRelativeSqliteFile() {
        Path path = SqliteStorageInitializer.resolveDatabaseFile("jdbc:sqlite:./data/whataday.db");

        assertThat(path).isNotNull();
        assertThat(path.toString()).endsWith("data/whataday.db");
    }

    @Test
    void stripsJdbcUrlParameters() {
        Path path = SqliteStorageInitializer.resolveDatabaseFile(
                "jdbc:sqlite:./data/whataday.db?journal_mode=WAL&busy_timeout=5000");

        assertThat(path).isNotNull();
        // 参数不能被当成文件名的一部分，否则会去创建一个叫 "whataday.db?journal_mode=WAL" 的目录
        assertThat(path.toString()).doesNotContain("?");
        assertThat(path.toString()).endsWith("whataday.db");
    }

    @Test
    void resolvesAbsoluteSqliteFile() {
        Path path = SqliteStorageInitializer.resolveDatabaseFile("jdbc:sqlite:/tmp/whataday/x.db");

        assertThat(path).isNotNull();
        assertThat(path.isAbsolute()).isTrue();
    }

    @Test
    void ignoresMemoryAndNonSqliteDataSources() {
        assertThat(SqliteStorageInitializer.resolveDatabaseFile("jdbc:sqlite::memory:")).isNull();
        assertThat(SqliteStorageInitializer.resolveDatabaseFile("jdbc:sqlite:file:memdb?mode=memory"))
                .isNull();
        assertThat(SqliteStorageInitializer.resolveDatabaseFile("jdbc:h2:mem:test")).isNull();
        assertThat(SqliteStorageInitializer.resolveDatabaseFile(null)).isNull();
        assertThat(SqliteStorageInitializer.resolveDatabaseFile("jdbc:sqlite:")).isNull();
    }
}
