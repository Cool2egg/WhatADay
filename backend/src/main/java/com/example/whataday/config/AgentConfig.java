package com.example.whataday.config;

import com.example.whataday.report.DailyReportAgent;
import com.example.whataday.report.ReportTools;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.service.AiServices;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** 日报 Agent 的装配。 */
@Configuration
public class AgentConfig {

    /**
     * 日报 Agent，与视觉分析共用同一个模型。
     *
     * <p>未配置 API Key 时不创建这个 Bean，{@code ReportService} 会自动回退到
     * 按活动类型挑选内容的启发式实现——所以没有任何模型的环境下，日报功能依然完整可用。
     *
     * <p>这里是 Agent 权限的<b>唯一</b>来源：{@code tools(reportTools)} 只交出那三个受限工具，
     * Agent 拿不到数据库连接、文件系统或任何其它能力。
     */
    @Bean
    @ConditionalOnExpression(ModelConditions.API_KEY_PRESENT)
    public DailyReportAgent dailyReportAgent(ChatLanguageModel chatLanguageModel,
                                             ReportTools reportTools) {
        return AiServices.builder(DailyReportAgent.class)
                .chatLanguageModel(chatLanguageModel)
                .tools(reportTools)
                .build();
    }
}
