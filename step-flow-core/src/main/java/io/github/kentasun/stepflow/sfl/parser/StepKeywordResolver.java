package io.github.kentasun.stepflow.sfl.parser;

import io.github.kentasun.stepflow.flow.constants.FlowContentType;
import io.github.kentasun.stepflow.flow.dto.node.FlowNode;
import io.github.kentasun.stepflow.flow.dto.node.StepFlowNode;
import io.github.kentasun.stepflow.sfl.SflException;
import io.github.kentasun.stepflow.sfl.SflSyntaxParser;
import io.github.kentasun.stepflow.sfl.constants.SlfKeyWords;
import io.github.kentasun.stepflow.sfl.SflToken;
import io.github.kentasun.stepflow.sfl.SflTokenType;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * STEP 关键字解析策略：解析 {@code STEP(stepCode)[.param(k=v,...)][.result(k=v,...)]}
 * 并构造 {@link StepFlowNode}。
 * <p>
 * param / result 后缀各最多出现一次；空映射列表规范化为 {@code null}，与 JSON 路径下
 * "无映射" 语义一致，避免下游需要区分空 map 与 null 的双重判断。
 * 重复声明（如两次 .param）在解析期即失败。
 * </p>
 */
public class StepKeywordResolver implements KeywordResolver {

    @Override
    public FlowNode parse(SflSyntaxParser parser, int keywordPos) {
        parser.expect(SflTokenType.LPAREN);
        SflToken stepCodeToken = parser.expect(SflTokenType.IDENT);
        parser.expect(SflTokenType.RPAREN);

        Map<String, String> paramNameMap = null;
        Map<String, String> resultNameMap = null;

        // 循环消费可选的 .param(...) / .result(...) 后缀
        while (parser.peek().getType() == SflTokenType.DOT) {
            parser.consume(); // 消费 '.'
            SflToken suffix = parser.expect(SflTokenType.IDENT);
            switch (suffix.getText()) {
                case SlfKeyWords.STEP_PARAM:
                    if (paramNameMap != null) {
                        throw new SflException(
                                SlfKeyWords.STEP + " 不允许重复声明 ." + SlfKeyWords.STEP_PARAM
                                        + "(...)，位置: " + suffix.getPosition());
                    }
                    paramNameMap = parseMappingList(parser, SlfKeyWords.STEP_PARAM);
                    break;
                case SlfKeyWords.STEP_RESULT:
                    if (resultNameMap != null) {
                        throw new SflException(
                                SlfKeyWords.STEP + " 不允许重复声明 ." + SlfKeyWords.STEP_RESULT
                                        + "(...)，位置: " + suffix.getPosition());
                    }
                    resultNameMap = parseMappingList(parser, SlfKeyWords.STEP_RESULT);
                    break;
                default:
                    throw new SflException(
                            SlfKeyWords.STEP + " 后缀未知 [" + suffix.getText() + "]，仅支持 "
                                    + SlfKeyWords.STEP_PARAM + " / " + SlfKeyWords.STEP_RESULT
                                    + "，位置: " + suffix.getPosition());
            }
        }

        return new StepFlowNode(FlowContentType.STEP, stepCodeToken.getText(), paramNameMap, resultNameMap);
    }

    /**
     * 解析 {@code .param(a=b,c=d)} 或 {@code .result(x=y)} 括号内的键值映射列表。
     * <p>
     * 使用 {@link LinkedHashMap} 保持声明顺序；重复键在解析期拒绝；空括号返回 {@code null}。
     * </p>
     *
     * @param parser     当前语法分析器
     * @param suffixName 后缀名，仅用于错误消息（param / result）
     * @return 非空映射，或括号内无任何条目时返回 {@code null}
     */
    private Map<String, String> parseMappingList(SflSyntaxParser parser, String suffixName) {
        parser.expect(SflTokenType.LPAREN);
        Map<String, String> map = new LinkedHashMap<>();

        // 空括号 → 直接返回 null
        if (parser.peek().getType() == SflTokenType.RPAREN) {
            parser.expect(SflTokenType.RPAREN);
            return null;
        }

        parseMappingEntry(parser, map, suffixName);
        while (parser.peek().getType() == SflTokenType.COMMA) {
            parser.consume(); // 消费 ','
            if (parser.peek().getType() == SflTokenType.RPAREN) {
                throw new SflException(
                        suffixName + " 映射列表末尾不允许有多余逗号，位置: " + parser.peek().getPosition());
            }
            parseMappingEntry(parser, map, suffixName);
        }

        parser.expect(SflTokenType.RPAREN);
        return map.isEmpty() ? null : map;
    }

    /**
     * 解析单条 {@code key=value} 映射项并写入 map；重复键立即报错。
     *
     * @param parser     当前语法分析器
     * @param map        目标映射
     * @param suffixName 后缀名，用于重复键错误消息
     */
    private void parseMappingEntry(SflSyntaxParser parser, Map<String, String> map, String suffixName) {
        SflToken key = parser.expect(SflTokenType.IDENT);
        parser.expect(SflTokenType.EQ);
        SflToken value = parser.expect(SflTokenType.IDENT);
        if (map.containsKey(key.getText())) {
            throw new SflException(
                    suffixName + " 映射键重复: " + key.getText() + "，位置: " + key.getPosition());
        }
        map.put(key.getText(), value.getText());
    }
}
