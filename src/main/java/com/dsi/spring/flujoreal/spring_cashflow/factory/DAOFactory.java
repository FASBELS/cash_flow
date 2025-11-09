package com.dsi.spring.flujoreal.spring_cashflow.factory;

import com.dsi.spring.flujoreal.spring_cashflow.dao.PartidaDAO;
import com.dsi.spring.flujoreal.spring_cashflow.dao.ProyectoDAO;
import com.dsi.spring.flujoreal.spring_cashflow.dao.impl.PartidaDAOImpl;
import com.dsi.spring.flujoreal.spring_cashflow.dao.impl.ProyectoDAOImpl;

/**
 * Fábrica Abstracta con soporte para creación directa (Factory Method)
 * y para subfábricas específicas (Abstract Factory).
 */
public abstract class DAOFactory {

    // --- Constantes para los tipos de base de datos ---
    public static final int ORACLE = 1;
    public static final int MYSQL = 2;
    public static final int POSTGRESQL = 3;

    // --- Método estático que elige la fábrica según el motor ---
    public static DAOFactory getDAOFactory(int tipoMotor) {
        switch (tipoMotor) {
            case ORACLE:
                return new OracleDAOFactory();
            // Aquí podrías agregar más en el futuro:
            // case MYSQL: return new MySQLDAOFactory();
            // case POSTGRESQL: return new PostgreSQLDAOFactory();
            default:
                throw new IllegalArgumentException("Motor de base de datos no soportado: " + tipoMotor);
        }
    }

    // --- Métodos estáticos directos (modo simplificado) ---
    // Usados por controladores que no dependen de una subfábrica.
    public static PartidaDAO createPartidaDAO() {
        return new PartidaDAOImpl();
    }

    public static ProyectoDAO createProyectoDAO() {
        return new ProyectoDAOImpl();
    }

    // --- Métodos abstractos (modo extensible con subfábricas) ---
    public abstract ProyectoDAO getProyectoDAO();
    public abstract PartidaDAO getPartidaDAO();
}
