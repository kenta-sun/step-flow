package io.github.kentasun.stepflow.api.utils;

import java.util.*;

/**
 * @author kenta-sun
 */
public final class StepVarsUtils {

    private StepVarsUtils() {
    }

    /**
     * <p>根据 key 列表 + context + 映射，组装 vars。</p>
     * <p>注意，{@code paramNameList} 中的参数，格式不能是 {@code a.b}。对于 {@code a.b} 的情况做如下处理：</p>
     * <p>如果paramNameMap中存在映射，则会直接将 {@code a.b} 作为key放入 {@code vars}</p>
     * <p>如果paramNameMap中不存在映射，则会直接将 {@code a} 放入 {@code vars}</p>
     *
     * @param paramNameList 改方法需要返回的参数的key，如 ["a", "b"]。
     * @param contextMap   全局上下文
     * @param paramNameMap 逻辑名 -> context真实key，如 a -> dto.num1
     */
    public static Map<String, Object> buildVars(List<String> paramNameList,
                                                Map<String, Object> contextMap,
                                                Map<String, String> paramNameMap) {
        if (StepFlowUtils.isEmpty(paramNameList)) {
            return new HashMap<>();
        }
        Map<String, Object> vars = new HashMap<>();
        if (StepFlowUtils.isNotEmpty(paramNameList)) {
            for (String paramName : paramNameList) {
                Object value;
                // 获取映射名
                String mappingName = getTempName(paramName, paramNameMap);
                if (StepFlowUtils.isNotBlank(mappingName)) {
                    // 不为空说明需要映射，必须取到 tempName 代表的值本身。比如 a.b，就必须取到 b 的值
                    value = GetValueFromMapUtils.getValueFromContextMap(mappingName, contextMap);
                } else { // 不需要映射，取 root 值。比如 a.b，取 a
                    // 获取 rootName
                    String rootName = extractRootName(paramName);
                    value = contextMap.get(rootName);
                }
                if (value != null) {
                    vars.put(paramName, value);
                }
            }
        }

        return vars;
    }

    private static String getTempName(String paramName, Map<String, String> map) {
        if (StepFlowUtils.isNotEmpty(map)) {
            return map.get(paramName);
        } else {
            return null;
        }
    }

    /**
     * 从 {@code a.b.c} {@code a[0].b} {@code a(0).b} 里取 root 名 a，示意用
     * TODO 将单测转移到该方法上面，删除原来的工具方法。
     */
    private static String extractRootName(String key) {
        int end = -1;
        end = minIndexOf(key, '.', end);
        end = minIndexOf(key, '[', end);
        end = minIndexOf(key, '(', end);
        return end < 0 ? key : key.substring(0, end);
    }

    private static int minIndexOf(String str, char aChar, int lastIndex) {
        int i = str.indexOf(aChar);
        if (i < 0) { // -1，说明没找到，直接返回旧的 index
            return lastIndex;
        } else {
            // i 更小，返回 i；相等，或者 lastIndex 更小，返回 lastIndex
            return Math.min(i, lastIndex);
        }
    }
}