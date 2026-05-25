package io.github.kentasun.stepflow.sfl;

import io.github.kentasun.stepflow.flow.constants.FlowContentType;
import io.github.kentasun.stepflow.flow.dto.node.FlowNode;
import io.github.kentasun.stepflow.flow.dto.node.IfElseFlowNode;
import io.github.kentasun.stepflow.flow.dto.node.ParallelFlowNode;
import io.github.kentasun.stepflow.flow.dto.node.SequenceFlowNode;
import io.github.kentasun.stepflow.flow.dto.node.StepFlowNode;
import io.github.kentasun.stepflow.flow.dto.node.SubFlowNode;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * SFL 语法分析器：基于 {@link SflLexer} 的记号流，用递归下降将编排文本构造成 {@link FlowNode} 树。
 * <p>
 * 每个顶层关键字对应一种 {@link FlowNode} 实现；容器节点（SEQ、PARALLEL）通过反射调用
 * {@code (String type, List<FlowNode>)} 构造器，与 JSON 反序列化路径共用同一领域模型，避免
 * 维护两套节点类型。解析期校验（空列表、尾随逗号、重复映射键、IF 条件必须为 STEP 等）
 * 在构建树之前失败，防止非法结构进入 {@link io.github.kentasun.stepflow.flow.FlowExecutor}。
 * </p>
 */
final class SflSyntaxParser {

    private final SflLexer lexer;

    /**
     * @param lexer 已初始化且至少含一个前瞻记号的词法器
     */
    SflSyntaxParser(SflLexer lexer) {
        this.lexer = lexer;
    }

    /**
     * 解析一条 flow 产生式，入口为标识符关键字。
     * <pre>
     * flow ::= SEQ '(' argList ')'
     *        | PARALLEL '(' argList ')'
     *        | STEP '(' stepCode ')' stepSuffix
     *        | SUB_FLOW '(' flowCode ')'
     *        | IF '(' flow ')' ifSuffix
     * </pre>
     *
     * @return 与关键字对应的流程节点
     * @throws SflException 未知关键字或子规则违反约束时
     */
    FlowNode parseFlow() {
        SflToken ident = expect(SflTokenType.IDENT);
        String keyword = ident.getText();

        switch (keyword) {
            case "SEQ":
                return parseContainer(FlowContentType.SEQUENCE, SequenceFlowNode.class, ident.getPosition());
            case "PARALLEL":
                return parseContainer(FlowContentType.PARALLEL, ParallelFlowNode.class, ident.getPosition());
            case "STEP":
                return parseStep(ident.getPosition());
            case "SUB_FLOW":
                return parseSubFlow(ident.getPosition());
            case "IF":
                return parseIf(ident.getPosition());
            default:
                throw parseError("未知的关键字 [" + keyword + "]", ident.getPosition());
        }
    }

    /**
     * 消费与 {@code type} 匹配的记号，供 {@link SflParser} 在根解析后确认输入无残留。
     *
     * @param type 期望的记号类型，通常为 {@link SflTokenType#EOF}
     * @return 实际消费到的记号
     * @throws SflException 类型不匹配时
     */
    SflToken expect(SflTokenType type) {
        SflToken token = lexer.consume();
        if (token.getType() != type) {
            throw parseError("期望 " + type + "，实际为 " + token.getType()
                    + (token.getText().isEmpty() ? "" : " [" + token.getText() + "]"), token.getPosition());
        }
        return token;
    }

    /**
     * 解析 SEQ / PARALLEL 容器：圆括号内为至少一项子 flow 的逗号列表。
     *
     * @param type       {@link FlowContentType} 中的 SEQUENCE 或 PARALLEL
     * @param clazz      目标节点类
     * @param keywordPos 关键字在源文本中的位置，用于错误报告
     * @param <T>        容器节点类型
     * @return 带子节点列表的容器实例
     */
    private <T extends FlowNode> T parseContainer(String type, Class<T> clazz, int keywordPos) {
        expect(SflTokenType.LPAREN);
        List<FlowNode> children = parseFlowList();
        expect(SflTokenType.RPAREN);
        return newContainerInstance(clazz, type, children);
    }

    /**
     * 解析 {@code STEP(stepCode)[.param(...)][.result(...)]}。
     * <p>
     * param/result 各最多出现一次；空映射列表规范化为 {@code null}，与 JSON 路径下「无映射」语义一致。
     * </p>
     *
     * @param keywordPos 关键字位置（保留供扩展校验）
     * @return 步骤节点
     */
    private StepFlowNode parseStep(int keywordPos) {
        expect(SflTokenType.LPAREN);
        SflToken stepCodeToken = expect(SflTokenType.IDENT);
        expect(SflTokenType.RPAREN);

        Map<String, String> paramNameMap = null;
        Map<String, String> resultNameMap = null;

        while (lexer.peek().getType() == SflTokenType.DOT) {
            lexer.consume();
            SflToken suffix = expect(SflTokenType.IDENT);
            if ("param".equals(suffix.getText())) {
                if (paramNameMap != null) {
                    throw parseError("STEP 不允许重复声明 .param(...)", suffix.getPosition());
                }
                paramNameMap = parseMappingList("param");
            } else if ("result".equals(suffix.getText())) {
                if (resultNameMap != null) {
                    throw parseError("STEP 不允许重复声明 .result(...)", suffix.getPosition());
                }
                resultNameMap = parseMappingList("result");
            } else {
                throw parseError("STEP 后缀未知 [" + suffix.getText() + "]，仅支持 param / result",
                        suffix.getPosition());
            }
        }

        return new StepFlowNode(FlowContentType.STEP, stepCodeToken.getText(), paramNameMap, resultNameMap);
    }

    /**
     * 解析 {@code SUB_FLOW(flowCode)}，子流程编码为单一标识符。
     *
     * @param keywordPos 关键字位置
     * @return 子流程引用节点
     */
    private SubFlowNode parseSubFlow(int keywordPos) {
        expect(SflTokenType.LPAREN);
        SflToken flowCodeToken = expect(SflTokenType.IDENT);
        expect(SflTokenType.RPAREN);
        return new SubFlowNode(FlowContentType.SUB_FLOW, flowCodeToken.getText());
    }

    /**
     * 解析 {@code IF(条件).TRUE(真分支)[.FALSE(假分支)]}。
     * <p>
     * 条件必须为 {@link StepFlowNode}，与执行引擎将条件当作单步求值的设计一致。
     * TRUE 分支 mandatory；FALSE 省略时假分支为 {@code null}，执行器不调度任何假分支逻辑。
     * </p>
     *
     * @param keywordPos IF 关键字位置，用于条件类型错误时的定位
     * @return 条件分支节点
     */
    private IfElseFlowNode parseIf(int keywordPos) {
        expect(SflTokenType.LPAREN);
        FlowNode conditionNode = parseFlow();
        expect(SflTokenType.RPAREN);

        if (!(conditionNode instanceof StepFlowNode)) {
            throw parseError("IF 的条件必须是 STEP(...)，实际为 [" + conditionNode.getType() + "]", keywordPos);
        }
        StepFlowNode condition = (StepFlowNode) conditionNode;

        FlowNode trueFlowNode = parseIfBranch("TRUE");

        FlowNode falseFlowNode = null;
        if (lexer.peek().getType() == SflTokenType.DOT) {
            lexer.consume();
            SflToken falseToken = expect(SflTokenType.IDENT);
            if (!"FALSE".equals(falseToken.getText())) {
                throw parseError("IF 在 .TRUE(...) 之后仅允许 .FALSE(...)，实际为 [." + falseToken.getText() + "]",
                        falseToken.getPosition());
            }
            expect(SflTokenType.LPAREN);
            falseFlowNode = parseFlow();
            expect(SflTokenType.RPAREN);
        }

        return new IfElseFlowNode(FlowContentType.IF_ELSE, condition, trueFlowNode, falseFlowNode);
    }

    /**
     * 解析 IF 的 {@code .TRUE(...)} 或 {@code .FALSE(...)} 后缀块。
     *
     * @param branchName 期望的后缀标识符字面量：{@code TRUE} 或 {@code FALSE}
     * @return 分支内的 flow 子树
     */
    private FlowNode parseIfBranch(String branchName) {
        if (lexer.peek().getType() != SflTokenType.DOT) {
            throw parseError("IF 缺少 ." + branchName + "(...) 分支", lexer.peek().getPosition());
        }
        lexer.consume();
        SflToken branchToken = expect(SflTokenType.IDENT);
        if (!branchName.equals(branchToken.getText())) {
            if ("TRUE".equals(branchName)) {
                throw parseError("IF 必须包含 .TRUE(...)，当前为 [." + branchToken.getText() + "]",
                        branchToken.getPosition());
            }
            throw parseError("IF 期望 ." + branchName + "(...)，实际为 [." + branchToken.getText() + "]",
                    branchToken.getPosition());
        }
        expect(SflTokenType.LPAREN);
        FlowNode branch = parseFlow();
        expect(SflTokenType.RPAREN);
        return branch;
    }

    /**
     * 解析逗号分隔的子 flow 列表，至少包含一项；拒绝 {@code ()} 与尾随逗号。
     *
     * @return 子节点列表，顺序与源文本一致
     */
    private List<FlowNode> parseFlowList() {
        List<FlowNode> list = new ArrayList<>();
        if (lexer.peek().getType() == SflTokenType.RPAREN) {
            throw parseError("参数列表不能为空", lexer.peek().getPosition());
        }
        list.add(parseFlow());
        while (lexer.peek().getType() == SflTokenType.COMMA) {
            lexer.consume();
            if (lexer.peek().getType() == SflTokenType.RPAREN) {
                throw parseError("参数列表末尾不允许有多余逗号", lexer.peek().getPosition());
            }
            list.add(parseFlow());
        }
        return list;
    }

    /**
     * 解析 {@code .param(a=b,c=d)} 或 {@code .result(x=y)} 中的键值映射。
     * <p>
     * 使用 {@link LinkedHashMap} 保持声明顺序，便于日志与调试；重复键在解析期拒绝。
     * </p>
     *
     * @param suffixName 后缀名，仅用于错误消息（param / result）
     * @return 非空映射，或无任何条目时返回 {@code null}
     */
    private Map<String, String> parseMappingList(String suffixName) {
        expect(SflTokenType.LPAREN);
        Map<String, String> map = new LinkedHashMap<>();
        if (lexer.peek().getType() == SflTokenType.RPAREN) {
            expect(SflTokenType.RPAREN);
            return map.isEmpty() ? null : map;
        }
        parseMappingEntry(map, suffixName);
        while (lexer.peek().getType() == SflTokenType.COMMA) {
            lexer.consume();
            if (lexer.peek().getType() == SflTokenType.RPAREN) {
                throw parseError(suffixName + " 映射列表末尾不允许有多余逗号", lexer.peek().getPosition());
            }
            parseMappingEntry(map, suffixName);
        }
        expect(SflTokenType.RPAREN);
        return map.isEmpty() ? null : map;
    }

    /**
     * 解析单条 {@code key=value} 映射项并写入 map。
     *
     * @param map        目标映射，调用方保证非 null
     * @param suffixName 后缀名，用于重复键错误消息
     */
    private void parseMappingEntry(Map<String, String> map, String suffixName) {
        SflToken key = expect(SflTokenType.IDENT);
        expect(SflTokenType.EQ);
        SflToken value = expect(SflTokenType.IDENT);
        if (map.containsKey(key.getText())) {
            throw parseError(suffixName + " 映射键重复: " + key.getText(), key.getPosition());
        }
        map.put(key.getText(), value.getText());
    }

    /**
     * 通过反射构造 SEQ / PARALLEL 节点，复用领域类已有的 {@code (String, List)} 构造器。
     *
     * @param clazz    节点类
     * @param type     流程内容类型常量
     * @param children 子节点列表
     * @param <T>      节点类型
     * @return 新实例
     */
    private <T extends FlowNode> T newContainerInstance(Class<T> clazz, String type, List<FlowNode> children) {
        try {
            Constructor<T> constructor = clazz.getDeclaredConstructor(String.class, List.class);
            constructor.setAccessible(true);
            return constructor.newInstance(type, children);
        } catch (ReflectiveOperationException e) {
            throw new SflException("无法实例化 " + clazz.getSimpleName() + ": " + e.getMessage(), e);
        }
    }

    private static SflException parseError(String msg, int position) {
        return new SflException(msg + "，位置: " + position);
    }
}
