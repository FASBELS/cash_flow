package com.dsi.spring.flujoreal.spring_cashflow.dao;

import java.util.List;

import com.dsi.spring.flujoreal.spring_cashflow.model.Proyecto;

public interface ProyectoDAO {
    List<Proyecto> listarProyectos() throws Exception;
}