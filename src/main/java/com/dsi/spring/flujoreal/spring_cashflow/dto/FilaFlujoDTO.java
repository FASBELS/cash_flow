package com.dsi.spring.flujoreal.spring_cashflow.dto;

public class FilaFlujoDTO {
    public int codPartida;
    public String desPartida;
    public String ingEgr; // 'I' o 'E'
    public MonthValues valores = new MonthValues();
}
