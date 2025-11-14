package com.dsi.spring.flujoreal.spring_cashflow.controller;

import com.dsi.spring.flujoreal.spring_cashflow.factory.DAOFactory;
import com.dsi.spring.flujoreal.spring_cashflow.dao.ProyectoDAO;
import com.dsi.spring.flujoreal.spring_cashflow.dto.ProyectoDetalleDTO;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@CrossOrigin(origins = "http://127.0.0.1:5501")
@RestController
@RequestMapping("/api/proyectos")
public class ProyectoController {

    private final ProyectoDAO proyectoDAO;

    public ProyectoController() {
        this.proyectoDAO = DAOFactory.createProyectoDAO();
    }

    @GetMapping
    public ResponseEntity<?> listarProyectos() {
        try {
            List<ProyectoDetalleDTO> proyectos = proyectoDAO.listarProyectos();
            return ResponseEntity.ok(proyectos);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(java.util.Map.of("error", e.getMessage()));
        }
    }
}
