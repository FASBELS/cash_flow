package com.dsi.spring.flujoreal.spring_cashflow.dto;
public class FilaFlujoDTO {
    public int codPartida;
    public String desPartida;
    public String ingEgr; // 'I' o 'E'
    public MonthValues valores = new MonthValues();
    
    public String codPartidas; // El código de texto, ej: 'ING-001-01'
    public int nivel;

    public String getIngEgr() {
        return ingEgr;
    }
    public String getCodPartidas() {
        return codPartidas;
    }
}
