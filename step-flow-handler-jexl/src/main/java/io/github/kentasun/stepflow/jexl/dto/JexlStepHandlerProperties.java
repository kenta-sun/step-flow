package io.github.kentasun.stepflow.jexl.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 表达式引擎通用配置项
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JexlStepHandlerProperties {

    /**
     * 表达式缓存最大条数。
     */
    private Integer cache;

    /**
     * 是否开启引擎调试日志
     */
    private Boolean debug;
}
