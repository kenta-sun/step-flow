package io.github.kentasun.stepflow.dto;

import java.math.BigDecimal;

/**
 * 流程计算单测使用的入参 DTO，对应上下文中的 {@code dto} 对象。
 */
public class CalcDTO {

    private BigDecimal num1;
    private BigDecimal num2;
    private BigDecimal num3;
    private BigDecimal num4;

    public CalcDTO(BigDecimal num1, BigDecimal num2, BigDecimal num3, BigDecimal num4) {
        this.num1 = num1;
        this.num2 = num2;
        this.num3 = num3;
        this.num4 = num4;
    }

    public CalcDTO() {
    }

    public BigDecimal getNum1() {
        return this.num1;
    }

    public BigDecimal getNum2() {
        return this.num2;
    }

    public BigDecimal getNum3() {
        return this.num3;
    }

    public BigDecimal getNum4() {
        return this.num4;
    }

    public void setNum1(BigDecimal num1) {
        this.num1 = num1;
    }

    public void setNum2(BigDecimal num2) {
        this.num2 = num2;
    }

    public void setNum3(BigDecimal num3) {
        this.num3 = num3;
    }

    public void setNum4(BigDecimal num4) {
        this.num4 = num4;
    }

    @Override
    public String toString() {
        return "CalcDTO(num1=" + this.getNum1() + ", num2=" + this.getNum2()
                + ", num3=" + this.getNum3() + ", num4=" + this.getNum4() + ")";
    }

    public static CalcDTOBuilder builder() {
        return new CalcDTOBuilder();
    }

    public static class CalcDTOBuilder {
        private BigDecimal num1;
        private BigDecimal num2;
        private BigDecimal num3;
        private BigDecimal num4;

        CalcDTOBuilder() {
        }

        public CalcDTOBuilder num1(BigDecimal num1) {
            this.num1 = num1;
            return this;
        }

        public CalcDTOBuilder num2(BigDecimal num2) {
            this.num2 = num2;
            return this;
        }

        public CalcDTOBuilder num3(BigDecimal num3) {
            this.num3 = num3;
            return this;
        }

        public CalcDTOBuilder num4(BigDecimal num4) {
            this.num4 = num4;
            return this;
        }

        public CalcDTO build() {
            return new CalcDTO(this.num1, this.num2, this.num3, this.num4);
        }

        @Override
        public String toString() {
            return "CalcDTO.CalcDTOBuilder(num1=" + this.num1 + ", num2=" + this.num2
                    + ", num3=" + this.num3 + ", num4=" + this.num4 + ")";
        }
    }
}
