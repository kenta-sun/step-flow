package io.github.kentasun.stepflow.sfl.token;

import io.github.kentasun.stepflow.sfl.constants.SflTokenType;
import io.github.kentasun.stepflow.sfl.constants.SlfKeyWords;
import io.github.kentasun.stepflow.sfl.exception.SflException;

/**
 * SFL 词法分析器：将编排文本切分为 {@link SflToken} 流。
 * <p>空白符在记号边界处跳过，不参与 Token 产出，从而允许在关键字与括号间自由换行，降低存储时的格式化约束。</p>
 */
public class SflLexer {

    /** sfl脚本全文 */
    private final String text;
    /** 当前游标位置 */
    private int pos;
    /** 当前游标所在行号，从 1 起计 */
    private int line;
    /** 当前游标所在列号（该行第几个字符），从 1 起计 */
    private int column;
    /** 下个 token */
    private SflToken nextToken;

    /**
     * 绑定完整 SFL 源文本并预读第一个记号。
     *
     * @param sflText slf 字符串
     */
    public SflLexer(String sflText) {
        this.text = sflText;
        this.pos = 0;
        this.line = 1;
        this.column = 1;
        this.nextToken = this.getNextToken();
    }

    /**
     * 查看当前记号但不前进游标，供语法分析做分支判断。
     *
     * @return 下一个待消费的记号
     */
    public SflToken peek() {
        return this.nextToken;
    }

    /**
     * 取出当前记号并将前瞻推进到后继记号。
     *
     * @return 本次消费掉的记号
     */
    public SflToken consume() {
        SflToken current = this.nextToken;
        this.nextToken = this.getNextToken();
        return current;
    }

    /**
     * 从当前 {@link #pos} 扫描并生成下一个记号。
     * <p>
     * 遇非法字符立即失败，避免将脏数据传入语法层产生误导性「期望 RPAREN」类消息。
     * </p>
     *
     * @return 新记号，输入耗尽时返回 type={@link SflTokenType#SYMBOL}、text 为空串的 EOF 记号
     */
    private SflToken getNextToken() {
        this.skipWhitespace();
        // 记号起始处的行、列与偏移，供 SflToken 记录及异常定位
        int tokenLine = this.line;
        int tokenColumn = this.column;
        if (this.pos >= this.text.length()) {
            return new SflToken(SflTokenType.SYMBOL, SlfKeyWords.EOF_TEXT, this.pos, tokenLine, tokenColumn);
        }
        char c = this.text.charAt(this.pos);
        int start = this.pos;
        switch (c) {
            case SlfKeyWords.CHAR_LPAREN:
                this.advance();
                return this.symbolToken(SlfKeyWords.LPAREN, start, tokenLine, tokenColumn);
            case SlfKeyWords.CHAR_RPAREN:
                this.advance();
                return this.symbolToken(SlfKeyWords.RPAREN, start, tokenLine, tokenColumn);
            case SlfKeyWords.CHAR_COMMA:
                this.advance();
                return this.symbolToken(SlfKeyWords.COMMA, start, tokenLine, tokenColumn);
            case SlfKeyWords.CHAR_DOT:
                this.advance();
                return this.symbolToken(SlfKeyWords.DOT, start, tokenLine, tokenColumn);
            case SlfKeyWords.CHAR_EQ:
                this.advance();
                return this.symbolToken(SlfKeyWords.EQ, start, tokenLine, tokenColumn);
            case SlfKeyWords.CHAR_DOUBLE_QUOTE:
                return this.readQuotedString(start, tokenLine, tokenColumn);
            default:
                if (isIdentStart(c)) {
                    return this.readWord(start, tokenLine, tokenColumn);
                }
                throw new SflException(String.format(
                        "无法识别的字符: '%s'，位置: %s",
                        c,
                        this.formatLocation(start, tokenLine, tokenColumn)
                ));
        }
    }

    /**
     * 构造符号类记号。
     */
    private SflToken symbolToken(String symbolText, int position, int line, int column) {
        return new SflToken(SflTokenType.SYMBOL, symbolText, position, line, column);
    }

    /**
     * 读取标识符体：字母或下划线开头，后续允许字母、数字、下划线及点号。
     * <p>
     * 点号纳入标识符体是为了将 {@code dto.num1} 作为单一 {@link SflTokenType#LITERAL} 交给
     * 语法层，映射值侧无需再拆路径表达式，与引擎按字符串键解析上下文的行为一致。
     * </p>
     * <p>
     * 若文本命中 {@link SlfKeyWords#isKeywordText(String)} 则产出 {@link SflTokenType#KEYWORD}，
     * 否则产出 {@link SflTokenType#LITERAL}。
     * </p>
     *
     * @param start       标识符在源文本中的起始下标
     * @param tokenLine   标识符起始行号
     * @param tokenColumn 标识符起始列号
     * @return KEYWORD 或 LITERAL 记号
     */
    private SflToken readWord(int start, int tokenLine, int tokenColumn) {
        do {
            this.advance();
        } while (this.pos < this.text.length() && isIdentPart(this.text.charAt(this.pos)));
        String word = this.text.substring(start, this.pos);
        SflTokenType type = SlfKeyWords.isKeywordText(word)
                ? SflTokenType.KEYWORD
                : SflTokenType.LITERAL;
        return new SflToken(type, word, start, tokenLine, tokenColumn);
    }

    /**
     * 读取双引号字符串：{@code "..."}，内部双引号须写作 {@code \"}，其它字符按字面保留。
     * <p>
     * 产出 {@link SflTokenType#QUOTED_STRING}，{@link SflToken#getText()} 为去掉转义后的正文。
     * </p>
     *
     * @param start       起始双引号在源文本中的下标
     * @param tokenLine   起始双引号所在行号
     * @param tokenColumn 起始双引号所在列号
     * @return 字符串记号
     */
    private SflToken readQuotedString(int start, int tokenLine, int tokenColumn) {
        this.advance(); // 跳过起始 "
        StringBuilder sb = new StringBuilder();
        while (this.pos < this.text.length()) {
            char c = this.text.charAt(this.pos);
            if (c == SlfKeyWords.CHAR_BACKSLASH) {
                // 记录反斜杠位置，便于转义错误时给出准确行、列
                int escapeLine = this.line;
                int escapeColumn = this.column;
                int escapeOffset = this.pos;
                this.advance();
                if (this.pos >= this.text.length()) {
                    throw new SflException(String.format(
                            "字符串转义不完整，位置: %s",
                            this.formatLocation(escapeOffset, escapeLine, escapeColumn)
                    ));
                }
                char escaped = this.text.charAt(this.pos);
                if (escaped != SlfKeyWords.CHAR_DOUBLE_QUOTE) {
                    throw new SflException(String.format(
                            "字符串内仅支持转义双引号[\\\"]，实际为[\\%s]，位置: %s",
                            escaped,
                            this.formatLocation(escapeOffset, escapeLine, escapeColumn)
                    ));
                }
                sb.append(SlfKeyWords.CHAR_DOUBLE_QUOTE);
                this.advance();
                continue;
            }
            if (c == SlfKeyWords.CHAR_DOUBLE_QUOTE) {
                this.advance(); // 跳过结束 "
                return new SflToken(SflTokenType.QUOTED_STRING, sb.toString(), start, tokenLine, tokenColumn);
            }
            sb.append(c);
            this.advance();
        }
        throw new SflException(String.format(
                "字符串缺少结束双引号，位置: %s",
                this.formatLocation(start, tokenLine, tokenColumn)
        ));
    }

    /**
     * 跳过空格、制表符及换行，不生成空白类 Token。
     */
    private void skipWhitespace() {
        while (this.pos < this.text.length()) {
            char c = this.text.charAt(this.pos);
            if (c == ' ' || c == '\t' || c == '\n' || c == '\r') {
                this.advance();
            } else {
                break;
            }
        }
    }

    /**
     * 将游标前进一格，并同步更新 {@link #line}、{@link #column}。
     * <p>
     * {@code \r\n} 视为一次换行；单独的 {@code \r} 或 {@code \n} 同样换行。
     * </p>
     */
    private void advance() {
        char c = this.text.charAt(this.pos);
        this.pos++;
        if (c == '\r') {
            this.line++;
            this.column = 1;
            // Windows 换行：吞掉紧随其后的 \n，避免重复换行
            if (this.pos < this.text.length() && this.text.charAt(this.pos) == '\n') {
                this.pos++;
            }
        } else if (c == '\n') {
            this.line++;
            this.column = 1;
        } else {
            this.column++;
        }
    }

    /**
     * 将指定偏移处的行、列与偏移格式化为可读位置描述。
     */
    private String formatLocation(int offset, int line, int column) {
        return String.format("第 %d 行第 %d 列（偏移 %d）", line, column, offset);
    }

    private static boolean isIdentStart(char c) {
        return Character.isLetter(c) || c == '_';
    }

    private static boolean isIdentPart(char c) {
        return Character.isLetterOrDigit(c) || c == '_' || c == '.';
    }
}
