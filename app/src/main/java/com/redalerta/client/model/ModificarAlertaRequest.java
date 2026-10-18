package com.redalerta.client.model;

import java.io.Serializable;
import java.util.List;

/**
 * Corresponde a ModificarAlertaRequestDto en OpenAPI.
 */
public class ModificarAlertaRequest implements Serializable {
    private static final long serialVersionUID = 1L;

    private String descripcion;
    private UbicacionAlertaDto ubicacion;
    private List<String> evidenciasUrls;
    private Boolean esAnonimo;

    public ModificarAlertaRequest() {
    }

    public ModificarAlertaRequest(String descripcion, UbicacionAlertaDto ubicacion, List<String> evidenciasUrls, Boolean esAnonimo) {
        this.descripcion = descripcion;
        this.ubicacion = ubicacion;
        this.evidenciasUrls = evidenciasUrls;
        this.esAnonimo = esAnonimo;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public UbicacionAlertaDto getUbicacion() {
        return ubicacion;
    }

    public void setUbicacion(UbicacionAlertaDto ubicacion) {
        this.ubicacion = ubicacion;
    }

    public List<String> getEvidenciasUrls() {
        return evidenciasUrls;
    }

    public void setEvidenciasUrls(List<String> evidenciasUrls) {
        this.evidenciasUrls = evidenciasUrls;
    }

    public Boolean getEsAnonimo() {
        return esAnonimo;
    }

    public void setEsAnonimo(Boolean esAnonimo) {
        this.esAnonimo = esAnonimo;
    }
}
