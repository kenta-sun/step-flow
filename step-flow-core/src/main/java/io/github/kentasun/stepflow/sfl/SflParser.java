package io.github.kentasun.stepflow.sfl;

import io.github.kentasun.stepflow.flow.dto.node.FlowNode;
import io.github.kentasun.stepflow.flow.dto.node.IfElseFlowNode;

/**
 * Step Flow Language（SFL）解析入口：将存于 {@code InputFlow.content} 的编排文本解析为 {@link FlowNode} 树。
 * <p>
 * SFL 是面向流程编排的文本记号，与 JSON 形式的 content 互为等价表示，便于在库表中手写或生成
 * 紧凑编排。本类仅负责词法、语法分析及领域节点构建，不包含流程执行逻辑。
 * </p>
 * <p>支持的编排关键字：</p>
 * <ul>
 *   <li>{@code SEQ(a, b, ...)} — 顺序执行</li>
 *   <li>{@code PARALLEL(a, b, ...)} — 并行执行</li>
 *   <li>{@code STEP(stepCode).param(k=v,...).result(k=v,...)} — 执行单个步骤</li>
 *   <li>{@code SUB_FLOW(flowCode)} — 调用子流程</li>
 *   <li>{@code IF(条件).TRUE(分支)[.FALSE(分支)]} — 条件分支（IF、TRUE 必填，FALSE 可省略）</li>
 * </ul>
 * <p>示例（对应 AviatorExpressionTest CALC001）：</p>
 * <pre>
 * SEQ(
 *   PARALLEL(
 *     STEP(COMMON001).param(a=dto.num1,b=dto.num2).result(add=calc_add),
 *     STEP(COMMON002).param(a=dto.num3,b=dto.num4).result(subtract=calc_subtract)
 *   ),
 *   IF(STEP(CONDITION001)).TRUE(STEP(COMMON003).param(a=calc_add,b=calc_subtract).result(multiply=calc_multiply))
 *     .FALSE(STEP(COMMON004).param(a=calc_add,b=calc_subtract).result(divide=calc_divide)),
 *   STEP(JAVA001)
 * )
 * </pre>
 *
 * @see SflLexer
 * @see SflSyntaxParser
 * @see SflException
 */
public final class SflParser {

    private SflParser() {
    }

    /**
     * 将 SFL 文本解析为流程树根节点。
     * <p>
     * 解析成功后额外消费 {@link SflTokenType#EOF}，确保源字符串尾部无未解析的残留记号，
     * 避免「只解析了前缀、后半段被静默忽略」类隐患。
     * </p>
     *
     * @param sfl 存于 {@code InputFlow.content} 的 SFL 文本，不可为 null 或空白
     * @return 流程节点树，类型为具体 {@link FlowNode} 子类
     * @throws SflException 文本为空、词法非法、语法不符合产生式或语义约束（如 IF 条件非 STEP）时
     */
    public static FlowNode parse(String sfl) {
        if (sfl == null || sfl.trim().isEmpty()) {
            throw new SflException("SFL 不能为空");
        }
        SflLexer sflLexer = new SflLexer(sfl);
        SflSyntaxParser parser = new SflSyntaxParser(sflLexer);
        FlowNode root = parser.parseFlow();
        parser.expect(SflTokenType.EOF);
        return root;
    }

    /**
     * 将已解析的 {@link FlowNode} 树格式化为缩进文本，便于肉眼核对结构。
     * <p>
     * 委托 {@link SflDebugPrinter}，保持本类 API 稳定的同时将调试格式化职责分离。
     * </p>
     *
     * @param node 解析结果根节点
     * @return 多行调试文本
     */
    public static String debugPrint(FlowNode node) {
        return SflDebugPrinter.format(node);
    }

    /**
     * 本地演示入口：使用与 AviatorExpressionTest CALC001 等价的 SFL 样例验证解析与约束。
     *
     * @param args 未使用
     */
    public static void main(String[] args) {
        String sfl = "SEQ("
                + "PARALLEL("
                + "STEP(COMMON001).param(a=dto.num1,b=dto.num2).result(add=calc_add),"
                + "STEP(COMMON002).param(a=dto.num3,b=dto.num4).result(subtract=calc_subtract)"
                + "),"
                + "IF(STEP(CONDITION001)).TRUE(STEP(COMMON003).param(a=calc_add,b=calc_subtract).result(multiply=calc_multiply))"
                + ".FALSE(STEP(COMMON004).param(a=calc_add,b=calc_subtract).result(divide=calc_divide)),"
                + "STEP(JAVA001)"
                + ")";

        System.out.println("========== 输入 SFL ==========");
        System.out.println(sfl);
        System.out.println();

        FlowNode root = parse(sfl);
        System.out.println("========== 解析成功 ==========");
        System.out.println(debugPrint(root));

        String sflNoFalse = "IF(STEP(CONDITION001)).TRUE(STEP(COMMON003))";
        FlowNode ifOnly = parse(sflNoFalse);
        System.out.println("========== 无 FALSE 的 IF ==========");
        System.out.println(debugPrint(ifOnly));
        System.out.println("falseFlowNode == null: " + (((IfElseFlowNode) ifOnly).getFalseFlowNode() == null));

        try {
            parse("IF(STEP(CONDITION001)).FALSE(STEP(COMMON004))");
            System.err.println("错误：缺少 TRUE 应抛异常但未抛出");
        } catch (SflException e) {
            System.out.println("========== 缺少 TRUE（预期报错）==========");
            System.out.println(e.getMessage());
        }
    }
}
