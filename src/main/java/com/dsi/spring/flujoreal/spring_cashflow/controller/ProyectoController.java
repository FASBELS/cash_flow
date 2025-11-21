package com.dsi.spring.flujoreal.spring_cashflow.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.dsi.spring.flujoreal.spring_cashflow.dao.ProyectoDAO;
import com.dsi.spring.flujoreal.spring_cashflow.dto.ProyectoDetalleDTO;
import com.dsi.spring.flujoreal.spring_cashflow.factory.DAOFactory;

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

    // Nuevo: listar proyectos filtrando por compañía
    @GetMapping(params = "codCia")
    public ResponseEntity<?> listarProyectosPorCia(@RequestParam int codCia) {
        try {
            List<ProyectoDetalleDTO> proyectos = proyectoDAO.listarProyectosPorCia(codCia);
            return ResponseEntity.ok(proyectos);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity
                    .status(500)
                    .body(java.util.Map.of("error", e.getMessage()));
        }
    }


}
