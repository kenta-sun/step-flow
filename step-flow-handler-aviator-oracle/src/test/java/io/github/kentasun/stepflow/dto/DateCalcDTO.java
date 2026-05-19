package io.github.kentasun.stepflow.dto;

import io.github.kentasun.aviatororacle.number.OraDecimal;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

/**
 * 日期与费率综合计算场景 DTO
 * <p>
 * 对应 PLSQL 脚本 Oracle_PLSQL_1.sql 中的输入参数，用于覆盖：
 * <ul>
 *   <li>日期加减小数（{@code date + 1.5}）</li>
 *   <li>数字四则运算</li>
 *   <li>Oracle 函数：decode / nvl / coalesce / abs / floor / ceil / round /
 *       trunc / power / months_between / add_months / last_day</li>
 * </ul>
 */
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
public class DateCalcDTO {

    /** 贷款开始日期（2023-03-15） */
    private Date startDate;

    /** 贷款结束日期（2024-09-20） */
    private Date endDate;

    /** 本金基数 = 100 */
    private OraDecimal principal;

    /** 费率系数 = 3.7 */
    private OraDecimal rateInput;

    /**
     * 带负号的数值，用于测试 abs 函数（= -7.4325923421）
     */
    private OraDecimal signedValue;

    /**
     * 额外因子，故意置为 {@code null}，触发 nvl / coalesce / decode 的 NULL 分支。
     * <p>
     * Oracle 侧：{@code decode(nvl(null, 5), 5, coalesce(null, nvl(null, 5)), 0) = 5}
     */
    private OraDecimal extraFactor;
}
