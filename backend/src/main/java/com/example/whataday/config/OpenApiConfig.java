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
                .version("0.4.0")
                .description("""
                        本地优先的个人工作记录、复盘与效率分析工具。

                        基础链路：桌面采集 → 活动理解 → 结构化持久化 → 时间线与日报。

                        工作助手链路：工作指标统计 + 历史工作记忆检索 → 受控 Agent 回答工作状态问题。

                        视觉模型、Embedding 和聊天模型均为可选能力；未配置模型时，
                        本地采集、统计和规则日报仍可运行。
                        """));
    }
}
