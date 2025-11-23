package com.dsi.spring.flujoreal.spring_cashflow.dto;

import java.math.BigDecimal;
import java.util.Arrays;

public class MonthValues {
    public final BigDecimal[] mes = new BigDecimal[12]; 
    public BigDecimal suma = BigDecimal.ZERO;
    public BigDecimal acumAnt = BigDecimal.ZERO;
    public BigDecimal total = BigDecimal.ZERO;

    public MonthValues() {
        Arrays.fill(mes, BigDecimal.ZERO);
    }
}
