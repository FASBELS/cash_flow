package com.dsi.spring.flujoreal.spring_cashflow.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.dsi.spring.flujoreal.spring_cashflow.dto.FilaFlujoDTO;
import com.dsi.spring.flujoreal.spring_cashflow.dto.FlujoCajaDetSaveDTO;
import com.dsi.spring.flujoreal.spring_cashflow.dto.RealMesDTO;
import com.dsi.spring.flujoreal.spring_cashflow.dto.RegistroMesRealDTO;
import com.dsi.spring.flujoreal.spring_cashflow.service.FlujoRealService;

@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/api/valores")
public class FlujoRealController {

    private final FlujoRealService service;

    public FlujoRealController(FlujoRealService service) {
        this.service = service;
    }

    @GetMapping("/real")
    public ResponseEntity<?> real(
            @RequestParam int codCia,
            @RequestParam int codPyto,
            @RequestParam int anno
    ) {
        try {
            List<FilaFlujoDTO> data = service.obtener(codCia, codPyto, anno);
            return ResponseEntity.ok(data);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity
                    .status(500)
                    .body(java.util.Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/real/mes")
    public ResponseEntity<?> realMes(
            @RequestParam int codCia,
            @RequestParam int codPyto,
            @RequestParam int anno,
            @RequestParam int mes
    ) {
        try {
            List<RealMesDTO> data = service.obtenerMesDesdeBoletas(codCia, codPyto, anno, mes);
            return ResponseEntity.ok(data);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity
                    .status(500)
                    .body(java.util.Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/guardar")
    public ResponseEntity<?> guardar(@RequestBody List<FlujoCajaDetSaveDTO> filas) {
        try {
            service.guardar(filas);
            return ResponseEntity.ok(java.util.Map.of("mensaje", "Flujo real guardado correctamente"));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity
                    .status(500)
                    .body(java.util.Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/real/mes/guardar")
    public ResponseEntity<?> guardarMesReal(@RequestBody List<RegistroMesRealDTO> filas) {
        try {
            if (filas == null || filas.isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(java.util.Map.of("error", "No se recibieron filas para guardar."));
            }

            service.guardarMesReal(filas);
            return ResponseEntity.ok(java.util.Map.of(
                    "mensaje", "Mes refactorizado y guardado correctamente"
            ));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity
                    .status(500)
                    .body(java.util.Map.of("error", e.getMessage()));
        }
    }


}
