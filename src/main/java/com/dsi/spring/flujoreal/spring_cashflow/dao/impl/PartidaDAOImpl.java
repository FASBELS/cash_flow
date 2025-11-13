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

    // SQL SIMPLE: sin MEZCLA, sin join por CodCia en PARTIDA (evita excluir filas)
    private static final String SQL =
        "SELECT pr.IngEgr, " +
        "       pa.CodPartida, " +
        "       pa.CodPartidas, " +
        "       pa.DesPartida, " +
        "       pr.Nivel, " +
        "       pa.Semilla, " +
        "       NULL AS Orden " +
        "FROM " + /* PROY_PARTIDA */  "" + T("PROY_PARTIDA") + " pr " +
        "JOIN " + /* PARTIDA */       "" + T("PARTIDA")      + " pa " +
        "  ON pa.IngEgr = pr.IngEgr " +                 // <- SIN pa.CodCia
        " AND pa.CodPartida = pr.CodPartida " +
        "WHERE pr.CodCia = ? " +
        "  AND pr.CodPyto = ? " +
        "  AND pr.NroVersion = ? " +
        "  AND pr.Vigente = 'S' " +
        "  AND pa.Vigente = 'S' " +
        "  AND pa.Nivel IN (1,2) " +
        "ORDER BY pr.IngEgr, pr.Nivel, pa.CodPartida";

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

                    int ord = rs.getInt("Orden");                  // aquí siempre será null (SQL SIMPLE)
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
