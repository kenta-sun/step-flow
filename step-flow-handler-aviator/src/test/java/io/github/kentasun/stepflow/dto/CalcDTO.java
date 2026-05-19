package io.github.kentasun.stepflow.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
public class CalcDTO {

    private BigDecimal num1;
    private BigDecimal num2;
    private BigDecimal num3;
    private BigDecimal num4;
}
