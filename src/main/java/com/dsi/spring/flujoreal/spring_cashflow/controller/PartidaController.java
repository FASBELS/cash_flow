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
import com.dsi.spring.flujoreal.spring_cashflow.service.PartidaServiceProyectado;
import com.dsi.spring.flujoreal.spring_cashflow.tree.ArbolProyecto;

@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/api")
public class PartidaController {

    private final PartidaService service = new PartidaService();
    private final PartidaServiceProyectado serviceProy = new PartidaServiceProyectado();


    // ======================================================
    //                     FLUJO REAL
    // ======================================================

    @GetMapping("/conceptos")
    public List<PartidaDTO> conceptos(
            @RequestParam(defaultValue = "1") int codCia,
            @RequestParam int codPyto,
            @RequestParam(defaultValue = "1") int nroVersion) {

        return service.conceptosDeProyecto(codCia, codPyto, nroVersion);
    }

    @GetMapping("/proyectos/{codPyto}/conceptos")
    public List<PartidaDTO> conceptosPorProyecto(@PathVariable int codPyto) {
        return service.conceptosDeProyecto(1, codPyto, 1);
    }

    @GetMapping("/proyectos/{codCia}/{codPyto}/{nroVersion}/arbol")
    public ArbolProyecto arbolProyecto(
            @PathVariable int codCia,
            @PathVariable int codPyto,
            @PathVariable int nroVersion) {

        return service.buildArbolProyecto(codCia, codPyto, nroVersion);
    }

    @GetMapping("/proyectos/{codPyto}/arbol")
    public ArbolProyecto arbolProyectoDefault(@PathVariable int codPyto) {
        return service.buildArbolProyecto(1, codPyto, 1);
    }


    // ======================================================
    //                   FLUJO PROYECTADO
    // ======================================================

    @GetMapping("/conceptos-proyectados")
    public List<PartidaDTO> conceptosProyectados(
            @RequestParam(defaultValue = "1") int codCia,
            @RequestParam int codPyto,
            @RequestParam(defaultValue = "1") int nroVersion) {

        return serviceProy.conceptosProyectados(codCia, codPyto, nroVersion);
    }

    @GetMapping("/proyectos/{codPyto}/conceptos-proyectados")
    public List<PartidaDTO> conceptosProyectadosPorProyecto(@PathVariable int codPyto) {
        return serviceProy.conceptosProyectados(1, codPyto, 1);
    }

    @GetMapping("/proyectos/{codCia}/{codPyto}/{nroVersion}/arbol-proyectado")
    public ArbolProyecto arbolProyectoProyectado(
            @PathVariable int codCia,
            @PathVariable int codPyto,
            @PathVariable int nroVersion) {

        return serviceProy.buildArbolProyecto(codCia, codPyto, nroVersion);
    }

    @GetMapping("/proyectos/{codPyto}/arbol-proyectado")
    public ArbolProyecto arbolProyectoProyectadoDefault(@PathVariable int codPyto) {
        return serviceProy.buildArbolProyecto(1, codPyto, 1);
    }
}
