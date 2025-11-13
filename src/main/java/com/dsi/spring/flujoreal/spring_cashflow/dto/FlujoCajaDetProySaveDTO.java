// src/main/java/.../dto/FlujoCajaDetProySaveDTO.java
package com.dsi.spring.flujoreal.spring_cashflow.dto;

import java.math.BigDecimal;

public class FlujoCajaDetProySaveDTO {

    public int anno;
    public int codCia;
    public int codPyto;
    public String ingEgr;
    public int codPartida;
    public int orden;
    public String tipo;        // A/M/T, etc.
    public String desPartida;  // opcional, ayuda para FLUJOCAJA
    public BigDecimal[] impMes;    // longitud 12: [ene, feb, ..., dic]
}
