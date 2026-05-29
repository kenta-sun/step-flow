package io.github.kentasun.stepflow.sfl.constants;

import io.github.kentasun.stepflow.sfl.token.SflLexer;
import io.github.kentasun.stepflow.sfl.SflParser;
import io.github.kentasun.stepflow.sfl.flowbuilder.FlowNodeBuilder;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * SFL（Step Flow Language）语法关键字与符号字面量常量集。
 * <p>
 * 取值与源文本中的标识符或符号完全一致，供 {@link SflLexer}、{@link SflParser}
 * 注册表及各 {@link FlowNodeBuilder} 实现统一引用，避免魔法字符串分散在解析逻辑中。
 * </p>
 */
public final class SlfKeyWords {

    private SlfKeyWords() {
        // 工具类，禁止实例化
    }

    // -------------------------------------------------------------------------
    // 符号（char 供词法器单字符扫描，String 供 type+text 匹配）
    // -------------------------------------------------------------------------

    public static final char CHAR_LPAREN = '(';
    public static final String LPAREN = String.valueOf(CHAR_LPAREN);

    public static final char CHAR_RPAREN = ')';
    public static final String RPAREN = String.valueOf(CHAR_RPAREN);

    public static final char CHAR_COMMA = ',';
    public static final String COMMA = String.valueOf(CHAR_COMMA);

    public static final char CHAR_DOT = '.';
    public static final String DOT = String.valueOf(CHAR_DOT);

    public static final char CHAR_EQ = '=';
    public static final String EQ = String.valueOf(CHAR_EQ);

    /** 输入结束标记的文本 */
    public static final String EOF_TEXT = "";

    /** 双引号字符，用于词法器扫描内联表达式字符串 */
    public static final char CHAR_DOUBLE_QUOTE = '"';

    /** 转义符，双引号字符串内仅用于转义 {@link #CHAR_DOUBLE_QUOTE} */
    public static final char CHAR_BACKSLASH = '\\';

    // -------------------------------------------------------------------------
    // 顶层编排关键字（flow 产生式入口）
    // -------------------------------------------------------------------------

    /** 顺序执行容器：{@code SEQ(child, child, ...)} */
    public static final String SEQ = "SEQ";

    /** 并行执行容器：{@code PARALLEL(child, child, ...)} */
    public static final String PARALLEL = "PARALLEL";

    /** 单步执行：{@code STEP(stepCode)[.PARAM(...)][.result(...)]} */
    public static final String STEP = "STEP";

    /** 子流程引用：{@code SUB_FLOW(flowCode)} */
    public static final String SUB_FLOW = "SUB_FLOW";

    /**
     * 条件分支块入口：{@code IF(条件) THEN(...) [ELSIF(条件) THEN(...)]* [ELSE(...)] ENDIF}
     */
    public static final String IF = "IF";

    // -------------------------------------------------------------------------
    // IF 块内关键字（THEN / ELSIF / ELSE / ENDIF）
    // -------------------------------------------------------------------------

    /** IF 块真分支：{@code THEN(...)}，在 IF 或每个 ELSIF 之后必填 */
    public static final String THEN = "THEN";

    /** IF 块 elsif：{@code ELSIF(条件)}，可重复 */
    public static final String ELSIF = "ELSIF";

    /** IF 块默认分支：{@code ELSE(...)}，可省略 */
    public static final String ELSE = "ELSE";

    /** IF 块结束：{@code ENDIF}，必填 */
    public static final String ENDIF = "ENDIF";

    // -------------------------------------------------------------------------
    // STEP 后缀标识符
    // -------------------------------------------------------------------------

    /** STEP 入参映射后缀：{@code .PARAM(k=v,...)} */
    public static final String PARAM = "PARAM";

    /** STEP 出参映射后缀：{@code .result(k=v,...)} */
    public static final String RESULT = "result";

    // -------------------------------------------------------------------------
    // 关键字集合（供词法器区分 KEYWORD 与 LITERAL）
    // -------------------------------------------------------------------------

    private static final Set<String> KEYWORD_TEXTS;

    static {
        Set<String> set = new HashSet<>();
        set.add(SEQ);
        set.add(PARALLEL);
        set.add(STEP);
        set.add(SUB_FLOW);
        set.add(IF);
        set.add(THEN);
        set.add(ELSIF);
        set.add(ELSE);
        set.add(ENDIF);
        set.add(PARAM);
        set.add(RESULT);
        KEYWORD_TEXTS = Collections.unmodifiableSet(set);
    }

    /**
     * 判断给定文本是否为 SFL 保留关键字。
     *
     * @param text 词法器读出的标识符文本
     * @return {@code true} 表示应产出 {@link SflTokenType#KEYWORD}
     */
    public static boolean isKeywordText(String text) {
        return KEYWORD_TEXTS.contains(text);
    }
}
