package com.dsi.spring.flujoreal.spring_cashflow.dao;

import java.util.List;
import java.util.Map;

import com.dsi.spring.flujoreal.spring_cashflow.dto.DetValoresDTO;
import com.dsi.spring.flujoreal.spring_cashflow.dto.FilaFlujoDTO;

public interface FlujoRealDAO {

    // Leer detalles reales desde FLUJOCAJA_DET
    Map<Integer, DetValoresDTO> leerDetalleReal(int codCia, int codPyto, int anno) throws Exception;

    // Estructura de conceptos (PROY_PARTIDA + PARTIDA)
    List<FilaFlujoDTO> conceptosProyecto(int codCia, int codPyto) throws Exception;

    // Info de una partida específica (para partidas no proyectadas)
    FilaFlujoDTO obtenerInfoPartida(int codCia, int codPartida) throws Exception;
}
