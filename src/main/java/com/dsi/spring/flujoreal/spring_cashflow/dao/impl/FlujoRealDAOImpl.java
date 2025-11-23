package com.dsi.spring.flujoreal.spring_cashflow.dao.impl;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.dsi.spring.flujoreal.spring_cashflow.config.DBConnection;
import com.dsi.spring.flujoreal.spring_cashflow.dao.FlujoRealDAO;
import com.dsi.spring.flujoreal.spring_cashflow.dto.DetValoresDTO;
import com.dsi.spring.flujoreal.spring_cashflow.dto.FilaFlujoDTO;
import com.dsi.spring.flujoreal.spring_cashflow.dto.MonthValues;

public class FlujoRealDAOImpl implements FlujoRealDAO {

    @Override
    public Map<Integer, DetValoresDTO> leerDetalleReal(int codCia, int codPyto, int anno) throws Exception {
        String sql = """
            SELECT d.CodPartida, d.ImpRealIni, d.ImpRealAcum,
                   d.ImpRealEne, d.ImpRealFeb, d.ImpRealMar, d.ImpRealAbr,
                   d.ImpRealMay, d.ImpRealJun, d.ImpRealJul, d.ImpRealAgo,
                   d.ImpRealSep, d.ImpRealOct, d.ImpRealNov, d.ImpRealDic
            FROM FLUJOCAJA_DET d
            WHERE d.Anno = ? AND d.CodCia = ? AND d.CodPyto = ?
            """;

        Map<Integer, DetValoresDTO> out = new HashMap<>();

        try (Connection cn = DBConnection.getInstance().getConnection();
             PreparedStatement ps = cn.prepareStatement(sql)) {

            ps.setInt(1, anno);
            ps.setInt(2, codCia);
            ps.setInt(3, codPyto);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    int codPartida = rs.getInt("CodPartida");
                    DetValoresDTO det = new DetValoresDTO();
                    det.impRealIni  = nvl(rs, "ImpRealIni");
                    det.impRealAcum = nvl(rs, "ImpRealAcum");

                    det.mes[0]  = nvl(rs, "ImpRealEne");
                    det.mes[1]  = nvl(rs, "ImpRealFeb");
                    det.mes[2]  = nvl(rs, "ImpRealMar");
                    det.mes[3]  = nvl(rs, "ImpRealAbr");
                    det.mes[4]  = nvl(rs, "ImpRealMay");
                    det.mes[5]  = nvl(rs, "ImpRealJun");
                    det.mes[6]  = nvl(rs, "ImpRealJul");
                    det.mes[7]  = nvl(rs, "ImpRealAgo");
                    det.mes[8]  = nvl(rs, "ImpRealSep");
                    det.mes[9]  = nvl(rs, "ImpRealOct");
                    det.mes[10] = nvl(rs, "ImpRealNov");
                    det.mes[11] = nvl(rs, "ImpRealDic");

                    out.put(codPartida, det);
                }
            }
        }
        return out;
    }

    private BigDecimal nvl(ResultSet rs, String col) throws SQLException {
        BigDecimal x = rs.getBigDecimal(col);
        return x != null ? x : BigDecimal.ZERO;
    }

    @Override
    public List<FilaFlujoDTO> conceptosProyecto(int codCia, int codPyto) throws Exception {

        String sql = """
            SELECT pp.IngEgr, pp.CodPartida, pa.DesPartida,
                   pp.Nivel, pp.CodPartidas
            FROM PROY_PARTIDA pp
            JOIN PARTIDA pa
              ON pa.CodCia     = pp.CodCia
             AND pa.IngEgr     = pp.IngEgr
             AND pa.CodPartida = pp.CodPartida
            WHERE pp.CodCia = ? AND pp.CodPyto = ?
            ORDER BY pp.IngEgr, pp.CodPartidas
            """;

        List<FilaFlujoDTO> out = new ArrayList<>();

        try (Connection cn = DBConnection.getInstance().getConnection();
             PreparedStatement ps = cn.prepareStatement(sql)) {

            ps.setInt(1, codCia);
            ps.setInt(2, codPyto);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    FilaFlujoDTO f = new FilaFlujoDTO();
                    f.ingEgr      = rs.getString("IngEgr");
                    f.codPartida  = rs.getInt("CodPartida");
                    f.desPartida  = rs.getString("DesPartida");
                    f.nivel       = rs.getInt("Nivel");
                    f.codPartidas = rs.getString("CodPartidas");
                    f.valores     = new MonthValues();
                    out.add(f);
                }
            }
        }

        return out;
    }

    @Override
    public FilaFlujoDTO obtenerInfoPartida(int codCia, int codPartida) throws Exception {

        String sql = """
            SELECT IngEgr, CodPartida, DesPartida, Nivel, CodPartidas
            FROM PARTIDA
            WHERE CodCia = ? AND CodPartida = ?
            """;

        try (Connection cn = DBConnection.getInstance().getConnection();
             PreparedStatement ps = cn.prepareStatement(sql)) {

            ps.setInt(1, codCia);
            ps.setInt(2, codPartida);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    FilaFlujoDTO f = new FilaFlujoDTO();
                    f.ingEgr      = rs.getString("IngEgr");
                    f.codPartida  = rs.getInt("CodPartida");
                    f.desPartida  = rs.getString("DesPartida");
                    f.nivel       = rs.getInt("Nivel");
                    f.codPartidas = rs.getString("CodPartidas");
                    f.valores     = new MonthValues();
                    return f;
                }
            }
        }

        return null;
    }
}
