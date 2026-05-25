package io.github.kentasun.stepflow.sfl;

/**
 * SFL 词法单元类型枚举。
 * <p>
 * 将源文本中的符号与标识符分为有限集合，供 {@link SflLexer} 产出 {@link SflToken}、
 * 供 {@link SflSyntaxParser} 在递归下降中做期望匹配。{@link #IDENT} 的 {@link #getLiteral()}
 * 为 {@code null}，因其文本来自源文件而非固定字面量。
 * </p>
 */
enum SflTokenType {

    /** 标识符或关键字（SEQ、STEP、dto.num1 等），文本由词法器读取 */
    IDENT(null),

    /** 左圆括号，用于参数列表与后缀块 */
    LPAREN("("),

    /** 右圆括号 */
    RPAREN(")"),

    /** 逗号，分隔同级子流程或映射项 */
    COMMA(","),

    /** 点号，连接 STEP 的 param/result 及 IF 的 TRUE/FALSE 后缀 */
    DOT("."),

    /** 等号，映射中的键值分隔符 */
    EQ("="),

    /** 输入结束标记 */
    EOF("");

    private final String literal;

    SflTokenType(String literal) {
        this.literal = literal;
    }

    /**
     * 返回该类型对应的固定字面量；仅 {@link #IDENT} 返回 {@code null}。
     *
     * @return 单字符或空串字面量，标识符类型为 {@code null}
     */
    String getLiteral() {
        return literal;
    }
}
