package com.dsi.spring.flujoreal.spring_cashflow.dao.impl;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import com.dsi.spring.flujoreal.spring_cashflow.config.DBConnection;
import com.dsi.spring.flujoreal.spring_cashflow.dao.FlujoCajaDetDAO;
import com.dsi.spring.flujoreal.spring_cashflow.dto.FlujoCajaDetProySaveDTO;
import com.dsi.spring.flujoreal.spring_cashflow.dto.FlujoCajaDetSaveDTO;

public class FlujoCajaDetDAOImpl implements FlujoCajaDetDAO {  // 👈 IMPORTANTE

    // ============================================================
    //  FLUJO REAL  (YA EXISTENTE)
    // ============================================================

@Override
public void saveOrUpdate(FlujoCajaDetSaveDTO f) throws Exception {
    if (f == null) return;

    // Para el flujo REAL queremos que comparta la misma fila que el proyectado,
    // por eso forzamos Tipo = 'M' (igual que saveOrUpdateProyectado).
    String tipo = "M";

    // Aseguramos array de 12 posiciones (ene..dic)
    BigDecimal[] m = (f.impRealMes != null && f.impRealMes.length >= 12)
            ? f.impRealMes
            : new BigDecimal[12];

    BigDecimal ene = nz(m[0]);
    BigDecimal feb = nz(m[1]);
    BigDecimal mar = nz(m[2]);
    BigDecimal abr = nz(m[3]);
    BigDecimal may = nz(m[4]);
    BigDecimal jun = nz(m[5]);
    BigDecimal jul = nz(m[6]);
    BigDecimal ago = nz(m[7]);
    BigDecimal sep = nz(m[8]);
    BigDecimal oct = nz(m[9]);
    BigDecimal nov = nz(m[10]);
    BigDecimal dic = nz(m[11]);

    // Suma sólo del año actual (los 12 meses reales)
    BigDecimal sumaAnual = ene
            .add(feb).add(mar).add(abr).add(may).add(jun)
            .add(jul).add(ago).add(sep).add(oct).add(nov).add(dic);

    try (Connection cn = DBConnection.getInstance().getConnection()) {

        // -----------------------------------------------------------------
        // 1) Calcular ImpRealIni = ImpRealAcum del año anterior (Y-1)
        // -----------------------------------------------------------------
        BigDecimal impRealIni = BigDecimal.ZERO;

        String sqlIni = """
            SELECT ImpRealAcum
              FROM FLUJOCAJA_DET
             WHERE Anno       = ?
               AND CodCia     = ?
               AND CodPyto    = ?
               AND IngEgr     = ?
               AND Tipo       = ?
               AND CodPartida = ?
        """;

        try (PreparedStatement psIni = cn.prepareStatement(sqlIni)) {
            psIni.setInt(1, f.anno - 1);   // año anterior
            psIni.setInt(2, f.codCia);
            psIni.setInt(3, f.codPyto);
            psIni.setString(4, f.ingEgr);
            psIni.setString(5, tipo);
            psIni.setInt(6, f.codPartida);

            try (ResultSet rs = psIni.executeQuery()) {
                if (rs.next()) {
                    BigDecimal prev = rs.getBigDecimal(1);
                    if (prev != null) {
                        impRealIni = prev;
                    }
                }
            }
        }

        // ImpRealAcum = acumulado anterior + año actual
        BigDecimal impRealAcum = impRealIni.add(sumaAnual);

        // -----------------------------------------------------------------
        // 2) Intentar UPDATE en FLUJOCAJA_DET (columnas reales)
        // -----------------------------------------------------------------
        String upd = """
            UPDATE FLUJOCAJA_DET
               SET ImpRealIni  = ?,
                   ImpRealEne  = ?,
                   ImpRealFeb  = ?,
                   ImpRealMar  = ?,
                   ImpRealAbr  = ?,
                   ImpRealMay  = ?,
                   ImpRealJun  = ?,
                   ImpRealJul  = ?,
                   ImpRealAgo  = ?,
                   ImpRealSep  = ?,
                   ImpRealOct  = ?,
                   ImpRealNov  = ?,
                   ImpRealDic  = ?,
                   ImpRealAcum = ?
             WHERE Anno       = ?
               AND CodCia     = ?
               AND CodPyto    = ?
               AND IngEgr     = ?
               AND Tipo       = ?
               AND CodPartida = ?
        """;

        int rows;
        try (PreparedStatement ps = cn.prepareStatement(upd)) {
            int i = 1;
            ps.setBigDecimal(i++, impRealIni);
            ps.setBigDecimal(i++, ene);
            ps.setBigDecimal(i++, feb);
            ps.setBigDecimal(i++, mar);
            ps.setBigDecimal(i++, abr);
            ps.setBigDecimal(i++, may);
            ps.setBigDecimal(i++, jun);
            ps.setBigDecimal(i++, jul);
            ps.setBigDecimal(i++, ago);
            ps.setBigDecimal(i++, sep);
            ps.setBigDecimal(i++, oct);
            ps.setBigDecimal(i++, nov);
            ps.setBigDecimal(i++, dic);
            ps.setBigDecimal(i++, impRealAcum);

            ps.setInt(i++, f.anno);
            ps.setInt(i++, f.codCia);
            ps.setInt(i++, f.codPyto);
            ps.setString(i++, f.ingEgr);
            ps.setString(i++, tipo);
            ps.setInt(i++, f.codPartida);

            rows = ps.executeUpdate();
        }

        // -----------------------------------------------------------------
        // 3) Si no existe detalle, garantizar padre en FLUJOCAJA e INSERT
        // -----------------------------------------------------------------
        if (rows == 0) {
            // Crea la fila padre en FLUJOCAJA si aún no existe
            ensureFlujoCajaRow(
                    cn,
                    f.codCia,
                    f.codPyto,
                    f.ingEgr,
                    tipo,
                    f.codPartida,
                    f.orden
            );

            String ins = """
                INSERT INTO FLUJOCAJA_DET (
                    Anno, CodCia, CodPyto, IngEgr, Tipo, CodPartida, Orden,
                    ImpIni,     ImpRealIni,
                    ImpEne,     ImpRealEne,
                    ImpFeb,     ImpRealFeb,
                    ImpMar,     ImpRealMar,
                    ImpAbr,     ImpRealAbr,
                    ImpMay,     ImpRealMay,
                    ImpJun,     ImpRealJun,
                    ImpJul,     ImpRealJul,
                    ImpAgo,     ImpRealAgo,
                    ImpSep,     ImpRealSep,
                    ImpOct,     ImpRealOct,
                    ImpNov,     ImpRealNov,
                    ImpDic,     ImpRealDic,
                    ImpAcum,    ImpRealAcum
                ) VALUES (
                    ?, ?, ?, ?, ?, ?, ?,
                    0, ?,      -- ImpIni = 0, ImpRealIni = ?
                    0, ?,      -- ImpEne / ImpRealEne
                    0, ?,      -- ImpFeb / ImpRealFeb
                    0, ?,      -- ImpMar / ImpRealMar
                    0, ?,      -- ImpAbr / ImpRealAbr
                    0, ?,      -- ImpMay / ImpRealMay
                    0, ?,      -- ImpJun / ImpRealJun
                    0, ?,      -- ImpJul / ImpRealJul
                    0, ?,      -- ImpAgo / ImpRealAgo
                    0, ?,      -- ImpSep / ImpRealSep
                    0, ?,      -- ImpOct / ImpRealOct
                    0, ?,      -- ImpNov / ImpRealNov
                    0, ?,      -- ImpDic / ImpRealDic
                    0, ?       -- ImpAcum = 0, ImpRealAcum = ?
                )
            """;

            try (PreparedStatement ps = cn.prepareStatement(ins)) {
                int i = 1;
                ps.setInt(i++, f.anno);
                ps.setInt(i++, f.codCia);
                ps.setInt(i++, f.codPyto);
                ps.setString(i++, f.ingEgr);
                ps.setString(i++, tipo);
                ps.setInt(i++, f.codPartida);
                ps.setInt(i++, f.orden <= 0 ? 1 : f.orden);

                ps.setBigDecimal(i++, impRealIni);
                ps.setBigDecimal(i++, ene);
                ps.setBigDecimal(i++, feb);
                ps.setBigDecimal(i++, mar);
                ps.setBigDecimal(i++, abr);
                ps.setBigDecimal(i++, may);
                ps.setBigDecimal(i++, jun);
                ps.setBigDecimal(i++, jul);
                ps.setBigDecimal(i++, ago);
                ps.setBigDecimal(i++, sep);
                ps.setBigDecimal(i++, oct);
                ps.setBigDecimal(i++, nov);
                ps.setBigDecimal(i++, dic);
                ps.setBigDecimal(i++, impRealAcum);

                ps.executeUpdate();
            }
        }
    }
}
// ============================================================
//  FLUJO PROYECTADO  (ImpIni / ImpAcum por año)
// ============================================================
@Override
public void saveOrUpdateProyectado(FlujoCajaDetProySaveDTO f) throws Exception {
    if (f == null) return;

    // Tipo de fila (M, A, etc.) – por defecto "M"
    String tipo = (f.tipo == null || f.tipo.isBlank()) ? "M" : f.tipo;

    // Aseguramos array de 12 posiciones (ene..dic)
    BigDecimal[] m = (f.impMes != null && f.impMes.length >= 12)
            ? f.impMes
            : new BigDecimal[12];

    BigDecimal ene = nz(m[0]);
    BigDecimal feb = nz(m[1]);
    BigDecimal mar = nz(m[2]);
    BigDecimal abr = nz(m[3]);
    BigDecimal may = nz(m[4]);
    BigDecimal jun = nz(m[5]);
    BigDecimal jul = nz(m[6]);
    BigDecimal ago = nz(m[7]);
    BigDecimal sep = nz(m[8]);
    BigDecimal oct = nz(m[9]);
    BigDecimal nov = nz(m[10]);
    BigDecimal dic = nz(m[11]);

    // Suma SOLO del año actual (proyectado)
    BigDecimal sumaAnual = ene
            .add(feb).add(mar).add(abr).add(may).add(jun)
            .add(jul).add(ago).add(sep).add(oct).add(nov).add(dic);

    try (Connection cn = DBConnection.getInstance().getConnection()) {

        // ---------------------------------------------------------
        // 1) Calcular ImpIni = ImpAcum del año anterior (Y-1)
        // ---------------------------------------------------------
        BigDecimal impIni = BigDecimal.ZERO;

        String sqlIni = """
            SELECT ImpAcum
              FROM FLUJOCAJA_DET
             WHERE Anno       = ?
               AND CodCia     = ?
               AND CodPyto    = ?
               AND IngEgr     = ?
               AND Tipo       = ?
               AND CodPartida = ?
        """;

        try (PreparedStatement psIni = cn.prepareStatement(sqlIni)) {
            psIni.setInt(1, f.anno - 1);  // año anterior
            psIni.setInt(2, f.codCia);
            psIni.setInt(3, f.codPyto);
            psIni.setString(4, f.ingEgr);
            psIni.setString(5, tipo);
            psIni.setInt(6, f.codPartida);

            try (ResultSet rs = psIni.executeQuery()) {
                if (rs.next()) {
                    BigDecimal prev = rs.getBigDecimal(1);
                    if (prev != null) {
                        impIni = prev;   // acumulado del año anterior
                    }
                }
            }
        }

        // ImpAcum = acumulado anterior + año actual proyectado
        BigDecimal impAcum = impIni.add(sumaAnual);

        // ---------------------------------------------------------
        // 2) Intentar UPDATE solo sobre columnas PROYECTADAS
        // ---------------------------------------------------------
        String upd = """
            UPDATE FLUJOCAJA_DET
               SET ImpIni  = ?,
                   ImpEne  = ?,
                   ImpFeb  = ?,
                   ImpMar  = ?,
                   ImpAbr  = ?,
                   ImpMay  = ?,
                   ImpJun  = ?,
                   ImpJul  = ?,
                   ImpAgo  = ?,
                   ImpSep  = ?,
                   ImpOct  = ?,
                   ImpNov  = ?,
                   ImpDic  = ?,
                   ImpAcum = ?
             WHERE Anno       = ?
               AND CodCia     = ?
               AND CodPyto    = ?
               AND IngEgr     = ?
               AND Tipo       = ?
               AND CodPartida = ?
        """;

        int rows;
        try (PreparedStatement ps = cn.prepareStatement(upd)) {
            int i = 1;
            ps.setBigDecimal(i++, impIni);
            ps.setBigDecimal(i++, ene);
            ps.setBigDecimal(i++, feb);
            ps.setBigDecimal(i++, mar);
            ps.setBigDecimal(i++, abr);
            ps.setBigDecimal(i++, may);
            ps.setBigDecimal(i++, jun);
            ps.setBigDecimal(i++, jul);
            ps.setBigDecimal(i++, ago);
            ps.setBigDecimal(i++, sep);
            ps.setBigDecimal(i++, oct);
            ps.setBigDecimal(i++, nov);
            ps.setBigDecimal(i++, dic);
            ps.setBigDecimal(i++, impAcum);

            ps.setInt(i++, f.anno);
            ps.setInt(i++, f.codCia);
            ps.setInt(i++, f.codPyto);
            ps.setString(i++, f.ingEgr);
            ps.setString(i++, tipo);
            ps.setInt(i++, f.codPartida);

            rows = ps.executeUpdate();
        }

        // ---------------------------------------------------------
        // 3) Si no existe fila, crear padre FLUJOCAJA e INSERT
        // ---------------------------------------------------------
        if (rows == 0) {

            // Asegurar fila padre en FLUJOCAJA
            ensureFlujoCajaRow(
                    cn,
                    f.codCia,
                    f.codPyto,
                    f.ingEgr,
                    tipo,
                    f.codPartida,
                    f.orden
            );

            String ins = """
                INSERT INTO FLUJOCAJA_DET (
                    Anno, CodCia, CodPyto, IngEgr, Tipo, CodPartida, Orden,
                    ImpIni,     ImpRealIni,
                    ImpEne,     ImpRealEne,
                    ImpFeb,     ImpRealFeb,
                    ImpMar,     ImpRealMar,
                    ImpAbr,     ImpRealAbr,
                    ImpMay,     ImpRealMay,
                    ImpJun,     ImpRealJun,
                    ImpJul,     ImpRealJul,
                    ImpAgo,     ImpRealAgo,
                    ImpSep,     ImpRealSep,
                    ImpOct,     ImpRealOct,
                    ImpNov,     ImpRealNov,
                    ImpDic,     ImpRealDic,
                    ImpAcum,    ImpRealAcum
                ) VALUES (
                    ?, ?, ?, ?, ?, ?, ?,
                    ?, 0,
                    ?, 0,
                    ?, 0,
                    ?, 0,
                    ?, 0,
                    ?, 0,
                    ?, 0,
                    ?, 0,
                    ?, 0,
                    ?, 0,
                    ?, 0,
                    ?, 0,
                    ?, 0,
                    ?, 0
                )
            """;

            try (PreparedStatement ps = cn.prepareStatement(ins)) {
                int i = 1;
                ps.setInt(i++, f.anno);
                ps.setInt(i++, f.codCia);
                ps.setInt(i++, f.codPyto);
                ps.setString(i++, f.ingEgr);
                ps.setString(i++, tipo);
                ps.setInt(i++, f.codPartida);
                ps.setInt(i++, f.orden <= 0 ? 1 : f.orden);

                ps.setBigDecimal(i++, impIni);
                ps.setBigDecimal(i++, ene);
                ps.setBigDecimal(i++, feb);
                ps.setBigDecimal(i++, mar);
                ps.setBigDecimal(i++, abr);
                ps.setBigDecimal(i++, may);
                ps.setBigDecimal(i++, jun);
                ps.setBigDecimal(i++, jul);
                ps.setBigDecimal(i++, ago);
                ps.setBigDecimal(i++, sep);
                ps.setBigDecimal(i++, oct);
                ps.setBigDecimal(i++, nov);
                ps.setBigDecimal(i++, dic);
                ps.setBigDecimal(i++, impAcum);

                ps.executeUpdate();
            }
        }
    }
}


 

    // ============================================================
    //  HELPERS COMUNES
    // ============================================================
    private BigDecimal nz(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }

    /**
     * Helper genérico para crear la fila padre en FLUJOCAJA.
     * Lo usamos tanto para el REAL como para el PROYECTADO.
     */
private void ensureFlujoCajaRow(
        Connection cn,
        int codCia,
        int codPyto,
        String ingEgr,
        String tipo,
        int codPartida,
        int orden
) throws Exception {

    // ¿Ya existe?
    String q = """
        SELECT 1
          FROM FLUJOCAJA
         WHERE CodCia     = ?
           AND CodPyto    = ?
           AND IngEgr     = ?
           AND Tipo       = ?
           AND CodPartida = ?
    """;
    try (PreparedStatement ps = cn.prepareStatement(q)) {
        ps.setInt(1, codCia);
        ps.setInt(2, codPyto);
        ps.setString(3, ingEgr);
        ps.setString(4, tipo);
        ps.setInt(5, codPartida);
        try (var rs = ps.executeQuery()) {
            if (rs.next()) {
                return; // ya existe
            }
        }
    }

    // Buscar descripción en PARTIDA
    String des = "PARTIDA " + codPartida;
    String q2 = """
        SELECT DesPartida
          FROM PARTIDA
         WHERE CodCia     = ?
           AND IngEgr     = ?
           AND CodPartida = ?
    """;
    try (PreparedStatement ps = cn.prepareStatement(q2)) {
        ps.setInt(1, codCia);
        ps.setString(2, ingEgr);
        ps.setInt(3, codPartida);
        try (var rs = ps.executeQuery()) {
            if (rs.next()) {
                des = rs.getString(1);
            }
        }
    }

    // Insert en FLUJOCAJA
    String ins = """
        INSERT INTO FLUJOCAJA (
            CodCia, CodPyto, IngEgr, Tipo, CodPartida,
            Nivel, Orden,
            DesConcepto, DesConceptoCorto,
            Semilla, Raiz,
            TabEstado, CodEstado, Vigente
        ) VALUES (
            ?, ?, ?, ?, ?,
            1, ?,
            ?, SUBSTRB(?,1,10),
            1, 0,
            'EST', 'ACT', 'S'
        )
    """;
    try (PreparedStatement ps = cn.prepareStatement(ins)) {
        ps.setInt(1, codCia);
        ps.setInt(2, codPyto);
        ps.setString(3, ingEgr);
        ps.setString(4, tipo);
        ps.setInt(5, codPartida);
        ps.setInt(6, orden <= 0 ? 1 : orden);
        ps.setString(7, des);
        ps.setString(8, des);
        ps.executeUpdate();
    }
}

}
