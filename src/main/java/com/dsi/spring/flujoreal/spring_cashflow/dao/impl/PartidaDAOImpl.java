package com.dsi.spring.flujoreal.spring_cashflow.dao.impl;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import com.dsi.spring.flujoreal.spring_cashflow.config.DBConnection;
import com.dsi.spring.flujoreal.spring_cashflow.dao.PartidaDAO;
import com.dsi.spring.flujoreal.spring_cashflow.dto.PartidaDTO;

public class PartidaDAOImpl implements PartidaDAO {

    private static final String SQL =
        "SELECT pr.IngEgr, pa.CodPartida, pa.DesPartida, pr.Nivel, MIN(ppm.Orden) AS Orden " +
        "FROM PROY_PARTIDA pr " +
        "JOIN PARTIDA pa ON pa.CodCia = pr.CodCia AND pa.IngEgr = pr.IngEgr AND pa.CodPartida = pr.CodPartida " +
        "LEFT JOIN PROY_PARTIDA_MEZCLA ppm ON ppm.CodCia = pr.CodCia AND ppm.CodPyto = pr.CodPyto " +
        "  AND ppm.IngEgr = pr.IngEgr AND ppm.CodPartida = pr.CodPartida AND ppm.NroVersion = pr.NroVersion " +
        "WHERE pr.CodCia = ? AND pr.CodPyto = ? AND pr.NroVersion = ? " +
        "GROUP BY pr.IngEgr, pa.CodPartida, pa.DesPartida, pr.Nivel " +
        "ORDER BY pr.IngEgr, NVL(MIN(ppm.Orden), pa.CodPartida)";

    @Override
    public List<PartidaDTO> listarPorProyecto(int codCia, int codPyto, int nroVersion) {
        List<PartidaDTO> lista = new ArrayList<>();

        try (Connection cn = DBConnection.getInstance().getConnection();
             PreparedStatement ps = cn.prepareStatement(SQL)) {

            ps.setInt(1, codCia);
            ps.setInt(2, codPyto);
            ps.setInt(3, nroVersion);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    PartidaDTO dto = new PartidaDTO();
                    dto.setIngEgr(rs.getString("IngEgr"));
                    dto.setCodPartida(rs.getInt("CodPartida"));
                    dto.setDesPartida(rs.getString("DesPartida"));
                    dto.setNivel(rs.getInt("Nivel"));
                    int ord = rs.getInt("Orden");
                    dto.setOrden(rs.wasNull() ? null : ord);
                    lista.add(dto);
                }
            }

        } catch (SQLException e) {
            System.err.println("Error en PartidaDAOImpl.listarPorProyecto: " + e.getMessage());
            e.printStackTrace();
        }

        return lista;
    }
}
