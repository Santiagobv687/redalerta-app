package com.redalerta.client.model.alerta;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Corresponde a CrearAlertaRequestDto en OpenAPI.
 */
public class CrearAlertaRequest implements Serializable {
    private static final long serialVersionUID = 1L;

    private String descripcion;
    private UbicacionAlertaDto ubicacion = new UbicacionAlertaDto();
    private List<String> evidenciasUrls = new ArrayList<>();
    private boolean esAnonimo;

    // Campos locales para UI
    private transient String titulo;
    private transient String categoria;
    private transient String prioridad;
    private transient boolean hayHeridos;
    private transient String usuarioId;

    public CrearAlertaRequest() {
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

    public boolean isEsAnonimo() {
        return esAnonimo;
    }

    public void setEsAnonimo(boolean esAnonimo) {
        this.esAnonimo = esAnonimo;
    }

    // Helper UI setters/getters
    public String getDireccion() {
        return ubicacion != null ? ubicacion.getDireccionDetallada() : null;
    }

    public void setDireccion(String direccion) {
        if (this.ubicacion == null) this.ubicacion = new UbicacionAlertaDto();
        this.ubicacion.setDireccionDetallada(direccion);
        if (this.ubicacion.getDepartamento() == null) this.ubicacion.setDepartamento("Quindío");
        if (this.ubicacion.getMunicipio() == null) this.ubicacion.setMunicipio("Armenia");
    }

    public String getBarrio() {
        return ubicacion != null ? ubicacion.getMunicipio() : null;
    }

    public void setBarrio(String barrio) {
        if (this.ubicacion == null) this.ubicacion = new UbicacionAlertaDto();
        if (barrio != null && !barrio.trim().isEmpty()) {
            this.ubicacion.setMunicipio(barrio);
        }
    }

    public String getEvidenciaUrl() {
        return (evidenciasUrls != null && !evidenciasUrls.isEmpty()) ? evidenciasUrls.get(0) : null;
    }

    public void setEvidenciaUrl(String url) {
        if (this.evidenciasUrls == null) this.evidenciasUrls = new ArrayList<>();
        this.evidenciasUrls.clear();
        if (url != null && !url.trim().isEmpty()) {
            this.evidenciasUrls.add(url);
        }
    }

    public String getTitulo() { return titulo; }
    public void setTitulo(String titulo) { this.titulo = titulo; }

    public String getCategoria() { return categoria; }
    public void setCategoria(String categoria) { this.categoria = categoria; }

    public String getPrioridad() { return prioridad; }
    public void setPrioridad(String prioridad) { this.prioridad = prioridad; }

    public boolean isHayHeridos() { return hayHeridos; }
    public void setHayHeridos(boolean hayHeridos) { this.hayHeridos = hayHeridos; }

    public boolean isEsAnonima() { return esAnonimo; }
    public void setEsAnonima(boolean esAnonima) { this.esAnonimo = esAnonima; }

    public String getUsuarioId() { return usuarioId; }
    public void setUsuarioId(String usuarioId) { this.usuarioId = usuarioId; }
}
