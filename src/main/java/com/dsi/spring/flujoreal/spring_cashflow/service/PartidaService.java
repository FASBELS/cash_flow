package com.dsi.spring.flujoreal.spring_cashflow.service;

import java.util.List;

import com.dsi.spring.flujoreal.spring_cashflow.dto.PartidaDTO;
import com.dsi.spring.flujoreal.spring_cashflow.model.tree.ArbolProyecto;

/**
 * Servicio para operaciones relacionadas con las partidas de un proyecto.
 */
public interface PartidaService {

    /**
     * Devuelve la lista de partidas (conceptos) activas de un proyecto,
     * tanto de nivel 1 como de nivel 2.
     */
    List<PartidaDTO> conceptosDeProyecto(int codCia, int codPyto, int nroVersion);

    /**
     * Construye en memoria la jerarquía (árbol) de partidas
     * del proyecto, agrupando niveles 1 y 2 según su estructura jerárquica.
     */
    ArbolProyecto buildArbolProyecto(int codCia, int codPyto, int nroVersion);
}
