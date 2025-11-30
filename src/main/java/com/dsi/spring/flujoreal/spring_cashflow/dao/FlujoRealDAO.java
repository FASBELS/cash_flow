package com.dsi.spring.flujoreal.spring_cashflow.dao;

import java.util.List;
import java.util.Map;

import com.dsi.spring.flujoreal.spring_cashflow.dto.DetValoresDTO;
import com.dsi.spring.flujoreal.spring_cashflow.dto.FilaFlujoDTO;
import com.dsi.spring.flujoreal.spring_cashflow.dto.RegistroMesRealDTO;

public interface FlujoRealDAO {

    Map<Integer, DetValoresDTO> leerDetalleReal(int codCia, int codPyto, int anno) throws Exception;

    List<FilaFlujoDTO> conceptosProyecto(int codCia, int codPyto) throws Exception;

    FilaFlujoDTO obtenerInfoPartida(int codCia, int codPartida) throws Exception;

    void guardarMesReal(List<RegistroMesRealDTO> filas) throws Exception;
}
