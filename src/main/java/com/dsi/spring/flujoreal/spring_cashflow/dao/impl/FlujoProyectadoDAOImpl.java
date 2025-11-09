// src/main/java/com/dsi/spring/flujoreal/spring_cashflow/dao/impl/FlujoProyectadoDAOImpl.java
package com.dsi.spring.flujoreal.spring_cashflow.dao.impl;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.HashMap;
import java.util.Map;

import com.dsi.spring.flujoreal.spring_cashflow.config.DBConnection;
import com.dsi.spring.flujoreal.spring_cashflow.dao.FlujoProyectadoDAO;

public class FlujoProyectadoDAOImpl implements FlujoProyectadoDAO {

    /**
     * Montos por mes de desembolsos proyectados (ImpDesembTot) por partida,
     * para un año y tipo (IngEgr).
     *
     * Usa última NroVersion registrada para ese proyecto y tipo.
     */
    private static final String SQL_MESES = """
        SELECT d.CodPartida,
               SUM(CASE WHEN EXTRACT(MONTH FROM d.FecDesembolso)=1  THEN d.ImpDesembTot ELSE 0 END) AS ene,
               SUM(CASE WHEN EXTRACT(MONTH FROM d.FecDesembolso)=2  THEN d.ImpDesembTot ELSE 0 END) AS feb,
               SUM(CASE WHEN EXTRACT(MONTH FROM d.FecDesembolso)=3  THEN d.ImpDesembTot ELSE 0 END) AS mar,
               SUM(CASE WHEN EXTRACT(MONTH FROM d.FecDesembolso)=4  THEN d.ImpDesembTot ELSE 0 END) AS abr,
               SUM(CASE WHEN EXTRACT(MONTH FROM d.FecDesembolso)=5  THEN d.ImpDesembTot ELSE 0 END) AS may,
               SUM(CASE WHEN EXTRACT(MONTH FROM d.FecDesembolso)=6  THEN d.ImpDesembTot ELSE 0 END) AS jun,
               SUM(CASE WHEN EXTRACT(MONTH FROM d.FecDesembolso)=7  THEN d.ImpDesembTot ELSE 0 END) AS jul,
               SUM(CASE WHEN EXTRACT(MONTH FROM d.FecDesembolso)=8  THEN d.ImpDesembTot ELSE 0 END) AS ago,
               SUM(CASE WHEN EXTRACT(MONTH FROM d.FecDesembolso)=9  THEN d.ImpDesembTot ELSE 0 END) AS sep,
               SUM(CASE WHEN EXTRACT(MONTH FROM d.FecDesembolso)=10 THEN d.ImpDesembTot ELSE 0 END) AS oct,
               SUM(CASE WHEN EXTRACT(MONTH FROM d.FecDesembolso)=11 THEN d.ImpDesembTot ELSE 0 END) AS nov,
               SUM(CASE WHEN EXTRACT(MONTH FROM d.FecDesembolso)=12 THEN d.ImpDesembTot ELSE 0 END) AS dic
        FROM DPROY_PARTIDA_MEZCLA d
        JOIN PROY_PARTIDA_MEZCLA ppm
          ON ppm.CodCia      = d.CodCia
         AND ppm.CodPyto     = d.CodPyto
         AND ppm.IngEgr      = d.IngEgr
         AND ppm.NroVersion  = d.NroVersion
         AND ppm.CodPartida  = d.CodPartida
         AND ppm.Corr        = d.Corr
        WHERE d.CodCia    = ?
          AND d.CodPyto   = ?
          AND d.IngEgr    = ?
          AND d.NroVersion = (
                SELECT MAX(p2.NroVersion)
                FROM PROY_PARTIDA_MEZCLA p2
                WHERE p2.CodCia  = d.CodCia
                  AND p2.CodPyto = d.CodPyto
                  AND p2.IngEgr  = d.IngEgr
            )
          AND EXTRACT(YEAR FROM d.FecDesembolso) = ?
        GROUP BY d.CodPartida
        """;

    /**
     * Acumulado de desembolsos proyectados antes del año indicado,
     * por partida y tipo (IngEgr), usando última versión.
     */
    private static final String SQL_ACUM_ANT = """
        SELECT d.CodPartida,
               SUM(d.ImpDesembTot) AS acum
        FROM DPROY_PARTIDA_MEZCLA d
        JOIN PROY_PARTIDA_MEZCLA ppm
          ON ppm.CodCia      = d.CodCia
         AND ppm.CodPyto     = d.CodPyto
         AND ppm.IngEgr      = d.IngEgr
         AND ppm.NroVersion  = d.NroVersion
         AND ppm.CodPartida  = d.CodPartida
         AND ppm.Corr        = d.Corr
        WHERE d.CodCia    = ?
          AND d.CodPyto   = ?
          AND d.IngEgr    = ?
          AND d.NroVersion = (
                SELECT MAX(p2.NroVersion)
                FROM PROY_PARTIDA_MEZCLA p2
                WHERE p2.CodCia  = d.CodCia
                  AND p2.CodPyto = d.CodPyto
                  AND p2.IngEgr  = d.IngEgr
            )
          AND EXTRACT(YEAR FROM d.FecDesembolso) < ?
        GROUP BY d.CodPartida
        """;

    @Override
    public Map<Integer, BigDecimal[]> mesesPorAnno(
            int codCia,
            int codPyto,
            int anno,
            String ingEgr
    ) throws Exception {

        Map<Integer, BigDecimal[]> out = new HashMap<>();

        try (Connection cn = DBConnection.getInstance().getConnection();
             PreparedStatement ps = cn.prepareStatement(SQL_MESES)) {

            ps.setInt(1, codCia);
            ps.setInt(2, codPyto);
            ps.setString(3, ingEgr);
            ps.setInt(4, anno);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    BigDecimal[] m = new BigDecimal[12];
                    // columnas 2..13 = ene..dic
                    for (int i = 0; i < 12; i++) {
                        BigDecimal v = rs.getBigDecimal(i + 2);
                        m[i] = (v == null) ? BigDecimal.ZERO : v;
                    }
                    out.put(rs.getInt("CodPartida"), m);
                }
            }
        }

        return out;
    }

    @Override
    public Map<Integer, BigDecimal> acumuladoAnterior(
            int codCia,
            int codPyto,
            int anno,
            String ingEgr
    ) throws Exception {

        Map<Integer, BigDecimal> out = new HashMap<>();

        try (Connection cn = DBConnection.getInstance().getConnection();
             PreparedStatement ps = cn.prepareStatement(SQL_ACUM_ANT)) {

            ps.setInt(1, codCia);
            ps.setInt(2, codPyto);
            ps.setString(3, ingEgr);
            ps.setInt(4, anno);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    BigDecimal v = rs.getBigDecimal("acum");
                    out.put(rs.getInt("CodPartida"),
                            (v == null) ? BigDecimal.ZERO : v);
                }
            }
        }

        return out;
    }
}
