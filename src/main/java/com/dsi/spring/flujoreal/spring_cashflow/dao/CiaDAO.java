package com.dsi.spring.flujoreal.spring_cashflow.dao;

import java.util.List;

import com.dsi.spring.flujoreal.spring_cashflow.dto.CiaDTO;

public interface CiaDAO {

    /**
     * Lista todas las compañías vigentes (VIGENTE = 'S').
     */
    List<CiaDTO> listarCiasVigentes() throws Exception;

}
