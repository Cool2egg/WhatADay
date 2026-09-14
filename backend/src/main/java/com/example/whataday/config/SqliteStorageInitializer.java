package com.example.whataday.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.EnvironmentAware;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * 启动前把 SQLite 数据库文件所在的目录建好。
 *
 * <p>SQLite 会创建数据库文件，但<b>不会创建它所在的目录</b>。而默认数据源用的是相对路径
 * {@code ./data/whataday.db}——只要进程的工作目录不是 {@code backend}，这个目录就不存在，
 * SQLite 会直接抛 {@code path to './data/whataday.db' ... does not exist}，让整个应用起不来。
 *
 * <p>两个很常见的触发场景：在 IDEA 里从项目根目录启动（工作目录默认是项目根而不是模块目录）、
 * 从仓库根目录执行打包好的 jar。两者都不该导致启动失败。
 *
 * <p>这里在 Bean 实例化之前把目录补上，让「从哪里启动」不再影响能否运行；
 * 同时把解析后的绝对路径打进日志——否则数据库可能悄悄落在意料之外的地方而没人发现。
 */
@Component
public class SqliteStorageInitializer implements BeanFactoryPostProcessor, EnvironmentAware {

    private static final Logger log = LoggerFactory.getLogger(SqliteStorageInitializer.class);

    private static final String SQLITE_PREFIX = "jdbc:sqlite:";

    private Environment environment;

    @Override
    public void setEnvironment(Environment environment) {
        this.environment = environment;
    }

    @Override
    public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
        Path databaseFile = resolveDatabaseFile(environment.getProperty("spring.datasource.url"));
        if (databaseFile == null) {
            return;
        }

        Path absolute = databaseFile.toAbsolutePath();
        Path directory = absolute.getParent();
        if (directory == null) {
            return;
        }

        try {
            Files.createDirectories(directory);
            log.info("SQLite 数据库位置：{}", absolute);
        } catch (IOException e) {
            throw new UncheckedIOException("创建数据库目录失败：" + directory, e);
        }
    }

    /**
     * 从 JDBC URL 中解析出数据库文件路径。
     *
     * <p>非 SQLite 数据源、内存库、以及 {@code file:} 形式的 URL 都返回 {@code null}——
     * 这些情况没有「需要预先创建的目录」。
     */
    static Path resolveDatabaseFile(String url) {
        if (url == null || !url.startsWith(SQLITE_PREFIX)) {
            return null;
        }

        String path = url.substring(SQLITE_PREFIX.length());
        int queryIndex = path.indexOf('?');
        if (queryIndex >= 0) {
            path = path.substring(0, queryIndex);
        }

        if (path.isBlank() || path.startsWith(":memory:") || path.startsWith("file:")) {
            return null;
        }
        return Paths.get(path);
    }
}
