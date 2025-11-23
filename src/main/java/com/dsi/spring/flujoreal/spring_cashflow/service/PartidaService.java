package com.dsi.spring.flujoreal.spring_cashflow.service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.dsi.spring.flujoreal.spring_cashflow.dao.PartidaDAO;
import com.dsi.spring.flujoreal.spring_cashflow.dao.impl.PartidaDAOImpl;
import com.dsi.spring.flujoreal.spring_cashflow.dto.PartidaDTO;
import com.dsi.spring.flujoreal.spring_cashflow.factory.DAOFactory;
import com.dsi.spring.flujoreal.spring_cashflow.tree.ArbolProyecto;
import com.dsi.spring.flujoreal.spring_cashflow.tree.NodoPartidaArbol;
import com.dsi.spring.flujoreal.spring_cashflow.utils.PartidaHierarchyResolver;

public class PartidaService {

    private final DAOFactory factory = DAOFactory.getDAOFactory(DAOFactory.ORACLE);
    private final PartidaDAO dao = factory.getPartidaDAO();

    private static final Comparator<PartidaDTO> ORDEN_COMPARATOR =
            Comparator.comparing(
                    (PartidaDTO p) -> p.getOrden(),
                    Comparator.nullsLast(Integer::compareTo)
            ).thenComparing(PartidaDTO::getCodPartida);


    public List<PartidaDTO> conceptosDeProyecto(int codCia, int codPyto, int nroVersion) {

        List<PartidaDTO> proyectadas = dao.listarPorProyecto(codCia, codPyto, nroVersion);
        Map<Integer, PartidaDTO> idx = proyectadas.stream()
                .collect(Collectors.toMap(PartidaDTO::getCodPartida, p -> p));

        List<PartidaDTO> desdeComprobantes =
                ((PartidaDAOImpl) dao).listarPartidasDesdeComprobantes(codCia, codPyto);

        List<PartidaDTO> noProyectadas = desdeComprobantes.stream()
                .filter(p -> !idx.containsKey(p.getCodPartida()))
                .collect(Collectors.toList());

        List<PartidaDTO> finalList = new ArrayList<>();
        finalList.addAll(proyectadas);
        finalList.addAll(noProyectadas);

        return finalList;
    }


public ArbolProyecto buildArbolProyecto(int codCia, int codPyto, int nroVersion) {

    List<PartidaDTO> partidas = dao.listarPorProyecto(codCia, codPyto, nroVersion);

    ArbolProyecto arbol = new ArbolProyecto();
    if (partidas == null || partidas.isEmpty()) return arbol;

    partidas = partidas.stream()
            .sorted(ORDEN_COMPARATOR)
            .collect(Collectors.toList());

    Map<Integer, NodoPartidaArbol> idx = new LinkedHashMap<>();


    Map<String, NodoPartidaArbol> idxPorCodPartidas = new LinkedHashMap<>();

    for (PartidaDTO p : partidas) {
        NodoPartidaArbol nodo = new NodoPartidaArbol(p);
        nodo.setNoProyectado(p.isNoProyectado());

        idx.put(p.getCodPartida(), nodo);

        if (p.getCodPartidas() != null && !p.getCodPartidas().isBlank()) {
            idxPorCodPartidas.put(p.getCodPartidas(), nodo);
        }
    }


    for (PartidaDTO p : partidas) {

        NodoPartidaArbol nodo = idx.get(p.getCodPartida());

        var parentCodes = PartidaHierarchyResolver.deriveParent(
                p.getCodPartidas(),
                p.getCodPartida()
        );

        NodoPartidaArbol padre = null;

        if (parentCodes.codPartidaPadreNum.isPresent()) {
            padre = idx.get(parentCodes.codPartidaPadreNum.get());
        }

        if (padre == null && parentCodes.codPartidasPadreStr.isPresent()) {
            padre = idxPorCodPartidas.get(parentCodes.codPartidasPadreStr.get());
        }

        if (padre != null) padre.getHijos().add(nodo);
    }


for (PartidaDTO p : partidas) {
    var parentCodes = PartidaHierarchyResolver.deriveParent(
            p.getCodPartidas(),
            p.getCodPartida()
    );

    NodoPartidaArbol posiblePadre = null;

    if (parentCodes.codPartidaPadreNum.isPresent()) {
        posiblePadre = idx.get(parentCodes.codPartidaPadreNum.get());
    }

    if (posiblePadre == null && parentCodes.codPartidasPadreStr.isPresent()) {
        posiblePadre = idxPorCodPartidas.get(parentCodes.codPartidasPadreStr.get());
    }

    if (posiblePadre == null) {
        arbol.getPorIngEgr()
                .computeIfAbsent(p.getIngEgr(), k -> new ArrayList<>())
                .add(idx.get(p.getCodPartida()));
    }
}


    return arbol;
}

}
