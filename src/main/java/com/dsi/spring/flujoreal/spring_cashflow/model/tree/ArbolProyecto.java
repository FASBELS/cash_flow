package com.dsi.spring.flujoreal.spring_cashflow.model.tree;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Representa el árbol jerárquico de partidas del proyecto:
 * - Ingresos (I)
 * - Egresos (E)
 * Cada sección contiene una lista de nodos de nivel 1 (con sus hijos nivel 2).
 */
public class ArbolProyecto {

    // "I" (ingresos) o "E" (egresos) -> lista de nivel 1
    private Map<String, List<NodoNivel1>> porIngEgr = new HashMap<>();

    public ArbolProyecto() {
        porIngEgr.put("I", new ArrayList<>());
        porIngEgr.put("E", new ArrayList<>());
    }

    public Map<String, List<NodoNivel1>> getPorIngEgr() {
        return porIngEgr;
    }

    public void setPorIngEgr(Map<String, List<NodoNivel1>> porIngEgr) {
        this.porIngEgr = porIngEgr;
    }
}
