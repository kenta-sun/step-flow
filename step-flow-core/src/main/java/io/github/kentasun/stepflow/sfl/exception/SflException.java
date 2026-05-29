package io.github.kentasun.stepflow.sfl.exception;

/**
 * Step Flow Language（SFL）解析过程中抛出的运行时异常。
 * <p>
 * 用于区分「编排文本本身不合法」与「流程执行期」的 {@link io.github.kentasun.stepflow.exception.StepFlowException}：
 * 本异常仅在词法/语法分析阶段产生，消息中通常附带源文本中的字符偏移，便于在
 * {@code InputFlow.content} 中定位错误。
 * </p>
 */
public final class SflException extends RuntimeException {

    /**
     * 构造仅含描述信息的解析异常。
     *
     * @param message 人类可读的错误说明（可含位置信息）
     */
    public SflException(String message) {
        super(message);
    }

    /**
     * 构造带根本原因的解析异常，例如通过反射实例化 {@link io.github.kentasun.stepflow.flow.dto.node.FlowNode}
     * 子类失败时保留原始 {@link ReflectiveOperationException}。
     *
     * @param message 人类可读的错误说明
     * @param cause   底层异常，不可为 {@code null} 时由调用方保证
     */
    public SflException(String message, Throwable cause) {
        super(message, cause);
    }
}
