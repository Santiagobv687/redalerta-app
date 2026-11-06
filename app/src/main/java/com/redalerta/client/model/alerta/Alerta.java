package com.redalerta.client.model.alerta;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Corresponde a AlertaResponseDto en OpenAPI.
 */
public class Alerta implements Serializable {
    private static final long serialVersionUID = 1L;

    private String id;
    private String descripcion;
    private UbicacionAlertaDto ubicacion;
    private Long autorId;
    private String autorNombre;
    private String estado;
    private List<EvidenciaResponse> evidencias = new ArrayList<>();
    private boolean esAnonimo;
    private int totalEvidencias;
    private String fechaCreacion;
    private String fechaActualizacion;

    private Long liderId;
    private String liderNombre;
    private Long organizacionId;
    private String organizacionNombre;
    private String observacionTecnica;

    // Campos de conveniencia para la UI / Adapters
    private String titulo;
    private String categoria;
    private String prioridad;
    private String direccion;
    private String barrio;
    private boolean hayHeridos;
    private String evidenciaUrl;
    private String usuarioId;
    private String usuarioNombre;

    public Alerta() {
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }

    public UbicacionAlertaDto getUbicacion() { return ubicacion; }
    public void setUbicacion(UbicacionAlertaDto ubicacion) { this.ubicacion = ubicacion; }

    public Long getAutorId() { return autorId; }
    public void setAutorId(Long autorId) { this.autorId = autorId; }

    public String getAutorNombre() { return autorNombre != null ? autorNombre : usuarioNombre; }
    public void setAutorNombre(String autorNombre) { this.autorNombre = autorNombre; }

    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }

    public List<EvidenciaResponse> getEvidencias() { return evidencias; }
    public void setEvidencias(List<EvidenciaResponse> evidencias) { this.evidencias = evidencias; }

    public boolean isEsAnonimo() { return esAnonimo; }
    public void setEsAnonimo(boolean esAnonimo) { this.esAnonimo = esAnonimo; }

    public int getTotalEvidencias() { return totalEvidencias; }
    public void setTotalEvidencias(int totalEvidencias) { this.totalEvidencias = totalEvidencias; }

    public String getFechaCreacion() { return fechaCreacion; }
    public void setFechaCreacion(String fechaCreacion) { this.fechaCreacion = fechaCreacion; }

    public String getFechaActualizacion() { return fechaActualizacion; }
    public void setFechaActualizacion(String fechaActualizacion) { this.fechaActualizacion = fechaActualizacion; }

    public Long getLiderId() { return liderId; }
    public void setLiderId(Long liderId) { this.liderId = liderId; }

    public String getLiderNombre() { return liderNombre; }
    public void setLiderNombre(String liderNombre) { this.liderNombre = liderNombre; }

    public Long getOrganizacionId() { return organizacionId; }
    public void setOrganizacionId(Long organizacionId) { this.organizacionId = organizacionId; }

    public String getOrganizacionNombre() { return organizacionNombre; }
    public void setOrganizacionNombre(String organizacionNombre) { this.organizacionNombre = organizacionNombre; }

    public String getObservacionTecnica() { return observacionTecnica; }
    public void setObservacionTecnica(String observacionTecnica) { this.observacionTecnica = observacionTecnica; }

    // Helpers UI
    public String getTitulo() {
        if (titulo != null) return titulo;
        if (categoria != null) return categoria;
        return "Alerta Comunitaria";
    }
    public void setTitulo(String titulo) { this.titulo = titulo; }

    public String getCategoria() { return categoria; }
    public void setCategoria(String categoria) { this.categoria = categoria; }

    public String getPrioridad() { return prioridad; }
    public void setPrioridad(String prioridad) { this.prioridad = prioridad; }

    public String getDireccion() {
        if (direccion != null) return direccion;
        return ubicacion != null ? ubicacion.getDireccionDetallada() : "—";
    }
    public void setDireccion(String direccion) { this.direccion = direccion; }

    public String getBarrio() {
        if (barrio != null) return barrio;
        return ubicacion != null ? ubicacion.getMunicipio() : "—";
    }
    public void setBarrio(String barrio) { this.barrio = barrio; }

    public boolean isHayHeridos() { return hayHeridos; }
    public void setHayHeridos(boolean hayHeridos) { this.hayHeridos = hayHeridos; }

    public boolean isEsAnonima() { return esAnonimo; }
    public void setEsAnonima(boolean esAnonima) { this.esAnonimo = esAnonima; }

    public String getEvidenciaUrl() {
        if (evidenciaUrl != null) return evidenciaUrl;
        if (evidencias != null && !evidencias.isEmpty()) return evidencias.get(0).getUrl();
        return null;
    }
    public void setEvidenciaUrl(String evidenciaUrl) { this.evidenciaUrl = evidenciaUrl; }

    public String getUsuarioId() { return usuarioId; }
    public void setUsuarioId(String usuarioId) { this.usuarioId = usuarioId; }

    public String getUsuarioNombre() { return getAutorNombre(); }
    public void setUsuarioNombre(String usuarioNombre) { this.usuarioNombre = usuarioNombre; }
}
