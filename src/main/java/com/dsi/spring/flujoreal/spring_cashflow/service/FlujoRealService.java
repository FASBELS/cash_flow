package com.dsi.spring.flujoreal.spring_cashflow.service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.dsi.spring.flujoreal.spring_cashflow.dao.EgresoRealDAO;
import com.dsi.spring.flujoreal.spring_cashflow.dao.FlujoCajaDetDAO;
import com.dsi.spring.flujoreal.spring_cashflow.dao.FlujoRealDAO;
import com.dsi.spring.flujoreal.spring_cashflow.dao.IngresoRealDAO;
import com.dsi.spring.flujoreal.spring_cashflow.dao.impl.EgresoRealDAOImpl;
import com.dsi.spring.flujoreal.spring_cashflow.dao.impl.FlujoCajaDetDAOImpl;
import com.dsi.spring.flujoreal.spring_cashflow.dao.impl.FlujoRealDAOImpl;
import com.dsi.spring.flujoreal.spring_cashflow.dao.impl.IngresoRealDAOImpl;
import com.dsi.spring.flujoreal.spring_cashflow.dto.DetValoresDTO;
import com.dsi.spring.flujoreal.spring_cashflow.dto.FilaFlujoDTO;
import com.dsi.spring.flujoreal.spring_cashflow.dto.FlujoCajaDetSaveDTO;
import com.dsi.spring.flujoreal.spring_cashflow.dto.MonthValues;
import com.dsi.spring.flujoreal.spring_cashflow.dto.RealMesDTO;
import com.dsi.spring.flujoreal.spring_cashflow.utils.PartidaHierarchyResolver;

@Service
public class FlujoRealService {

    private final FlujoCajaDetDAO flujoCajaDetDAO = new FlujoCajaDetDAOImpl();
    private final IngresoRealDAO ingresoRealDAO   = new IngresoRealDAOImpl();
    private final EgresoRealDAO  egresoRealDAO    = new EgresoRealDAOImpl();
    private final FlujoRealDAO   flujoRealDAO     = new FlujoRealDAOImpl();

    public List<FilaFlujoDTO> obtener(int codCia, int codPyto, int anno) throws Exception {

        List<FilaFlujoDTO> filasProy = flujoRealDAO.conceptosProyecto(codCia, codPyto);
        Map<Integer, FilaFlujoDTO> indexPorPartida = filasProy.stream()
                .collect(Collectors.toMap(f -> f.codPartida, f -> f, (a, b) -> a));

        Map<Integer, DetValoresDTO> detalle = flujoRealDAO.leerDetalleReal(codCia, codPyto, anno);

        for (Map.Entry<Integer, DetValoresDTO> e : detalle.entrySet()) {
            int codPartida = e.getKey();
            if (!indexPorPartida.containsKey(codPartida)) {
                FilaFlujoDTO info = flujoRealDAO.obtenerInfoPartida(codCia, codPartida);
                if (info == null) continue;
                info.noProyectado = true;
                info.valores = new MonthValues();
                filasProy.add(info);
                indexPorPartida.put(codPartida, info);
            }
        }

        BigDecimal[] netoMes = new BigDecimal[12];
        Arrays.fill(netoMes, BigDecimal.ZERO);
        BigDecimal netoAcumAnt = BigDecimal.ZERO;

        for (FilaFlujoDTO fila : filasProy) {
            if (fila.valores == null) fila.valores = new MonthValues();

            DetValoresDTO det = detalle.get(fila.codPartida);

            if (det != null) {
                fila.valores.acumAnt = det.impRealIni;
                fila.valores.suma = BigDecimal.ZERO;

                for (int i = 0; i < 12; i++) {
                    BigDecimal v = det.mes[i];
                    fila.valores.mes[i] = v;
                    fila.valores.suma = fila.valores.suma.add(v);
                }

                fila.valores.total = fila.valores.acumAnt.add(fila.valores.suma);
            } else {
                fila.valores.acumAnt = BigDecimal.ZERO;
                fila.valores.suma = BigDecimal.ZERO;
                fila.valores.total = BigDecimal.ZERO;
            }

            if (fila.nivel == 1) {
                if ("I".equals(fila.ingEgr)) {
                    netoAcumAnt = netoAcumAnt.add(fila.valores.acumAnt);
                } else if ("E".equals(fila.ingEgr)) {
                    netoAcumAnt = netoAcumAnt.subtract(fila.valores.acumAnt);
                }

                for (int i = 0; i < 12; i++) {
                    BigDecimal v = fila.valores.mes[i];
                    if ("I".equals(fila.ingEgr)) {
                        netoMes[i] = netoMes[i].add(v);
                    } else if ("E".equals(fila.ingEgr)) {
                        netoMes[i] = netoMes[i].subtract(v);
                    }
                }
            }
        }

        FilaFlujoDTO neto = new FilaFlujoDTO();
        neto.codPartida = 0;
        neto.desPartida = "FLUJO DE CAJA NETO";
        neto.ingEgr     = "N";
        neto.codPartidas = "ZZZ";
        neto.nivel = 1;
        neto.valores = new MonthValues();

        neto.valores.acumAnt = netoAcumAnt;
        neto.valores.suma = BigDecimal.ZERO;

        for (int i = 0; i < 12; i++) {
            neto.valores.mes[i] = netoMes[i];
            neto.valores.suma = neto.valores.suma.add(netoMes[i]);
        }

        neto.valores.total = neto.valores.acumAnt.add(neto.valores.suma);

        List<FilaFlujoDTO> salida = new ArrayList<>(filasProy);
        salida.add(neto);

        return salida;
    }

    public void guardar(List<FlujoCajaDetSaveDTO> filas) throws Exception {
        if (filas == null || filas.isEmpty()) return;
        flujoCajaDetDAO.saveOrUpdateBatch(filas);
    }

    public List<RealMesDTO> obtenerMesDesdeBoletas(
            int codCia,
            int codPyto,
            int anno,
            int mes) throws Exception {

        if (mes < 1 || mes > 12) {
            throw new IllegalArgumentException("El mes debe estar entre 1 y 12");
        }

        int idx = mes - 1;

        Map<Integer, BigDecimal[]> ingMeses = ingresoRealDAO.mesesPorAnno(codCia, codPyto, anno);
        Map<Integer, BigDecimal[]> egrMeses = egresoRealDAO.mesesPorAnno(codCia, codPyto, anno);

        List<RealMesDTO> resultado = new ArrayList<>();

        java.util.function.Function<Integer, RealMesDTO> construirDTO = (codPartida) -> {
            try {
                FilaFlujoDTO info = flujoRealDAO.obtenerInfoPartida(codCia, codPartida);
                if (info == null) return null;

                var parentCodes = PartidaHierarchyResolver.deriveParent(
                        info.codPartidas,
                        info.codPartida
                );

                Integer padre = parentCodes.codPartidaPadreNum.orElse(null);

                RealMesDTO dto = new RealMesDTO();
                dto.codPartida    = info.codPartida;
                dto.ingEgr        = info.ingEgr;
                dto.desPartida    = info.desPartida;
                dto.nivel         = info.nivel;
                dto.codPartidas   = info.codPartidas;
                dto.parentPartida = padre;
                dto.mes           = mes;

                return dto;

            } catch (Exception ex) {
                ex.printStackTrace();
                return null;
            }
        };

        if (ingMeses != null) {
            for (Map.Entry<Integer, BigDecimal[]> e : ingMeses.entrySet()) {
                int codPartida = e.getKey();
                BigDecimal[] arr = e.getValue();
                BigDecimal val = BigDecimal.ZERO;

                if (arr != null && arr.length > idx && arr[idx] != null)
                    val = arr[idx];

                if (val.signum() != 0) {
                    RealMesDTO dto = construirDTO.apply(codPartida);
                    if (dto != null) {
                        dto.monto = val;
                        resultado.add(dto);
                    }
                }
            }
        }

        if (egrMeses != null) {
            for (Map.Entry<Integer, BigDecimal[]> e : egrMeses.entrySet()) {
                int codPartida = e.getKey();
                BigDecimal[] arr = e.getValue();
                BigDecimal val = BigDecimal.ZERO;

                if (arr != null && arr.length > idx && arr[idx] != null)
                    val = arr[idx];

                if (val.signum() != 0) {
                    RealMesDTO dto = construirDTO.apply(codPartida);
                    if (dto != null) {
                        dto.ingEgr = "E";
                        dto.monto  = val;
                        resultado.add(dto);
                    }
                }
            }
        }

        List<FilaFlujoDTO> estructura = flujoRealDAO.conceptosProyecto(codCia, codPyto);

        for (FilaFlujoDTO p : estructura) {

            boolean existe = resultado.stream()
                    .anyMatch(r -> r.codPartida == p.codPartida);

            if (!existe) {
                RealMesDTO dto = new RealMesDTO();
                dto.codPartida  = p.codPartida;
                dto.ingEgr      = p.ingEgr;
                dto.desPartida  = p.desPartida;
                dto.nivel       = p.nivel;
                dto.codPartidas = p.codPartidas;
                dto.mes         = mes;
                dto.monto       = BigDecimal.ZERO;

                var parentCodes = PartidaHierarchyResolver.deriveParent(
                        p.codPartidas,
                        p.codPartida
                );
                dto.parentPartida = parentCodes.codPartidaPadreNum.orElse(null);

                resultado.add(dto);
            }
        }

        return resultado;
    }
}
