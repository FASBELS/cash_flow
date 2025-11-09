// src/main/java/com/dsi/spring/flujoreal/spring_cashflow/service/FlujoProyectadoService.java
package com.dsi.spring.flujoreal.spring_cashflow.service;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.dsi.spring.flujoreal.spring_cashflow.config.DBConnection;
import com.dsi.spring.flujoreal.spring_cashflow.dao.FlujoProyectadoDAO;
import com.dsi.spring.flujoreal.spring_cashflow.dao.impl.FlujoProyectadoDAOImpl;
import com.dsi.spring.flujoreal.spring_cashflow.dto.FilaFlujoDTO;
import com.dsi.spring.flujoreal.spring_cashflow.dto.MonthValues;



@Service
public class FlujoProyectadoService {

    // DAO específico para leer los montos proyectados (lo creamos en el siguiente paso)
    private final FlujoProyectadoDAO proyectadoDAO = new FlujoProyectadoDAOImpl();

    /**
     * Devuelve las filas del Flujo de Caja Proyectado para un proyecto y año.
     * Formato compatible con FlujoRealService:
     *  - Una fila por partida (I/E)
     *  - 12 meses, suma, acumulado anterior, total
     *  - Última fila: FLUJO DE CAJA NETO (proyectado)
     */
    public List<FilaFlujoDTO> obtenerFlujoProyectado(int codCia, int codPyto, int anno) throws Exception {

        // 1) Obtener lista base de conceptos del proyecto (IngEgr + CodPartida + DesPartida)
        List<FilaFlujoDTO> filas = conceptosProyectoProyectado(codCia, codPyto);

        if (filas.isEmpty()) {
            return filas;
        }

        // 2) Obtener montos proyectados por meses y acumulado anterior (desde DAO)
        //    El DAO ya agrupa por CodPartida usando DPROY_PARTIDA_MEZCLA.
        Map<Integer, BigDecimal[]> ingMeses = proyectadoDAO.mesesPorAnno(codCia, codPyto, anno, "I");
        Map<Integer, BigDecimal[]> egrMeses = proyectadoDAO.mesesPorAnno(codCia, codPyto, anno, "E");

        Map<Integer, BigDecimal> ingAcumAnt = proyectadoDAO.acumuladoAnterior(codCia, codPyto, anno, "I");
        Map<Integer, BigDecimal> egrAcumAnt = proyectadoDAO.acumuladoAnterior(codCia, codPyto, anno, "E");

        // 3) Construir valores por fila y calcular el neto
        BigDecimal[] netoMes = new BigDecimal[12];
        Arrays.fill(netoMes, BigDecimal.ZERO);
        BigDecimal netoAcumAnt = BigDecimal.ZERO;

        for (FilaFlujoDTO f : filas) {
            f.valores = new MonthValues();

            Map<Integer, BigDecimal[]> sourceMeses;
            Map<Integer, BigDecimal> sourceAcum;

            if ("I".equals(f.ingEgr)) {
                sourceMeses = ingMeses;
                sourceAcum  = ingAcumAnt;
            } else if ("E".equals(f.ingEgr)) {
                sourceMeses = egrMeses;
                sourceAcum  = egrAcumAnt;
            } else {
                // Por si hubiera algo raro, lo dejamos en cero
                continue;
            }

            BigDecimal[] meses = sourceMeses.get(f.codPartida);
            if (meses == null) {
                meses = new BigDecimal[12];
            }

            for (int i = 0; i < 12; i++) {
                BigDecimal v = (meses[i] == null) ? BigDecimal.ZERO : meses[i];
                f.valores.mes[i] = v;
                f.valores.suma = f.valores.suma.add(v);

                if ("I".equals(f.ingEgr)) {
                    netoMes[i] = netoMes[i].add(v);
                } else {
                    netoMes[i] = netoMes[i].subtract(v);
                }
            }

            BigDecimal acumAnt = sourceAcum.getOrDefault(f.codPartida, BigDecimal.ZERO);
            f.valores.acumAnt = acumAnt;
            f.valores.total   = f.valores.suma.add(acumAnt);

            if ("I".equals(f.ingEgr)) {
                netoAcumAnt = netoAcumAnt.add(acumAnt);
            } else {
                netoAcumAnt = netoAcumAnt.subtract(acumAnt);
            }
        }

        // 4) Fila de FLUJO DE CAJA NETO PROYECTADO
        FilaFlujoDTO neto = new FilaFlujoDTO();
        neto.codPartida = 0;
        neto.desPartida = "FLUJO DE CAJA NETO PROYECTADO";
        neto.ingEgr = "N";
        neto.valores = new MonthValues();
        for (int i = 0; i < 12; i++) {
            neto.valores.mes[i] = netoMes[i];
            neto.valores.suma = neto.valores.suma.add(netoMes[i]);
        }
        neto.valores.acumAnt = netoAcumAnt;
        neto.valores.total   = neto.valores.suma.add(netoAcumAnt);

        // 5) Ordenar: primero ingresos, luego egresos, al final el neto
        List<FilaFlujoDTO> salida = new ArrayList<>();
        filas.stream().filter(f -> "I".equals(f.ingEgr)).forEach(salida::add);
        filas.stream().filter(f -> "E".equals(f.ingEgr)).forEach(salida::add);
        salida.add(neto);

        return salida;
    }
    
    /**
     * Obtiene los conceptos del proyecto (solo estructura):
     *  - Usa PROY_PARTIDA + PARTIDA
     *  - Toma la última versión vigente del proyecto
     *  - Devuelve filas con IngEgr, CodPartida, DesPartida (sin montos)
     *
     * Esta lógica copia el enfoque de FlujoRealService pero sobre tablas PROY_*
     */
    private List<FilaFlujoDTO> conceptosProyectoProyectado(int codCia, int codPyto) throws Exception {
        List<FilaFlujoDTO> lista = new ArrayList<>();

        String sql = """
            SELECT pp.IngEgr,
                   pp.CodPartida,
                   pa.DesPartida
            FROM PROY_PARTIDA pp
            JOIN PARTIDA pa
              ON pa.CodPartida = pp.CodPartida
            WHERE pp.CodCia = ?
              AND pp.CodPyto = ?
              AND pp.Vigente = 'S'
              AND pp.NroVersion = (
                    SELECT MAX(p2.NroVersion)
                    FROM PROY_PARTIDA p2
                    WHERE p2.CodCia = pp.CodCia
                      AND p2.CodPyto = pp.CodPyto
                )
            ORDER BY pp.IngEgr, pp.CodPartida
            """;

        try (Connection cn = DBConnection.getInstance().getConnection();
             PreparedStatement ps = cn.prepareStatement(sql)) {

            ps.setInt(1, codCia);
            ps.setInt(2, codPyto);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    FilaFlujoDTO fila = new FilaFlujoDTO();
                    fila.ingEgr = rs.getString("IngEgr");
                    fila.codPartida = rs.getInt("CodPartida");
                    fila.desPartida = rs.getString("DesPartida");
                    fila.valores = new MonthValues(); // inicia en cero
                    lista.add(fila);
                }
            }
        }

        return lista;
    }
}
