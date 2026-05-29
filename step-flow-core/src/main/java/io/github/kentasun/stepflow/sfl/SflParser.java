package io.github.kentasun.stepflow.sfl;

import io.github.kentasun.stepflow.flow.dto.node.FlowNode;
import io.github.kentasun.stepflow.sfl.constants.SflTokenType;
import io.github.kentasun.stepflow.sfl.constants.SlfKeyWords;
import io.github.kentasun.stepflow.sfl.flowbuilder.*;
import io.github.kentasun.stepflow.sfl.flowbuilder.impl.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * SFL 语法分析器：基于 {@link SflLexer} 的记号流，用递归下降将编排文本构造成 {@link FlowNode} 树。
 * <p>
 * 每个顶层关键字通过 {@link #FLOW_NODE_BUILDERS} 策略注册表分发给对应的 {@link FlowNodeBuilder} 实现，
 * 避免 switch-case 扩张并完全消除反射。新增关键字只需实现 {@link FlowNodeBuilder} 并在注册表中添加一行。
 * </p>
 * <p>
 * 容器节点（SEQ、PARALLEL）分别由 {@link SeqFlowNodeBuilder} / {@link ParallelFlowNodeBuilder} 构建，
 * 直接 new 对象，不再通过反射调用 protected 构造器。
 * </p>
 * <p>
 * 解析期校验（空列表、尾随逗号、重复映射键、IF 条件格式等）在构建树之前失败，
 * 防止非法结构进入 {@link io.github.kentasun.stepflow.flow.FlowExecutor}。
 * </p>
 */
public class SflParser {

    /**
     * 关键字 → 解析策略的不可变注册表。
     * <p>
     * 在类加载时一次性初始化，后续只读，天然线程安全。
     * 扩展时在此添加新条目，无需修改 {@link #keywordToFlow()} 主逻辑。
     * </p>
     */
    private static final Map<String, FlowNodeBuilder> FLOW_NODE_BUILDERS;

    static {
        Map<String, FlowNodeBuilder> map = new HashMap<>();
        map.put(SlfKeyWords.SEQ, new SeqFlowNodeBuilder());
        map.put(SlfKeyWords.PARALLEL, new ParallelFlowNodeBuilder());
        map.put(SlfKeyWords.STEP, new StepFlowNodeBuilder());
        map.put(SlfKeyWords.SUB_FLOW, new SubFlowFlowNodeBuilder());
        map.put(SlfKeyWords.IF, new IfFlowNodeBuilder());
        FLOW_NODE_BUILDERS = Collections.unmodifiableMap(map);
    }

    /**
     * 词法解析器，仅提供 peek / consume
     */
    private final SflLexer lexer;

    /**
     * 构造方法只允许 {@link #parse} 方法调用
     *
     * @param sflText slf 字符串
     */
    private SflParser(String sflText) {
        this.lexer = new SflLexer(sflText);
    }

    /* ---------- 工具方法 ---------- */

    /**
     * 返回当前前瞻 token，不消费。供各 {@link FlowNodeBuilder} 实现判断后续记号。
     *
     * @return 当前前瞻记号
     */
    public SflToken peek() {
        return lexer.peek();
    }

    /**
     * 判断 {@code nextToken} 是否满足指定的 {@link SflTokenType}
     *
     * @param type 期望的语法角色
     * @return {@code true} 表示当前前瞻记号与期望一致
     */
    public boolean nextTokenMatches(SflTokenType type) {
        return nextTokenMatches(type, null);
    }

    /**
     * 判断前瞻记号是否满足指定的 type；若 {@code text} 非 null，则同时校验文本。
     *
     * @param type 期望的语法角色
     * @param text 期望的文本字面量；为 {@code null} 时仅校验 type
     * @return {@code true} 表示当前前瞻记号与期望一致
     */
    public boolean nextTokenMatches(SflTokenType type, String text) {
        SflToken token = lexer.peek();
        if (token.getType() != type) {
            return false;
        }
        return text == null || token.getText().equals(text);
    }

    /**
     * 若前瞻记号匹配 type（及可选 text）则消费并返回 {@code true}，否则不前进游标返回 {@code false}。
     * <p>
     * 供语法层实现 {@code while (tryConsumeToken...)} 或 {@code if (tryConsumeToken...)} 等可选/重复片段。
     * </p>
     *
     * @param type 期望的语法角色
     * @param text 期望的文本字面量；为 {@code null} 时仅校验 type
     * @return 是否已成功消费匹配的记号
     */
    public boolean tryConsumeToken(SflTokenType type, String text) {
        if (!nextTokenMatches(type, text)) {
            return false;
        }
        lexer.consume();
        return true;
    }

    /**
     * 消费当前 token，且必须匹配 {@link SflTokenType}，否则抛出 {@link SflException}。
     *
     * @param type 期望的语法角色
     * @return 已消费且校验通过的记号
     */
    public SflToken consumeMatched(SflTokenType type) {
        return consumeMatched(type, null);
    }

    /**
     * 消费当前记号，且必须匹配 type（及可选 text），否则抛出 {@link SflException}。
     *
     * @param type 期望的语法角色
     * @param text 期望的文本字面量；为 {@code null} 时仅校验 type
     * @return 已消费且校验通过的记号
     */
    public SflToken consumeMatched(SflTokenType type, String text) {
        if (!nextTokenMatches(type, text)) {
            throw unexpectedToken(type, text);
        }
        return lexer.consume();
    }

    /**
     * 构造「期望记号与实际不符」的通用错误，供 {@link #consumeMatched(SflTokenType, String)} 使用。
     */
    private SflException unexpectedToken(SflTokenType expectedType, String expectedText) {
        SflToken actual = lexer.peek();
        String expected = expectedText == null
                ? String.valueOf(expectedType)
                : expectedType + " [" + expectedText + "]";
        return new SflException(String.format(
                "期望 %s，实际为 %s，位置: %s",
                expected,
                actual.getType() + (actual.getText().isEmpty() ? "" : " [" + actual.getText() + "]"),
                actual.getPosition()
        ));
    }

    /* ---------- parse 方法 ---------- */

    /**
     * 将一个顶层 flow 关键字解析成对应的 {@link FlowNode}。
     *
     * @return 与关键字对应的 {@link FlowNode}
     * @throws SflException 未知关键字或子规则违反约束时
     */
    public FlowNode keywordToFlow() {
        SflToken keywordToken = consumeMatched(SflTokenType.KEYWORD);
        String keyword = keywordToken.getText();

        FlowNodeBuilder flowNodeBuilder = FLOW_NODE_BUILDERS.get(keyword);
        if (flowNodeBuilder == null) {
            throw new SflException(String.format(
                    "未知的关键字[%s]，位置: [%s]",
                    keyword,
                    keywordToken.getPosition()
            ));
        }
        return flowNodeBuilder.parse(this, keywordToken.getPosition());
    }

    /**
     * 解析逗号分隔的子 flow 列表，至少包含一项；拒绝空列表与尾随逗号。
     * <p>
     * 供 {@link SeqFlowNodeBuilder}、{@link ParallelFlowNodeBuilder} 及 IF 分支解析共享。
     * </p>
     *
     * @return 子节点列表，顺序与源文本一致，至少含一项
     */
    public List<FlowNode> parseFlowList() {
        List<FlowNode> list = new ArrayList<>();
        if (nextTokenMatches(SflTokenType.SYMBOL, SlfKeyWords.RPAREN)) {
            throw new SflException("参数列表不能为空，位置: " + lexer.peek().getPosition());
        }
        list.add(keywordToFlow());
        while (tryConsumeToken(SflTokenType.SYMBOL, SlfKeyWords.COMMA)) {
            if (nextTokenMatches(SflTokenType.SYMBOL, SlfKeyWords.RPAREN)) {
                throw new SflException("参数列表末尾不允许有多余逗号，位置: " + lexer.peek().getPosition());
            }
            list.add(keywordToFlow());
        }
        return list;
    }

    /* ---------- 入口方法 ---------- */

    /**
     * 【入口方法】将 SFL 文本解析为流程树根节点。
     * <p>
     * 解析成功后额外消费 EOF 记号，确保源字符串尾部无未解析的残留记号，
     * 避免「只解析了前缀、后半段被静默忽略」类隐患。
     * </p>
     *
     * @param sflText 存于 {@code InputFlow.content} 的 SFL 文本，不可为 null 或空白
     * @return 流程节点树，类型为具体 {@link FlowNode} 子类
     * @throws SflException 文本为空、词法非法、语法不符合产生式或语义约束（如 IF 条件格式非法）时
     */
    public static FlowNode parse(String sflText) {
        if (sflText == null || sflText.trim().isEmpty()) {
            throw new SflException("SFL 不能为空");
        }
        SflParser parser = new SflParser(sflText);
        FlowNode root = parser.keywordToFlow();
        parser.consumeMatched(SflTokenType.SYMBOL, SlfKeyWords.EOF_TEXT);
        return root;
    }
}
