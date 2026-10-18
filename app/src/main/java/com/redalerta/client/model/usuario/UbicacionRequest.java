package com.redalerta.client.model.usuario;

import java.io.Serializable;

/**
 * Corresponde a UbicacionRequestDto / UbicacionResponseDto en OpenAPI.
 */
public class UbicacionRequest implements Serializable {
    private static final long serialVersionUID = 1L;

    private String departamento;
    private String municipio;
    private String direccionResidencia;

    public UbicacionRequest() {
    }

    public UbicacionRequest(String departamento, String municipio, String direccionResidencia) {
        this.departamento = departamento;
        this.municipio = municipio;
        this.direccionResidencia = direccionResidencia;
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

    public String getDireccionResidencia() {
        return direccionResidencia;
    }

    public void setDireccionResidencia(String direccionResidencia) {
        this.direccionResidencia = direccionResidencia;
    }
}
