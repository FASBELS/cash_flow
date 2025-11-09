package com.dsi.spring.flujoreal.spring_cashflow.dto;

/**
 * DTO para representar partidas de un proyecto.
 * Reemplaza a ConceptoDTO.
 */
public class PartidaDTO {
    private String ingEgr;      // "I" ingreso, "E" egreso
    private int codPartida;
    private String desPartida;
    private int nivel;
    private Integer orden;      // puede ser null

    public String getIngEgr() { return ingEgr; }
    public void setIngEgr(String ingEgr) { this.ingEgr = ingEgr; }

    public int getCodPartida() { return codPartida; }
    public void setCodPartida(int codPartida) { this.codPartida = codPartida; }

    public String getDesPartida() { return desPartida; }
    public void setDesPartida(String desPartida) { this.desPartida = desPartida; }

    public int getNivel() { return nivel; }
    public void setNivel(int nivel) { this.nivel = nivel; }

    public Integer getOrden() { return orden; }
    public void setOrden(Integer orden) { this.orden = orden; }
}
