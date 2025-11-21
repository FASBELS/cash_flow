package com.dsi.spring.flujoreal.spring_cashflow.tree;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import com.dsi.spring.flujoreal.spring_cashflow.dto.PartidaDTO;

/**
 * Nodo genérico para representar la jerarquía de partidas del proyecto.
 * Soporta cualquier nivel (1,2,3,4,5,...) mediante la lista recursiva de hijos.
 *
 * Estructura pensada para serializarse a JSON como:
 * {
 *   "partida": { ... },
 *   "hijos": [ { ... }, { ... } ],
 *   "noProyectado": false,
 *   "meses": [ ... 12 valores ... ]
 * }
 */
public class NodoPartidaArbol {

    /** Información de la partida (Ing/Egr, nivel, orden, descripción, etc.) */
    private PartidaDTO partida;

    /** Hijos directos de este nodo (nivel +1) */
    private List<NodoPartidaArbol> hijos = new ArrayList<>();

    /** Valores por mes (enero..diciembre). Se puede usar tanto para real como proyectado. */
    private BigDecimal[] meses = new BigDecimal[12];

    /** Marca si esta partida viene de "no proyectado" (para estilos en el front). */
    private boolean noProyectado;

    public NodoPartidaArbol() {
    }

    public NodoPartidaArbol(PartidaDTO partida) {
        this.partida = partida;
    }

    public PartidaDTO getPartida() {
        return partida;
    }

    public void setPartida(PartidaDTO partida) {
        this.partida = partida;
    }

    public List<NodoPartidaArbol> getHijos() {
        return hijos;
    }

    public void setHijos(List<NodoPartidaArbol> hijos) {
        this.hijos = hijos;
    }

    public BigDecimal[] getMeses() {
        return meses;
    }

    public void setMeses(BigDecimal[] meses) {
        this.meses = meses;
    }

    public boolean isNoProyectado() {
        return noProyectado;
    }

    public void setNoProyectado(boolean noProyectado) {
        this.noProyectado = noProyectado;
    }
}
