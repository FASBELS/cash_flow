package com.dsi.spring.flujoreal.spring_cashflow.dao.impl;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.HashMap;
import java.util.Map;

import com.dsi.spring.flujoreal.spring_cashflow.config.DBConnection;
import com.dsi.spring.flujoreal.spring_cashflow.dao.IngresoRealDAO;

public class IngresoRealDAOImpl implements IngresoRealDAO {

    // Usa FecAbono para ubicar en mes/año (flujo de caja real)
    private static final String SQL_MESES = """
        -- Nivel 3 (Hijos)
        SELECT d.CodPartida,
               SUM(CASE WHEN EXTRACT(MONTH FROM c.FecAbono)=1  THEN d.ImpTotalMN ELSE 0 END) AS ene,
               SUM(CASE WHEN EXTRACT(MONTH FROM c.FecAbono)=2  THEN d.ImpTotalMN ELSE 0 END) AS feb,
               SUM(CASE WHEN EXTRACT(MONTH FROM c.FecAbono)=3  THEN d.ImpTotalMN ELSE 0 END) AS mar,
               SUM(CASE WHEN EXTRACT(MONTH FROM c.FecAbono)=4  THEN d.ImpTotalMN ELSE 0 END) AS abr,
               SUM(CASE WHEN EXTRACT(MONTH FROM c.FecAbono)=5  THEN d.ImpTotalMN ELSE 0 END) AS may,
               SUM(CASE WHEN EXTRACT(MONTH FROM c.FecAbono)=6  THEN d.ImpTotalMN ELSE 0 END) AS jun,
               SUM(CASE WHEN EXTRACT(MONTH FROM c.FecAbono)=7  THEN d.ImpTotalMN ELSE 0 END) AS jul,
               SUM(CASE WHEN EXTRACT(MONTH FROM c.FecAbono)=8  THEN d.ImpTotalMN ELSE 0 END) AS ago,
               SUM(CASE WHEN EXTRACT(MONTH FROM c.FecAbono)=9  THEN d.ImpTotalMN ELSE 0 END) AS sep,
               SUM(CASE WHEN EXTRACT(MONTH FROM c.FecAbono)=10 THEN d.ImpTotalMN ELSE 0 END) AS oct,
               SUM(CASE WHEN EXTRACT(MONTH FROM c.FecAbono)=11 THEN d.ImpTotalMN ELSE 0 END) AS nov,
               SUM(CASE WHEN EXTRACT(MONTH FROM c.FecAbono)=12 THEN d.ImpTotalMN ELSE 0 END) AS dic
        FROM VTACOMP_PAGOCAB c
        JOIN VTACOMP_PAGODET d
          ON d.CodCia = c.CodCia
         AND d.NroCP  = c.NroCP
        WHERE c.CodCia = ?
          AND c.CodPyto = ?
          AND EXTRACT(YEAR FROM c.FecAbono) = ?
        GROUP BY d.CodPartida

        UNION ALL

        -- Nivel 2 (Padres) - Calculados desde los hijos
        SELECT pm.CodPartida AS CodPartida,
               SUM(CASE WHEN EXTRACT(MONTH FROM c.FecAbono)=1  THEN d.ImpTotalMN ELSE 0 END) AS ene,
               SUM(CASE WHEN EXTRACT(MONTH FROM c.FecAbono)=2  THEN d.ImpTotalMN ELSE 0 END) AS feb,
               SUM(CASE WHEN EXTRACT(MONTH FROM c.FecAbono)=3  THEN d.ImpTotalMN ELSE 0 END) AS mar,
               SUM(CASE WHEN EXTRACT(MONTH FROM c.FecAbono)=4  THEN d.ImpTotalMN ELSE 0 END) AS abr,
               SUM(CASE WHEN EXTRACT(MONTH FROM c.FecAbono)=5  THEN d.ImpTotalMN ELSE 0 END) AS may,
               SUM(CASE WHEN EXTRACT(MONTH FROM c.FecAbono)=6  THEN d.ImpTotalMN ELSE 0 END) AS jun,
               SUM(CASE WHEN EXTRACT(MONTH FROM c.FecAbono)=7  THEN d.ImpTotalMN ELSE 0 END) AS jul,
               SUM(CASE WHEN EXTRACT(MONTH FROM c.FecAbono)=8  THEN d.ImpTotalMN ELSE 0 END) AS ago,
               SUM(CASE WHEN EXTRACT(MONTH FROM c.FecAbono)=9  THEN d.ImpTotalMN ELSE 0 END) AS sep,
               SUM(CASE WHEN EXTRACT(MONTH FROM c.FecAbono)=10 THEN d.ImpTotalMN ELSE 0 END) AS oct,
               SUM(CASE WHEN EXTRACT(MONTH FROM c.FecAbono)=11 THEN d.ImpTotalMN ELSE 0 END) AS nov,
               SUM(CASE WHEN EXTRACT(MONTH FROM c.FecAbono)=12 THEN d.ImpTotalMN ELSE 0 END) AS dic
        FROM VTACOMP_PAGOCAB c
        JOIN VTACOMP_PAGODET d
          ON d.CodCia = c.CodCia
         AND d.NroCP  = c.NroCP
        JOIN PARTIDA_MEZCLA pm
          ON d.CodCia     = pm.CodCia
         AND d.IngEgr     = pm.IngEgr
         AND d.CodPartida = pm.PadCodPartida
        WHERE c.CodCia = ?
          AND c.CodPyto = ?
          AND EXTRACT(YEAR FROM c.FecAbono) = ?
        GROUP BY pm.CodPartida
        """;

    // También usa FecAbono para el acumulado de años anteriores
    private static final String SQL_ACUM_ANT = """
        -- Nivel 3 (Hijos)
        SELECT d.CodPartida,
               SUM(d.ImpTotalMN) AS acum
        FROM VTACOMP_PAGOCAB c
        JOIN VTACOMP_PAGODET d
          ON d.CodCia = c.CodCia
         AND d.NroCP  = c.NroCP
        WHERE c.CodCia = ?
          AND c.CodPyto = ?
          AND EXTRACT(YEAR FROM c.FecAbono) < ?
        GROUP BY d.CodPartida

        UNION ALL

        -- Nivel 2 (Padres)
        SELECT pm.CodPartida AS CodPartida,
               SUM(d.ImpTotalMN) AS acum
        FROM VTACOMP_PAGOCAB c
        JOIN VTACOMP_PAGODET d
          ON d.CodCia = c.CodCia
         AND d.NroCP  = c.NroCP
        JOIN PARTIDA_MEZCLA pm
          ON d.CodCia     = pm.CodCia
         AND d.IngEgr     = pm.IngEgr
         AND d.CodPartida = pm.PadCodPartida
        WHERE c.CodCia = ?
          AND c.CodPyto = ?
          AND EXTRACT(YEAR FROM c.FecAbono) < ?
        GROUP BY pm.CodPartida
        """;

    @Override
    public Map<Integer, BigDecimal[]> mesesPorAnno(int cia, int pyto, int anno) throws Exception {
        Map<Integer, BigDecimal[]> out = new HashMap<>();
        try (Connection cn = DBConnection.getInstance().getConnection();
             PreparedStatement ps = cn.prepareStatement(SQL_MESES)) {

            // Parte hijos
            ps.setInt(1, cia);
            ps.setInt(2, pyto);
            ps.setInt(3, anno);
            // Parte padres
            ps.setInt(4, cia);
            ps.setInt(5, pyto);
            ps.setInt(6, anno);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    BigDecimal[] m = new BigDecimal[12];
                    for (int i = 0; i < 12; i++) {
                        m[i] = rs.getBigDecimal(i + 2); // columnas ene..dic
                    }
                    out.put(rs.getInt("CodPartida"), m);
                }
            }
        }
        return out;
    }

    @Override
    public Map<Integer, BigDecimal> acumuladoAnterior(int cia, int pyto, int anno) throws Exception {
        Map<Integer, BigDecimal> out = new HashMap<>();
        try (Connection cn = DBConnection.getInstance().getConnection();
             PreparedStatement ps = cn.prepareStatement(SQL_ACUM_ANT)) {

            // Parte hijos
            ps.setInt(1, cia);
            ps.setInt(2, pyto);
            ps.setInt(3, anno);
            // Parte padres
            ps.setInt(4, cia);
            ps.setInt(5, pyto);
            ps.setInt(6, anno);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    out.put(rs.getInt("CodPartida"), rs.getBigDecimal("acum"));
                }
            }
        }
        return out;
    }
}
