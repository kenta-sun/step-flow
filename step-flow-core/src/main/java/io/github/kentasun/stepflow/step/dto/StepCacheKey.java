package io.github.kentasun.stepflow.step.dto;

import java.util.Objects;

/**
 * 缓存內联表达式的Step
 * <p>可防止因字符串拼接导致的哈希碰撞风险</p>
 */
public final class StepCacheKey {

    // 内容类型
    private final String contentType;
    // 表达式
    private final String expression;

    public StepCacheKey(String contentType, String expression) {
        this.contentType = contentType;
        this.expression = expression;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        StepCacheKey other = (StepCacheKey) obj;
        return Objects.equals(this.contentType, other.contentType) &&
                Objects.equals(this.expression, other.expression);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.contentType, this.expression);
    }
}
