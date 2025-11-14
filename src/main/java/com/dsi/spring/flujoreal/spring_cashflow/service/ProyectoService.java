package com.dsi.spring.flujoreal.spring_cashflow.service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;

import com.dsi.spring.flujoreal.spring_cashflow.dao.ProyectoDAO;
import com.dsi.spring.flujoreal.spring_cashflow.dto.ProyectoDetalleDTO;
import com.dsi.spring.flujoreal.spring_cashflow.factory.DAOFactory;

@Service
public class ProyectoService {

    private final ProyectoDAO proyectoDAO;

    public ProyectoService() {
        var f = DAOFactory.getDAOFactory(DAOFactory.ORACLE);
        this.proyectoDAO = f.getProyectoDAO();
    }

    public List<ProyectoDetalleDTO> listar() throws Exception {
        return proyectoDAO.listarProyectos();
    }

    public ProyectoDetalleDTO detalle(int codCia, int codPyto) throws Exception {
        // como tu DAO no tiene "obtenerPorId", buscamos en memoria tras listar
        ProyectoDetalleDTO base = proyectoDAO.listarProyectos().stream()
                .filter(p -> p.getCodPyto()==codPyto)
                .findFirst()
                .orElse(null);
        if (base == null) return null;

        ProyectoDetalleDTO dto = new ProyectoDetalleDTO();
        dto.setCodPyto(base.getCodPyto());
        dto.setNombPyto(base.getNombPyto());
        dto.setAnnoIni(base.getAnnoIni());
        dto.setAnnoFin(base.getAnnoFin());

        // años: [AnnoIni..AnnoFin]
        List<Integer> anios = new ArrayList<>();
        if (base.getAnnoIni()!=0 && base.getAnnoFin()!=0 && base.getAnnoIni()<=base.getAnnoFin()) {
            for (int y = base.getAnnoIni(); y <= base.getAnnoFin(); y++) anios.add(y);
        }
        dto.setAnios(anios);

        // Inicio/Fin (usamos 01/01 y 31/12 de esos años)
        if (base.getAnnoIni()!=0) dto.setInicio(LocalDate.of(base.getAnnoIni(), 1, 1));
        if (base.getAnnoFin()!=0) dto.setFin(LocalDate.of(base.getAnnoFin(), 12, 31));

        return dto;
    }
}
