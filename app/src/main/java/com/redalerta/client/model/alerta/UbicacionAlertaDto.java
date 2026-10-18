package com.redalerta.client.model.alerta;

import java.io.Serializable;

/**
 * Corresponde a UbicacionAlertaDto en OpenAPI.
 */
public class UbicacionAlertaDto implements Serializable {
    private static final long serialVersionUID = 1L;

    private String departamento;
    private String municipio;
    private String direccionDetallada;

    public UbicacionAlertaDto() {
    }

    public UbicacionAlertaDto(String departamento, String municipio, String direccionDetallada) {
        this.departamento = departamento;
        this.municipio = municipio;
        this.direccionDetallada = direccionDetallada;
    }

    public String getDepartamento() {
        return departamento;
    }

    public void setDepartamento(String departamento) {
        this.departamento = departamento;
    }

    public String getMunicipio() {
        return municipio;
    }

    public void setMunicipio(String municipio) {
        this.municipio = municipio;
    }

    public String getDireccionDetallada() {
        return direccionDetallada;
    }

    public void setDireccionDetallada(String direccionDetallada) {
        this.direccionDetallada = direccionDetallada;
    }
}
