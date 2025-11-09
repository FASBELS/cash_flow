package com.dsi.spring.flujoreal.spring_cashflow.model;

public class Partida {
    private int codPartida;
    private String desPartida;
    private String ingEgr;
    private int nivel;
    private int orden;

    public Partida() {}

    public int getCodPartida() { return codPartida; }
    public void setCodPartida(int codPartida) { this.codPartida = codPartida; }

    public String getDesPartida() { return desPartida; }
    public void setDesPartida(String desPartida) { this.desPartida = desPartida; }

    public String getIngEgr() { return ingEgr; }
    public void setIngEgr(String ingEgr) { this.ingEgr = ingEgr; }

    public int getNivel() { return nivel; }
    public void setNivel(int nivel) { this.nivel = nivel; }

    public int getOrden() { return orden; }
    public void setOrden(int orden) { this.orden = orden; }
}
