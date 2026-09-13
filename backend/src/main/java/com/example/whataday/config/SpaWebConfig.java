package com.example.whataday.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.resource.PathResourceResolver;

import java.io.IOException;

/**
 * 单 jar 运行时的前端路由回退。
 *
 * <p>前端用的是 React Router 的 history 模式，路径形如 {@code /reports} 并不对应任何真实文件。
 * 开发时 Vite 会自动回退到 index.html，但把构建产物打进 jar 后没有这一层——
 * 用户在浏览器里刷新 {@code /reports} 会直接 404。这个配置补上那一层。
 *
 * <p>三条边界必须守住，否则会误伤后端自身的路径：
 * <ul>
 *   <li>{@code /api/**} —— 后端接口，找不到就该 404，不能回退成前端页面</li>
 *   <li>{@code /swagger-ui/**} 与 {@code /v3/api-docs} —— 接口文档</li>
 *   <li>真实存在的静态资源 —— 原样返回</li>
 * </ul>
 * 只有「既不是真实文件、也不是后端路径」的请求才会拿到 index.html，交给前端路由处理。
 */
@Configuration
public class SpaWebConfig implements WebMvcConfigurer {

    private static final String INDEX_LOCATION = "static/index.html";

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/**")
                .addResourceLocations("classpath:/static/")
                .resourceChain(true)
                .addResolver(new PathResourceResolver() {
                    @Override
                    protected Resource getResource(String resourcePath, Resource location) throws IOException {
                        Resource requested = location.createRelative(resourcePath);
                        if (!resourcePath.isEmpty() && requested.exists() && requested.isReadable()) {
                            return requested;
                        }
                        if (isBackendPath(resourcePath)) {
                            return null;
                        }
                        Resource index = new ClassPathResource(INDEX_LOCATION);
                        return index.exists() ? index : null;
                    }
                });
    }

    private static boolean isBackendPath(String resourcePath) {
        return resourcePath.startsWith("api/")
                || resourcePath.startsWith("swagger-ui")
                || resourcePath.startsWith("v3/api-docs");
    }
}
