package com.dsi.spring.flujoreal.spring_cashflow.service.impl;

import java.util.List;

import com.dsi.spring.flujoreal.spring_cashflow.dao.PartidaDAO;
import com.dsi.spring.flujoreal.spring_cashflow.dto.PartidaDTO;
import com.dsi.spring.flujoreal.spring_cashflow.factory.DAOFactory;
import com.dsi.spring.flujoreal.spring_cashflow.service.PartidaService;

public class PartidaServiceImpl implements PartidaService {

    private final DAOFactory factory = DAOFactory.getDAOFactory(DAOFactory.ORACLE);
    private final PartidaDAO dao = factory.getPartidaDAO();

    @Override
    public List<PartidaDTO> conceptosDeProyecto(int codCia, int codPyto, int nroVersion) {
        return dao.listarPorProyecto(codCia, codPyto, nroVersion);
    }
}
