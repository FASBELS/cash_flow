package com.dsi.spring.flujoreal.spring_cashflow.dto;

import java.time.LocalDate;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonFormat;

public class ProyectoDetalleDTO {

    private int codCia;
    private int codPyto;
    private String nombPyto;
    private Integer annoIni;
    private Integer annoFin;
    private List<Integer> anios;

    @JsonFormat(pattern = "yyyy-MM-dd") 
    private LocalDate inicio;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate fin;

    public int getCodCia() {
        return codCia;
    }

    public void setCodCia(int codCia) {
        this.codCia = codCia;
    }

    public int getCodPyto() {
        return codPyto;
    }

    public void setCodPyto(int codPyto) {
        this.codPyto = codPyto;
    }

    public String getNombPyto() {
        return nombPyto;
    }

    public void setNombPyto(String nombPyto) {
        this.nombPyto = nombPyto;
    }

    public Integer getAnnoIni() {
        return annoIni;
    }

    public void setAnnoIni(Integer annoIni) {
        this.annoIni = annoIni;
    }

    public Integer getAnnoFin() {
        return annoFin;
    }

    public void setAnnoFin(Integer annoFin) {
        this.annoFin = annoFin;
    }

    public List<Integer> getAnios() {
        return anios;
    }

    public void setAnios(List<Integer> anios) {
        this.anios = anios;
    }

    public LocalDate getInicio() {
        return inicio;
    }

    public void setInicio(LocalDate inicio) {
        this.inicio = inicio;
    }

    public LocalDate getFin() {
        return fin;
    }

    public void setFin(LocalDate fin) {
        this.fin = fin;
    }
}
