package com.dsi.spring.flujoreal.spring_cashflow.dao.impl;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.HashMap;
import java.util.Map;

import com.dsi.spring.flujoreal.spring_cashflow.config.DBConnection;
import com.dsi.spring.flujoreal.spring_cashflow.dao.EgresoRealDAO;

public class EgresoRealDAOImpl implements EgresoRealDAO {

    private static final String SQL_MESES = """
      -- PARTE 1: Valores directos
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
      FROM COMP_PAGOCAB c
      JOIN COMP_PAGODET d ON d.CodCia = c.CodCia AND d.NroCP = c.NroCP AND d.CodProveedor = c.CodProveedor
      WHERE c.CodCia = ? AND c.CodPyto = ? AND EXTRACT(YEAR FROM c.FecAbono) = ?
      GROUP BY d.CodPartida
      
      UNION ALL

      -- PARTE 2: Agregación recursiva
      SELECT p_padre.CodPartida,
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
      FROM COMP_PAGOCAB c
      JOIN COMP_PAGODET d ON d.CodCia = c.CodCia AND d.NroCP = c.NroCP AND d.CodProveedor = c.CodProveedor
      JOIN PARTIDA p_trans ON p_trans.CodCia = d.CodCia AND p_trans.IngEgr = d.IngEgr AND p_trans.CodPartida = d.CodPartida
      JOIN PARTIDA p_padre ON p_padre.CodCia = p_trans.CodCia AND p_padre.IngEgr = p_trans.IngEgr
      WHERE c.CodCia = ? AND c.CodPyto = ? AND EXTRACT(YEAR FROM c.FecAbono) = ?
        AND (
            (p_trans.Nivel = 3 AND p_padre.Nivel = 2 AND p_padre.CodPartidas = SUBSTR(p_trans.CodPartidas, 1, 10))
            OR
            (p_trans.Nivel = 3 AND p_padre.Nivel = 1 AND p_padre.CodPartidas = SUBSTR(p_trans.CodPartidas, 1, 7))
            OR
            (p_trans.Nivel = 2 AND p_padre.Nivel = 1 AND p_padre.CodPartidas = SUBSTR(p_trans.CodPartidas, 1, 7))
        )
      GROUP BY p_padre.CodPartida

      UNION ALL

        -- PARTE 3: Valores directos de empleados
        SELECT d.CodPartida,
            SUM(CASE WHEN EXTRACT(MONTH FROM e.FecAbono)=1  THEN d.ImpTotalMN ELSE 0 END) AS ene,
            SUM(CASE WHEN EXTRACT(MONTH FROM e.FecAbono)=2  THEN d.ImpTotalMN ELSE 0 END) AS feb,
            SUM(CASE WHEN EXTRACT(MONTH FROM e.FecAbono)=3  THEN d.ImpTotalMN ELSE 0 END) AS mar,
            SUM(CASE WHEN EXTRACT(MONTH FROM e.FecAbono)=4  THEN d.ImpTotalMN ELSE 0 END) AS abr,
            SUM(CASE WHEN EXTRACT(MONTH FROM e.FecAbono)=5  THEN d.ImpTotalMN ELSE 0 END) AS may,
            SUM(CASE WHEN EXTRACT(MONTH FROM e.FecAbono)=6  THEN d.ImpTotalMN ELSE 0 END) AS jun,
            SUM(CASE WHEN EXTRACT(MONTH FROM e.FecAbono)=7  THEN d.ImpTotalMN ELSE 0 END) AS jul,
            SUM(CASE WHEN EXTRACT(MONTH FROM e.FecAbono)=8  THEN d.ImpTotalMN ELSE 0 END) AS ago,
            SUM(CASE WHEN EXTRACT(MONTH FROM e.FecAbono)=9  THEN d.ImpTotalMN ELSE 0 END) AS sep,
            SUM(CASE WHEN EXTRACT(MONTH FROM e.FecAbono)=10 THEN d.ImpTotalMN ELSE 0 END) AS oct,
            SUM(CASE WHEN EXTRACT(MONTH FROM e.FecAbono)=11 THEN d.ImpTotalMN ELSE 0 END) AS nov,
            SUM(CASE WHEN EXTRACT(MONTH FROM e.FecAbono)=12 THEN d.ImpTotalMN ELSE 0 END) AS dic
        FROM COMP_PAGOEMPLEADO e
        JOIN COMP_PAGOEMPLEADO_DET d
        ON d.CodCia = e.CodCia
        AND d.CodEmpleado = e.CodEmpleado
        AND d.NroCP = e.NroCP
        WHERE e.CodCia = ? AND e.CodPyto = ? AND EXTRACT(YEAR FROM e.FecAbono) = ?
        GROUP BY d.CodPartida

        UNION ALL

        -- PARTE 4: Agregación recursiva para empleados
        SELECT p_padre.CodPartida,
            SUM(CASE WHEN EXTRACT(MONTH FROM e.FecAbono)=1  THEN d.ImpTotalMN ELSE 0 END) AS ene,
            SUM(CASE WHEN EXTRACT(MONTH FROM e.FecAbono)=2  THEN d.ImpTotalMN ELSE 0 END) AS feb,
            SUM(CASE WHEN EXTRACT(MONTH FROM e.FecAbono)=3  THEN d.ImpTotalMN ELSE 0 END) AS mar,
            SUM(CASE WHEN EXTRACT(MONTH FROM e.FecAbono)=4  THEN d.ImpTotalMN ELSE 0 END) AS abr,
            SUM(CASE WHEN EXTRACT(MONTH FROM e.FecAbono)=5  THEN d.ImpTotalMN ELSE 0 END) AS may,
            SUM(CASE WHEN EXTRACT(MONTH FROM e.FecAbono)=6  THEN d.ImpTotalMN ELSE 0 END) AS jun,
            SUM(CASE WHEN EXTRACT(MONTH FROM e.FecAbono)=7  THEN d.ImpTotalMN ELSE 0 END) AS jul,
            SUM(CASE WHEN EXTRACT(MONTH FROM e.FecAbono)=8  THEN d.ImpTotalMN ELSE 0 END) AS ago,
            SUM(CASE WHEN EXTRACT(MONTH FROM e.FecAbono)=9  THEN d.ImpTotalMN ELSE 0 END) AS sep,
            SUM(CASE WHEN EXTRACT(MONTH FROM e.FecAbono)=10 THEN d.ImpTotalMN ELSE 0 END) AS oct,
            SUM(CASE WHEN EXTRACT(MONTH FROM e.FecAbono)=11 THEN d.ImpTotalMN ELSE 0 END) AS nov,
            SUM(CASE WHEN EXTRACT(MONTH FROM e.FecAbono)=12 THEN d.ImpTotalMN ELSE 0 END) AS dic
        FROM COMP_PAGOEMPLEADO e
        JOIN COMP_PAGOEMPLEADO_DET d
        ON d.CodCia = e.CodCia
        AND d.CodEmpleado = e.CodEmpleado
        AND d.NroCP = e.NroCP
        JOIN PARTIDA p_trans
        ON p_trans.CodCia = d.CodCia
        AND p_trans.IngEgr = d.IngEgr
        AND p_trans.CodPartida = d.CodPartida
        JOIN PARTIDA p_padre
        ON p_padre.CodCia = p_trans.CodCia
        AND p_padre.IngEgr = p_trans.IngEgr
        WHERE e.CodCia = ? AND e.CodPyto = ? AND EXTRACT(YEAR FROM e.FecAbono) = ?
        AND (
                (p_trans.Nivel = 3 AND p_padre.Nivel = 2 AND p_padre.CodPartidas = SUBSTR(p_trans.CodPartidas, 1, 10))
                OR
                (p_trans.Nivel = 3 AND p_padre.Nivel = 1 AND p_padre.CodPartidas = SUBSTR(p_trans.CodPartidas, 1, 7))
                OR
                (p_trans.Nivel = 2 AND p_padre.Nivel = 1 AND p_padre.CodPartidas = SUBSTR(p_trans.CodPartidas, 1, 7))
            )
        GROUP BY p_padre.CodPartida
      """;

    private static final String SQL_ACUM_ANT = """
        SELECT d.CodPartida, SUM(d.ImpTotalMN) AS acum
        FROM COMP_PAGOCAB c
        JOIN COMP_PAGODET d ON d.CodCia = c.CodCia AND d.NroCP = c.NroCP AND d.CodProveedor = c.CodProveedor
        WHERE c.CodCia = ? AND c.CodPyto = ? AND EXTRACT(YEAR FROM c.FecAbono) < ?
        GROUP BY d.CodPartida

        UNION ALL

        SELECT p_padre.CodPartida, SUM(d.ImpTotalMN) AS acum
        FROM COMP_PAGOCAB c
        JOIN COMP_PAGODET d ON d.CodCia = c.CodCia AND d.NroCP = c.NroCP AND d.CodProveedor = c.CodProveedor
        JOIN PARTIDA p_trans ON p_trans.CodCia = d.CodCia AND p_trans.IngEgr = d.IngEgr AND p_trans.CodPartida = d.CodPartida
        JOIN PARTIDA p_padre ON p_padre.CodCia = p_trans.CodCia AND p_padre.IngEgr = p_trans.IngEgr
        WHERE c.CodCia = ? AND c.CodPyto = ? AND EXTRACT(YEAR FROM c.FecAbono) < ?
          AND (
              (p_trans.Nivel = 3 AND p_padre.Nivel = 2 AND p_padre.CodPartidas = SUBSTR(p_trans.CodPartidas, 1, 10))
              OR
              (p_trans.Nivel = 3 AND p_padre.Nivel = 1 AND p_padre.CodPartidas = SUBSTR(p_trans.CodPartidas, 1, 7))
              OR
              (p_trans.Nivel = 2 AND p_padre.Nivel = 1 AND p_padre.CodPartidas = SUBSTR(p_trans.CodPartidas, 1, 7))
          )
        GROUP BY p_padre.CodPartida

        UNION ALL

        -- EMPLEADOS: partidas finales
        SELECT d.CodPartida, SUM(d.ImpTotalMN) AS acum
        FROM COMP_PAGOEMPLEADO e
        JOIN COMP_PAGOEMPLEADO_DET d
          ON d.CodCia = e.CodCia
         AND d.CodEmpleado = e.CodEmpleado
         AND d.NroCP = e.NroCP
        WHERE e.CodCia = ? 
          AND e.CodPyto = ? 
          AND EXTRACT(YEAR FROM e.FecAbono) < ?
        GROUP BY d.CodPartida

        UNION ALL

        -- EMPLEADOS: sumar a padres
        SELECT p_padre.CodPartida, SUM(d.ImpTotalMN) AS acum
        FROM COMP_PAGOEMPLEADO e
        JOIN COMP_PAGOEMPLEADO_DET d
          ON d.CodCia = e.CodCia
         AND d.CodEmpleado = e.CodEmpleado
         AND d.NroCP = e.NroCP
        JOIN PARTIDA p_trans
          ON p_trans.CodCia = d.CodCia
         AND p_trans.IngEgr = d.IngEgr
         AND p_trans.CodPartida = d.CodPartida
        JOIN PARTIDA p_padre
          ON p_padre.CodCia = p_trans.CodCia
         AND p_padre.IngEgr = p_trans.IngEgr
        WHERE e.CodCia = ? 
          AND e.CodPyto = ? 
          AND EXTRACT(YEAR FROM e.FecAbono) < ?
          AND (
              (p_trans.Nivel = 3 AND p_padre.Nivel = 2 AND p_padre.CodPartidas = SUBSTR(p_trans.CodPartidas, 1, 10))
              OR
              (p_trans.Nivel = 3 AND p_padre.Nivel = 1 AND p_padre.CodPartidas = SUBSTR(p_trans.CodPartidas, 1, 7))
              OR
              (p_trans.Nivel = 2 AND p_padre.Nivel = 1 AND p_padre.CodPartidas = SUBSTR(p_trans.CodPartidas, 1, 7))
          )
        GROUP BY p_padre.CodPartida
        """;

    @Override
    public Map<Integer, BigDecimal[]> mesesPorAnno(int cia, int pyto, int anno) throws Exception {
        Map<Integer, BigDecimal[]> out = new HashMap<>();
        try (Connection cn = DBConnection.getInstance().getConnection();
            PreparedStatement ps = cn.prepareStatement(SQL_MESES)) {

            // PROVEEDOR – partidas finales
            ps.setInt(1, cia); 
            ps.setInt(2, pyto); 
            ps.setInt(3, anno);

            // PROVEEDOR – padres
            ps.setInt(4, cia); 
            ps.setInt(5, pyto); 
            ps.setInt(6, anno);

            // EMPLEADO – partidas finales
            ps.setInt(7, cia);
            ps.setInt(8, pyto);
            ps.setInt(9, anno);

            // EMPLEADO – padres
            ps.setInt(10, cia);
            ps.setInt(11, pyto);
            ps.setInt(12, anno);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {

                    int cod = rs.getInt("CodPartida");

                    BigDecimal[] m = new BigDecimal[12];
                    for (int i = 0; i < 12; i++) {
                        m[i] = rs.getBigDecimal(i + 2);
                        if (m[i] == null) m[i] = BigDecimal.ZERO;
                    }

                    if (out.containsKey(cod)) {
                        BigDecimal[] existing = out.get(cod);
                        for (int i = 0; i < 12; i++) {
                            existing[i] = existing[i].add(m[i]);
                        }
                    } else {
                        out.put(cod, m);
                    }
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

            // PROVEEDOR – partidas finales
            ps.setInt(1, cia); 
            ps.setInt(2, pyto); 
            ps.setInt(3, anno);

            // PROVEEDOR – padres
            ps.setInt(4, cia); 
            ps.setInt(5, pyto); 
            ps.setInt(6, anno);

            // EMPLEADO – partidas finales
            ps.setInt(7, cia); 
            ps.setInt(8, pyto); 
            ps.setInt(9, anno);

            // EMPLEADO – padres
            ps.setInt(10, cia); 
            ps.setInt(11, pyto); 
            ps.setInt(12, anno);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {

                    int cod = rs.getInt("CodPartida");
                    BigDecimal val = rs.getBigDecimal("acum");
                    if (val == null) val = BigDecimal.ZERO;

                    if (out.containsKey(cod)) {
                        out.put(cod, out.get(cod).add(val));
                    } else {
                        out.put(cod, val);
                    }
                }
            }
        }
        return out;
    }
}