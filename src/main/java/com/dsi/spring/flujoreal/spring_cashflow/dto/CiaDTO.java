package com.dsi.spring.flujoreal.spring_cashflow.dto;

public class CiaDTO {

    private int codCia;
    private String desCia;
    private String desCorta;
    private String vigente;   // 'S' / 'N'

    public int getCodCia() {
        return codCia;
    }

    public void setCodCia(int codCia) {
        this.codCia = codCia;
    }

    public String getDesCia() {
        return desCia;
    }

    public void setDesCia(String desCia) {
        this.desCia = desCia;
    }

    public String getDesCorta() {
        return desCorta;
    }

    public void setDesCorta(String desCorta) {
        this.desCorta = desCorta;
    }

    public String getVigente() {
        return vigente;
    }

    public void setVigente(String vigente) {
        this.vigente = vigente;
    }
}
