package com.dsi.spring.flujoreal.spring_cashflow.factory;

import com.dsi.spring.flujoreal.spring_cashflow.dao.PartidaDAO;
import com.dsi.spring.flujoreal.spring_cashflow.dao.ProyectoDAO;
import com.dsi.spring.flujoreal.spring_cashflow.dao.impl.PartidaDAOImpl;
import com.dsi.spring.flujoreal.spring_cashflow.dao.impl.ProyectoDAOImpl; 

public class OracleDAOFactory extends DAOFactory {

    @Override
    public ProyectoDAO getProyectoDAO() {
        return new ProyectoDAOImpl();
    }

    @Override
    public PartidaDAO getPartidaDAO() {
        return new PartidaDAOImpl();
    }

}
