package io.github.kentasun.stepflow.api.step;

import io.github.kentasun.stepflow.api.dto.OneOffParams;
import io.github.kentasun.stepflow.api.exception.StepFlowException;
import io.github.kentasun.stepflow.api.step.dto.StepData;
import io.github.kentasun.stepflow.api.utils.TypeReference;

import java.util.*;

/**
 * Java 步骤使用的 Java 方法
 * TODO 想把获取 paramNameList 测操作拿出去，但是 isInit 的安全问题如何解决？
 */
public abstract class AbstractJavaStep {

    private final List<String> paramNameList;

    /**
     * 是否是初始化状态。
     * <p>用于关闭 {@link #getAs} {@link #getAsByType} 的异常抛出，保证正常获取key</p>
     */
    private boolean isInit;

    public AbstractJavaStep() {
        this.isInit = true; // 关闭抛异常功能，才能正常获取key
        Set<String> keys = new HashSet<>();
        Map<String, Object> vars = new HashMap<String, Object>() {
            private static final long serialVersionUID = 1862003486085862406L;
            @Override
            public Object get(Object key) {
                if (key instanceof String) {
                    keys.add((String) key);
                }
                return null;
            }
        };
        // 调用一次 invoke 方法，获取所有key
        try {
            this.invoke(null, OneOffParams.builder().vars(vars).build());
        } catch (Throwable ignored) {
            // 由于只需要获取key，所以忽略所有报错
        }
        this.isInit = false; // 获取key完成，恢复正常抛异常功能
        this.paramNameList = Collections.unmodifiableList(new ArrayList<>(keys));;
    }

    public abstract Object invoke(StepData stepData, OneOffParams oneOffParams);

    protected <T> T getAs(OneOffParams oneOffParams, String key) {
        return this.getAs(oneOffParams, key, false);
    }

    protected <T> T getAs(OneOffParams oneOffParams, String key, boolean required) {
        Object val = oneOffParams.getVar(key);
        if (val == null) {
            if (required && !this.isInit) { // 非 init 状态，才报错
                throw new StepFlowException((String.format("The value for key[%s] is null", key)));
            }
            return null;
        } else {
            //noinspection unchecked
            return (T) val;
        }
    }

    protected <T> T getAsByType(OneOffParams oneOffParams, String key, TypeReference<T> typeReference) {
        return this.getAsByType(oneOffParams, key, typeReference, false);
    }

    /**
     * 从map中获取参数并根据 {@link TypeReference} 来做类型转换，可以支持泛型嵌套并及时抛出类型错误
     *
     * @param oneOffParams 仅供本次使用的参数集合
     * @param key 目标key
     * @param typeReference 类型
     * @param required {@code true}-不能为null，否则报错；{@code false}-可以为null
     * @return 目标value
     * @param <T> 目标value的类型
     */
    protected <T> T getAsByType(OneOffParams oneOffParams, String key, TypeReference<T> typeReference, boolean required) {
        Object val = oneOffParams.getVar(key);
        if (val == null) {
            if (required && !this.isInit) { // 非 init 状态，才报错
                throw new StepFlowException((String.format("The value for key[%s] is null", key)));
            }
            return null;
        } else {
            //noinspection unchecked
            return (T) val;
        }
    }

    public List<String> getParamNameList() {
        return this.paramNameList;
    }
}
