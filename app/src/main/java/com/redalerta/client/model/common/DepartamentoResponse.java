package com.redalerta.client.model.common;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Corresponde a DepartamentoResponseDto en el catálogo OpenAPI (/catalogo/ubicaciones).
 */
public class DepartamentoResponse implements Serializable {
    private static final long serialVersionUID = 1L;

    private String departamento;
    private List<String> municipios = new ArrayList<>();

    public DepartamentoResponse() {
    }

    public DepartamentoResponse(String departamento, List<String> municipios) {
        this.departamento = departamento;
        this.municipios = municipios;
    }

    public String getDepartamento() {
        return departamento;
    }

    public void setDepartamento(String departamento) {
        this.departamento = departamento;
    }

    public List<String> getMunicipios() {
        return municipios;
    }

    public void setMunicipios(List<String> municipios) {
        this.municipios = municipios;
    }
}
