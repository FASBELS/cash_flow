// src/main/java/com/dsi/spring/flujoreal/spring_cashflow/dao/FlujoProyectadoDAO.java
package com.dsi.spring.flujoreal.spring_cashflow.dao;

import java.math.BigDecimal;
import java.util.Map;

/**
 * DAO para leer los montos del flujo de caja proyectado
 * desde DPROY_PARTIDA_MEZCLA / PROY_PARTIDA_MEZCLA.
 *
 * Separa por tipo:
 *  - IngEgr = 'I' para ingresos
 *  - IngEgr = 'E' para egresos
 *
 * Retorna:
 *  - mesesPorAnno: montos por mes (12 posiciones) agrupados por CodPartida
 *  - acumuladoAnterior: montos antes del año indicado agrupados por CodPartida
 */
public interface FlujoProyectadoDAO {

    Map<Integer, BigDecimal[]> mesesPorAnno(
            int codCia,
            int codPyto,
            int anno,
            String ingEgr
    ) throws Exception;

    Map<Integer, BigDecimal> acumuladoAnterior(
            int codCia,
            int codPyto,
            int anno,
            String ingEgr
    ) throws Exception;
}
