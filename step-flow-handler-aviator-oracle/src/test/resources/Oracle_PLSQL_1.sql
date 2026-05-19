-- ============================================================
-- 场景：贷款期限与费用综合计算
-- 覆盖：日期加减小数、数字四则运算、Oracle常用函数
--        decode, nvl, coalesce, abs, floor, ceil, round,
--        trunc, power, months_between, add_months, last_day
-- ============================================================
DECLARE
-- ---- 输入参数 ----
v_start_date   DATE   := TO_DATE('2023-03-15', 'YYYY-MM-DD'); -- 贷款开始日期
    v_end_date     DATE   := TO_DATE('2024-09-20', 'YYYY-MM-DD'); -- 贷款结束日期
    v_principal    NUMBER := 100;   -- 本金基数
    v_rate_input   NUMBER := 3.7;   -- 费率系数
    v_signed_value NUMBER := -7.4325923421;  -- 带负号的数值，用于测试 abs
    v_extra_factor NUMBER := NULL;  -- 额外因子（故意为 NULL，用于测试 nvl/coalesce/decode）

    -- ---- 中间计算结果 ----

    -- Step 1: 并行 - 月数差（months_between，带小数）
    -- 期望值: 18.16129032258064516129032258064516129032
    v_calc_months_raw    NUMBER;

    -- Step 2: 并行 - abs 处理带负号数值
    -- 期望值: 8.6
    v_calc_abs_val       NUMBER;

    -- Step 3: IF_ELSE - 根据 calc_months_raw > 18 决定计算分支
    -- true  分支: floor(months) * principal + ceil(rate * abs_val)  = 18*100 + ceil(31.82) = 1832
    -- false 分支: round(months, 1) * principal - floor(rate * abs_val)
    -- 期望值 (true 分支): 1832
    v_calc_base          NUMBER;

    -- Sub-Flow 步骤 (CALC_DATE_SUB):
    -- Step 4a: 并行 - 日期加小数天数（1.5 天 = 1天12小时）
    -- 期望值: 2023-03-16 12:00:00
    v_calc_date_shifted  DATE;

    -- Step 4b: 并行 - add_months，整月偏移
    -- 期望值: 2023-09-15
    v_calc_add_months    DATE;

    -- Step 4c: 并行 - last_day，本月最后一天
    -- 期望值: 2024-09-30
    v_calc_last_day      DATE;

    -- Step 4d: 顺序（依赖 calc_last_day）- 日期相减得天数
    -- 期望值: 10
    v_calc_date_diff     NUMBER;

    -- Step 5: decode + coalesce + nvl 空值处理
    -- decode(sign(NULL), 1, NULL, coalesce(NULL, nvl(NULL, 5))) = 5
    -- 期望值: 5
    v_calc_extra         NUMBER;

    -- Step 6: 综合最终结果，使用 round / trunc / power
    -- round(base + date_diff * extra - trunc(power(extra,2) / abs_val, 1), 2)
    -- = round(1832 + 10*5 - trunc(25/8.6, 1), 2)
    -- = round(1882 - 2.9, 2)
    -- 期望值: 1879.1
    v_calc_final         NUMBER(12, 2);

BEGIN
    DBMS_OUTPUT.ENABLE(1000000);

    -- ============================================================
    -- 主流程 CALC_DATE_MAIN - SEQUENCE 顺序执行
    -- ============================================================

    -- [ 节点1: PARALLEL 并行 ]
    -- Step 1: months_between 计算贷款跨越月数（含小数部分，每月按31天计小数）
    v_calc_months_raw := trunc(months_between(v_end_date, v_start_date), 4);
    DBMS_OUTPUT.PUT_LINE('calc_months_raw = ' || v_calc_months_raw);

    -- Step 2: abs 取绝对值
    v_calc_abs_val := abs(v_signed_value);
    DBMS_OUTPUT.PUT_LINE('calc_abs_val    = ' || v_calc_abs_val);

    -- [ 节点2: IF_ELSE 条件分支 ] 条件: calc_months_raw > 18
    IF v_calc_months_raw < 18.16121 THEN
        -- true 分支: floor 向下取整 + ceil 向上取整
        v_calc_base := floor(v_calc_months_raw) * v_principal
            + ceil(v_rate_input * v_calc_abs_val);
        DBMS_OUTPUT.PUT_LINE('calc_base (true 分支) = ' || v_calc_base);
ELSE
        -- false 分支: round 四舍五入 + floor 向下取整（本例不会走到这里）
        v_calc_base := round(v_calc_months_raw, 1) * v_principal
            - floor(v_rate_input * v_calc_abs_val);
        DBMS_OUTPUT.PUT_LINE('calc_base (false 分支) = ' || v_calc_base);
END IF;

    -- [ 节点3: FLOW 子流程 CALC_DATE_SUB ]
    -- ---- Sub-Flow 内部: SEQUENCE ----
    -- Sub-Flow 节点1: PARALLEL 并行执行 3 个日期步骤

    -- Step 4a: 日期加小数（加 1.5 天 = 加 1 天 12 小时）
    v_calc_date_shifted := v_start_date + 1.5;
    DBMS_OUTPUT.PUT_LINE('calc_date_shifted = '
        || TO_CHAR(v_calc_date_shifted, 'YYYY-MM-DD HH24:MI:SS'));

    -- Step 4b: add_months 整月偏移（开始日期往后推 6 个月）
    v_calc_add_months := add_months(v_start_date, 6);
    DBMS_OUTPUT.PUT_LINE('calc_add_months   = '
        || TO_CHAR(v_calc_add_months, 'YYYY-MM-DD'));

    -- Step 4c: last_day 取结束日期所在月份的最后一天
    v_calc_last_day := last_day(v_end_date);
    DBMS_OUTPUT.PUT_LINE('calc_last_day     = '
        || TO_CHAR(v_calc_last_day, 'YYYY-MM-DD'));

    -- Sub-Flow 节点2: STEP 顺序（依赖 calc_last_day）
    -- Step 4d: 两个日期相减，结果为天数（NUMBER 类型）
    v_calc_date_diff := v_calc_last_day - v_end_date;
    DBMS_OUTPUT.PUT_LINE('calc_date_diff    = ' || v_calc_date_diff);

    -- [ 节点4: STEP 独立步骤 ] decode + nvl + coalesce 空值处理
    -- sign(NULL) = NULL，decode 匹配不到 1，走 ELSE 分支
    -- coalesce(NULL, nvl(NULL, 5)) = coalesce(NULL, 5) = 5
    -- 注意：DECODE 在 PL/SQL 中只能通过 SELECT ... INTO ... FROM DUAL 使用
SELECT decode(sign(v_extra_factor), 1, v_extra_factor,
              coalesce(v_extra_factor, nvl(v_extra_factor, 5)))
INTO v_calc_extra FROM dual;
DBMS_OUTPUT.PUT_LINE('calc_extra        = ' || v_calc_extra);

    -- [ 节点5: STEP 最终计算步骤 ] round + trunc + power 综合运算
    -- round(base + date_diff * extra - trunc(power(extra, 2) / abs_val, 1), 2)
    -- = round(1832 + 10*5 - trunc(25/8.6, 1), 2)
    -- = round(1882 - 2.9, 2)
    -- = 1879.1
    v_calc_final :=
            v_calc_base
                + v_calc_date_diff * v_calc_extra
                - power(v_calc_extra, 2) / v_calc_abs_val;
    DBMS_OUTPUT.PUT_LINE('calc_final        = ' || v_calc_final);

    DBMS_OUTPUT.PUT_LINE('');
    DBMS_OUTPUT.PUT_LINE('=== 期望值汇总（供 Java 单测断言使用）===');
    DBMS_OUTPUT.PUT_LINE('calc_months_raw   = ' || v_calc_months_raw);
    DBMS_OUTPUT.PUT_LINE('calc_abs_val      = ' || v_calc_abs_val);
    DBMS_OUTPUT.PUT_LINE('calc_base         = ' || v_calc_base);
    DBMS_OUTPUT.PUT_LINE('calc_date_shifted = ' || TO_CHAR(v_calc_date_shifted, 'YYYY-MM-DD HH24:MI:SS'));
    DBMS_OUTPUT.PUT_LINE('calc_add_months   = ' || TO_CHAR(v_calc_add_months, 'YYYY-MM-DD'));
    DBMS_OUTPUT.PUT_LINE('calc_last_day     = ' || TO_CHAR(v_calc_last_day, 'YYYY-MM-DD'));
    DBMS_OUTPUT.PUT_LINE('calc_date_diff    = ' || v_calc_date_diff);
    DBMS_OUTPUT.PUT_LINE('calc_extra        = ' || v_calc_extra);
    DBMS_OUTPUT.PUT_LINE('calc_final        = ' || v_calc_final);

END;
/
