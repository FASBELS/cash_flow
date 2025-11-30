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
import com.dsi.spring.flujoreal.spring_cashflow.dto.RegistroMesRealDTO;

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

@Override
    public void guardarMesReal(List<RegistroMesRealDTO> filas) throws Exception {
        if (filas == null || filas.isEmpty()) {
            return;
        }

        RegistroMesRealDTO first = filas.get(0);

        final int codCia  = first.getCodCia();
        final int codPyto = first.getCodPyto();
        final int anno    = first.getAnno();
        final int mes     = first.getMes();

        // Validación básica: mismo contexto para todas las filas
        for (RegistroMesRealDTO f : filas) {
            if (f.getCodCia() != codCia ||
                f.getCodPyto() != codPyto ||
                f.getAnno() != anno ||
                f.getMes() != mes) {

                throw new IllegalArgumentException(
                    "Todas las filas deben ser del mismo codCia, codPyto, año y mes."
                );
            }
        }

        // Determinar la columna a actualizar según el mes
        final String colMes;
        switch (mes) {
            case 1:  colMes = "ImpRealEne"; break;
            case 2:  colMes = "ImpRealFeb"; break;
            case 3:  colMes = "ImpRealMar"; break;
            case 4:  colMes = "ImpRealAbr"; break;
            case 5:  colMes = "ImpRealMay"; break;
            case 6:  colMes = "ImpRealJun"; break;
            case 7:  colMes = "ImpRealJul"; break;
            case 8:  colMes = "ImpRealAgo"; break;
            case 9:  colMes = "ImpRealSep"; break;
            case 10: colMes = "ImpRealOct"; break;
            case 11: colMes = "ImpRealNov"; break;
            case 12: colMes = "ImpRealDic"; break;
            default:
                throw new IllegalArgumentException("Mes inválido: " + mes);
        }

        String sqlClear = "UPDATE FLUJOCAJA_DET " +
                "SET " + colMes + " = 0 " +
                "WHERE CodCIA = ? AND CodPyto = ? AND Anno = ? AND Tipo = 'R'";

        String sqlUpdate =
                "UPDATE FLUJOCAJA_DET " +
                "   SET " + colMes + " = ? " +
                " WHERE CodCIA = ? AND CodPyto = ? AND Anno = ? " +
                "   AND IngEgr = ? AND Tipo = ? AND CodPartida = ?";

        String sqlInsert =
                "INSERT INTO FLUJOCAJA_DET (" +
                " CodCIA, CodPyto, Anno, IngEgr, Tipo, CodPartida, Orden, " +
                " ImpRealIni, ImpRealAcum, " +
                " ImpRealEne, ImpRealFeb, ImpRealMar, ImpRealAbr, ImpRealMay, ImpRealJun, " +
                " ImpRealJul, ImpRealAgo, ImpRealSep, ImpRealOct, ImpRealNov, ImpRealDic" +
                ") VALUES (" +
                " ?, ?, ?, ?, ?, ?, ?, " +  // claves + orden
                " 0, 0, " +                  // ImpRealIni, ImpRealAcum
                " ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?" + // 12 meses
                ")";

        try (Connection cn = DBConnection.getInstance().getConnection()) {
            cn.setAutoCommit(false);

            // 1️⃣ Poner en 0 ese mes SOLO para ese proyecto/año/tipo R
            try (PreparedStatement psClear = cn.prepareStatement(sqlClear)) {
                psClear.setInt(1, codCia);
                psClear.setInt(2, codPyto);
                psClear.setInt(3, anno);
                psClear.executeUpdate();
            }

            // 2️⃣ Para cada partida de nivel 3: UPDATE, y si no existe -> INSERT
            try (PreparedStatement psUpdate = cn.prepareStatement(sqlUpdate);
                 PreparedStatement psInsert = cn.prepareStatement(sqlInsert)) {

                for (RegistroMesRealDTO f : filas) {
                    // UPDATE primero
                    psUpdate.setBigDecimal(1, f.getMonto());
                    psUpdate.setInt(2, f.getCodCia());
                    psUpdate.setInt(3, f.getCodPyto());
                    psUpdate.setInt(4, f.getAnno());
                    psUpdate.setString(5, f.getIngEgr());
                    psUpdate.setString(6, f.getTipo());
                    psUpdate.setInt(7, f.getCodPartida());

                    int updated = psUpdate.executeUpdate();
                    if (updated == 0) {
                        // INSERT si no existía
                        int idx = 1;
                        psInsert.setInt(idx++, f.getCodCia());
                        psInsert.setInt(idx++, f.getCodPyto());
                        psInsert.setInt(idx++, f.getAnno());
                        psInsert.setString(idx++, f.getIngEgr());
                        psInsert.setString(idx++, f.getTipo());
                        psInsert.setInt(idx++, f.getCodPartida());
                        psInsert.setInt(idx++, f.getOrden());

                        // ImpRealIni, ImpRealAcum -> 0 (ya puestos en el SQL)
                        // Asignar los 12 meses: sólo el mes pedido con monto, resto 0
                        for (int m = 1; m <= 12; m++) {
                            if (m == mes) {
                                psInsert.setBigDecimal(idx++, f.getMonto());
                            } else {
                                psInsert.setBigDecimal(idx++, BigDecimal.ZERO);
                            }
                        }

                        psInsert.executeUpdate();
                    }
                }
            }

            cn.commit();
        } catch (SQLException ex) {
            ex.printStackTrace();
            throw new Exception("Error al guardar mes real: " + ex.getMessage(), ex);
        }
    }



}
