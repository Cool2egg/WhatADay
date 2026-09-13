package com.example.whataday.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** OpenAPI 文档配置。Swagger UI： http://127.0.0.1:8080/swagger-ui.html */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI whatADayOpenApi() {
        return new OpenAPI().info(new Info()
                .title("WhatADay API")
                .version("0.3.0")
                .description("""
                        本地优先的工作复盘 Agent。

                        链路：桌面采集 → 多模态理解 → 结构化持久化 → Agent Tool Calling → 自动日报。

                        当前进度为 M2：Mock 数据 + REST API。采集与分析由 Mock 实现，
                        M6 会接入视觉模型与 LangChain4j 日报 Agent，接口保持不变。
                        """));
    }
}
