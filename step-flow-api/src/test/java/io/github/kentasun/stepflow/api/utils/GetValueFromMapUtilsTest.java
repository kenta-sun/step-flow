package io.github.kentasun.stepflow.api.utils;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * {@link GetValueFromMapUtils} 工具类单元测试。
 *
 * <p>覆盖 {@link GetValueFromMapUtils#getValueFromContextMap} 与
 * {@link GetValueFromMapUtils#getStepVars} 的各类分支与极端场景。</p>
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
    // getStepVars：从路径表达式中提取 root 数据组成新 env
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("getStepVars")
    class GetRootValueFromContextMapTests {

        /**
         * 断言 newEnv 仅含指定键值对（键集合完全一致）。
         */
        private void assertNewEnvExactly(Map<String, Object> expected, Map<String, Object> actual) {
            assertEquals(expected.size(), actual.size(), "newEnv 键数量不一致");
            for (Map.Entry<String, Object> entry : expected.entrySet()) {
                assertTrue(actual.containsKey(entry.getKey()), "缺少键: " + entry.getKey());
                assertSame(entry.getValue(), actual.get(entry.getKey()), "键值引用不一致: " + entry.getKey());
            }
        }

        @Nested
        @DisplayName("空入参与边界")
        class EmptyAndBoundaryInputs {

            @Test
            @DisplayName("nameList 为 null 时返回空 map")
            void nullNameListReturnsEmptyMap() {
                Map<String, Object> env = env("a", "v");
                Map<String, Object> result = GetValueFromMapUtils.getStepVars(null, env);
                assertTrue(result.isEmpty());
            }

            @Test
            @DisplayName("nameList 为空集合时返回空 map")
            void emptyNameListReturnsEmptyMap() {
                Map<String, Object> env = env("a", "v");
                Map<String, Object> result = GetValueFromMapUtils.getStepVars(
                        Collections.emptyList(), env);
                assertTrue(result.isEmpty());
            }

            @Test
            @DisplayName("env 为 null 时返回空 map")
            void nullEnvReturnsEmptyMap() {
                Map<String, Object> result = GetValueFromMapUtils.getStepVars(
                        Arrays.asList("a.b"), null);
                assertTrue(result.isEmpty());
            }

            @Test
            @DisplayName("env 为空 map 时返回空 map")
            void emptyEnvReturnsEmptyMap() {
                Map<String, Object> result = GetValueFromMapUtils.getStepVars(
                        Arrays.asList("a.b", "foo"), new HashMap<>());
                assertTrue(result.isEmpty());
            }

            @Test
            @DisplayName("nameList 含 null 元素时跳过该元素")
            void nullElementInNameListIsSkipped() {
                Map<String, Object> env = env("foo", 42);
                Map<String, Object> result = GetValueFromMapUtils.getStepVars(
                        Arrays.asList(null, "foo", null), env);
                assertNewEnvExactly(env("foo", 42), result);
            }

            @Test
            @DisplayName("空字符串 name 作为字面量键写入 newEnv")
            void emptyStringNameAsLiteralKey() {
                Map<String, Object> env = new HashMap<>();
                env.put("", "empty-key-value");
                Map<String, Object> result = GetValueFromMapUtils.getStepVars(
                        Collections.singletonList(""), env);
                assertNewEnvExactly(env("", "empty-key-value"), result);
            }
        }

        @Nested
        @DisplayName("简单键与字面量带点/括号键")
        class SimpleAndLiteralKeys {

            @Test
            @DisplayName("无符号名称直接 env.get 并以原名作为 newEnv 键")
            void simpleKeyUsesOriginalName() {
                Map<String, Object> root = env("inner", "deep");
                Map<String, Object> env = env("a", root, "foo", 99);
                Map<String, Object> result = GetValueFromMapUtils.getStepVars(
                        Arrays.asList("a", "foo"), env);
                assertNewEnvExactly(env("a", root, "foo", 99), result);
            }

            @Test
            @DisplayName("简单键不存在时不写入 newEnv（当前实现跳过 null 值）")
            void missingSimpleKeyIsSkipped() {
                // env 不能为空，否则会在方法入口直接返回空 map
                Map<String, Object> env = env("other", "x");
                Map<String, Object> result = GetValueFromMapUtils.getStepVars(
                        Collections.singletonList("missing"), env);
                assertTrue(result.isEmpty(), "缺失键对应 null，当前实现不会 put");
            }

            @Test
            @DisplayName("简单键对应 env 值为 null 时不写入 newEnv")
            void simpleKeyWithNullValueIsSkipped() {
                Map<String, Object> env = new HashMap<>();
                env.put("key", null);
                Map<String, Object> result = GetValueFromMapUtils.getStepVars(
                        Collections.singletonList("key"), env);
                assertTrue(result.isEmpty(), "null 值当前实现不会写入 newEnv");
            }

            @Test
            @DisplayName("env 含字面量带点键 a.b 时整体作为 root，不做路径解析")
            void literalDottedKeyStoredAsWhole() {
                Map<String, Object> nested = env("b", "navigated");
                Map<String, Object> env = env("a.b", "literal", "a", nested);
                Map<String, Object> result = GetValueFromMapUtils.getStepVars(
                        Collections.singletonList("a.b"), env);
                assertNewEnvExactly(env("a.b", "literal"), result);
                assertFalse(result.containsKey("a"));
            }

            @Test
            @DisplayName("env 含字面量括号键 list[0] 时直接写入")
            void literalBracketKeyStoredAsWhole() {
                List<String> list = Arrays.asList("first", "second");
                Map<String, Object> env = env("list[0]", "literal", "list", list);
                Map<String, Object> result = GetValueFromMapUtils.getStepVars(
                        Collections.singletonList("list[0]"), env);
                assertNewEnvExactly(env("list[0]", "literal"), result);
                assertFalse(result.containsKey("list"));
            }

            @Test
            @DisplayName("env 含字面量 Map 键表达式 data(k2) 时直接写入")
            void literalParenKeyStoredAsWhole() {
                Map<String, Object> dataMap = env("k1", "v1", "k2", "v2");
                Map<String, Object> env = env("data(k2)", "literal", "data", dataMap);
                Map<String, Object> result = GetValueFromMapUtils.getStepVars(
                        Collections.singletonList("data(k2)"), env);
                assertNewEnvExactly(env("data(k2)", "literal"), result);
                assertFalse(result.containsKey("data"));
            }
        }

        @Nested
        @DisplayName("路径表达式提取 root（点路径）")
        class DotPathRootExtraction {

            @Test
            @DisplayName("a.b 提取 root 键 a")
            void oneLevelPathExtractsRootA() {
                Map<String, Object> inner = env("b", "value");
                Map<String, Object> env = env("a", inner);
                Map<String, Object> result = GetValueFromMapUtils.getStepVars(
                        Collections.singletonList("a.b"), env);
                assertNewEnvExactly(env("a", inner), result);
            }

            @Test
            @DisplayName("a.b.c 仍只提取第一段 root 键 a")
            void multiLevelPathExtractsFirstSegmentOnly() {
                Map<String, Object> c = env("c", 99);
                Map<String, Object> b = env("b", c);
                Map<String, Object> env = env("a", b);
                Map<String, Object> result = GetValueFromMapUtils.getStepVars(
                        Collections.singletonList("a.b.c"), env);
                assertNewEnvExactly(env("a", b), result);
            }

            @Test
            @DisplayName("路径 root 在 env 中不存在时不写入 newEnv")
            void missingRootIsSkipped() {
                Map<String, Object> env = env("other", "x");
                Map<String, Object> result = GetValueFromMapUtils.getStepVars(
                        Collections.singletonList("a.b"), env);
                assertTrue(result.isEmpty(), "root 缺失得到 null，当前实现不会 put");
            }

            @Test
            @DisplayName("路径 root 值为 null 时不写入 newEnv")
            void nullRootValueIsSkipped() {
                Map<String, Object> env = new HashMap<>();
                env.put("a", null);
                Map<String, Object> result = GetValueFromMapUtils.getStepVars(
                        Collections.singletonList("a.b"), env);
                assertTrue(result.isEmpty(), "root 值为 null 时当前实现不会 put");
            }

            @Test
            @DisplayName("连续点 a..b 仍提取第一段 a")
            void consecutiveDotsStillUseFirstSegment() {
                Map<String, Object> root = env("b", "v");
                Map<String, Object> env = env("a", root);
                Map<String, Object> result = GetValueFromMapUtils.getStepVars(
                        Collections.singletonList("a..b"), env);
                assertNewEnvExactly(env("a", root), result);
            }
        }

        @Nested
        @DisplayName("路径表达式提取 root（下标与 Map 键）")
        class BracketAndParenRootExtraction {

            @Test
            @DisplayName("list[0] 提取 root 键 list")
            void listIndexExtractsListRoot() {
                List<String> list = Arrays.asList("first", "second");
                Map<String, Object> env = env("list", list);
                Map<String, Object> result = GetValueFromMapUtils.getStepVars(
                        Collections.singletonList("list[0]"), env);
                assertNewEnvExactly(env("list", list), result);
            }

            @Test
            @DisplayName("list[0].name 提取 root 键 list")
            void listIndexWithSuffixExtractsListRoot() {
                List<String> list = Arrays.asList("first", "second");
                Map<String, Object> env = env("list", list);
                Map<String, Object> result = GetValueFromMapUtils.getStepVars(
                        Collections.singletonList("list[0].name"), env);
                assertNewEnvExactly(env("list", list), result);
            }

            @Test
            @DisplayName("root.list[0] 提取 root 键 root")
            void nestedListIndexExtractsOuterRoot() {
                List<String> list = Arrays.asList("first", "second");
                Map<String, Object> env = env("root", env("list", list));
                Map<String, Object> result = GetValueFromMapUtils.getStepVars(
                        Collections.singletonList("root.list[0]"), env);
                assertSame(env.get("root"), result.get("root"));
            }

            @Test
            @DisplayName("data(k2) 提取 root 键 data")
            void mapKeyExtractsDataRoot() {
                Map<String, Object> dataMap = env("k1", "v1", "k2", "v2");
                Map<String, Object> env = env("data", dataMap);
                Map<String, Object> result = GetValueFromMapUtils.getStepVars(
                        Collections.singletonList("data(k2)"), env);
                assertNewEnvExactly(env("data", dataMap), result);
            }

            @Test
            @DisplayName("a.attrs(code) 提取 root 键 a")
            void nestedMapKeyExtractsFirstRoot() {
                Map<String, Object> attrs = env("code", "ERR_001");
                Map<String, Object> env = env("a", env("attrs", attrs));
                Map<String, Object> result = GetValueFromMapUtils.getStepVars(
                        Collections.singletonList("a.attrs(code)"), env);
                assertSame(env.get("a"), result.get("a"));
            }

            @Test
            @DisplayName("a(b).c 提取 root 键 a")
            void parenBeforeDotExtractsRootA() {
                Map<String, Object> inner = env("b", "v2", "c", "v3");
                Map<String, Object> env = env("a", inner);
                Map<String, Object> result = GetValueFromMapUtils.getStepVars(
                        Collections.singletonList("a(b).c"), env);
                assertNewEnvExactly(env("a", inner), result);
            }
        }

        @Nested
        @DisplayName("去重与处理顺序")
        class DeduplicationAndOrder {

            @Test
            @DisplayName("多个路径共享同一 root 时只保留首次写入")
            void sameRootFromMultiplePathsDeduped() {
                Map<String, Object> rootA = env("b", "bv", "c", "cv");
                Map<String, Object> env = env("a", rootA);
                Map<String, Object> result = GetValueFromMapUtils.getStepVars(
                        Arrays.asList("a.b", "a.c", "a.b.c"), env);
                assertEquals(1, result.size());
                assertSame(rootA, result.get("a"));
            }

            @Test
            @DisplayName("先写入简单键 a 后路径 a.b 不再覆盖")
            void simpleKeyFirstThenPathDoesNotOverwrite() {
                Map<String, Object> rootA = env("b", "bv");
                Map<String, Object> env = env("a", rootA);
                Map<String, Object> result = GetValueFromMapUtils.getStepVars(
                        Arrays.asList("a", "a.b"), env);
                assertSame(rootA, result.get("a"));
            }

            @Test
            @DisplayName("先路径 a.b 后简单键 a 会覆盖为 env.get(a)（同引用则不变）")
            void pathFirstThenSimpleKeyOverwritesWithSameReference() {
                Map<String, Object> rootA = env("b", "bv");
                Map<String, Object> env = env("a", rootA);
                Map<String, Object> result = GetValueFromMapUtils.getStepVars(
                        Arrays.asList("a.b", "a"), env);
                assertSame(rootA, result.get("a"));
            }

            @Test
            @DisplayName("不同 root 的路径各自独立写入")
            void differentRootsBothPresent() {
                Map<String, Object> rootA = env("x", 1);
                Map<String, Object> rootB = env("y", 2);
                Map<String, Object> env = env("a", rootA, "b", rootB);
                Map<String, Object> result = GetValueFromMapUtils.getStepVars(
                        Arrays.asList("a.x", "b.y"), env);
                assertNewEnvExactly(env("a", rootA, "b", rootB), result);
            }

            @Test
            @DisplayName("重复简单键以后者覆盖前者")
            void duplicateSimpleKeysLastWins() {
                Map<String, Object> env = env("foo", "first");
                Map<String, Object> result = GetValueFromMapUtils.getStepVars(
                        Arrays.asList("foo", "foo"), env);
                assertEquals("first", result.get("foo"));
                assertEquals(1, result.size());
            }
        }

        @Nested
        @DisplayName("混合场景")
        class MixedScenarios {

            @Test
            @DisplayName("字面量键 a.b 与路径 a.c 并存于 newEnv")
            void literalDottedKeyAndPathRootCoexist() {
                Map<String, Object> nested = env("c", "cv");
                Map<String, Object> env = env("a.b", "literal", "a", nested);
                Map<String, Object> result = GetValueFromMapUtils.getStepVars(
                        Arrays.asList("a.b", "a.c"), env);
                assertEquals(2, result.size());
                assertEquals("literal", result.get("a.b"));
                assertSame(nested, result.get("a"));
            }

            @Test
            @DisplayName("简单键、点路径、下标路径、Map 键路径混合")
            void mixedNameTypes() {
                List<String> list = Arrays.asList("L0");
                Map<String, Object> dataMap = env("k", "v");
                Map<String, Object> rootA = env("b", "ab");
                // 四组键值超出 env(...) 辅助方法容量，直接构造 HashMap
                Map<String, Object> env = new HashMap<>();
                env.put("plain", "P");
                env.put("a", rootA);
                env.put("list", list);
                env.put("data", dataMap);
                Map<String, Object> result = GetValueFromMapUtils.getStepVars(
                        Arrays.asList("plain", "a.b.c", "list[0].x", "data(k)"), env);
                assertEquals(4, result.size());
                assertEquals("P", result.get("plain"));
                assertSame(rootA, result.get("a"));
                assertSame(list, result.get("list"));
                assertSame(dataMap, result.get("data"));
            }

            @Test
            @DisplayName("JavaBean 作为 root 对象时存入原始引用")
            void beanRootStoredByReference() {
                SampleBean bean = new SampleBean("Alice", true);
                Map<String, Object> env = env("user", bean);
                Map<String, Object> result = GetValueFromMapUtils.getStepVars(
                        Collections.singletonList("user.name"), env);
                assertSame(bean, result.get("user"));
            }
        }

        @Nested
        @DisplayName("极端与异常路径")
        class ExtremePaths {

            @Test
            @DisplayName("以点开头 .a.b 解析失败时不写入任何键")
            void leadingDotPathFailsSilently() {
                Map<String, Object> env = env("a", env("b", "v"));
                Map<String, Object> result = GetValueFromMapUtils.getStepVars(
                        Collections.singletonList(".a.b"), env);
                assertTrue(result.isEmpty(), "异常被吞掉后 newEnv 应为空");
            }

            @Test
            @DisplayName("仅一个点 . 解析失败时不写入任何键")
            void dotOnlyPathFailsSilently() {
                Map<String, Object> env = env("a", "v");
                Map<String, Object> result = GetValueFromMapUtils.getStepVars(
                        Collections.singletonList("."), env);
                assertTrue(result.isEmpty());
            }

            @Test
            @DisplayName("[0].x 解析后 root 名为空串且 env 中空串键有非 null 值时写入")
            void bracketOnlyFirstSegmentProducesEmptyRootKey() {
                Map<String, Object> env = new HashMap<>();
                env.put("", "empty-root");
                Map<String, Object> result = GetValueFromMapUtils.getStepVars(
                        Collections.singletonList("[0].x"), env);
                assertTrue(result.containsKey(""));
                assertEquals("empty-root", result.get(""));
            }

            @Test
            @DisplayName("[0] 解析后 root 名为空串且对应值为 null 时不写入")
            void bracketZeroOnlyWithNullRootIsSkipped() {
                // env 需非空才能进入循环；此处不含空串键，env.get("") 为 null
                Map<String, Object> env = env("placeholder", "x");
                Map<String, Object> result = GetValueFromMapUtils.getStepVars(
                        Collections.singletonList("[0]"), env);
                assertTrue(result.isEmpty(), "空串 root 对应 null 时不写入");
            }

            @Test
            @DisplayName("[0] 在 env 为空 map 时因入口校验直接返回空 map")
            void bracketZeroWithEmptyEnvReturnsEmptyMap() {
                Map<String, Object> result = GetValueFromMapUtils.getStepVars(
                        Collections.singletonList("[0]"), new HashMap<>());
                assertTrue(result.isEmpty(), "env 为空时在入口即返回，不会解析 [0]");
            }

            @Test
            @DisplayName("仅含左括号无右括号时不走路径解析，按字面量键处理")
            void unbalancedBracketTreatedAsLiteralKey() {
                Map<String, Object> env = env("a[1", "literal");
                Map<String, Object> result = GetValueFromMapUtils.getStepVars(
                        Collections.singletonList("a[1"), env);
                assertNewEnvExactly(env("a[1", "literal"), result);
            }

            @Test
            @DisplayName("name 含点、env 无字面量键且 root 不存在时不写入")
            void pathWithMissingRootAndNoLiteralKey() {
                Map<String, Object> env = env("other", "x");
                Map<String, Object> result = GetValueFromMapUtils.getStepVars(
                        Collections.singletonList("missing.path"), env);
                assertTrue(result.isEmpty());
            }
        }

        @Nested
        @DisplayName("null 过滤与去重交互")
        class NullFilteringAndDedup {

            @Test
            @DisplayName("首次路径因 root 为 null 未写入时，后续同 root 路径仍可写入")
            void secondPathCanFillRootAfterFirstSkippedNull() {
                Map<String, Object> rootA = env("b", "bv");
                Map<String, Object> env = new HashMap<>();
                env.put("a", null);
                env.put("other", rootA);
                // 第一条 a.b：root a 为 null，跳过；第二条 other.x：正常写入
                Map<String, Object> result = GetValueFromMapUtils.getStepVars(
                        Arrays.asList("a.b", "other.x"), env);
                assertEquals(1, result.size());
                assertSame(rootA, result.get("other"));
                assertFalse(result.containsKey("a"));
            }

            @Test
            @DisplayName("root 已成功写入后，同 root 的后续路径不会重复处理")
            void dedupAfterSuccessfulRootWrite() {
                Map<String, Object> rootA = env("b", "bv", "c", "cv");
                Map<String, Object> env = env("a", rootA);
                Map<String, Object> result = GetValueFromMapUtils.getStepVars(
                        Arrays.asList("a.b", "a.c"), env);
                assertEquals(1, result.size());
                assertSame(rootA, result.get("a"));
            }

            @Test
            @DisplayName("字面量键值为 null 时不写入")
            void literalKeyWithNullValueIsSkipped() {
                Map<String, Object> env = new HashMap<>();
                env.put("a.b", null);
                Map<String, Object> result = GetValueFromMapUtils.getStepVars(
                        Collections.singletonList("a.b"), env);
                assertTrue(result.isEmpty());
            }
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
                    "name='" + this.name + '\'' +
                    ", active=" + this.active +
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
                    "isFlag=" + this.isFlag +
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
                    "url='" + this.url + '\'' +
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
                    "bean=" + this.bean +
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
