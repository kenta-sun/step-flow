package io.github.kentasun.stepflow.step.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 步骤信息表
 */
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
public class StepData {

    // 步骤标识
    private String stepCode;

    // 步骤名称
    private String stepName;

    // 步骤类型
    private String stepType;

    // 内容类型，详情见 StepContentType
    private String contentType;

    // 步骤内容
    private String content;

    /*
     * 该步骤需要的参数名称列表
     * 表达式或javaMethod类型可以有参数。如果公共参数map中的名称不对，需要映射成该列表中的名称。
     */
    private List<String> paramNameList;

    // 返回字段列表，多个返回字段配置在这里，否则为空
    private List<String> returnFieldList;
}
