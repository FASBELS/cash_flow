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
import com.dsi.spring.flujoreal.spring_cashflow.tree.ArbolProyecto;

@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/api")
public class PartidaController {

    private final PartidaService service = new PartidaService();

    // 🔹 Lista plana de partidas (niveles 1 y 2)
    @GetMapping("/conceptos")
    public List<PartidaDTO> conceptos(
            @RequestParam(defaultValue = "1") int codCia,
            @RequestParam int codPyto,
            @RequestParam(defaultValue = "1") int nroVersion) {
        return service.conceptosDeProyecto(codCia, codPyto, nroVersion);
    }

    // 🔹 Endpoint corto: /api/proyectos/{codPyto}/conceptos
    @GetMapping("/proyectos/{codPyto}/conceptos")
    public List<PartidaDTO> conceptosPorProyecto(@PathVariable int codPyto) {
        return service.conceptosDeProyecto(1, codPyto, 1);
    }

    // 🔹 NUEVO: Árbol jerárquico Nivel 1 → Nivel 2 agrupado por Ingreso/Egreso
    @GetMapping("/proyectos/{codCia}/{codPyto}/{nroVersion}/arbol")
    public ArbolProyecto arbolProyecto(
            @PathVariable int codCia,
            @PathVariable int codPyto,
            @PathVariable int nroVersion) {
        return service.buildArbolProyecto(codCia, codPyto, nroVersion);
    }

    // (opcional) Endpoint más corto si quieres probar rápido
    // /api/proyectos/{codPyto}/arbol
    @GetMapping("/proyectos/{codPyto}/arbol")
    public ArbolProyecto arbolPorProyecto(@PathVariable int codPyto) {
        return service.buildArbolProyecto(1, codPyto, 1);
    }
}
