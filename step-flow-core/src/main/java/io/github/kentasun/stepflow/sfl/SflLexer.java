package io.github.kentasun.stepflow.sfl;

import io.github.kentasun.stepflow.sfl.constants.SflTokenType;
import io.github.kentasun.stepflow.sfl.constants.SlfKeyWords;

/**
 * SFL 词法分析器：将编排文本切分为 {@link SflToken} 流。
 * <p>
 * 采用单字符前瞻（{@link #nextToken}）实现 {@link #peek()} / {@link #consume()}，
 * 语法层只需关心当前记号而无需管理下标。空白符在记号边界处跳过，不参与 Token 产出，
 * 从而允许在关键字与括号间自由换行，降低存储时的格式化约束。
 * </p>
 * <p>
 * 产出记号的 {@link SflTokenType} 仅区分符号、关键字与用户字面量三类；
 * 具体符号或关键字由 {@link SflToken#getText()} 承载，匹配时须 type 与 text 同时正确。
 * </p>
 */
public class SflLexer {

    private final String text;
    private int pos;
    private SflToken nextToken;

    /**
     * 绑定完整 SFL 源文本并预读第一个记号。
     *
     * @param sflText slf 字符串
     */
    public SflLexer(String sflText) {
        this.text = sflText;
        this.pos = 0;
        this.nextToken = nextToken();
    }

    /**
     * 查看当前记号但不前进游标，供语法分析做分支判断。
     *
     * @return 下一个待消费的记号
     */
    public SflToken peek() {
        return nextToken;
    }

    /**
     * 取出当前记号并将前瞻推进到后继记号。
     *
     * @return 本次消费掉的记号
     */
    public SflToken consume() {
        SflToken current = nextToken;
        nextToken = nextToken();
        return current;
    }

    /**
     * 判断前瞻记号是否同时满足指定的 type 与 text。
     *
     * @param type 期望的语法角色
     * @param text 期望的文本字面量
     * @return {@code true} 表示 {@link #nextToken} 与期望一致
     */
    public boolean nextTokenMatches(SflTokenType type, String text) {
        return nextToken.matches(type, text);
    }

    /**
     * 判断前瞻记号是否为指定符号（{@link SflTokenType#SYMBOL} + 符号文本）。
     *
     * @param symbolText 符号字面量，见 {@link SlfKeyWords} 中的 {@code *_TEXT} 常量
     * @return {@code true} 表示下一个待消费记号为该符号
     */
    public boolean nextTokenIsSymbol(String symbolText) {
        return nextTokenMatches(SflTokenType.SYMBOL, symbolText);
    }

    /**
     * 判断前瞻记号是否为指定关键字（{@link SflTokenType#KEYWORD} + 关键字文本）。
     *
     * @param keywordText 关键字字面量，见 {@link SlfKeyWords}
     * @return {@code true} 表示下一个待消费记号为该关键字
     */
    public boolean nextTokenIsKeyword(String keywordText) {
        return nextTokenMatches(SflTokenType.KEYWORD, keywordText);
    }

    /**
     * 判断前瞻记号是否为用户字面量（{@link SflTokenType#LITERAL}）。
     *
     * @return {@code true} 表示下一个待消费记号为用户自定义内容
     */
    public boolean nextTokenIsLiteral() {
        return nextToken.getType() == SflTokenType.LITERAL;
    }

    /**
     * 消费当前记号，且必须同时匹配 type 与 text，否则抛出 {@link SflException}。
     *
     * @param type 期望的语法角色
     * @param text 期望的文本字面量
     * @return 已消费且校验通过的记号
     */
    public SflToken consumeMatched(SflTokenType type, String text) {
        if (!nextTokenMatches(type, text)) {
            throw unexpectedToken(type, text);
        }
        return consume();
    }

    /**
     * 消费指定符号记号。
     *
     * @param symbolText 符号字面量
     * @return 已消费的符号记号
     */
    public SflToken consumeSymbol(String symbolText) {
        return consumeMatched(SflTokenType.SYMBOL, symbolText);
    }

    /**
     * 消费指定关键字记号。
     *
     * @param keywordText 关键字字面量
     * @return 已消费的关键字记号
     */
    public SflToken consumeKeyword(String keywordText) {
        return consumeMatched(SflTokenType.KEYWORD, keywordText);
    }

    /**
     * 消费用户字面量记号（stepCode、表达式路径等）。
     *
     * @return 已消费的字面量记号
     */
    public SflToken consumeLiteral() {
        if (!nextTokenIsLiteral()) {
            throw unexpectedToken(SflTokenType.LITERAL, null);
        }
        return consume();
    }

    /**
     * 从当前 {@link #pos} 扫描并生成下一个记号。
     * <p>
     * 遇非法字符立即失败，避免将脏数据传入语法层产生误导性「期望 RPAREN」类消息。
     * </p>
     *
     * @return 新记号，输入耗尽时返回 type={@link SflTokenType#SYMBOL}、text 为空串的 EOF 记号
     */
    private SflToken nextToken() {
        skipWhitespace();
        if (pos >= text.length()) {
            return new SflToken(SflTokenType.SYMBOL, SlfKeyWords.EOF_TEXT, pos);
        }
        char c = text.charAt(pos);
        int start = pos;
        switch (c) {
            case SlfKeyWords.LPAREN:
                pos++;
                return symbolToken(SlfKeyWords.LPAREN_TEXT, start);
            case SlfKeyWords.RPAREN:
                pos++;
                return symbolToken(SlfKeyWords.RPAREN_TEXT, start);
            case SlfKeyWords.COMMA:
                pos++;
                return symbolToken(SlfKeyWords.COMMA_TEXT, start);
            case SlfKeyWords.DOT:
                pos++;
                return symbolToken(SlfKeyWords.DOT_TEXT, start);
            case SlfKeyWords.EQ:
                pos++;
                return symbolToken(SlfKeyWords.EQ_TEXT, start);
            default:
                if (isIdentStart(c)) {
                    return readWord(start);
                }
                throw new SflException(String.format("无法识别的字符: '%s'，位置: %s", c, start));
        }
    }

    /**
     * 构造符号类记号。
     */
    private static SflToken symbolToken(String symbolText, int position) {
        return new SflToken(SflTokenType.SYMBOL, symbolText, position);
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
     * @param start 标识符在源文本中的起始下标
     * @return KEYWORD 或 LITERAL 记号
     */
    private SflToken readWord(int start) {
        do {
            pos++;
        } while (pos < text.length() && isIdentPart(text.charAt(pos)));
        String word = text.substring(start, pos);
        SflTokenType type = SlfKeyWords.isKeywordText(word)
                ? SflTokenType.KEYWORD
                : SflTokenType.LITERAL;
        return new SflToken(type, word, start);
    }

    /**
     * 跳过空格、制表符及换行，不生成空白类 Token。
     */
    private void skipWhitespace() {
        while (pos < text.length()) {
            char c = text.charAt(pos);
            if (c == ' ' || c == '\t' || c == '\n' || c == '\r') {
                pos++;
            } else {
                break;
            }
        }
    }

    private static boolean isIdentStart(char c) {
        return Character.isLetter(c) || c == '_';
    }

    private static boolean isIdentPart(char c) {
        return Character.isLetterOrDigit(c) || c == '_' || c == '.';
    }

    private SflException unexpectedToken(SflTokenType expectedType, String expectedText) {
        SflToken actual = nextToken;
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
}
