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
import com.dsi.spring.flujoreal.spring_cashflow.model.Proyecto;

public class ProyectoDAOImpl implements ProyectoDAO {

    @Override
    public List<Proyecto> listarProyectos() throws Exception {
        String sql = """
            SELECT CodPyto, NombPyto, AnnoIni, AnnoFin, Vigente
            FROM PROYECTO
            WHERE Vigente = 'S'
            ORDER BY NombPyto
        """;

        List<Proyecto> out = new ArrayList<>();
        try (Connection cn = DBConnection.getInstance().getConnection();
             PreparedStatement ps = cn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                Proyecto p = new Proyecto();
                p.setCodPyto(rs.getInt("CodPyto"));
                p.setNombPyto(rs.getString("NombPyto"));
                p.setAnnoIni(rs.getInt("AnnoIni"));
                p.setAnnoFin(rs.getInt("AnnoFin"));
                p.setVigente(rs.getString("Vigente"));
                out.add(p);
            }
            return out;

        } catch (SQLException e) {
            throw new Exception("Error listarProyectos()", e);
        }
    }
}
