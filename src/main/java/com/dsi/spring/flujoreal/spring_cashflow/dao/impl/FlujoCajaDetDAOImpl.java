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

//real

@Override
public void saveOrUpdate(FlujoCajaDetSaveDTO f) throws Exception {
    if (f == null) return;

    String tipo = "M";

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

    BigDecimal sumaAnual = ene.add(feb).add(mar).add(abr).add(may).add(jun)
            .add(jul).add(ago).add(sep).add(oct).add(nov).add(dic);

    try (Connection cn = DBConnection.getInstance().getConnection()) {

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

        try (PreparedStatement ps = cn.prepareStatement(sqlIni)) {
            ps.setInt(1, f.anno - 1);
            ps.setInt(2, f.codCia);
            ps.setInt(3, f.codPyto);
            ps.setString(4, f.ingEgr);
            ps.setString(5, tipo);
            ps.setInt(6, f.codPartida);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    BigDecimal prev = rs.getBigDecimal(1);
                    if (prev != null) impRealIni = prev;
                }
            }
        }
        BigDecimal impRealAcum = impRealIni.add(sumaAnual);

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

        if (rows == 0) {

            ensureFlujoCajaRow(
                    cn, f.codCia, f.codPyto, f.ingEgr,
                    tipo, f.codPartida, f.orden
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
                    0, ?,     -- ImpIni (proy) siempre 0
                    0, ?,     -- meses proyectados siempre 0
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
                    0, ?,     -- imp proy dic
                    0, ?      -- ImpAcum proy =0, real=?
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

        recalcCadenaRealForward(
                cn,
                f.codCia,
                f.codPyto,
                f.ingEgr,
                tipo,
                f.codPartida,
                f.anno
        );
    }
}


private void recalcCadenaRealForward(
        Connection cn,
        int codCia,
        int codPyto,
        String ingEgr,
        String tipo,
        int codPartida,
        int annoBase
) throws Exception {

    String sqlSel = """
        SELECT Anno,
               NVL(ImpRealEne,0) AS ImpRealEne,
               NVL(ImpRealFeb,0) AS ImpRealFeb,
               NVL(ImpRealMar,0) AS ImpRealMar,
               NVL(ImpRealAbr,0) AS ImpRealAbr,
               NVL(ImpRealMay,0) AS ImpRealMay,
               NVL(ImpRealJun,0) AS ImpRealJun,
               NVL(ImpRealJul,0) AS ImpRealJul,
               NVL(ImpRealAgo,0) AS ImpRealAgo,
               NVL(ImpRealSep,0) AS ImpRealSep,
               NVL(ImpRealOct,0) AS ImpRealOct,
               NVL(ImpRealNov,0) AS ImpRealNov,
               NVL(ImpRealDic,0) AS ImpRealDic,
               NVL(ImpRealAcum,0) AS ImpRealAcum
          FROM FLUJOCAJA_DET
         WHERE CodCia     = ?
           AND CodPyto    = ?
           AND IngEgr     = ?
           AND Tipo       = ?
           AND CodPartida = ?
           AND Anno      >= ?
         ORDER BY Anno
    """;

    try (PreparedStatement ps = cn.prepareStatement(sqlSel)) {
        ps.setInt(1, codCia);
        ps.setInt(2, codPyto);
        ps.setString(3, ingEgr);
        ps.setString(4, tipo);
        ps.setInt(5, codPartida);
        ps.setInt(6, annoBase);

        try (ResultSet rs = ps.executeQuery()) {

            BigDecimal acumuladoAnterior = null;

            while (rs.next()) {
                int anno = rs.getInt("Anno");

                BigDecimal ene = nz(rs.getBigDecimal("ImpRealEne"));
                BigDecimal feb = nz(rs.getBigDecimal("ImpRealFeb"));
                BigDecimal mar = nz(rs.getBigDecimal("ImpRealMar"));
                BigDecimal abr = nz(rs.getBigDecimal("ImpRealAbr"));
                BigDecimal may = nz(rs.getBigDecimal("ImpRealMay"));
                BigDecimal jun = nz(rs.getBigDecimal("ImpRealJun"));
                BigDecimal jul = nz(rs.getBigDecimal("ImpRealJul"));
                BigDecimal ago = nz(rs.getBigDecimal("ImpRealAgo"));
                BigDecimal sep = nz(rs.getBigDecimal("ImpRealSep"));
                BigDecimal oct = nz(rs.getBigDecimal("ImpRealOct"));
                BigDecimal nov = nz(rs.getBigDecimal("ImpRealNov"));
                BigDecimal dic = nz(rs.getBigDecimal("ImpRealDic"));

                BigDecimal sumaAnual = ene
                        .add(feb).add(mar).add(abr).add(may).add(jun)
                        .add(jul).add(ago).add(sep).add(oct).add(nov).add(dic);

               
                if (acumuladoAnterior == null) {
                    acumuladoAnterior = nz(rs.getBigDecimal("ImpRealAcum"));
                    continue;
                }

                BigDecimal impRealIni = acumuladoAnterior;
                BigDecimal impRealAcum = impRealIni.add(sumaAnual);

                String upd = """
                    UPDATE FLUJOCAJA_DET
                       SET ImpRealIni  = ?,
                           ImpRealAcum = ?
                     WHERE Anno       = ?
                       AND CodCia     = ?
                       AND CodPyto    = ?
                       AND IngEgr     = ?
                       AND Tipo       = ?
                       AND CodPartida = ?
                """;

                try (PreparedStatement psUpd = cn.prepareStatement(upd)) {
                    int i = 1;
                    psUpd.setBigDecimal(i++, impRealIni);
                    psUpd.setBigDecimal(i++, impRealAcum);
                    psUpd.setInt(i++, anno);
                    psUpd.setInt(i++, codCia);
                    psUpd.setInt(i++, codPyto);
                    psUpd.setString(i++, ingEgr);
                    psUpd.setString(i++, tipo);
                    psUpd.setInt(i++, codPartida);
                    psUpd.executeUpdate();
                }

                acumuladoAnterior = impRealAcum;
            }
        }
    }
}



//proyectado
@Override
public void saveOrUpdateProyectado(FlujoCajaDetProySaveDTO f) throws Exception {
    if (f == null) return;

    String tipo = (f.tipo == null || f.tipo.isBlank()) ? "M" : f.tipo;

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

    BigDecimal sumaAnual = ene
            .add(feb).add(mar).add(abr).add(may).add(jun)
            .add(jul).add(ago).add(sep).add(oct).add(nov).add(dic);

    try (Connection cn = DBConnection.getInstance().getConnection()) {

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
                        impIni = prev; 
                    }
                }
            }
        }

        BigDecimal impAcum = impIni.add(sumaAnual);

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

    private BigDecimal nz(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }
private void ensureFlujoCajaRow(
        Connection cn,
        int codCia,
        int codPyto,
        String ingEgr,
        String tipo,
        int codPartida,
        int orden
) throws Exception {

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
                return; 
            }
        }
    }

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
