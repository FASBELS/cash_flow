package com.dsi.spring.flujoreal.spring_cashflow.dao;

import java.util.List;
import com.dsi.spring.flujoreal.spring_cashflow.dto.PartidaDTO;

public interface PartidaDAO {
    List<PartidaDTO> listarPorProyecto(int codCia, int codPyto, int nroVersion);
    List<PartidaDTO> listarPartidasDesdeComprobantes(int codCia, int codPyto);
}