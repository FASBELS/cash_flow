package com.dsi.spring.flujoreal.spring_cashflow.service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.dsi.spring.flujoreal.spring_cashflow.dao.PartidaDAO;
import com.dsi.spring.flujoreal.spring_cashflow.dto.PartidaDTO;
import com.dsi.spring.flujoreal.spring_cashflow.factory.DAOFactory;
import com.dsi.spring.flujoreal.spring_cashflow.tree.ArbolProyecto;
import com.dsi.spring.flujoreal.spring_cashflow.tree.NodoPartidaArbol;
import com.dsi.spring.flujoreal.spring_cashflow.utils.PartidaHierarchyResolver;

public class PartidaServiceProyectado {

    private final DAOFactory factory = DAOFactory.getDAOFactory(DAOFactory.ORACLE);
    private final PartidaDAO dao = factory.getPartidaDAO();

    private static final Comparator<PartidaDTO> ORDEN_COMPARATOR =
            Comparator.comparing(
                    (PartidaDTO p) -> p.getOrden(),
                    Comparator.nullsLast(Integer::compareTo)
            ).thenComparing(PartidaDTO::getCodPartida);

    public List<PartidaDTO> conceptosProyectados(int codCia, int codPyto, int nroVersion) {

        List<PartidaDTO> proyectadas = dao.listarPorProyecto(codCia, codPyto, nroVersion);

        proyectadas.forEach(p -> p.setNoProyectado(false));
        proyectadas.sort(ORDEN_COMPARATOR);

        return proyectadas;
    }

    public ArbolProyecto buildArbolProyecto(int codCia, int codPyto, int nroVersion) {

        List<PartidaDTO> partidas = conceptosProyectados(codCia, codPyto, nroVersion);
        ArbolProyecto arbol = new ArbolProyecto();

        if (partidas == null || partidas.isEmpty()) return arbol;

        List<PartidaDTO> sorted = partidas.stream()
                .sorted(ORDEN_COMPARATOR)
                .collect(Collectors.toList());

        // índices exactos igual al REAL
        Map<Integer, NodoPartidaArbol> idx = new LinkedHashMap<>();
        Map<String, NodoPartidaArbol> idxByCodPartidas = new LinkedHashMap<>();

        for (PartidaDTO p : sorted) {
            NodoPartidaArbol nodo = new NodoPartidaArbol(p);
            nodo.setNoProyectado(false);
            idx.put(p.getCodPartida(), nodo);

            String key = (p.getIngEgr() == null ? "" : p.getIngEgr())
                    + "|" +
                    (p.getCodPartidas() == null ? "" : p.getCodPartidas());
            idxByCodPartidas.put(key, nodo);
        }

        Set<Integer> attachedChildren = new HashSet<>();

        // ENLACE DE PADRES EXACTO AL REAL
        for (PartidaDTO p : sorted) {

            NodoPartidaArbol nodo = idx.get(p.getCodPartida());
            var parentCodes = PartidaHierarchyResolver.deriveParent(
                    p.getCodPartidas(),
                    p.getCodPartida()
            );

            boolean linked = false;

            // numérico
            if (parentCodes.codPartidaPadreNum.isPresent()) {
                Integer parentNum = parentCodes.codPartidaPadreNum.get();
                NodoPartidaArbol posiblePadre = idx.get(parentNum);

                if (posiblePadre != null
                        && posiblePadre.getPartida().getIngEgr().equals(p.getIngEgr())) {

                    posiblePadre.getHijos().add(nodo);
                    attachedChildren.add(p.getCodPartida());
                    linked = true;
                }
            }

            // textual
            if (!linked && parentCodes.codPartidasPadreStr.isPresent()) {
                String padreStr = parentCodes.codPartidasPadreStr.get();
                String key = p.getIngEgr() + "|" + padreStr;
                NodoPartidaArbol posiblePadre = idxByCodPartidas.get(key);

                if (posiblePadre != null) {
                    posiblePadre.getHijos().add(nodo);
                    attachedChildren.add(p.getCodPartida());
                }
            }
        }

        // raíces: igual al REAL
        for (PartidaDTO p : sorted) {
            if (!attachedChildren.contains(p.getCodPartida())) {
                arbol.getPorIngEgr()
                        .computeIfAbsent(p.getIngEgr(), k -> new ArrayList<>())
                        .add(idx.get(p.getCodPartida()));
            }
        }

        return arbol;
    }
}