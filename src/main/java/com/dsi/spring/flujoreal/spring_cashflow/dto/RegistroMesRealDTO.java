package com.dsi.spring.flujoreal.spring_cashflow.dto;

import java.math.BigDecimal;

public class RegistroMesRealDTO {

    private int anno;
    private int mes;
    private int codCia;
    private int codPyto;
    private String ingEgr;   // "I" o "E"
    private String tipo;     // "R"
    private int codPartida;
    private int orden;
    private BigDecimal monto;

    // getters y setters
    public int getAnno() { return anno; }
    public void setAnno(int anno) { this.anno = anno; }

    public int getMes() { return mes; }
    public void setMes(int mes) { this.mes = mes; }

    public int getCodCia() { return codCia; }
    public void setCodCia(int codCia) { this.codCia = codCia; }

    public int getCodPyto() { return codPyto; }
    public void setCodPyto(int codPyto) { this.codPyto = codPyto; }

    public String getIngEgr() { return ingEgr; }
    public void setIngEgr(String ingEgr) { this.ingEgr = ingEgr; }

    public String getTipo() { return tipo; }
    public void setTipo(String tipo) { this.tipo = tipo; }

    public int getCodPartida() { return codPartida; }
    public void setCodPartida(int codPartida) { this.codPartida = codPartida; }

    public int getOrden() { return orden; }
    public void setOrden(int orden) { this.orden = orden; }

    public BigDecimal getMonto() { return monto; }
    public void setMonto(BigDecimal monto) { this.monto = monto; }
}