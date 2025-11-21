package com.dsi.spring.flujoreal.spring_cashflow.dao.impl;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import com.dsi.spring.flujoreal.spring_cashflow.config.DBConnection;
import com.dsi.spring.flujoreal.spring_cashflow.dao.CiaDAO;
import com.dsi.spring.flujoreal.spring_cashflow.dto.CiaDTO;

public class CiaDAOImpl implements CiaDAO {

    private static final String SQL_LISTAR_VIGENTES = """
        SELECT CODCIA, DESCIA, DESCORTA, VIGENTE
          FROM CIA
         WHERE VIGENTE = 'S'
         ORDER BY DESCORTA
        """;

    @Override
    public List<CiaDTO> listarCiasVigentes() throws Exception {
        List<CiaDTO> lista = new ArrayList<>();

        try (Connection cn = DBConnection.getInstance().getConnection();
             PreparedStatement ps = cn.prepareStatement(SQL_LISTAR_VIGENTES);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                CiaDTO dto = new CiaDTO();
                dto.setCodCia(rs.getInt("CODCIA"));
                dto.setDesCia(rs.getString("DESCIA"));
                dto.setDesCorta(rs.getString("DESCORTA"));
                dto.setVigente(rs.getString("VIGENTE"));
                lista.add(dto);
            }

        } catch (SQLException e) {
            throw new Exception("Error listarCiasVigentes()", e);
        }

        return lista;
    }
}
