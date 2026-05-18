package io.github.kentasun.stepflow.utils;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

/**
 * {@link GetValueFromMapUtils#getValueFromContextMap} 的单元测试。
 *
 * <p>覆盖简单键查找、带点路径解析、Map 嵌套、JavaBean 属性、下标与 Map 键访问等分支。</p>
 *
 * @author kenta-sun
 */
public class GetValueFromMapUtilsTest {

    /**
     * 构造单键测试用 env（Java 8 无 {@code Map#of}）。
     */
    private static Map<String, Object> env(Object k1, Object v1) {
        Map<String, Object> m = new HashMap<>();
        m.put(String.valueOf(k1), v1);
        return m;
    }

    /**
     * 构造双键测试用 env。
     */
    private static Map<String, Object> env(Object k1, Object v1, Object k2, Object v2) {
        Map<String, Object> m = new HashMap<>();
        m.put(String.valueOf(k1), v1);
        m.put(String.valueOf(k2), v2);
        return m;
    }

    /**
     * 构造三键测试用 env。
     */
    private static Map<String, Object> env(Object k1, Object v1, Object k2, Object v2, Object k3, Object v3) {
        Map<String, Object> m = new HashMap<>();
        m.put(String.valueOf(k1), v1);
        m.put(String.valueOf(k2), v2);
        m.put(String.valueOf(k3), v3);
        return m;
    }

    // -------------------------------------------------------------------------
    // 简单键查找（名称不含点，或含点但 env 中存在同名完整键）
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("简单键与字面量带点键")
    class SimpleKeyLookup {

        @Test
        @DisplayName("无点名称时直接 env.get")
        void directGetWithoutDot() {
            Map<String, Object> env = env("foo", 42);
            assertEquals(42, GetValueFromMapUtils.getValueFromContextMap("foo", env));
        }

        @Test
        @DisplayName("键不存在时返回 null")
        void missingKeyReturnsNull() {
            Map<String, Object> env = new HashMap<>();
            assertNull(GetValueFromMapUtils.getValueFromContextMap("missing", env));
        }

        @Test
        @DisplayName("值为 null 时原样返回 null")
        void nullValueInEnv() {
            Map<String, Object> env = new HashMap<>();
            env.put("key", null);
            assertNull(GetValueFromMapUtils.getValueFromContextMap("key", env));
        }

        @Test
        @DisplayName("env 含同名带点键时走直接查找，不做路径解析")
        void literalDottedKeyTakesPrecedence() {
            Map<String, Object> env = new HashMap<>();
            env.put("a.b", "literal");
            env.put("a", env("b", "navigated"));

            assertEquals("literal", GetValueFromMapUtils.getValueFromContextMap("a.b", env));
        }

        @Test
        @DisplayName("名称无点时仅 env.get，不解析路径")
        void noDotSkipsPropertyResolution() {
            Map<String, Object> nested = env("inner", "deep");
            Map<String, Object> env = env("a", nested);

            assertSame(nested, GetValueFromMapUtils.getValueFromContextMap("a", env));
        }

        @Test
        @DisplayName("env 含无点字面量键 list[0] 时直接返回")
        void literalBracketKeyWithoutDot() {
            Map<String, Object> env = env("list[0]", "literal");

            assertEquals("literal", GetValueFromMapUtils.getValueFromContextMap("list[0]", env));
        }
    }

    // -------------------------------------------------------------------------
    // Map 嵌套路径（a.b.c）
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("Map 嵌套路径")
    class MapNestedPath {

        @Test
        @DisplayName("单层嵌套 a.b")
        void oneLevelNested() {
            Map<String, Object> inner = env("b", "value");
            Map<String, Object> env = env("a", inner);

            assertEquals("value", GetValueFromMapUtils.getValueFromContextMap("a.b", env));
        }

        @Test
        @DisplayName("多层嵌套 a.b.c")
        void multiLevelNested() {
            Map<String, Object> c = env("c", 99);
            Map<String, Object> b = env("b", c);
            Map<String, Object> env = env("a", b);

            assertEquals(99, GetValueFromMapUtils.getValueFromContextMap("a.b.c", env));
        }

        @Test
        @DisplayName("中间 Map 值为 null 时返回 null")
        void nullIntermediateMapValue() {
            Map<String, Object> env = new HashMap<>();
            env.put("a", null);

            assertNull(GetValueFromMapUtils.getValueFromContextMap("a.b", env));
        }
    }

    // -------------------------------------------------------------------------
    // JavaBean 属性（getXxx / isXxx / is 前缀属性名）
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("JavaBean 属性访问")
    class JavaBeanPropertyAccess {

        @Test
        @DisplayName("标准 get 方法")
        void standardGetter() {
            SampleBean bean = new SampleBean("Alice", true);
            Map<String, Object> env = env("user", bean);

            assertEquals("Alice", GetValueFromMapUtils.getValueFromContextMap("user.name", env));
        }

        @Test
        @DisplayName("boolean 属性走 is 前缀")
        void booleanIsGetter() {
            SampleBean bean = new SampleBean("bob", false);
            Map<String, Object> env = env("user", bean);

            assertEquals(false, GetValueFromMapUtils.getValueFromContextMap("user.active", env));
        }

        @Test
        @DisplayName("属性名以 is 开头时直接匹配 isXxx 方法")
        void propertyNameStartsWithIs() {
            IsFlagBean bean = new IsFlagBean(true);
            Map<String, Object> env = env("bean", bean);

            assertEquals(true, GetValueFromMapUtils.getValueFromContextMap("bean.isFlag", env));
        }

        @Test
        @DisplayName("第二字符大写时保持属性名大小写（如 URL）")
        void acronymPropertyCapitalization() {
            UrlBean bean = new UrlBean("https://example.com");
            Map<String, Object> env = env("res", bean);

            assertEquals("https://example.com", GetValueFromMapUtils.getValueFromContextMap("res.URL", env));
        }

        @Test
        @DisplayName("Map 与 Bean 混合路径")
        void mixedMapAndBeanPath() {
            NestedBeanHolder holder = new NestedBeanHolder(new SampleBean("mixed", true));
            Map<String, Object> env = env("root", env("inner", holder));

            assertEquals("mixed", GetValueFromMapUtils.getValueFromContextMap("root.inner.bean.name", env));
        }

        @Test
        @DisplayName("Bean 上不存在属性时返回 null")
        void missingBeanPropertyReturnsNull() {
            Map<String, Object> env = env("user", new SampleBean("x", true));

            assertNull(GetValueFromMapUtils.getValueFromContextMap("user.unknown", env));
        }

        @Test
        @DisplayName("同名 getter 存在有参重载时优先无参方法")
        void preferNoArgGetterOverOverload() {
            OverloadedGetterBean bean = new OverloadedGetterBean();
            Map<String, Object> env = env("bean", bean);

            assertEquals("no-arg", GetValueFromMapUtils.getValueFromContextMap("bean.value", env));
        }
    }

    // -------------------------------------------------------------------------
    // 下标访问 [index]：List、数组、CharSequence
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("下标访问 [index]")
    class IndexedAccess {

        @Test
        @DisplayName("List 下标 list[0]")
        void singleListIndex() {
            List<String> list = Arrays.asList("first", "second");
            Map<String, Object> env = env("list", list);

            assertEquals("first", GetValueFromMapUtils.getValueFromContextMap("list[0]", env));
        }

        @Test
        @DisplayName("List 下标 root.list[0]")
        void listIndex() {
            List<String> list = Arrays.asList("first", "second");
            Map<String, Object> env = env("root", env("list", list));

            assertEquals("first", GetValueFromMapUtils.getValueFromContextMap("root.list[0]", env));
        }

        @Test
        @DisplayName("Object 数组下标")
        void objectArrayIndex() {
            String[] arr = {"x", "y"};
            Map<String, Object> env = env("root", env("arr", arr));

            assertEquals("x", GetValueFromMapUtils.getValueFromContextMap("root.arr[0]", env));
        }

        @Test
        @DisplayName("int 数组下标")
        void intArrayIndex() {
            int[] arr = {10, 20};
            Map<String, Object> env = env("root", env("nums", arr));

            assertEquals(10, GetValueFromMapUtils.getValueFromContextMap("root.nums[0]", env));
        }

        @Test
        @DisplayName("byte 数组下标")
        void byteArrayIndex() {
            byte[] arr = {1, 2};
            Map<String, Object> env = env("root", env("bytes", arr));

            assertEquals((byte) 1, GetValueFromMapUtils.getValueFromContextMap("root.bytes[0]", env));
        }

        @Test
        @DisplayName("String 作为 CharSequence 按下标取字符")
        void charSequenceIndex() {
            Map<String, Object> env = env("root", env("text", "abc"));

            assertEquals('a', GetValueFromMapUtils.getValueFromContextMap("root.text[0]", env));
        }

        @Test
        @DisplayName("嵌套路径 a.items[1].name")
        void nestedListThenBeanProperty() {
            SampleBean b0 = new SampleBean("zero", true);
            SampleBean b1 = new SampleBean("one", false);
            Map<String, Object> env = env("a", env("items", Arrays.asList(b0, b1)));

            assertEquals("one", GetValueFromMapUtils.getValueFromContextMap("a.items[1].name", env));
        }

        @Test
        @DisplayName("空段 .[0] 对当前 Map 取下标（需 List 在 env 根）")
        void emptySegmentIndexesCurrentEnvAsList() {
            // 路径 ".[0]"：首段 rName 为空，val 取 innerEnv（整个 env）
            List<String> rootList = Arrays.asList("only", "str2");
            Map<String, Object> env = new HashMap<>();
            env.put("ignored", "x");
            // 通过 bean 持有 list，再用 .[0] 形式访问 list 本身较自然；此处测 a.list.[0]
            env.put("a", rootList);

            assertEquals("only", GetValueFromMapUtils.getValueFromContextMap("a.[0]", env));
        }

        @Test
        @DisplayName("对非数组/List/CharSequence 取下标时返回 null")
        void invalidIndexTargetReturnsNull() {
            Map<String, Object> env = env("root", env("n", 42));

            assertNull(GetValueFromMapUtils.getValueFromContextMap("root.n[0]", env));
        }
    }

    // -------------------------------------------------------------------------
    // Map 键访问 name(key)
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("Map 键访问 name(key)")
    class MapKeyAccess {

        @Test
        @DisplayName("单层 data(key)")
        void singleMapKey() {
            Map<String, Object> dataMap = env("k1", "v1", "k2", "v2");
            Map<String, Object> env = env("data", dataMap);

            assertEquals("v2", GetValueFromMapUtils.getValueFromContextMap("data(k2)", env));
        }

        @Test
        @DisplayName("嵌套 a.attrs(code)")
        void nestedMapKey() {
            Map<String, Object> attrs = env("code", "ERR_001");
            Map<String, Object> env = env("a", env("attrs", attrs));

            assertEquals("ERR_001", GetValueFromMapUtils.getValueFromContextMap("a.attrs(code)", env));
        }

        @Test
        @DisplayName("对非 Map 使用 (key) 时返回 null")
        void mapKeyOnNonMapReturnsNull() {
            Map<String, Object> env = env("wrap", env("x", "not-a-map"));

            assertNull(GetValueFromMapUtils.getValueFromContextMap("wrap.x(key)", env));
        }
    }

    // -------------------------------------------------------------------------
    // 组合场景与边界
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("组合与边界")
    class CombinedAndEdgeCases {

        @Test
        @DisplayName("返回中间对象引用")
        void returnsIntermediateObjectReference() {
            Map<String, Object> inner = new HashMap<>();
            inner.put("leaf", "v");
            Map<String, Object> env = env("mid", inner);

            Object result = GetValueFromMapUtils.getValueFromContextMap("mid", env);
            assertSame(inner, result);
        }

        @Test
        @DisplayName("long 与 double 原始数组下标")
        void primitiveLongAndDoubleArrays() {
            long[] longs = {100L, 200L};
            double[] doubles = {1.5, 2.5};
            Map<String, Object> env = env("root", env("longs", longs, "doubles", doubles));

            assertEquals(100L, GetValueFromMapUtils.getValueFromContextMap("root.longs[0]", env));
            assertEquals(1.5, GetValueFromMapUtils.getValueFromContextMap("root.doubles[0]", env));
        }

        @Test
        @DisplayName("short、float、String 数组下标")
        void otherPrimitiveAndStringArrays() {
            short[] shorts = {3, 4};
            float[] floats = {1.1f, 2.2f};
            String[] strings = {"s0", "s1"};
            Map<String, Object> root = env("shorts", shorts, "floats", floats, "strings", strings);
            Map<String, Object> env = env("root", root);

            assertEquals((short) 3, GetValueFromMapUtils.getValueFromContextMap("root.shorts[0]", env));
            assertEquals(1.1f, GetValueFromMapUtils.getValueFromContextMap("root.floats[0]", env));
            assertEquals("s0", GetValueFromMapUtils.getValueFromContextMap("root.strings[0]", env));
        }

        @Test
        @DisplayName("boolean 数组通过 Array.get 访问")
        void booleanArrayIndex() {
            boolean[] flags = {true, false};
            Map<String, Object> env = env("root", env("flags", flags));

            assertEquals(true, GetValueFromMapUtils.getValueFromContextMap("root.flags[0]", env));
        }
    }

    // -------------------------------------------------------------------------
    // 测试用 JavaBean 与辅助类型
    // -------------------------------------------------------------------------

    /**
     * 常规 getter / is 前缀示例 Bean。
     */
    public static class SampleBean {
        private final String name;
        private final boolean active;

        public SampleBean(String name, boolean active) {
            this.name = name;
            this.active = active;
        }

        public String getName() {
            return name;
        }

        public boolean isActive() {
            return active;
        }

        @Override
        public String toString() {
            return "SampleBean{" +
                    "name='" + name + '\'' +
                    ", active=" + active +
                    '}';
        }
    }

    /**
     * 属性名以 is 开头的 Bean。
     */
    public static class IsFlagBean {
        private final boolean isFlag;

        public IsFlagBean(boolean isFlag) {
            this.isFlag = isFlag;
        }

        public boolean isIsFlag() {
            return isFlag;
        }

        @Override
        public String toString() {
            return "IsFlagBean{" +
                    "isFlag=" + isFlag +
                    '}';
        }
    }

    /**
     * 第二字符大写的缩写属性 Bean。
     */
    public static class UrlBean {
        private final String url;

        public UrlBean(String url) {
            this.url = url;
        }

        public String getURL() {
            return url;
        }

        @Override
        public String toString() {
            return "UrlBean{" +
                    "url='" + url + '\'' +
                    '}';
        }
    }

    /**
     * 嵌套 Bean 容器。
     */
    public static class NestedBeanHolder {
        private final SampleBean bean;

        public NestedBeanHolder(SampleBean bean) {
            this.bean = bean;
        }

        public SampleBean getBean() {
            return bean;
        }

        @Override
        public String toString() {
            return "NestedBeanHolder{" +
                    "bean=" + bean +
                    '}';
        }
    }

    /**
     * is 方法返回非 Boolean 的 Bean，用于触发 boolean 类型校验失败分支。
     */
    public static class BadBooleanBean {
        public String isActive() {
            return "not-boolean";
        }
    }

    /**
     * 含 getValue 有参/无参重载，用于验证无参 getter 优先。
     */
    public static class OverloadedGetterBean {
        public String getValue() {
            return "no-arg";
        }

        public String getValue(int ignored) {
            return "with-arg";
        }
    }
}
