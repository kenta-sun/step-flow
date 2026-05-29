package io.github.kentasun.stepflow.sfl.token;

import io.github.kentasun.stepflow.sfl.constants.SflTokenType;
import io.github.kentasun.stepflow.sfl.exception.SflException;

/**
 * 词法分析产出的不可变 token。
 * <p>
 * {@link #getType()} 仅表示语法角色（符号 / 关键字 / 用户字面量），
 * 具体符号或关键字字面量由 {@link #getText()} 承载。
 * 判断是否为某个特定符号或关键字，须同时校验 type 与 text，
 * 见 {@link #matches(SflTokenType, String)}。
 * </p>
 * <p>
 * {@link #getPosition()} 为记号在原始 SFL 串中的起始下标，
 * 用于构造带偏移的 {@link SflException}。
 * </p>
 * <p>
 * {@link #getLine()} / {@link #getColumn()} 为记号起始处的行号与列号（均从 1 起计），
 * 便于在异常消息中给出可读位置；可用 {@link #formatLocation()} 一次性格式化为完整描述。
 * </p>
 */
public class SflToken {

    /**
     * token 类型
     */
    private final SflTokenType type;
    /**
     * token 内容
     */
    private final String text;
    /**
     * token 位置
     */
    private final int position;
    /**
     * 行号，从 1 起计
     */
    private final int line;
    /**
     * 列号（该行第几个字符），从 1 起计
     */
    private final int column;

    /**
     * 构造词法记号。
     *
     * @param type     语法角色，不可为 null
     * @param text     记号文本，不可为 null（EOF 使用空串）
     * @param position 源文本起始偏移
     * @param line     记号起始行号（从 1 起计）
     * @param column   记号起始列号（从 1 起计）
     */
    SflToken(SflTokenType type, String text, int position, int line, int column) {
        this.type = type;
        this.text = text;
        this.position = position;
        this.line = line;
        this.column = column;
    }

    public SflTokenType getType() {
        return type;
    }

    public String getText() {
        return text;
    }

    public int getPosition() {
        return position;
    }

    /**
     * @return 记号在源文本中的起始行号，从 1 起计
     */
    public int getLine() {
        return line;
    }

    /**
     * @return 记号在起始行中的列号（第几个字符），从 1 起计
     */
    public int getColumn() {
        return column;
    }

    /**
     * 将行号、列号与字符偏移格式化为统一的位置描述，供 {@link SflException} 消息拼接。
     *
     * @return 形如「第 2 行第 5 列（偏移 12）」的可读字符串
     */
    public String formatLocation() {
        return String.format("第 %d 行第 %d 列（偏移 %d）", this.line, this.column, this.position);
    }

    /**
     * 同时校验 type 与 text 是否与期望一致。
     * <p>
     * 符号、关键字等固定字面量的匹配均应通过本方法（或基于它的便捷方法）完成，
     * 避免仅用 type 或仅用 text 做片面判断。
     * </p>
     *
     * @param expectedType 期望的语法角色
     * @param expectedText 期望的文本字面量
     * @return {@code true} 表示 type 与 text 均匹配
     */
    public boolean matches(SflTokenType expectedType, String expectedText) {
        return this.type == expectedType && this.text.equals(expectedText);
    }

    /**
     * 是否是 符号
     * @return {@code true} 表示当前记号为 {@link SflTokenType#SYMBOL} 且 text 等于 {@code symbolText}
     */
    public boolean isSymbol(String symbolText) {
        return this.matches(SflTokenType.SYMBOL, symbolText);
    }

    /**
     * 是否是 关键字
     * @return {@code true} 表示当前记号为 {@link SflTokenType#KEYWORD} 且 text 等于 {@code keywordText}
     */
    public boolean isKeyword(String keywordText) {
        return this.matches(SflTokenType.KEYWORD, keywordText);
    }

    /**
     * 是否是 自定义内容
     * @return {@code true} 表示当前记号为 {@link SflTokenType#LITERAL}（不校验具体文本）
     */
    public boolean isLiteral() {
        return this.type == SflTokenType.LITERAL;
    }
}
