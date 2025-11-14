package com.dsi.spring.flujoreal.spring_cashflow.dao;

import java.util.List;

import com.dsi.spring.flujoreal.spring_cashflow.dto.ProyectoDetalleDTO;

public interface ProyectoDAO {
    List<ProyectoDetalleDTO> listarProyectos() throws Exception;
}