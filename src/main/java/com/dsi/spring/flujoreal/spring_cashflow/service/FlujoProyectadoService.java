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
import com.dsi.spring.flujoreal.spring_cashflow.dao.FlujoCajaDetDAO;
import com.dsi.spring.flujoreal.spring_cashflow.dao.FlujoProyectadoDAO;
import com.dsi.spring.flujoreal.spring_cashflow.dao.impl.FlujoCajaDetDAOImpl;
import com.dsi.spring.flujoreal.spring_cashflow.dao.impl.FlujoProyectadoDAOImpl;
import com.dsi.spring.flujoreal.spring_cashflow.dto.FilaFlujoDTO;
import com.dsi.spring.flujoreal.spring_cashflow.dto.FlujoCajaDetProySaveDTO;
import com.dsi.spring.flujoreal.spring_cashflow.dto.MonthValues;

@Service
public class FlujoProyectadoService {

    private final FlujoProyectadoDAO proyectadoDAO = new FlujoProyectadoDAOImpl();
    private final FlujoCajaDetDAO flujoCajaDetDAO = new FlujoCajaDetDAOImpl();

    public List<FilaFlujoDTO> obtenerFlujoProyectado(int codCia, int codPyto, int anno) throws Exception {

        List<FilaFlujoDTO> filas = conceptosProyectoProyectado(codCia, codPyto);

        if (filas.isEmpty()) {
            return filas;
        }

        Map<Integer, BigDecimal[]> ingMeses = proyectadoDAO.mesesPorAnno(codCia, codPyto, anno, "I");
        Map<Integer, BigDecimal[]> egrMeses = proyectadoDAO.mesesPorAnno(codCia, codPyto, anno, "E");

        Map<Integer, BigDecimal> ingAcumAnt = proyectadoDAO.acumuladoAnterior(codCia, codPyto, anno, "I");
        Map<Integer, BigDecimal> egrAcumAnt = proyectadoDAO.acumuladoAnterior(codCia, codPyto, anno, "E");

        BigDecimal[] netoMes = new BigDecimal[12];
        Arrays.fill(netoMes, BigDecimal.ZERO);
        BigDecimal netoAcumAnt = BigDecimal.ZERO;

        for (FilaFlujoDTO f : filas) {
            f.valores = new MonthValues();

            Map<Integer, BigDecimal[]> sourceMeses;
            Map<Integer, BigDecimal> sourceAcum;

            if ("I".equals(f.ingEgr)) {
                sourceMeses = ingMeses;
                sourceAcum = ingAcumAnt;
            } else if ("E".equals(f.ingEgr)) {
                sourceMeses = egrMeses;
                sourceAcum = egrAcumAnt;
            } else {
                continue;
            }

            BigDecimal[] meses = sourceMeses.get(f.codPartida);
            if (meses == null) meses = new BigDecimal[12];

            for (int i = 0; i < 12; i++) {
                BigDecimal v = meses[i] == null ? BigDecimal.ZERO : meses[i];
                f.valores.mes[i] = v;
                f.valores.suma = f.valores.suma.add(v);

                if ("I".equals(f.ingEgr))
                    netoMes[i] = netoMes[i].add(v);
                else
                    netoMes[i] = netoMes[i].subtract(v);
            }

            BigDecimal acumAnt = sourceAcum.getOrDefault(f.codPartida, BigDecimal.ZERO);
            f.valores.acumAnt = acumAnt;
            f.valores.total = f.valores.suma.add(acumAnt);

            if ("I".equals(f.ingEgr))
                netoAcumAnt = netoAcumAnt.add(acumAnt);
            else
                netoAcumAnt = netoAcumAnt.subtract(acumAnt);
        }

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
        neto.valores.total = neto.valores.suma.add(netoAcumAnt);

        List<FilaFlujoDTO> salida = new ArrayList<>();
        filas.stream().filter(f -> "I".equals(f.ingEgr)).forEach(salida::add);
        filas.stream().filter(f -> "E".equals(f.ingEgr)).forEach(salida::add);
        salida.add(neto);

        return salida;
    }

    public void guardar(List<FlujoCajaDetProySaveDTO> filas) throws Exception {
        if (filas == null || filas.isEmpty()) return;
        flujoCajaDetDAO.saveOrUpdateProyectadoBatch(filas);
    }

    private List<FilaFlujoDTO> conceptosProyectoProyectado(int codCia, int codPyto) throws Exception {

        List<FilaFlujoDTO> lista = new ArrayList<>();

        String sqlProy = """
            SELECT p.IngEgr,
                p.CodPartida,
                p.CodPartidas,
                p.DesPartida,
                p.Nivel,
                p.Semilla
            FROM PARTIDA p
            JOIN PROY_PARTIDA pp
            ON pp.CodCia = p.CodCia
            AND pp.IngEgr = p.IngEgr
            AND pp.CodPartida = p.CodPartida
            WHERE pp.CodCia = ?
            AND pp.CodPyto = ?
            AND pp.Vigente = '1'
            AND pp.NroVersion = (
                    SELECT MAX(p2.NroVersion)
                    FROM PROY_PARTIDA p2
                    WHERE p2.CodCia = pp.CodCia
                    AND p2.CodPyto = pp.CodPyto
                    AND p2.Vigente = '1'
            )
            ORDER BY p.IngEgr, p.CodPartida
            """;

        try (Connection cn = DBConnection.getInstance().getConnection();
            PreparedStatement ps = cn.prepareStatement(sqlProy)) {

            ps.setInt(1, codCia);
            ps.setInt(2, codPyto);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    FilaFlujoDTO fila = new FilaFlujoDTO();
                    fila.ingEgr = rs.getString("IngEgr");
                    fila.codPartida = rs.getInt("CodPartida");
                    fila.desPartida = rs.getString("DesPartida");
                    fila.codPartidas = rs.getString("CodPartidas");
                    fila.nivel = rs.getInt("Nivel");
                    fila.valores = new MonthValues();
                    fila.noProyectado = false;
                    lista.add(fila);
                }
            }
        }

        return lista;
    }
}
