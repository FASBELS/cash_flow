// src/main/java/com/dsi/spring/flujoreal/spring_cashflow/dao/FlujoCajaDetDAO.java
package com.dsi.spring.flujoreal.spring_cashflow.dao;

import java.util.List;

import com.dsi.spring.flujoreal.spring_cashflow.dto.FlujoCajaDetProySaveDTO;      // flujo REAL
import com.dsi.spring.flujoreal.spring_cashflow.dto.FlujoCajaDetSaveDTO; // flujo PROYECTADO

public interface FlujoCajaDetDAO {

    // =======================
    // FLUJO REAL (ya lo tenías)
    // =======================
    void saveOrUpdate(FlujoCajaDetSaveDTO fila) throws Exception;

    default void saveOrUpdateBatch(List<FlujoCajaDetSaveDTO> filas) throws Exception {
        if (filas == null) return;
        for (FlujoCajaDetSaveDTO f : filas) {
            saveOrUpdate(f);
        }
    }

    // =======================
    // FLUJO PROYECTADO (NUEVO)
    // =======================
    void saveOrUpdateProyectado(FlujoCajaDetProySaveDTO fila) throws Exception;

    default void saveOrUpdateProyectadoBatch(List<FlujoCajaDetProySaveDTO> filas) throws Exception {
        if (filas == null) return;
        for (FlujoCajaDetProySaveDTO f : filas) {
            saveOrUpdateProyectado(f);
        }
    }
}
