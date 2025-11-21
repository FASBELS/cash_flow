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

    /**
     * Si tus tablas están en otro esquema (p.ej. DSI), pon aquí el owner:
     *  - Deja "" si están en el mismo usuario de conexión.
     *  - Ejemplo: SCHEMA = "DSI";
     *
     * También puedes definir -DDB_SCHEMA=DSI al ejecutar.
     */
    private static final String SCHEMA =
            System.getProperty("DB_SCHEMA", "").trim();

    private static String T(String table) {
        return (SCHEMA.isEmpty()) ? table : (SCHEMA + "." + table);
    }

    // SQL SOLO con PROY_PARTIDA (para armar el árbol de conceptos)
    // Ya no dependemos de PROY_PARTIDA_MEZCLA para nivel ni para orden.
    // ⬇️ IMPORTANTE: ya NO filtramos por pa.Nivel IN (1, 2) para poder traer niveles 3, 4, 5, ...
    private static final String SQL =
        "SELECT pr.IngEgr, " +
        "       pa.CodPartida, " +
        "       pa.CodPartidas, " +
        "       pa.DesPartida, " +
        "       pr.Nivel       AS Nivel, " +   // nivel del árbol viene directo de PROY_PARTIDA
        "       pa.Semilla, " +
        "       NULL           AS Orden " +   // ya no usamos ppm.Orden; se ordena por código
        "FROM " + T("PROY_PARTIDA") + " pr " +
        "JOIN " + T("PARTIDA") + " pa " +
        "  ON pa.CodCia     = pr.CodCia " +
        " AND pa.IngEgr     = pr.IngEgr " +
        " AND pa.CodPartida = pr.CodPartida " +
        "WHERE pr.CodCia      = ? " +
        "  AND pr.CodPyto     = ? " +
        "  AND pr.NroVersion  = ? " +
        "  AND pr.Vigente     = 'S' " +
        "  AND pa.Vigente     = 'S' " +
        "ORDER BY pr.IngEgr, " +
        "         pr.Nivel, " +
        "         pa.CodPartida";


    private static final String SQL_PARTIDAS_COMPROBANTES =
        "SELECT DISTINCT d.IngEgr, d.CodPartida, p.CodPartidas, p.DesPartida, p.Nivel, p.Semilla " +
        "FROM " + T("COMP_PAGODET") + " d " +
        "JOIN " + T("COMP_PAGOCAB") + " c " +
        "  ON c.CodCia = d.CodCia " +
        " AND c.NroCP = d.NroCP " +
        " AND c.CodProveedor = d.CodProveedor " +
        "JOIN " + T("PARTIDA") + " p " +
        "  ON p.CodCIA = d.CodCia " +
        " AND p.IngEgr = d.IngEgr " +
        " AND p.CodPartida = d.CodPartida " +
        "WHERE d.CodCia = ? " +
        "  AND c.CodPyto = ?";

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
                    dto.setCodPartidas(rs.getString("CodPartidas")); // necesario para derivar padre
                    dto.setDesPartida(rs.getString("DesPartida"));
                    dto.setNivel(rs.getInt("Nivel"));

                    int sem = rs.getInt("Semilla");
                    dto.setSemilla(rs.wasNull() ? null : sem);

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

    @Override
    public List<PartidaDTO> listarPartidasDesdeComprobantes(int codCia, int codPyto) {
        List<PartidaDTO> lista = new ArrayList<>();

        try (Connection cn = DBConnection.getInstance().getConnection();
             PreparedStatement ps = cn.prepareStatement(SQL_PARTIDAS_COMPROBANTES)) {

            ps.setInt(1, codCia);
            ps.setInt(2, codPyto);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    PartidaDTO dto = new PartidaDTO();
                    dto.setIngEgr(rs.getString("IngEgr"));
                    dto.setCodPartida(rs.getInt("CodPartida"));
                    dto.setCodPartidas(rs.getString("CodPartidas"));
                    dto.setDesPartida(rs.getString("DesPartida"));
                    dto.setNivel(rs.getInt("Nivel"));

                    int sem = rs.getInt("Semilla");
                    dto.setSemilla(rs.wasNull() ? null : sem);

                    dto.setNoProyectado(true); // 👈 CLAVE

                    lista.add(dto);
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return lista;
    }
}
