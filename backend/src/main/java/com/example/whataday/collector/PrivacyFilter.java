package com.example.whataday.collector;

import org.springframework.stereotype.Component;

import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 敏感应用过滤。
 *
 * <p>隐私设计的关键在于<b>时机</b>：这个判断发生在截图之前。命中黑名单的应用
 * 既不截图、也不调用模型，只留一条「已忽略」的记录，从源头上避免敏感内容离开本机。
 *
 * <p>匹配忽略大小写与首尾空格，避免配置里的写法差异导致漏过滤。
 */
@Component
public class PrivacyFilter {

    private final Set<String> excludedProcesses;

    public PrivacyFilter(CollectorProperties properties) {
        this.excludedProcesses = properties.getExcludedProcesses().stream()
                .filter(Objects::nonNull)
                .map(name -> name.trim().toLowerCase(Locale.ROOT))
                .filter(name -> !name.isEmpty())
                .collect(Collectors.toUnmodifiableSet());
    }

    /** 该进程是否在敏感应用黑名单中。 */
    public boolean isExcluded(String processName) {
        if (processName == null || processName.isBlank()) {
            return false;
        }
        return excludedProcesses.contains(processName.trim().toLowerCase(Locale.ROOT));
    }

    /** 当前生效的黑名单，用于日志与排查。 */
    public Set<String> excludedProcesses() {
        return excludedProcesses;
    }
}
