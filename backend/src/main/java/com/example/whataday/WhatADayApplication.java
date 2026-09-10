package com.example.whataday;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * WhatADay 应用入口。
 *
 * <p>本地优先的工作复盘 Agent：桌面采集 → 多模态理解 → 结构化持久化 →
 * Agent Tool Calling → 自动日报 → 前端展示。
 */
@SpringBootApplication
@EnableScheduling
public class WhatADayApplication {

    public static void main(String[] args) {
        SpringApplication.run(WhatADayApplication.class, args);
    }
}
