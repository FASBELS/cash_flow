package com.dsi.spring.flujoreal.spring_cashflow.controller;

import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.dsi.spring.flujoreal.spring_cashflow.dao.CiaDAO;
import com.dsi.spring.flujoreal.spring_cashflow.dto.CiaDTO;
import com.dsi.spring.flujoreal.spring_cashflow.factory.DAOFactory;

@CrossOrigin(origins = "http://127.0.0.1:5501")
@RestController
@RequestMapping("/api/cias")
public class CiaController {

    private final CiaDAO ciaDAO;

    public CiaController() {
        this.ciaDAO = DAOFactory.createCiaDAO();
    }

    @GetMapping
    public ResponseEntity<?> listarCiasVigentes() {
        try {
            List<CiaDTO> cias = ciaDAO.listarCiasVigentes();
            return ResponseEntity.ok(cias);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity
                    .status(500)
                    .body(Map.of("error", e.getMessage()));
        }
    }
}
