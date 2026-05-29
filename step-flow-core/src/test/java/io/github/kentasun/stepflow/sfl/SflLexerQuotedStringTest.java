package io.github.kentasun.stepflow.sfl;

import io.github.kentasun.stepflow.sfl.constants.SflTokenType;
import io.github.kentasun.stepflow.sfl.exception.SflException;
import io.github.kentasun.stepflow.sfl.token.SflLexer;
import io.github.kentasun.stepflow.sfl.token.SflToken;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * {@link SflLexer} 双引号字符串及 {@code \"} 转义行为测试。
 */
class SflLexerQuotedStringTest {

    @Test
    void shouldParsePlainQuotedString() {
        SflLexer lexer = new SflLexer("\"a > b\"");
        SflToken token = lexer.consume();
        Assertions.assertEquals(SflTokenType.QUOTED_STRING, token.getType());
        Assertions.assertEquals("a > b", token.getText());
    }

    @Test
    void shouldUnescapeDoubleQuoteInsideString() {
        SflLexer lexer = new SflLexer("\"say \\\"hi\\\"\"");
        SflToken token = lexer.consume();
        Assertions.assertEquals("say \"hi\"", token.getText());
    }

    @Test
    void shouldRejectInvalidEscape() {
        SflException ex = Assertions.assertThrows(
                SflException.class,
                () -> new SflLexer("\"a\\nb\"").consume());
        Assertions.assertTrue(ex.getMessage().contains("仅支持转义双引号"));
    }
}
