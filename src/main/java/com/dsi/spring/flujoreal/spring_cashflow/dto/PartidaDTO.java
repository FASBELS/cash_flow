package com.dsi.spring.flujoreal.spring_cashflow.dto;

/**
 * DTO para representar partidas de un proyecto.
 * Ampliado para incluir los códigos usados en la jerarquía (CODPARTIDAS y SEMILLA).
 */
public class PartidaDTO {

    private String ingEgr;        // "I" ingreso, "E" egreso
    private Integer codPartida;   // puede ser null
    private String codPartidas;   // código jerárquico (ej: ING-001-01)
    private String desPartida;
    private int nivel;
    private Integer semilla;      // vínculo único (boletas)
    private Integer orden;        // puede ser null

    // --- Getters y Setters ---
    public String getIngEgr() { return ingEgr; }
    public void setIngEgr(String ingEgr) { this.ingEgr = ingEgr; }

    public Integer getCodPartida() { return codPartida; }
    public void setCodPartida(Integer codPartida) { this.codPartida = codPartida; }

    public String getCodPartidas() { return codPartidas; }
    public void setCodPartidas(String codPartidas) { this.codPartidas = codPartidas; }

    public String getDesPartida() { return desPartida; }
    public void setDesPartida(String desPartida) { this.desPartida = desPartida; }

    public int getNivel() { return nivel; }
    public void setNivel(int nivel) { this.nivel = nivel; }

    public Integer getSemilla() { return semilla; }
    public void setSemilla(Integer semilla) { this.semilla = semilla; }

    public Integer getOrden() { return orden; }
    public void setOrden(Integer orden) { this.orden = orden; }
}
