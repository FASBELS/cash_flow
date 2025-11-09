package com.dsi.spring.flujoreal.spring_cashflow.dto;

import java.math.BigDecimal;

public class FlujoCajaDetSaveDTO {

    public int anno;
    public int codCia;
    public int codPyto;
    public String ingEgr;     // 'I' ingreso, 'E' egreso
    public String tipo;       // 'R' flujo real
    public int codPartida;
    public int orden;         // posición de la fila en la grilla

    // Montos reales por mes: ene..dic
    public BigDecimal[] impRealMes;  // 🔹 SIN new[], SIN constructor
}
