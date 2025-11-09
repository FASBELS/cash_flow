package com.dsi.spring.flujoreal.spring_cashflow.controller;

import java.util.List;

import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.dsi.spring.flujoreal.spring_cashflow.dto.PartidaDTO;
import com.dsi.spring.flujoreal.spring_cashflow.service.PartidaService;
import com.dsi.spring.flujoreal.spring_cashflow.service.impl.PartidaServiceImpl;

@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/api")
public class PartidaController {

    private final PartidaService service = new PartidaServiceImpl();

    // MOD: codCia y nroVersion con defaultValue="1"
    @GetMapping("/conceptos")
    public List<PartidaDTO> conceptos(
            @RequestParam(defaultValue = "1") int codCia,     // MOD
            @RequestParam int codPyto,
            @RequestParam(defaultValue = "1") int nroVersion  // MOD
    ) {
        return service.conceptosDeProyecto(codCia, codPyto, nroVersion);
    }

    // (opcional) endpoint corto que asume 1/1 sin query params:
    // /api/proyectos/{codPyto}/conceptos
    @GetMapping("/proyectos/{codPyto}/conceptos")
    public List<PartidaDTO> conceptosPorProyecto(
            @PathVariable int codPyto
    ) {
        return service.conceptosDeProyecto(1, codPyto, 1);   // MOD: fijos
    }


}
