// src/main/java/com/dsi/spring/flujoreal/spring_cashflow/controller/FlujoProyectadoController.java
package com.dsi.spring.flujoreal.spring_cashflow.controller;

import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;          // 👈 NUEVO
import org.springframework.web.bind.annotation.RequestBody;     // 👈 NUEVO
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.dsi.spring.flujoreal.spring_cashflow.dto.FilaFlujoDTO;
import com.dsi.spring.flujoreal.spring_cashflow.dto.FlujoCajaDetProySaveDTO; // 👈 NUEVO
import com.dsi.spring.flujoreal.spring_cashflow.service.FlujoProyectadoService;

@RestController
@RequestMapping("/api/flujo-proyectado")
@CrossOrigin(origins = "*") // si tu otro controller usa algo distinto, copia ese mismo config
public class FlujoProyectadoController {

    private final FlujoProyectadoService flujoProyectadoService;

    public FlujoProyectadoController(FlujoProyectadoService flujoProyectadoService) {
        this.flujoProyectadoService = flujoProyectadoService;
    }

    /**
     * Endpoint para obtener los valores del Flujo de Caja Proyectado.
     *
     * Parámetros:
     *  - codCia
     *  - codPyto
     *  - anno
     *
     * Retorna:
     *  - Lista de FilaFlujoDTO con:
     *      * filas de ingresos
     *      * filas de egresos
     *      * última fila: FLUJO DE CAJA NETO PROYECTADO
     */
    @GetMapping("/valores")
    public ResponseEntity<?> obtenerFlujoProyectado(
            @RequestParam("codCia") int codCia,
            @RequestParam("codPyto") int codPyto,
            @RequestParam("anno") int anno
    ) {
        try {
            List<FilaFlujoDTO> filas =
                    flujoProyectadoService.obtenerFlujoProyectado(codCia, codPyto, anno);

            return ResponseEntity.ok(filas);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity
                    .status(500)
                    .body(Map.of(
                            "error", "Error al obtener el flujo de caja proyectado",
                            "detalle", e.getMessage()
                    ));
        }
    }

    /**
     * 🔹 NUEVO:
     * Guarda las filas del flujo de caja proyectado que vienen del frontend.
     * Deben corresponder al año que el usuario tiene en pantalla.
     */
    @PostMapping("/guardar")
    public ResponseEntity<?> guardarFlujoProyectado(
            @RequestBody List<FlujoCajaDetProySaveDTO> filas
    ) {
        try {
            flujoProyectadoService.guardar(filas);
            return ResponseEntity.ok(
                    Map.of("mensaje", "Flujo de caja proyectado guardado correctamente")
            );
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity
                    .status(500)
                    .body(Map.of(
                            "error", "Error al guardar el flujo de caja proyectado",
                            "detalle", e.getMessage()
                    ));
        }
    }
}
