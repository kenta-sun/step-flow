package io.github.kentasun.stepflow.sfl;

import io.github.kentasun.stepflow.flow.dto.node.FlowNode;
import io.github.kentasun.stepflow.sfl.constants.SlfKeyWords;
import io.github.kentasun.stepflow.sfl.flowbuilder.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * SFL 语法分析器：基于 {@link SflLexer} 的记号流，用递归下降将编排文本构造成 {@link FlowNode} 树。
 * <p>
 * 每个顶层关键字通过 {@link #KEYWORD_PARSERS} 策略注册表分发给对应的 {@link FlowNodeBuilder} 实现，
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
    private static final Map<String, FlowNodeBuilder> KEYWORD_PARSERS;

    static {
        Map<String, FlowNodeBuilder> map = new HashMap<>();
        map.put(SlfKeyWords.SEQ, new SeqFlowNodeBuilder());
        map.put(SlfKeyWords.PARALLEL, new ParallelFlowNodeBuilder());
        map.put(SlfKeyWords.STEP, new StepFlowNodeBuilder());
        map.put(SlfKeyWords.SUB_FLOW, new SubFlowFlowNodeBuilder());
        map.put(SlfKeyWords.IF, new IfFlowNodeBuilder());
        KEYWORD_PARSERS = Collections.unmodifiableMap(map);
    }

    /** 词法解析器 */
    private final SflLexer lexer;

    /**
     * @param sflText slf字符串
     */
    public SflParser(String sflText) {
        this.lexer = new SflLexer(sflText);
    }

    /**
     * 将一个关键字解析成对应的 {@link FlowNode}
     *
     * @return 与关键字对应的 {@link FlowNode}
     * @throws SflException 未知关键字或子规则违反约束时
     */
    public FlowNode keywordToFlow() {
        SflToken ident = this.consumeTokenByType(SflTokenType.IDENT);
        String keyword = ident.getText();

        FlowNodeBuilder flowNodeBuilder = KEYWORD_PARSERS.get(keyword);
        if (flowNodeBuilder == null) {
            throw new SflException("未知的关键字 [" + keyword + "]，位置: " + ident.getPosition());
        }
        return flowNodeBuilder.parse(this, ident.getPosition());
    }

    /**
     * 消费并校验下一个 token 的类型。
     * <p>
     * 同时供各 {@link FlowNodeBuilder} 实现调用；包内可见（package-private）。
     * </p>
     *
     * @param type 期望的记号类型，通常为 {@link SflTokenType#EOF}
     * @return 实际消费到的记号
     * @throws SflException 类型不匹配时
     */
    public SflToken consumeTokenByType(SflTokenType type) {
        SflToken token = lexer.consume();
        if (token.getType() != type) {
            throw new SflException("期望 " + type + "，实际为 " + token.getType()
                    + (token.getText().isEmpty() ? "" : " [" + token.getText() + "]")
                    + "，位置: " + token.getPosition());
        }
        return token;
    }

    /**
     * 返回当前前瞻 token，不消费。供各 {@link FlowNodeBuilder} 实现判断后续记号类型。
     *
     * @return 当前前瞻记号
     */
    public SflToken peek() {
        return lexer.peek();
    }

    /**
     * 消费并返回当前 token，不做类型校验。
     * 用于 {@link FlowNodeBuilder} 实现在已知下一个 token 类型时直接跳过（如 '.' 分隔符）。
     *
     * @return 被消费的记号
     */
    public SflToken consume() {
        return lexer.consume();
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
        if (lexer.peek().getType() == SflTokenType.RPAREN) {
            throw new SflException("参数列表不能为空，位置: " + lexer.peek().getPosition());
        }
        list.add(keywordToFlow());
        while (lexer.peek().getType() == SflTokenType.COMMA) {
            lexer.consume(); // 消费 ','
            if (lexer.peek().getType() == SflTokenType.RPAREN) {
                throw new SflException("参数列表末尾不允许有多余逗号，位置: " + lexer.peek().getPosition());
            }
            list.add(keywordToFlow());
        }
        return list;
    }

    /**
     * 将 SFL 文本解析为流程树根节点。
     * <p>
     * 解析成功后额外消费 {@link SflTokenType#EOF}，确保源字符串尾部无未解析的残留记号，
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
        parser.consumeTokenByType(SflTokenType.EOF);
        return root;
    }
}
