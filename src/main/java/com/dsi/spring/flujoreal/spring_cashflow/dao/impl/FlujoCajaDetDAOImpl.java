package com.dsi.spring.flujoreal.spring_cashflow.dao.impl;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;

import com.dsi.spring.flujoreal.spring_cashflow.config.DBConnection;
import com.dsi.spring.flujoreal.spring_cashflow.dao.FlujoCajaDetDAO;
import com.dsi.spring.flujoreal.spring_cashflow.dto.FlujoCajaDetSaveDTO;

public class FlujoCajaDetDAOImpl implements FlujoCajaDetDAO {  // 👈 IMPORTANTE

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

        // 1) Intentar UPDATE en FLUJOCAJA_DET
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
            ensureFlujoCajaRow(cn, f, tipo);  // 🆕 nuevo método

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

private BigDecimal nz(BigDecimal v) {
    return v == null ? BigDecimal.ZERO : v;
}
// 🆕 Garantiza que exista el registro padre en FLUJOCAJA
private void ensureFlujoCajaRow(Connection cn, FlujoCajaDetSaveDTO f, String tipo) throws Exception {
    // ¿Ya existe?
    String q = """
        SELECT 1
          FROM FLUJOCAJA
         WHERE CodCia=? AND CodPyto=? AND IngEgr=? AND Tipo=? AND CodPartida=?
    """;
    try (PreparedStatement ps = cn.prepareStatement(q)) {
        ps.setInt(1, f.codCia);
        ps.setInt(2, f.codPyto);
        ps.setString(3, f.ingEgr);
        ps.setString(4, tipo);
        ps.setInt(5, f.codPartida);
        try (var rs = ps.executeQuery()) {
            if (rs.next()) {
                return; // ya existe
            }
        }
    }

    // Si no existe, obtenemos descripción desde PARTIDA
    String des = "PARTIDA " + f.codPartida;
    String q2 = """
        SELECT DesPartida
          FROM PARTIDA
         WHERE CodCia=? AND IngEgr=? AND CodPartida=?
    """;
    try (PreparedStatement ps = cn.prepareStatement(q2)) {
        ps.setInt(1, f.codCia);
        ps.setString(2, f.ingEgr);
        ps.setInt(3, f.codPartida);
        try (var rs = ps.executeQuery()) {
            if (rs.next()) {
                des = rs.getString(1);
            }
        }
    }

    // Insert mínimo en FLUJOCAJA (valores por defecto razonables)
    String ins = """
        INSERT INTO FLUJOCAJA (
            CodCia, CodPyto, IngEgr, Tipo, CodPartida,
            Nivel, Orden,
            DesConcepto, DesConceptoCorto,
            Semilla, Raiz,
            TabEstado, CodEstado, Vigente
        ) VALUES (
            ?, ?, ?, ?, ?,
            1, ?,                -- Nivel=1, Orden según lo que llega
            ?, SUBSTR(?,1,10),   -- Descripciones
            1, 0,                -- Semilla, Raiz
            'EST', 'ACT', 'S'    -- Estado activo
        )
    """;
    try (PreparedStatement ps = cn.prepareStatement(ins)) {
        ps.setInt(1, f.codCia);
        ps.setInt(2, f.codPyto);
        ps.setString(3, f.ingEgr);
        ps.setString(4, tipo);
        ps.setInt(5, f.codPartida);
        ps.setInt(6, f.orden <= 0 ? 1 : f.orden);
        ps.setString(7, des);
        ps.setString(8, des);
        ps.executeUpdate();
    }
}
 

}
