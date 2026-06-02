package io.github.kentasun.stepflow.api.step;

import io.github.kentasun.stepflow.api.step.dto.StepInputData;

import java.util.List;

/**
 * Step 信息提供者接口。
 */
public interface StepDataProvider {

    /**
     * 加载并返回所有 StepData。
     *
     * @return 已拼装完成的 StepData 列表
     */
    List<StepInputData> loadStepDataList();
}
