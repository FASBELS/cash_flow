package com.dsi.spring.flujoreal.spring_cashflow.service;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.dsi.spring.flujoreal.spring_cashflow.config.DBConnection;
import com.dsi.spring.flujoreal.spring_cashflow.dao.impl.EgresoRealDAOImpl;
import com.dsi.spring.flujoreal.spring_cashflow.dao.impl.IngresoRealDAOImpl;
import com.dsi.spring.flujoreal.spring_cashflow.dto.FilaFlujoDTO;
import com.dsi.spring.flujoreal.spring_cashflow.dto.MonthValues;

import com.dsi.spring.flujoreal.spring_cashflow.dao.FlujoCajaDetDAO;
import com.dsi.spring.flujoreal.spring_cashflow.dao.impl.FlujoCajaDetDAOImpl;
import com.dsi.spring.flujoreal.spring_cashflow.dto.FlujoCajaDetSaveDTO;

@Service
public class FlujoRealService {

    private final IngresoRealDAOImpl ingresoDAO = new IngresoRealDAOImpl();
    private final EgresoRealDAOImpl  egresoDAO  = new EgresoRealDAOImpl();
    private final FlujoCajaDetDAO flujoCajaDetDAO = new FlujoCajaDetDAOImpl();
    
    // Cargar también Nivel y CodPartidas para el ordenamiento
    private List<FilaFlujoDTO> conceptosProyecto(int codCia, int codPyto) throws Exception {
        String sql = """
            SELECT pp.IngEgr, pp.CodPartida, pa.DesPartida,
                   pp.Nivel, pp.CodPartidas
            FROM PROY_PARTIDA pp
            JOIN PARTIDA pa
              ON pa.CodCia=pp.CodCia AND pa.IngEgr=pp.IngEgr AND pa.CodPartida=pp.CodPartida
            WHERE pp.CodCia=? AND pp.CodPyto=?
            """; // Se quita el ORDER BY de aquí, lo haremos en Java
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
                    f.nivel = rs.getInt("Nivel"); // Guardamos el Nivel
                    f.codPartidas = rs.getString("CodPartidas"); // Guardamos el CodPartidas
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
        // (Con los DAOs modificados, estos mapas AHORA CONTIENEN Nivel 1 y Nivel 2)
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
            
            // Si no hay datos, inicializa en 0
            if (meses == null) {
                meses = new BigDecimal[12];
                Arrays.fill(meses, BigDecimal.ZERO);
            }

            for (int i=0;i<12;i++) {
                BigDecimal v = meses[i]==null ? BigDecimal.ZERO : meses[i];
                f.valores.mes[i] = v;
                f.valores.suma = f.valores.suma.add(v);

                // Para el FCN, SÓLO sumar los padres (Nivel 1 en tu BD)
                // Esto evita contar doble (el padre y los hijos)
                if (f.nivel == 1) { 
                    if ("I".equals(f.ingEgr)) netoMes[i] = netoMes[i].add(v);
                    else                      netoMes[i] = netoMes[i].subtract(v);
                }
            }
            
            BigDecimal acumAnt = ("I".equals(f.ingEgr))
                                   ? ingAcumA.getOrDefault(f.codPartida, BigDecimal.ZERO)
                                   : egrAcumA.getOrDefault(f.codPartida, BigDecimal.ZERO);
            f.valores.acumAnt = acumAnt;
            f.valores.total   = f.valores.suma.add(acumAnt);

            // Para el FCN Acum. Ant., SÓLO sumar los padres (Nivel 1 en tu BD)
            if (f.nivel == 1) {
                if ("I".equals(f.ingEgr)) netoAcumAnt = netoAcumAnt.add(acumAnt);
                else                      netoAcumAnt = netoAcumAnt.subtract(acumAnt);
            }
        }

        // 4) fila Neto
        FilaFlujoDTO neto = new FilaFlujoDTO();
        neto.codPartida = 0;
        neto.desPartida = "FLUJO DE CAJA NETO";
        neto.ingEgr = "N"; // 'N' para Neto
        neto.codPartidas = "ZZZ"; // Para que ordene al final
        neto.valores = new MonthValues();
        for (int i=0;i<12;i++) {
            neto.valores.mes[i] = netoMes[i];
            neto.valores.suma = neto.valores.suma.add(netoMes[i]);
        }
        neto.valores.acumAnt = netoAcumAnt;
        neto.valores.total   = neto.valores.suma.add(netoAcumAnt);

        // 5)
        List<FilaFlujoDTO> salida = new ArrayList<>(filas);
        salida.sort(Comparator
            .comparing(FilaFlujoDTO::getIngEgr)
            .reversed() // Pone 'I' (Ingresos) antes que 'E' (Egresos)
            .thenComparing(FilaFlujoDTO::getCodPartidas)); // Ordena jerárquicamente (ej: 'ING-001' antes de 'ING-001-01')
        
        salida.add(neto); // Añadir el neto al final
        return salida;
    }

    public void guardar(List<FlujoCajaDetSaveDTO> filas) throws Exception {
        if (filas == null || filas.isEmpty()) {
            return;
        }
        flujoCajaDetDAO.saveOrUpdateBatch(filas);
    }
}
