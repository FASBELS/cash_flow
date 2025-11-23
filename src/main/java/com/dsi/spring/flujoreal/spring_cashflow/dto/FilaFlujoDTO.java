package com.dsi.spring.flujoreal.spring_cashflow.dto;
public class FilaFlujoDTO {
    public int codPartida;
    public String desPartida;
    public String ingEgr; 
    public MonthValues valores = new MonthValues();
    
    public String codPartidas; 
    public int nivel;

    public boolean noProyectado;
    public String getIngEgr() {
        return ingEgr;
    }
    public String getCodPartidas() {
        return codPartidas;
    }
}