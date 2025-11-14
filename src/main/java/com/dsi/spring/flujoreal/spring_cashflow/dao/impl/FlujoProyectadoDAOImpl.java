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

    private static final String SQL_MESES = """
        SELECT CodPartida,
               SUM(IMPENE) AS ene,
               SUM(IMPFEB) AS feb,
               SUM(IMPMAR) AS mar,
               SUM(IMPABR) AS abr,
               SUM(IMPMAY) AS may,
               SUM(IMPJUN) AS jun,
               SUM(IMPJUL) AS jul,
               SUM(IMPAGO) AS ago,
               SUM(IMPSEP) AS sep,
               SUM(IMPOCT) AS oct,
               SUM(IMPNOV) AS nov,
               SUM(IMPDIC) AS dic
        FROM FLUJOCAJA_DET
        WHERE CodCia = ?
          AND CodPyto = ?
          AND IngEgr = ?
          AND Tipo = 'M'
          AND Anno = ?
        GROUP BY CodPartida
        """;

    private static final String SQL_ACUM_ANT = """
        SELECT CodPartida,
               SUM(IMPINI) AS acum
        FROM FLUJOCAJA_DET
        WHERE CodCia = ?
          AND CodPyto = ?
          AND IngEgr = ?
          AND Tipo = 'M'
          AND Anno < ?
        GROUP BY CodPartida
        """;

    @Override
    public Map<Integer, BigDecimal[]> mesesPorAnno(
            int codCia, int codPyto, int anno, String ingEgr) throws Exception {

        Map<Integer, BigDecimal[]> out = new HashMap<>();

        try (Connection cn = DBConnection.getInstance().getConnection();
             PreparedStatement ps = cn.prepareStatement(SQL_MESES)) {

            ps.setInt(1, codCia);
            ps.setInt(2, codPyto);
            ps.setString(3, ingEgr);
            ps.setInt(4, anno);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    BigDecimal[] meses = new BigDecimal[12];
                    for (int i = 0; i < 12; i++) {
                        BigDecimal v = rs.getBigDecimal(i + 2);
                        meses[i] = (v == null) ? BigDecimal.ZERO : v;
                    }
                    out.put(rs.getInt("CodPartida"), meses);
                }
            }
        }

        return out;
    }

    @Override
    public Map<Integer, BigDecimal> acumuladoAnterior(
            int codCia, int codPyto, int anno, String ingEgr) throws Exception {

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
