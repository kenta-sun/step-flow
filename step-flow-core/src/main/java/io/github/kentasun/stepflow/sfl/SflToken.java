package io.github.kentasun.stepflow.sfl;

/**
 * 词法分析产出的不可变记号。
 * <p>
 * 除类型外携带 {@link #getText()} 与 {@link #getPosition()}：位置为记号在原始 SFL 串中的
 * 起始下标，用于构造带偏移的 {@link SflException}，避免仅报「语法错误」却无法对照库表字段。
 * </p>
 */
final class SflToken {

    private final SflTokenType type;
    private final String text;
    private final int position;

    /**
     * 由固定字面量构造的标点类记号（括号、逗号等）。
     *
     * @param type     非 {@link SflTokenType#IDENT} 的类型
     * @param position 源文本起始偏移
     * @throws SflException 若对 {@link SflTokenType#IDENT} 误用本构造器（字面量为 null）
     */
    SflToken(SflTokenType type, int position) {
        this.type = type;
        if (type.getLiteral() == null) {
            throw new SflException(String.format("TokenType[%s] 无固定字面量，应使用文本构造器", type));
        }
        this.text = type.getLiteral();
        this.position = position;
    }

    /**
     * 构造标识符类记号，文本来自源文件切片。
     *
     * @param type     应为 {@link SflTokenType#IDENT}
     * @param text     标识符或路径片段，非 null
     * @param position 源文本起始偏移
     */
    SflToken(SflTokenType type, String text, int position) {
        this.type = type;
        this.text = text;
        this.position = position;
    }

    SflTokenType getType() {
        return type;
    }

    String getText() {
        return text;
    }

    int getPosition() {
        return position;
    }
}
