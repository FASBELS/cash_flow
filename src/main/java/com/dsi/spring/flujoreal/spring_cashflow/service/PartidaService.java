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

    // Comparator para respetar el ORDEN del proyecto
    private static final Comparator<PartidaDTO> ORDEN_COMPARATOR =
            Comparator.comparing(
                    (PartidaDTO p) -> p.getOrden(),
                    Comparator.nullsLast(Integer::compareTo)
            ).thenComparing(PartidaDTO::getCodPartida);

    /**
     * Obtiene la lista de partidas:
     * - Partidas presupuestadas del proyecto
     * - Partidas reales desde comprobantes (si no están presupuestadas)
     */
    public List<PartidaDTO> conceptosDeProyecto(int codCia, int codPyto, int nroVersion) {

        // 1) Partidas del proyecto (presupuestadas)
        List<PartidaDTO> proyectadas = dao.listarPorProyecto(codCia, codPyto, nroVersion);

        // Índice para saber si una partida ya estaba presupuestada
        Map<Integer, PartidaDTO> idx = proyectadas.stream()
                .collect(Collectors.toMap(PartidaDTO::getCodPartida, p -> p));

        // 2) Partidas reales
        List<PartidaDTO> desdeComprobantes =
                ((PartidaDAOImpl) dao).listarPartidasDesdeComprobantes(codCia, codPyto);

        // 3) Filtrar las no proyectadas
        List<PartidaDTO> noProyectadas = desdeComprobantes.stream()
                .filter(p -> !idx.containsKey(p.getCodPartida()))
                .collect(Collectors.toList());

        // 4) Combinar ambas
        List<PartidaDTO> finalList = new ArrayList<>();
        finalList.addAll(proyectadas);
        finalList.addAll(noProyectadas);

        return finalList;
    }

    /**
     * Construye un árbol multi-nivel usando NodoPartidaArbol
     * Soporta niveles 1,2,3,4,5... infinitos
     */
public ArbolProyecto buildArbolProyecto(int codCia, int codPyto, int nroVersion) {

    List<PartidaDTO> partidas = dao.listarPorProyecto(codCia, codPyto, nroVersion);

    ArbolProyecto arbol = new ArbolProyecto();
    if (partidas == null || partidas.isEmpty()) return arbol;

    // Ordenar por orden mezcla
    partidas = partidas.stream()
            .sorted(ORDEN_COMPARATOR)
            .collect(Collectors.toList());

    // Índice numérico
    Map<Integer, NodoPartidaArbol> idx = new LinkedHashMap<>();

    // Índice textual (codPartidas: "ING-001-01", etc.)
    Map<String, NodoPartidaArbol> idxPorCodPartidas = new LinkedHashMap<>();

    // --- 1) Crear nodos sueltos
    for (PartidaDTO p : partidas) {
        NodoPartidaArbol nodo = new NodoPartidaArbol(p);
        nodo.setNoProyectado(p.isNoProyectado());

        idx.put(p.getCodPartida(), nodo);

        if (p.getCodPartidas() != null && !p.getCodPartidas().isBlank()) {
            idxPorCodPartidas.put(p.getCodPartidas(), nodo);
        }
    }

    // --- 2) Enlazar jerarquía padre-hijo usando numérico o textual
    for (PartidaDTO p : partidas) {

        NodoPartidaArbol nodo = idx.get(p.getCodPartida());

        var parentCodes = PartidaHierarchyResolver.deriveParent(
                p.getCodPartidas(),
                p.getCodPartida()
        );

        NodoPartidaArbol padre = null;

        // a) Intentar por parentNum (padre numérico)
        if (parentCodes.codPartidaPadreNum.isPresent()) {
            padre = idx.get(parentCodes.codPartidaPadreNum.get());
        }

        // b) Si no tiene padre numérico, usar codPartidasPadreStr
        if (padre == null && parentCodes.codPartidasPadreStr.isPresent()) {
            padre = idxPorCodPartidas.get(parentCodes.codPartidasPadreStr.get());
        }

        // Enlazar si lo encontramos
        if (padre != null) padre.getHijos().add(nodo);
    }

    // --- 3) Raíces: las partidas que NO tienen padre (ni numérico ni textual)
// 3️⃣ Extraer raíces (nivel 1) y agregarlas al árbol por Ing/Egr
for (PartidaDTO p : partidas) {
    var parentCodes = PartidaHierarchyResolver.deriveParent(
            p.getCodPartidas(),
            p.getCodPartida()
    );

    NodoPartidaArbol posiblePadre = null;

    // Intentar encontrar padre numérico
    if (parentCodes.codPartidaPadreNum.isPresent()) {
        posiblePadre = idx.get(parentCodes.codPartidaPadreNum.get());
    }

    // Si no hay padre numérico, intentamos por texto (codPartidas)
    if (posiblePadre == null && parentCodes.codPartidasPadreStr.isPresent()) {
        posiblePadre = idxPorCodPartidas.get(parentCodes.codPartidasPadreStr.get());
    }

    // Si NO encontramos padre en ninguno de los dos índices, es raíz
    if (posiblePadre == null) {
        arbol.getPorIngEgr()
                .computeIfAbsent(p.getIngEgr(), k -> new ArrayList<>())
                .add(idx.get(p.getCodPartida()));
    }
}


    return arbol;
}

}
