package com.dsi.spring.flujoreal.spring_cashflow.tree;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Representa el árbol jerárquico de partidas del proyecto:
 * - Ingresos (I)
 * - Egresos (E)
 *
 * Ahora soporta niveles ilimitados usando NodoPartidaArbol,
 * por lo que ya no se usa NodoNivel1 / NodoNivel2.
 */
public class ArbolProyecto {

    /**
     * "I" (ingresos) o "E" (egresos) -> lista de nodos raíz.
     * Cada nodo raíz es un NodoPartidaArbol que puede tener hijos de nivel 2,
     * nivel 3, nivel 4, nivel 5, etc.
     */
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
