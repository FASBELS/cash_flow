package com.dsi.spring.flujoreal.spring_cashflow.dao.impl;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.StringJoiner;

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

        // 1) Traer partidas del PROYECTO (normalmente nivel 3)
        ps.setInt(1, codCia);
        ps.setInt(2, codPyto);
        ps.setInt(3, nroVersion);

        try (ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                PartidaDTO dto = new PartidaDTO();
                dto.setIngEgr(rs.getString("IngEgr"));
                dto.setCodPartida(rs.getInt("CodPartida"));
                dto.setCodPartidas(rs.getString("CodPartidas")); // ej: ING-101, ING-1101
                dto.setDesPartida(rs.getString("DesPartida"));
                dto.setNivel(rs.getInt("Nivel")); // en PROY_PARTIDA será 3

                int sem = rs.getInt("Semilla");
                dto.setSemilla(rs.wasNull() ? null : sem);

                int ord = rs.getInt("Orden");
                dto.setOrden(rs.wasNull() ? null : ord);

                lista.add(dto);
            }
        }

        // 🔹 Nos quedamos con una copia de SOLO las que vienen de PROY_PARTIDA
        List<PartidaDTO> soloNivel3 = new ArrayList<>(lista);

        // 2) A partir de esas, calcular los códigos de sus padres (nivel 2 y 1)
        //    usando la regla de tu hoja:
        //    - Nivel 3 → centena: 101..199 → 100, 1101..1199 → 1100
        //    - Nivel 2 → grupo: 100,200,... → grupo = /100, índiceNivel1 = grupo-1 (000..010)
        Set<String> codPadres = new HashSet<>();

        for (PartidaDTO dto : soloNivel3) {
            String cod = dto.getCodPartidas(); // p.ej. "ING-101", "ING-1101"
            if (cod == null) continue;

            int idxGuion = cod.indexOf('-');
            if (idxGuion < 0 || idxGuion == cod.length() - 1) {
                continue; // formato inesperado
            }

            String prefijo = cod.substring(0, idxGuion + 1);     // "ING-"
            String numStr  = cod.substring(idxGuion + 1);        // "101", "1101"

            int num;
            try {
                num = Integer.parseInt(numStr);
            } catch (NumberFormatException ex) {
                continue; // número raro, lo ignoramos
            }

            // 👉 Padre de NIVEL 3 → NIVEL 2: centena
            int padre2Num = (num / 100) * 100; // 101 → 100, 1101 → 1100
            if (padre2Num >= 100) {
                String codPadre2 = prefijo + String.format("%03d", padre2Num);
                codPadres.add(codPadre2);

                // 👉 Padre de ese NIVEL 2 → NIVEL 1
                int grupo = padre2Num / 100;    // 100→1, 200→2, 1100→11
                int indiceNivel1 = grupo - 1;   // 0..10

                if (indiceNivel1 >= 0) {
                    String codPadre1 = prefijo + String.format("%03d", indiceNivel1);
                    codPadres.add(codPadre1);   // ej: ING-000, ING-001, ..., ING-010
                }
            }
        }

        // 3) Si hay padres, los traemos desde PARTIDA en UN SOLO SELECT
        if (!codPadres.isEmpty()) {

            StringBuilder sb = new StringBuilder();
            sb.append("SELECT pa.IngEgr, pa.CodPartida, pa.CodPartidas, ")
              .append("       pa.DesPartida, pa.Nivel, pa.Semilla, ")
              .append("       NULL AS Orden ")
              .append("FROM ").append(T("PARTIDA")).append(" pa ")
              .append("WHERE pa.CodCia = ? ")
              .append("  AND pa.Vigente = 'S' ")
              .append("  AND pa.CodPartidas IN (");

            // tantos ? como códigos de padres tengamos
            StringJoiner sj = new StringJoiner(",");
            int totalPadres = codPadres.size();
            for (int i = 0; i < totalPadres; i++) {
                sj.add("?");
            }
            sb.append(sj.toString()).append(")");

            String sqlPadres = sb.toString();

            try (PreparedStatement psPadres = cn.prepareStatement(sqlPadres)) {
                int paramIndex = 1;
                psPadres.setInt(paramIndex++, codCia);

                for (String cp : codPadres) {
                    psPadres.setString(paramIndex++, cp);
                }

                try (ResultSet rs2 = psPadres.executeQuery()) {
                    while (rs2.next()) {
                        PartidaDTO dto = new PartidaDTO();
                        dto.setIngEgr(rs2.getString("IngEgr"));
                        dto.setCodPartida(rs2.getInt("CodPartida"));
                        dto.setCodPartidas(rs2.getString("CodPartidas"));
                        dto.setDesPartida(rs2.getString("DesPartida"));
                        dto.setNivel(rs2.getInt("Nivel"));

                        int sem = rs2.getInt("Semilla");
                        dto.setSemilla(rs2.wasNull() ? null : sem);

                        dto.setOrden(null); // los padres no usan Orden

                        lista.add(dto);
                    }
                }
            }
        }

        // 4) Ordenamos TODO para que el árbol salga consistente:
        //    primero por tipo (I/E), luego nivel (1,2,3), luego código
        lista.sort(
            Comparator.comparing(PartidaDTO::getIngEgr)
                      .thenComparing(PartidaDTO::getNivel)
                      .thenComparing(PartidaDTO::getCodPartidas)
        );

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