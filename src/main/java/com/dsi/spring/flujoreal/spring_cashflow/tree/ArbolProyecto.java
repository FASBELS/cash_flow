package com.dsi.spring.flujoreal.spring_cashflow.tree;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class ArbolProyecto {

    private Map<String, List<NodoPartidaArbol>> porIngEgr = new HashMap<>();

    public ArbolProyecto() {
        porIngEgr.put("I", new ArrayList<>());
        porIngEgr.put("E", new ArrayList<>());
    }

    public Map<String, List<NodoPartidaArbol>> getPorIngEgr() {
        return porIngEgr;
    }

    public void setPorIngEgr(Map<String, List<NodoPartidaArbol>> porIngEgr) {
        this.porIngEgr = porIngEgr;
    }
}
