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
import com.dsi.spring.flujoreal.spring_cashflow.service.FlujoRealService;

@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/api/valores")
public class FlujoRealController {

    private final FlujoRealService service;

    public FlujoRealController(FlujoRealService service) {
        this.service = service;
    }

    // ============================================================
    // 1) Cargar TODO el año desde FLUJOCAJA_DET
    // ============================================================
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

    // ============================================================
    // 2) NUEVO: Cargar SOLO UN MES desde boletas
    // ============================================================
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

    // ============================================================
    // 3) Guardar valores reales del año visible
    // ============================================================
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
}
