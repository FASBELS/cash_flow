// src/main/java/com/dsi/spring/flujoreal/spring_cashflow/dao/impl/EgresoRealDAOImpl.java
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
        SELECT d.CodPartida,
               SUM(CASE WHEN EXTRACT(MONTH FROM c.FecCP)=1  THEN d.ImpTotalMN ELSE 0 END) AS ene,
               SUM(CASE WHEN EXTRACT(MONTH FROM c.FecCP)=2  THEN d.ImpTotalMN ELSE 0 END) AS feb,
               SUM(CASE WHEN EXTRACT(MONTH FROM c.FecCP)=3  THEN d.ImpTotalMN ELSE 0 END) AS mar,
               SUM(CASE WHEN EXTRACT(MONTH FROM c.FecCP)=4  THEN d.ImpTotalMN ELSE 0 END) AS abr,
               SUM(CASE WHEN EXTRACT(MONTH FROM c.FecCP)=5  THEN d.ImpTotalMN ELSE 0 END) AS may,
               SUM(CASE WHEN EXTRACT(MONTH FROM c.FecCP)=6  THEN d.ImpTotalMN ELSE 0 END) AS jun,
               SUM(CASE WHEN EXTRACT(MONTH FROM c.FecCP)=7  THEN d.ImpTotalMN ELSE 0 END) AS jul,
               SUM(CASE WHEN EXTRACT(MONTH FROM c.FecCP)=8  THEN d.ImpTotalMN ELSE 0 END) AS ago,
               SUM(CASE WHEN EXTRACT(MONTH FROM c.FecCP)=9  THEN d.ImpTotalMN ELSE 0 END) AS sep,
               SUM(CASE WHEN EXTRACT(MONTH FROM c.FecCP)=10 THEN d.ImpTotalMN ELSE 0 END) AS oct,
               SUM(CASE WHEN EXTRACT(MONTH FROM c.FecCP)=11 THEN d.ImpTotalMN ELSE 0 END) AS nov,
               SUM(CASE WHEN EXTRACT(MONTH FROM c.FecCP)=12 THEN d.ImpTotalMN ELSE 0 END) AS dic
        FROM COMP_PAGOCAB c
        JOIN COMP_PAGODET d ON d.CodCia=c.CodCia AND d.NroCP=c.NroCP AND d.CodProveedor=c.CodProveedor
        JOIN PROY_PARTIDA pp ON pp.CodCia=c.CodCia AND pp.CodPyto=c.CodPyto
                             AND pp.IngEgr='E' AND pp.CodPartida=d.CodPartida
        WHERE c.CodCia=? AND c.CodPyto=? AND EXTRACT(YEAR FROM c.FecCP)=?
        GROUP BY d.CodPartida
        """;

    private static final String SQL_ACUM_ANT = """
        SELECT d.CodPartida, SUM(d.ImpTotalMN) AS acum
        FROM COMP_PAGOCAB c
        JOIN COMP_PAGODET d ON d.CodCia=c.CodCia AND d.NroCP=c.NroCP AND d.CodProveedor=c.CodProveedor
        JOIN PROY_PARTIDA pp ON pp.CodCia=c.CodCia AND pp.CodPyto=c.CodPyto
                             AND pp.IngEgr='E' AND pp.CodPartida=d.CodPartida
        WHERE c.CodCia=? AND c.CodPyto=? AND EXTRACT(YEAR FROM c.FecCP) < ?
        GROUP BY d.CodPartida
        """;

    @Override public Map<Integer, BigDecimal[]> mesesPorAnno(int cia, int pyto, int anno) throws Exception {
        Map<Integer, BigDecimal[]> out = new HashMap<>();
        try (Connection cn = DBConnection.getInstance().getConnection();
             PreparedStatement ps = cn.prepareStatement(SQL_MESES)) {
            ps.setInt(1, cia); ps.setInt(2, pyto); ps.setInt(3, anno);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    BigDecimal[] m = new BigDecimal[12];
                    for (int i=0;i<12;i++) m[i] = rs.getBigDecimal(i+2);
                    out.put(rs.getInt("CodPartida"), m);
                }
            }
        }
        return out;
    }

    @Override public Map<Integer, BigDecimal> acumuladoAnterior(int cia, int pyto, int anno) throws Exception {
        Map<Integer, BigDecimal> out = new HashMap<>();
        try (Connection cn = DBConnection.getInstance().getConnection();
             PreparedStatement ps = cn.prepareStatement(SQL_ACUM_ANT)) {
            ps.setInt(1, cia); ps.setInt(2, pyto); ps.setInt(3, anno);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) out.put(rs.getInt("CodPartida"), rs.getBigDecimal("acum"));
            }
        }
        return out;
    }
}
