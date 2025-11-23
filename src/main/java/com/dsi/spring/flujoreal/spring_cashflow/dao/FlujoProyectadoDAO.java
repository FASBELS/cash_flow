package com.dsi.spring.flujoreal.spring_cashflow.dao;

import java.math.BigDecimal;
import java.util.Map;


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
