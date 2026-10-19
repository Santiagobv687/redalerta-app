package com.redalerta.client.model.alerta;

import java.io.Serializable;

public class DireccionarAlertaRequest implements Serializable {
    private static final long serialVersionUID = 1L;

    private String categoria;
    private String prioridad;
    private Long organizacionId;
    private String observacionTecnica;

    public DireccionarAlertaRequest() {
    }

    public DireccionarAlertaRequest(String categoria, String prioridad, Long organizacionId, String observacionTecnica) {
        this.categoria = categoria;
        this.prioridad = prioridad;
        this.organizacionId = organizacionId;
        this.observacionTecnica = observacionTecnica;
    }

    public String getCategoria() {
        return categoria;
    }

    public void setCategoria(String categoria) {
        this.categoria = categoria;
    }

    public String getPrioridad() {
        return prioridad;
    }

    public void setPrioridad(String prioridad) {
        this.prioridad = prioridad;
    }

    public Long getOrganizacionId() {
        return organizacionId;
    }

    public void setOrganizacionId(Long organizacionId) {
        this.organizacionId = organizacionId;
    }

    public String getObservacionTecnica() {
        return observacionTecnica;
    }

    public void setObservacionTecnica(String observacionTecnica) {
        this.observacionTecnica = observacionTecnica;
    }
}
