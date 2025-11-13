package com.dsi.spring.flujoreal.spring_cashflow.dao.impl;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;

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

        String tipo = (f.tipo == null || f.tipo.isBlank()) ? "R" : f.tipo;

        // Aseguramos array de 12 posiciones
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

        BigDecimal acum = ene
                .add(feb).add(mar).add(abr).add(may).add(jun)
                .add(jul).add(ago).add(sep).add(oct).add(nov).add(dic);

        try (Connection cn = DBConnection.getInstance().getConnection()) {

            // 1) Intentar UPDATE en FLUJOCAJA_DET (columnas reales)
            String upd = """
                UPDATE FLUJOCAJA_DET
                   SET ImpRealEne  = ?,
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
                ps.setBigDecimal(i++, acum);

                ps.setInt(i++, f.anno);
                ps.setInt(i++, f.codCia);
                ps.setInt(i++, f.codPyto);
                ps.setString(i++, f.ingEgr);
                ps.setString(i++, tipo);
                ps.setInt(i++, f.codPartida);

                rows = ps.executeUpdate();
            }

            // 2) Si no existe detalle, garantizamos padre en FLUJOCAJA y hacemos INSERT
            if (rows == 0) {
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
                        0, 0,
                        0, ?,
                        0, ?,
                        0, ?,
                        0, ?,
                        0, ?,
                        0, ?,
                        0, ?,
                        0, ?,
                        0, ?,
                        0, ?,
                        0, ?,
                        0, ?,
                        0, ?
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
                    ps.setBigDecimal(i++, acum);

                    ps.executeUpdate();
                }
            }
        }
    }

    // ============================================================
    //  FLUJO PROYECTADO  (NUEVO)
    // ============================================================
    @Override
    public void saveOrUpdateProyectado(FlujoCajaDetProySaveDTO f) throws Exception {
        if (f == null) return;

        // Tipo de fila (M, A, etc.)
        String tipo = (f.tipo == null || f.tipo.isBlank()) ? "M" : f.tipo;

        // Aseguramos array de 12 posiciones
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

        BigDecimal acum = ene
                .add(feb).add(mar).add(abr).add(may).add(jun)
                .add(jul).add(ago).add(sep).add(oct).add(nov).add(dic);

        try (Connection cn = DBConnection.getInstance().getConnection()) {

            // 1) UPDATE sobre columnas PROYECTADAS (ImpEne..ImpDic, ImpAcum)
            //    13 SET + 6 WHERE = 19 parámetros
            String upd = """
                UPDATE FLUJOCAJA_DET
                   SET ImpEne  = ?,
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
                ps.setBigDecimal(i++, acum);   // 13

                ps.setInt(i++, f.anno);        // 14
                ps.setInt(i++, f.codCia);      // 15
                ps.setInt(i++, f.codPyto);     // 16
                ps.setString(i++, f.ingEgr);   // 17
                ps.setString(i++, tipo);       // 18
                ps.setInt(i++, f.codPartida);  // 19

                rows = ps.executeUpdate();
            }

            // 2) Si no existe detalle, garantizamos padre en FLUJOCAJA y hacemos INSERT
            if (rows == 0) {
                ensureFlujoCajaRow(
                        cn,
                        f.codCia,
                        f.codPyto,
                        f.ingEgr,
                        tipo,
                        f.codPartida,
                        f.orden
                );

                // 7 primeros ? + 12 meses + 1 acum = 20 parámetros
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
                        ?, ?, ?, ?, ?, ?, ?,   -- 7
                        0, 0,
                        ?, 0,                  -- ene
                        ?, 0,                  -- feb
                        ?, 0,                  -- mar
                        ?, 0,                  -- abr
                        ?, 0,                  -- may
                        ?, 0,                  -- jun
                        ?, 0,                  -- jul
                        ?, 0,                  -- ago
                        ?, 0,                  -- sep
                        ?, 0,                  -- oct
                        ?, 0,                  -- nov
                        ?, 0,                  -- dic
                        ?, 0                   -- acum
                    )
                """;

                try (PreparedStatement ps = cn.prepareStatement(ins)) {
                    int i = 1;
                    // 7 primeros
                    ps.setInt(i++, f.anno);                       // 1
                    ps.setInt(i++, f.codCia);                     // 2
                    ps.setInt(i++, f.codPyto);                    // 3
                    ps.setString(i++, f.ingEgr);                  // 4
                    ps.setString(i++, tipo);                      // 5
                    ps.setInt(i++, f.codPartida);                 // 6
                    ps.setInt(i++, f.orden <= 0 ? 1 : f.orden);   // 7

                    // 12 meses proyectados
                    ps.setBigDecimal(i++, ene);                   // 8
                    ps.setBigDecimal(i++, feb);                   // 9
                    ps.setBigDecimal(i++, mar);                   // 10
                    ps.setBigDecimal(i++, abr);                   // 11
                    ps.setBigDecimal(i++, may);                   // 12
                    ps.setBigDecimal(i++, jun);                   // 13
                    ps.setBigDecimal(i++, jul);                   // 14
                    ps.setBigDecimal(i++, ago);                   // 15
                    ps.setBigDecimal(i++, sep);                   // 16
                    ps.setBigDecimal(i++, oct);                   // 17
                    ps.setBigDecimal(i++, nov);                   // 18
                    ps.setBigDecimal(i++, dic);                   // 19

                    // ImpAcum proyectado
                    ps.setBigDecimal(i++, acum);                  // 20

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
