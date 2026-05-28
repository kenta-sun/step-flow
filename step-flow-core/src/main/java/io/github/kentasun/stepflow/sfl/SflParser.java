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
 * 解析期校验（空列表、尾随逗号、重复映射键、IF 条件必须为 STEP 等）在构建树之前失败，
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

    /** 词法解析器 */
    private final SflLexer lexer;

    /**
     * @param sflText slf 字符串
     */
    public SflParser(String sflText) {
        this.lexer = new SflLexer(sflText);
    }

    /**
     * 将一个顶层 flow 关键字解析成对应的 {@link FlowNode}。
     *
     * @return 与关键字对应的 {@link FlowNode}
     * @throws SflException 未知关键字或子规则违反约束时
     */
    public FlowNode keywordToFlow() {
        SflToken keywordToken = consumeKeyword();
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
     * 消费并校验下一个记号同时满足 type 与 text。
     *
     * @param type 期望的语法角色
     * @param text 期望的文本字面量
     * @return 实际消费到的记号
     */
    public SflToken consumeToken(SflTokenType type, String text) {
        return lexer.consumeMatched(type, text);
    }

    /**
     * 消费指定符号记号。
     *
     * @param symbolText 符号字面量，见 {@link SlfKeyWords} 中的 {@code *_TEXT} 常量
     * @return 已消费的符号记号
     */
    public SflToken consumeSymbol(String symbolText) {
        return lexer.consumeSymbol(symbolText);
    }

    /**
     * 消费顶层或子规则中的 flow 关键字（SEQ、STEP 等）。
     * <p>
     * 仅校验 type 为 {@link SflTokenType#KEYWORD}，不校验具体文本；
     * 未知关键字的语义错误由 {@link #keywordToFlow()} 在查表后抛出。
     * </p>
     *
     * @return 已消费的关键字记号
     */
    public SflToken consumeKeyword() {
        SflToken token = lexer.consume();
        if (token.getType() != SflTokenType.KEYWORD) {
            throw new SflException("期望 KEYWORD，实际为 " + token.getType()
                    + (token.getText().isEmpty() ? "" : " [" + token.getText() + "]")
                    + "，位置: " + token.getPosition());
        }
        return token;
    }

    /**
     * 消费指定文本的关键字记号。
     *
     * @param keywordText 关键字字面量
     * @return 已消费的关键字记号
     */
    public SflToken consumeKeyword(String keywordText) {
        return lexer.consumeKeyword(keywordText);
    }

    /**
     * 消费用户字面量（stepCode、flowCode、表达式路径等）。
     *
     * @return 已消费的字面量记号
     */
    public SflToken consumeLiteral() {
        return lexer.consumeLiteral();
    }

    /**
     * 返回当前前瞻 token，不消费。供各 {@link FlowNodeBuilder} 实现判断后续记号。
     *
     * @return 当前前瞻记号
     */
    public SflToken peek() {
        return lexer.peek();
    }

    /**
     * 判断前瞻记号是否同时满足 type 与 text。
     *
     * @param type 期望的语法角色
     * @param text 期望的文本字面量
     * @return {@code true} 表示当前前瞻记号与期望一致
     */
    public boolean nextTokenMatches(SflTokenType type, String text) {
        return lexer.nextTokenMatches(type, text);
    }

    /**
     * 判断前瞻记号是否为指定符号。
     *
     * @param symbolText 符号字面量
     * @return {@code true} 表示下一个待消费记号为该符号
     */
    public boolean nextTokenIsSymbol(String symbolText) {
        return lexer.nextTokenIsSymbol(symbolText);
    }

    /**
     * 判断前瞻记号是否为指定关键字。
     *
     * @param keywordText 关键字字面量
     * @return {@code true} 表示下一个待消费记号为该关键字
     */
    public boolean nextTokenIsKeyword(String keywordText) {
        return lexer.nextTokenIsKeyword(keywordText);
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
        if (nextTokenIsSymbol(SlfKeyWords.RPAREN_TEXT)) {
            throw new SflException("参数列表不能为空，位置: " + lexer.peek().getPosition());
        }
        list.add(keywordToFlow());
        while (nextTokenIsSymbol(SlfKeyWords.COMMA_TEXT)) {
            consumeSymbol(SlfKeyWords.COMMA_TEXT);
            if (nextTokenIsSymbol(SlfKeyWords.RPAREN_TEXT)) {
                throw new SflException("参数列表末尾不允许有多余逗号，位置: " + lexer.peek().getPosition());
            }
            list.add(keywordToFlow());
        }
        return list;
    }

    /**
     * 将 SFL 文本解析为流程树根节点。
     * <p>
     * 解析成功后额外消费 EOF 记号，确保源字符串尾部无未解析的残留记号，
     * 避免「只解析了前缀、后半段被静默忽略」类隐患。
     * </p>
     *
     * @param sflText 存于 {@code InputFlow.content} 的 SFL 文本，不可为 null 或空白
     * @return 流程节点树，类型为具体 {@link FlowNode} 子类
     * @throws SflException 文本为空、词法非法、语法不符合产生式或语义约束（如 IF 条件非 STEP）时
     */
    public static FlowNode parse(String sflText) {
        if (sflText == null || sflText.trim().isEmpty()) {
            throw new SflException("SFL 不能为空");
        }
        SflParser parser = new SflParser(sflText);
        FlowNode root = parser.keywordToFlow();
        parser.consumeSymbol(SlfKeyWords.EOF_TEXT);
        return root;
    }
}
