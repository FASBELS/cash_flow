package com.dsi.spring.flujoreal.spring_cashflow.model;

import java.util.Date;

public class Proyecto {
    private int codCia;
    private int codPyto;
    private String nombPyto;
    private int annoIni;
    private int annoFin;
    private Date fecReg;
    private String vigente;

    public Proyecto() {}

    public int getCodCia() { return codCia; }
    public void setCodCia(int codCia) { this.codCia = codCia; }

    public int getCodPyto() { return codPyto; }
    public void setCodPyto(int codPyto) { this.codPyto = codPyto; }

    public String getNombPyto() { return nombPyto; }
    public void setNombPyto(String nombPyto) { this.nombPyto = nombPyto; }

    public int getAnnoIni() { return annoIni; }
    public void setAnnoIni(int annoIni) { this.annoIni = annoIni; }

    public int getAnnoFin() { return annoFin; }
    public void setAnnoFin(int annoFin) { this.annoFin = annoFin; }

    public Date getFecReg() { return fecReg; }
    public void setFecReg(Date fecReg) { this.fecReg = fecReg; }
    
    public String getVigente() {return vigente;}
    public void setVigente(String vigente) {this.vigente = vigente;
    }

}
