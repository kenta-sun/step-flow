package io.github.kentasun.stepflow.jexl.handler;

import io.github.kentasun.stepflow.api.dto.OneOffParams;
import io.github.kentasun.stepflow.api.step.AbstractStepHandler;
import io.github.kentasun.stepflow.api.step.StepHandlerCustomizer;
import io.github.kentasun.stepflow.api.step.dto.StepData;
import io.github.kentasun.stepflow.api.utils.StepFlowUtils;
import io.github.kentasun.stepflow.jexl.JexlInstanceBuilder;
import io.github.kentasun.stepflow.jexl.constants.JexlStepContentType;
import io.github.kentasun.stepflow.jexl.dto.JexlStepHandlerProperties;
import org.apache.commons.jexl3.JexlBuilder;
import org.apache.commons.jexl3.JexlEngine;
import org.apache.commons.jexl3.JexlScript;
import org.apache.commons.jexl3.MapContext;

import java.util.*;

/**
 * JEXL 表达式引擎 步骤处理器
 */
public class JexlStepHandler extends AbstractStepHandler {

    // 表达式引擎
    private final JexlEngine jexl;

    public JexlStepHandler() {
        this(null, null);
    }

    public JexlStepHandler(JexlStepHandlerProperties config) {
        this(config, null);
    }

    public JexlStepHandler(StepHandlerCustomizer<JexlBuilder> customizer) {
        this(null, customizer);
    }

    public JexlStepHandler(JexlStepHandlerProperties config, StepHandlerCustomizer<JexlBuilder> customizer) {
        if (config == null) {
            config = new JexlStepHandlerProperties();
        }
        if (config.getCache() == null) {
            config.setCache(2048);
        }
        this.jexl = JexlInstanceBuilder.buildJexlEngine(config, customizer);
    }

    @Override
    public String getStepContentType() {
        return JexlStepContentType.JEXL;
    }

    @Override
    public List<String> getParamNameList(StepData stepData) {
        // 解析 jexl 脚本
        String expression = stepData.getContent();
        JexlScript script = this.jexl.createScript(expression);
        // 获取参数的路径set
        Set<List<String>> jexlVars = script.getVariables();
        if (StepFlowUtils.isEmpty(jexlVars)) {
            return Collections.emptyList();
        }
        // 根据参数路径set，得到 rootName 的集合
        Set<String> varSet = new HashSet<>();
        for (List<String> varPath : jexlVars) {
            // 将 ["user", "name"] 获取第一个，即是 rootName，作为 paramName
            varSet.add(varPath.get(0));
        }
        return new ArrayList<>(varSet);
    }

    @Override
    public Object doExecute(StepData stepData, OneOffParams oneOffParams) {
        // 表达式
        String expression = stepData.getContent();
        // 执行表达式并返回
        Map<String, Object> vars = oneOffParams.getVars();
        JexlScript script = this.jexl.createScript(expression);
        return script.execute(new MapContext(vars));
    }

    @Override
    public boolean isStepDataIllegal(StepData stepData) {
        return isBlank(stepData.getContent());
    }

    /**
     * <p>Checks if a CharSequence is empty (""), null or whitespace only.</p>
     * <p>Whitespace is defined by {@link Character#isWhitespace(char)}.</p>
     * <pre>
     * StringUtils.isBlank(null)      = true
     * StringUtils.isBlank("")        = true
     * StringUtils.isBlank(" ")       = true
     * StringUtils.isBlank("bob")     = false
     * StringUtils.isBlank("  bob  ") = false
     * </pre>
     *
     * @param cs the CharSequence to check, may be null
     * @return {@code true} if the CharSequence is null, empty or whitespace only
     */
    private static boolean isBlank(final CharSequence cs) {
        final int strLen = length(cs);
        if (strLen == 0) {
            return true;
        }
        for (int i = 0; i < strLen; i++) {
            if (!Character.isWhitespace(cs.charAt(i))) {
                return false;
            }
        }
        return true;
    }

    /**
     * Gets a CharSequence length or {@code 0} if the CharSequence is
     * {@code null}.
     *
     * @param cs a CharSequence or {@code null}
     * @return CharSequence length or {@code 0} if the CharSequence is
     * {@code null}.
     * @since 2.4
     * @since 3.0 Changed signature from length(String) to length(CharSequence)
     */
    public static int length(final CharSequence cs) {
        return cs == null ? 0 : cs.length();
    }
}
