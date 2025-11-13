package com.dsi.spring.flujoreal.spring_cashflow.service.impl;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.dsi.spring.flujoreal.spring_cashflow.dao.PartidaDAO;
import com.dsi.spring.flujoreal.spring_cashflow.dto.PartidaDTO;
import com.dsi.spring.flujoreal.spring_cashflow.factory.DAOFactory;
import com.dsi.spring.flujoreal.spring_cashflow.model.tree.ArbolProyecto;
import com.dsi.spring.flujoreal.spring_cashflow.model.tree.NodoNivel1;
import com.dsi.spring.flujoreal.spring_cashflow.model.tree.NodoNivel2;
import com.dsi.spring.flujoreal.spring_cashflow.service.PartidaService;
import com.dsi.spring.flujoreal.spring_cashflow.utils.PartidaHierarchyResolver;

public class PartidaServiceImpl implements PartidaService {

    private final DAOFactory factory = DAOFactory.getDAOFactory(DAOFactory.ORACLE);
    private final PartidaDAO dao = factory.getPartidaDAO();

    @Override
    public List<PartidaDTO> conceptosDeProyecto(int codCia, int codPyto, int nroVersion) {
        return dao.listarPorProyecto(codCia, codPyto, nroVersion);
    }

    @Override
    public ArbolProyecto buildArbolProyecto(int codCia, int codPyto, int nroVersion) {
        // 1️⃣ Obtener todas las partidas (niveles 1 y 2) del proyecto
        List<PartidaDTO> partidas = dao.listarPorProyecto(codCia, codPyto, nroVersion);
        ArbolProyecto arbol = new ArbolProyecto();

        if (partidas == null || partidas.isEmpty()) return arbol;

        // 2️⃣ Índices para buscar los padres por código
        Map<String, NodoNivel1> idxNivel1Str = new LinkedHashMap<>();
        Map<Integer, NodoNivel1> idxNivel1Num = new LinkedHashMap<>();

        // 3️⃣ Crear nodos de nivel 1 (ingresos/egresos)
        for (PartidaDTO p : partidas.stream()
                .filter(x -> x.getNivel() == 1)
                .sorted(Comparator.comparing(PartidaDTO::getCodPartida))
                .collect(Collectors.toList())) {

            NodoNivel1 n1 = new NodoNivel1(p);
            if (p.getCodPartidas() != null) idxNivel1Str.put(p.getCodPartidas(), n1);
            if (p.getCodPartida() != null)  idxNivel1Num.put(p.getCodPartida(),  n1);

            arbol.getPorIngEgr()
                 .computeIfAbsent(p.getIngEgr(), k -> new ArrayList<>())
                 .add(n1);
        }

        // 4️⃣ Vincular los nodos de nivel 2 con su nivel 1
        for (PartidaDTO p : partidas.stream()
                .filter(x -> x.getNivel() == 2)
                .sorted(Comparator.comparing(PartidaDTO::getCodPartida))
                .collect(Collectors.toList())) {

            var parentCodes = PartidaHierarchyResolver.deriveParent(p.getCodPartidas(), p.getCodPartida());
            NodoNivel1 padre = null;

            if (parentCodes.codPartidasPadreStr.isPresent()) {
                padre = idxNivel1Str.get(parentCodes.codPartidasPadreStr.get());
            } else if (parentCodes.codPartidaPadreNum.isPresent()) {
                padre = idxNivel1Num.get(parentCodes.codPartidaPadreNum.get());
            }

            if (padre != null) {
                padre.getHijos().add(new NodoNivel2(p));
            } else {
                NodoNivel1 suelto = new NodoNivel1(p);
                arbol.getPorIngEgr()
                    .computeIfAbsent(p.getIngEgr(), k -> new ArrayList<>())
                    .add(suelto);
            }
        }


        return arbol;
    }
}
