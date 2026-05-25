package io.github.kentasun.stepflow.sfl;

/**
 * SFL 词法分析器：将编排文本切分为 {@link SflToken} 流。
 * <p>
 * 采用单字符前瞻（{@link #lookahead}）实现 {@link #peek()} / {@link #consume()}，
 * 语法层只需关心当前记号而无需管理下标。空白符在记号边界处跳过，不参与 Token 产出，
 * 从而允许在关键字与括号间自由换行，降低存储时的格式化约束。
 * </p>
 */
final class SflLexer {

    private final String text;
    private int pos;
    private SflToken lookahead;

    /**
     * 绑定完整 SFL 源文本并预读第一个记号。
     *
     * @param text 非 null 的编排字符串（空串由 {@link SflParser} 在入口拦截）
     */
    SflLexer(String text) {
        this.text = text;
        this.pos = 0;
        this.lookahead = nextToken();
    }

    /**
     * 查看当前记号但不前进游标，供语法分析做分支判断。
     *
     * @return 下一个待消费的记号
     */
    SflToken peek() {
        return lookahead;
    }

    /**
     * 取出当前记号并将前瞻推进到后继记号。
     *
     * @return 本次消费掉的记号
     */
    SflToken consume() {
        SflToken current = lookahead;
        lookahead = nextToken();
        return current;
    }

    /**
     * 从当前 {@link #pos} 扫描并生成下一个记号。
     * <p>
     * 遇非法字符立即失败，避免将脏数据传入语法层产生误导性「期望 RPAREN」类消息。
     * </p>
     *
     * @return 新记号，输入耗尽时返回 {@link SflTokenType#EOF}
     */
    private SflToken nextToken() {
        skipWhitespace();
        if (pos >= text.length()) {
            return new SflToken(SflTokenType.EOF, "", pos);
        }
        char c = text.charAt(pos);
        int start = pos;
        switch (c) {
            case '(':
                pos++;
                return new SflToken(SflTokenType.LPAREN, start);
            case ')':
                pos++;
                return new SflToken(SflTokenType.RPAREN, start);
            case ',':
                pos++;
                return new SflToken(SflTokenType.COMMA, start);
            case '.':
                pos++;
                return new SflToken(SflTokenType.DOT, start);
            case '=':
                pos++;
                return new SflToken(SflTokenType.EQ, start);
            default:
                if (isIdentStart(c)) {
                    return readIdent(start);
                }
                throw lexError("无法识别的字符: '" + c + "'", start);
        }
    }

    /**
     * 读取标识符：字母或下划线开头，后续允许字母、数字、下划线及点号。
     * <p>
     * 点号纳入标识符体是为了将 {@code dto.num1} 作为单一 {@link SflTokenType#IDENT} 交给
     * 语法层，映射值侧无需再拆路径表达式，与引擎按字符串键解析上下文的行为一致。
     * </p>
     *
     * @param start 标识符在源文本中的起始下标
     * @return IDENT 记号
     */
    private SflToken readIdent(int start) {
        do {
            pos++;
        } while (pos < text.length() && isIdentPart(text.charAt(pos)));
        return new SflToken(SflTokenType.IDENT, text.substring(start, pos), start);
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

    private static SflException lexError(String msg, int position) {
        return new SflException(msg + "，位置: " + position);
    }
}
