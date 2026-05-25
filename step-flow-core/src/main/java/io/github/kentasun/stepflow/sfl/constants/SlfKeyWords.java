package io.github.kentasun.stepflow.sfl.constants;

/**
 * SFL（Step Flow Language）语法关键字字面量常量集。
 * <p>
 * 取值与源文本中的标识符完全一致，供 {@link io.github.kentasun.stepflow.sfl.SflSyntaxParser}
 * 注册表及各 {@link io.github.kentasun.stepflow.sfl.parser.KeywordParser} 实现统一引用，
 * 避免魔法字符串分散在解析逻辑中。
 * </p>
 */
public final class SlfKeyWords {

    private SlfKeyWords() {
        // 工具类，禁止实例化
    }

    // -------------------------------------------------------------------------
    // 顶层编排关键字（flow 产生式入口）
    // -------------------------------------------------------------------------

    /** 顺序执行容器：{@code SEQ(child, child, ...)} */
    public static final String SEQ = "SEQ";

    /** 并行执行容器：{@code PARALLEL(child, child, ...)} */
    public static final String PARALLEL = "PARALLEL";

    /** 单步执行：{@code STEP(stepCode)[.param(...)][.result(...)]} */
    public static final String STEP = "STEP";

    /** 子流程引用：{@code SUB_FLOW(flowCode)} */
    public static final String SUB_FLOW = "SUB_FLOW";

    /** 条件分支：{@code IF(条件).TRUE(真分支)[.FALSE(假分支)]} */
    public static final String IF = "IF";

    // -------------------------------------------------------------------------
    // IF 后缀分支标识符
    // -------------------------------------------------------------------------

    /** IF 真分支后缀：{@code .TRUE(...)}，必填 */
    public static final String IF_TRUE = "TRUE";

    /** IF 假分支后缀：{@code .FALSE(...)}，可省略 */
    public static final String IF_FALSE = "FALSE";

    // -------------------------------------------------------------------------
    // STEP 后缀标识符
    // -------------------------------------------------------------------------

    /** STEP 入参映射后缀：{@code .param(k=v,...)} */
    public static final String STEP_PARAM = "param";

    /** STEP 出参映射后缀：{@code .result(k=v,...)} */
    public static final String STEP_RESULT = "result";
}
