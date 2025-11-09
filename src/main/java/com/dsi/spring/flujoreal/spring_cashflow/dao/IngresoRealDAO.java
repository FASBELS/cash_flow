// src/main/java/com/dsi/spring/flujoreal/spring_cashflow/dao/IngresoRealDAO.java
package com.dsi.spring.flujoreal.spring_cashflow.dao;

import java.math.BigDecimal;
import java.util.Map;

public interface IngresoRealDAO {
    Map<Integer, BigDecimal[]> mesesPorAnno(int codCia, int codPyto, int anno) throws Exception;
    Map<Integer, BigDecimal>   acumuladoAnterior(int codCia, int codPyto, int anno) throws Exception;
}
