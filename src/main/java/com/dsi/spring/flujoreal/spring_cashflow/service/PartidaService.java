package com.dsi.spring.flujoreal.spring_cashflow.service;

import java.util.List;

import com.dsi.spring.flujoreal.spring_cashflow.dto.PartidaDTO;

public interface PartidaService {
    List<PartidaDTO> conceptosDeProyecto(int codCia, int codPyto, int nroVersion);
}
