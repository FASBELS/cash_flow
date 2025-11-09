// src/main/java/com/dsi/spring/flujoreal/spring_cashflow/service/FlujoRealService.java
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
import com.dsi.spring.flujoreal.spring_cashflow.dao.impl.EgresoRealDAOImpl;
import com.dsi.spring.flujoreal.spring_cashflow.dao.impl.IngresoRealDAOImpl;
import com.dsi.spring.flujoreal.spring_cashflow.dto.FilaFlujoDTO;
import com.dsi.spring.flujoreal.spring_cashflow.dto.MonthValues;

// 🔹 NUEVOS IMPORTS (necesarios para guardar)
import com.dsi.spring.flujoreal.spring_cashflow.dao.FlujoCajaDetDAO;
import com.dsi.spring.flujoreal.spring_cashflow.dao.impl.FlujoCajaDetDAOImpl;
import com.dsi.spring.flujoreal.spring_cashflow.dto.FlujoCajaDetSaveDTO;

@Service
public class FlujoRealService {

    private final IngresoRealDAOImpl ingresoDAO = new IngresoRealDAOImpl();
    private final EgresoRealDAOImpl  egresoDAO  = new EgresoRealDAOImpl();

    // 🔹 NUEVO DAO para guardar en FLUJOCAJA_DET
    private final FlujoCajaDetDAO flujoCajaDetDAO = new FlujoCajaDetDAOImpl();


    // Carga conceptos del proyecto (Ing/Egr + CodPartida + DesPartida)
    private List<FilaFlujoDTO> conceptosProyecto(int codCia, int codPyto) throws Exception {
        String sql = """
            SELECT pp.IngEgr, pp.CodPartida, pa.DesPartida
            FROM PROY_PARTIDA pp
            JOIN PARTIDA pa
              ON pa.CodCia=pp.CodCia AND pa.IngEgr=pp.IngEgr AND pa.CodPartida=pp.CodPartida
            WHERE pp.CodCia=? AND pp.CodPyto=?
            ORDER BY pp.IngEgr, pp.CodPartida
        """;
        List<FilaFlujoDTO> out = new ArrayList<>();
        try (Connection cn = DBConnection.getInstance().getConnection();
             PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setInt(1, codCia);
            ps.setInt(2, codPyto);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    FilaFlujoDTO f = new FilaFlujoDTO();
                    f.ingEgr = rs.getString("IngEgr");
                    f.codPartida = rs.getInt("CodPartida");
                    f.desPartida = rs.getString("DesPartida");
                    out.add(f);
                }
            }
        }
        return out;
    }

    public List<FilaFlujoDTO> obtener(int codCia, int codPyto, int anno) throws Exception {
        // 1) conceptos
        List<FilaFlujoDTO> filas = conceptosProyecto(codCia, codPyto);

        // 2) mapas de valores
        Map<Integer, BigDecimal[]> ingMeses = ingresoDAO.mesesPorAnno(codCia, codPyto, anno);
        Map<Integer, BigDecimal>   ingAcumA = ingresoDAO.acumuladoAnterior(codCia, codPyto, anno);

        Map<Integer, BigDecimal[]> egrMeses = egresoDAO.mesesPorAnno(codCia, codPyto, anno);
        Map<Integer, BigDecimal>   egrAcumA = egresoDAO.acumuladoAnterior(codCia, codPyto, anno);

        // 3) llenar por concepto
        BigDecimal[] netoMes = new BigDecimal[12];
        Arrays.fill(netoMes, BigDecimal.ZERO);
        BigDecimal netoAcumAnt = BigDecimal.ZERO;

        for (FilaFlujoDTO f : filas) {
            f.valores = new MonthValues();
            BigDecimal[] meses = ("I".equals(f.ingEgr) ? ingMeses.get(f.codPartida) : egrMeses.get(f.codPartida));
            if (meses == null) meses = new BigDecimal[12];
            for (int i=0;i<12;i++) {
                BigDecimal v = meses[i]==null ? BigDecimal.ZERO : meses[i];
                f.valores.mes[i] = v;
                f.valores.suma = f.valores.suma.add(v);
                if ("I".equals(f.ingEgr)) netoMes[i] = netoMes[i].add(v);
                else                      netoMes[i] = netoMes[i].subtract(v);
            }
            BigDecimal acumAnt = ("I".equals(f.ingEgr))
                                 ? ingAcumA.getOrDefault(f.codPartida, BigDecimal.ZERO)
                                 : egrAcumA.getOrDefault(f.codPartida, BigDecimal.ZERO);
            f.valores.acumAnt = acumAnt;
            f.valores.total   = f.valores.suma.add(acumAnt);

            if ("I".equals(f.ingEgr)) netoAcumAnt = netoAcumAnt.add(acumAnt);
            else                      netoAcumAnt = netoAcumAnt.subtract(acumAnt);
        }

        // 4) fila Neto
        FilaFlujoDTO neto = new FilaFlujoDTO();
        neto.codPartida = 0;
        neto.desPartida = "FLUJO DE CAJA NETO";
        neto.ingEgr = "N";
        neto.valores = new MonthValues();
        for (int i=0;i<12;i++) {
            neto.valores.mes[i] = netoMes[i];
            neto.valores.suma = neto.valores.suma.add(netoMes[i]);
        }
        neto.valores.acumAnt = netoAcumAnt;
        neto.valores.total   = neto.valores.suma.add(netoAcumAnt);

        // ordenar: ingresos, egresos, neto
        List<FilaFlujoDTO> salida = new ArrayList<>();
        filas.stream().filter(f->"I".equals(f.ingEgr)).forEach(salida::add);
        filas.stream().filter(f->"E".equals(f.ingEgr)).forEach(salida::add);
        salida.add(neto);
        return salida;
    }

    // 🔹 NUEVO MÉTODO: usado por el endpoint /flujo-real/guardar
    public void guardar(List<FlujoCajaDetSaveDTO> filas) throws Exception {
        if (filas == null || filas.isEmpty()) {
            return;
        }
        flujoCajaDetDAO.saveOrUpdateBatch(filas);
    }
}
