package com.redalerta.client.model.alerta;

import com.google.gson.annotations.SerializedName;
import java.io.Serializable;

/**
 * Corresponde a UbicacionAlertaDto en OpenAPI.
 * Datos geográficos y coordenadas geodésicas GPS de la ubicación del incidente.
 */
public class UbicacionAlertaDto implements Serializable {
    private static final long serialVersionUID = 1L;

    @SerializedName("departamento")
    private String departamento;

    @SerializedName("municipio")
    private String municipio;

    @SerializedName("direccionDetallada")
    private String direccionDetallada;

    @SerializedName("latitud")
    private Double latitud;

    @SerializedName("longitud")
    private Double longitud;

    @SerializedName("altitud")
    private Double altitud;

    public UbicacionAlertaDto() {
    }

    public UbicacionAlertaDto(String departamento, String municipio, String direccionDetallada) {
        this.departamento = departamento;
        this.municipio = municipio;
        this.direccionDetallada = direccionDetallada;
    }

    public UbicacionAlertaDto(String departamento,
                              String municipio,
                              String direccionDetallada,
                              Double latitud,
                              Double longitud,
                              Double altitud) {
        this.departamento = departamento;
        this.municipio = municipio;
        this.direccionDetallada = direccionDetallada;
        this.latitud = latitud;
        this.longitud = longitud;
        this.altitud = altitud;
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

    public Double getLatitud() {
        return latitud;
    }

    public void setLatitud(Double latitud) {
        this.latitud = latitud;
    }

    public Double getLongitud() {
        return longitud;
    }

    public void setLongitud(Double longitud) {
        this.longitud = longitud;
    }

    public Double getAltitud() {
        return altitud;
    }

    public void setAltitud(Double altitud) {
        this.altitud = altitud;
    }
}
