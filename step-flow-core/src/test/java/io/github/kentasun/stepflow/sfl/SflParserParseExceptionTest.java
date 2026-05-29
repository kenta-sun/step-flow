package io.github.kentasun.stepflow.sfl;

import io.github.kentasun.stepflow.sfl.exception.SflException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * {@link SflParser#parse(String)} 失败路径契约测试。
 * <p>
 * 用例输入刻意包含换行，以验证词法/语法错误定位中的行号、列号与字符偏移三者一致：
 * 第 2 行及以后，列号与偏移通常不再接近，可区分两种坐标系。
 * </p>
 */
class SflParserParseExceptionTest {

    /**
     * 单条失败用例：非法输入 + 期望的异常类型与消息。
     */
    private static final class ParseFailureCase {

        /** 用例标识，仅用于参数化测试展示名 */
        private final String id;
        /** 传入 {@link SflParser#parse} 的 SFL 文本 */
        private final String sflInput;
        /** 期望抛出的异常类型 */
        private final Class<? extends Throwable> expectedType;
        /** 期望的 {@link Throwable#getMessage()} 全文 */
        private final String expectedMessage;

        ParseFailureCase(
                String id,
                String sflInput,
                Class<? extends Throwable> expectedType,
                String expectedMessage) {
            this.id = id;
            this.sflInput = sflInput;
            this.expectedType = expectedType;
            this.expectedMessage = expectedMessage;
        }

        @Override
        public String toString() {
            return id;
        }
    }

    /** 词法阶段：字符串末尾反斜杠后无字符，触发「转义不完整」 */
    private static final String LEXER_ESCAPE_INCOMPLETE_EOF = "IF(\n  AVIATOR(\"x" + '\\';

    /**
     * 将多行 SFL 片段用换行符连接，便于在 Java 8 下编写多行用例（无需 Text Block）。
     *
     * @param lines 按行排列的源文本片段
     * @return 拼接后的完整 SFL 字符串
     */
    private static String lines(String... lines) {
        return String.join("\n", lines);
    }

    static Stream<ParseFailureCase> parseFailureCases() {
        return Stream.of(
                // ----- parse 入口：空文本 -----
                c("empty_blank", "   ", "SFL 不能为空"),

                // ----- SflLexer：非法字符与双引号字符串 -----
                c("lexer_bad_char", "\n@STEP(a)",
                        "无法识别的字符: '@'，位置: 第 2 行第 1 列（偏移 1）"),
                c("lexer_unclosed_string", lines(
                        "IF(",
                        "AVIATOR(\"unclosed))THEN(STEP(a))ENDIF"),
                        "字符串缺少结束双引号，位置: 第 2 行第 9 列（偏移 12）"),
                c("lexer_escape_incomplete_eof", LEXER_ESCAPE_INCOMPLETE_EOF,
                        "字符串转义不完整，位置: 第 2 行第 13 列（偏移 16）"),
                c("lexer_escape_invalid", lines(
                        "IF(",
                        "AVIATOR(\"x\\a\"))",
                        "THEN(STEP(a))",
                        "ENDIF"),
                        "字符串内仅支持转义双引号[\\\"]，实际为[\\a]，位置: 第 2 行第 11 列（偏移 14）"),
                c("lexer_escape_wrong_char", lines(
                        "IF(",
                        "AVIATOR(\"x\\))",
                        "THEN(STEP(a))",
                        "ENDIF"),
                        "字符串内仅支持转义双引号[\\\"]，实际为[\\)]，位置: 第 2 行第 11 列（偏移 14）"),

                // ----- parse 收尾：根后残留记号 -----
                c("trailing_content", lines(
                        "STEP(a)",
                        "x"),
                        "期望 SYMBOL []，实际为 LITERAL [x]，位置: 第 2 行第 1 列（偏移 8）"),

                // ----- keywordToFlow：根须为 flow 关键字 -----
                c("root_not_keyword", lines(
                        "",
                        "(STEP(a))"),
                        "期望 KEYWORD，实际为 SYMBOL [(]，位置: 第 2 行第 1 列（偏移 1）"),
                c("unknown_keyword", lines(
                        "",
                        "THEN(",
                        "STEP(a))"),
                        "未知的关键字[THEN]，位置: 第 2 行第 1 列（偏移 1）"),

                // ----- parseFlowList：SEQ / PARALLEL 子列表约束 -----
                c("seq_empty_list", lines(
                        "SEQ(",
                        ")"), "参数列表不能为空，位置: 第 2 行第 1 列（偏移 5）"),
                c("seq_trailing_comma", lines(
                        "SEQ(",
                        "  STEP(a),",
                        ")"), "参数列表末尾不允许有多余逗号，位置: 第 3 行第 1 列（偏移 16）"),
                c("parallel_empty_list", lines(
                        "PARALLEL(",
                        ")"), "参数列表不能为空，位置: 第 2 行第 1 列（偏移 10）"),
                c("parallel_trailing_comma", lines(
                        "PARALLEL(",
                        "  STEP(a),",
                        ")"), "参数列表末尾不允许有多余逗号，位置: 第 3 行第 1 列（偏移 21）"),
                c("seq_missing_rparen", lines(
                        "SEQ(",
                        "STEP(a)"),
                        "期望 SYMBOL [)]，实际为 SYMBOL，位置: 第 2 行第 8 列（偏移 12）"),

                // ----- StepFlowNodeBuilder -----
                c("step_no_paren", lines(
                        "  ",
                        "STEP"), "期望 SYMBOL [(]，实际为 SYMBOL，位置: 第 2 行第 5 列（偏移 7）"),
                c("step_missing_rparen", lines(
                        "STEP(",
                        "a"), "期望 SYMBOL [)]，实际为 SYMBOL，位置: 第 2 行第 2 列（偏移 7）"),
                c("step_empty_code", lines(
                        "STEP(",
                        ")"), "期望 LITERAL，实际为 SYMBOL [)]，位置: 第 2 行第 1 列（偏移 6）"),
                c("step_dup_param", lines(
                        "STEP(a)",
                        ".PARAM(x=y)",
                        ".PARAM(z=w)"),
                        "STEP 不允许重复声明 .PARAM(...)，位置: 第 3 行第 2 列（偏移 21）"),
                c("step_dup_result", lines(
                        "STEP(a)",
                        ".result(x=y)",
                        ".result(z=w)"),
                        "STEP 不允许重复声明 .result(...)，位置: 第 3 行第 2 列（偏移 22）"),
                c("step_unknown_suffix", lines(
                        "STEP(a)",
                        ".THEN(x=y)"),
                        "STEP 后缀未知 [THEN]，仅支持 PARAM / result，位置: 第 2 行第 2 列（偏移 9）"),
                c("step_param_trailing_comma", lines(
                        "STEP(a)",
                        ".PARAM(",
                        "  x=y,",
                        ")"),
                        "PARAM 映射列表末尾不允许有多余逗号，位置: 第 4 行第 1 列（偏移 23）"),
                c("step_param_dup_key", lines(
                        "STEP(a)",
                        ".PARAM(x=y,",
                        "       x=z)"), "PARAM 映射键重复: x，位置: 第 3 行第 8 列（偏移 27）"),
                c("step_result_trailing_comma", lines(
                        "STEP(a)",
                        ".result(",
                        "  x=y,",
                        ")"),
                        "result 映射列表末尾不允许有多余逗号，位置: 第 4 行第 1 列（偏移 24）"),
                c("step_result_dup_key", lines(
                        "STEP(a)",
                        ".result(x=y,",
                        "        x=z)"), "result 映射键重复: x，位置: 第 3 行第 9 列（偏移 29）"),

                // ----- SubFlowFlowNodeBuilder -----
                c("subflow_not_literal", lines(
                        "SUB_FLOW(",
                        "STEP)"),
                        "期望 LITERAL，实际为 KEYWORD [STEP]，位置: 第 2 行第 1 列（偏移 10）"),
                c("subflow_empty_code", lines(
                        "SUB_FLOW(",
                        ")"),
                        "期望 LITERAL，实际为 SYMBOL [)]，位置: 第 2 行第 1 列（偏移 10）"),

                // ----- IfFlowNodeBuilder -----
                c("if_missing_endif", lines(
                        "IF(STEP(a))",
                        "THEN(STEP(b))"),
                        "期望 KEYWORD [ENDIF]，实际为 SYMBOL，位置: 第 2 行第 14 列（偏移 25）"),
                c("if_invalid_condition_seq", lines(
                        "",
                        "IF(",
                        "SEQ(STEP(a)))",
                        "THEN(STEP(b))",
                        "ENDIF"),
                        "IF 的条件必须是 STEP(...) 或 TYPE(\"expression\")，位置: 第 2 行第 1 列（偏移 1）"),
                c("if_invalid_condition_reserved_kw", lines(
                        "",
                        "IF(",
                        "THEN)",
                        "THEN(STEP(a))",
                        "ENDIF"),
                        "IF 的条件必须是 STEP(...) 或 TYPE(\"expression\")，位置: 第 2 行第 1 列（偏移 1）"),
                c("if_missing_then", lines(
                        "IF(STEP(a))",
                        "STEP(b)",
                        "ENDIF"),
                        "期望 KEYWORD [THEN]，实际为 KEYWORD [STEP]，位置: 第 2 行第 1 列（偏移 12）"),
                c("if_inline_not_quoted", lines(
                        "IF(",
                        "AVIATOR(expr))",
                        "THEN(STEP(a))",
                        "ENDIF"),
                        "期望 QUOTED_STRING，实际为 LITERAL [expr]，位置: 第 2 行第 9 列（偏移 12）"),
                c("if_missing_lparen_after_kw", lines(
                        "IF",
                        "STEP(a))",
                        "THEN(STEP(b))",
                        "ENDIF"),
                        "期望 SYMBOL [(]，实际为 KEYWORD [STEP]，位置: 第 2 行第 1 列（偏移 3）"),
                c("if_missing_rparen_condition", lines(
                        "IF(STEP(a)",
                        "THEN(STEP(b))",
                        "ENDIF"),
                        "期望 SYMBOL [)]，实际为 KEYWORD [THEN]，位置: 第 2 行第 1 列（偏移 11）"),
                c("if_then_empty_seq", lines(
                        "IF(STEP(a))",
                        "THEN(SEQ())",
                        "ENDIF"), "参数列表不能为空，位置: 第 2 行第 10 列（偏移 21）"),
                c("if_elsif_missing_then", lines(
                        "IF(STEP(a))",
                        "THEN(STEP(b))",
                        "ELSIF(STEP(c))",
                        "ENDIF"),
                        "期望 KEYWORD [THEN]，实际为 KEYWORD [ENDIF]，位置: 第 4 行第 1 列（偏移 41）"),
                // THEN(...) 内仅允许单个子 flow，逗号导致期望右括号时遇到逗号
                c("if_then_unexpected_comma", lines(
                        "IF(STEP(a))",
                        "THEN(STEP(b),)",
                        "ENDIF"),
                        "期望 SYMBOL [)]，实际为 SYMBOL [,]，位置: 第 2 行第 13 列（偏移 24）"),
                // THEN 内嵌 SEQ 时走 parseFlowList，触发列表尾随逗号校验
                c("if_then_seq_trailing_comma", lines(
                        "IF(STEP(a))",
                        "THEN(SEQ(STEP(b),))",
                        "ENDIF"),
                        "参数列表末尾不允许有多余逗号，位置: 第 2 行第 18 列（偏移 29）")
        );
    }

    /** 简化构造：默认期望 {@link SflException} */
    private static ParseFailureCase c(String id, String sflInput, String expectedMessage) {
        return new ParseFailureCase(id, sflInput, SflException.class, expectedMessage);
    }

    /**
     * {@code null} 无法放入参数化 {@link MethodSource} 的输入流，单独断言
     * {@link SflParser#parse(String)} 入口处的空值校验。
     */
    @Test
    void parse_nullInput_shouldThrowEmptyMessage() {
        SflException ex = assertThrows(SflException.class, () -> SflParser.parse(null));
        assertEquals(SflException.class, ex.getClass());
        assertEquals("SFL 不能为空", ex.getMessage());
    }

    /**
     * 对每条非法 SFL 用例：调用 {@link SflParser#parse}，断言异常类型与消息全文
     * 与事先运行采集结果一致（消息中含行/列/偏移的用例，输入串须固定不变）。
     */
    @ParameterizedTest(name = "{0}")
    @MethodSource("parseFailureCases")
    void parse_invalidSfl_shouldThrowDocumentedError(ParseFailureCase failureCase) {
        Throwable thrown = assertThrows(
                failureCase.expectedType,
                () -> SflParser.parse(failureCase.sflInput)
        );
        assertEquals(
                failureCase.expectedMessage,
                thrown.getMessage(),
                () -> "用例 [" + failureCase.id + "] 异常消息与采集时不一致"
        );
    }
}
