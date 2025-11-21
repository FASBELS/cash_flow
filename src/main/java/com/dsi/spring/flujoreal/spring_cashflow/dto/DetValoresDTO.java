package com.dsi.spring.flujoreal.spring_cashflow.dto;

import java.math.BigDecimal;

public class DetValoresDTO {

    public BigDecimal impRealIni  = BigDecimal.ZERO;
    public BigDecimal impRealAcum = BigDecimal.ZERO;
    public BigDecimal[] mes       = new BigDecimal[12];

    public DetValoresDTO() {
        for (int i = 0; i < 12; i++) {
            mes[i] = BigDecimal.ZERO;
        }
    }
}
