// dao/impl/ProyectoDAOImpl.java
package com.dsi.spring.flujoreal.spring_cashflow.dao.impl;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import com.dsi.spring.flujoreal.spring_cashflow.config.DBConnection;
import com.dsi.spring.flujoreal.spring_cashflow.dao.ProyectoDAO;
import com.dsi.spring.flujoreal.spring_cashflow.dto.ProyectoDetalleDTO;

public class ProyectoDAOImpl implements ProyectoDAO {

    @Override
    public List<ProyectoDetalleDTO> listarProyectos() throws Exception {
        String sql = """
            SELECT CodPyto, NombPyto, AnnoIni, AnnoFin
            FROM PROYECTO
            WHERE Vigente = 'S'
            ORDER BY NombPyto
        """;

        List<ProyectoDetalleDTO> out = new ArrayList<>();
        try (Connection cn = DBConnection.getInstance().getConnection();
             PreparedStatement ps = cn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                ProyectoDetalleDTO p = new ProyectoDetalleDTO();
                p.setCodPyto(rs.getInt("CodPyto"));
                p.setNombPyto(rs.getString("NombPyto"));
                p.setAnnoIni(rs.getInt("AnnoIni"));
                p.setAnnoFin(rs.getInt("AnnoFin"));
                out.add(p);
            }
            return out;

        } catch (SQLException e) {
            throw new Exception("Error listarProyectos()", e);
        }
    }
    @Override
    
public List<ProyectoDetalleDTO> listarProyectosPorCia(int codCia) throws Exception {

    String sql = """
        SELECT CodCia, CodPyto, NombPyto, AnnoIni, AnnoFin
        FROM PROYECTO
        WHERE Vigente = 'S'
          AND CodCia = ?
        ORDER BY NombPyto
    """;

    List<ProyectoDetalleDTO> out = new ArrayList<>();

    try (Connection cn = DBConnection.getInstance().getConnection();
         PreparedStatement ps = cn.prepareStatement(sql)) {

        ps.setInt(1, codCia);

        try (ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                ProyectoDetalleDTO p = new ProyectoDetalleDTO();

                // 🔥 IMPORTANTE: ahora sí seteamos CodCia
                p.setCodCia(rs.getInt("CodCia"));

                p.setCodPyto(rs.getInt("CodPyto"));
                p.setNombPyto(rs.getString("NombPyto"));
                p.setAnnoIni(rs.getInt("AnnoIni"));
                p.setAnnoFin(rs.getInt("AnnoFin"));

                out.add(p);
            }
        }

        return out;

    } catch (SQLException e) {
        throw new Exception("Error listarProyectosPorCia()", e);
    }
}

}
