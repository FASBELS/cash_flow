package com.dsi.spring.flujoreal.spring_cashflow.dao;

import java.util.List;

import com.dsi.spring.flujoreal.spring_cashflow.dto.FlujoCajaDetSaveDTO;

public interface FlujoCajaDetDAO {

    void saveOrUpdate(FlujoCajaDetSaveDTO fila) throws Exception;

    default void saveOrUpdateBatch(List<FlujoCajaDetSaveDTO> filas) throws Exception {
        if (filas == null) return;
        for (FlujoCajaDetSaveDTO f : filas) {
            saveOrUpdate(f);
        }
    }
}
