package com.redalerta.client.model.common;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Corresponde a DepartamentoMunicipiosDto en el catálogo OpenAPI (/ubicaciones/catalogo).
 */
public class DepartamentoResponse implements Serializable {
    private static final long serialVersionUID = 1L;

    private Integer id;
    private String departamento;
    private List<String> municipios = new ArrayList<>();

    public DepartamentoResponse() {
    }

    public DepartamentoResponse(String departamento, List<String> municipios) {
        this.departamento = departamento;
        this.municipios = municipios;
    }

    public DepartamentoResponse(Integer id, String departamento, List<String> municipios) {
        this.id = id;
        this.departamento = departamento;
        this.municipios = municipios;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
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
