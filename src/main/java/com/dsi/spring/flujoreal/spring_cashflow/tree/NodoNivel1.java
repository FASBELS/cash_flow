package com.dsi.spring.flujoreal.spring_cashflow.tree;

import java.util.ArrayList;
import java.util.List;

import com.dsi.spring.flujoreal.spring_cashflow.dto.PartidaDTO;

public class NodoNivel1 {
    private PartidaDTO partida;
    private List<NodoNivel2> hijos = new ArrayList<>();
    private boolean noProyectado;

    public NodoNivel1(PartidaDTO partida) { this.partida = partida; }

    public PartidaDTO getPartida() { return partida; }
    public void setPartida(PartidaDTO partida) { this.partida = partida; }

    public List<NodoNivel2> getHijos() { return hijos; }
    public void setHijos(List<NodoNivel2> hijos) { this.hijos = hijos; }

    public boolean isNoProyectado() { return noProyectado; }
    public void setNoProyectado(boolean noProyectado) { this.noProyectado = noProyectado; }
}
