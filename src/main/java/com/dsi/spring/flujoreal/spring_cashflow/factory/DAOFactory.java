package com.dsi.spring.flujoreal.spring_cashflow.factory;

import com.dsi.spring.flujoreal.spring_cashflow.dao.CiaDAO;
import com.dsi.spring.flujoreal.spring_cashflow.dao.PartidaDAO;
import com.dsi.spring.flujoreal.spring_cashflow.dao.ProyectoDAO;
import com.dsi.spring.flujoreal.spring_cashflow.dao.impl.CiaDAOImpl;
import com.dsi.spring.flujoreal.spring_cashflow.dao.impl.PartidaDAOImpl;
import com.dsi.spring.flujoreal.spring_cashflow.dao.impl.ProyectoDAOImpl;

public abstract class DAOFactory {

    public static final int ORACLE = 1;
    public static final int MYSQL = 2;
    public static final int POSTGRESQL = 3;

    public static DAOFactory getDAOFactory(int tipoMotor) {
        switch (tipoMotor) {
            case ORACLE:
                return new OracleDAOFactory();

            default:
                throw new IllegalArgumentException("Motor de base de datos no soportado: " + tipoMotor);
        }
    }


    public static PartidaDAO createPartidaDAO() {
        return new PartidaDAOImpl();
    }

    public static ProyectoDAO createProyectoDAO() {
        return new ProyectoDAOImpl();
    }
    public static CiaDAO createCiaDAO() {
    return new CiaDAOImpl();
    }

    public abstract CiaDAO getCiaDAO();


    public abstract ProyectoDAO getProyectoDAO();
    public abstract PartidaDAO getPartidaDAO();
    
}
