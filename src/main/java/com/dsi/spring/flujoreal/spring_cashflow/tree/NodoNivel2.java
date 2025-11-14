package com.dsi.spring.flujoreal.spring_cashflow.tree;

import java.math.BigDecimal;

import com.dsi.spring.flujoreal.spring_cashflow.dto.PartidaDTO;

public class NodoNivel2 {
    private PartidaDTO partida;
    private BigDecimal[] meses = new BigDecimal[12]; // enero..diciembre

    public NodoNivel2(PartidaDTO partida) { this.partida = partida; }

    public PartidaDTO getPartida() { return partida; }
    public void setPartida(PartidaDTO partida) { this.partida = partida; }

    public BigDecimal[] getMeses() { return meses; }
    public void setMeses(BigDecimal[] meses) { this.meses = meses; }
}
