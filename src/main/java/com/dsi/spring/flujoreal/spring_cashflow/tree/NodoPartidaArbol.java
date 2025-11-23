package com.dsi.spring.flujoreal.spring_cashflow.tree;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import com.dsi.spring.flujoreal.spring_cashflow.dto.PartidaDTO;

public class NodoPartidaArbol {

    private PartidaDTO partida;

    private List<NodoPartidaArbol> hijos = new ArrayList<>();

    private BigDecimal[] meses = new BigDecimal[12];

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
