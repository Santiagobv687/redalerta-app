package com.redalerta.client.model;

import java.io.Serializable;

public class Alerta implements Serializable { // ⬅️ IMPLEMENTS SERIALIZABLE

    // Buena práctica: definir un serialVersionUID
    private static final long serialVersionUID = 1L;
    private String id;
    private String titulo;
    private String descripcion;
    private String categoria;
    private String prioridad;
    private String estado;
    private String direccion;
    private String barrio;
    private boolean hayHeridos;
    private boolean esAnonima;
    private String evidenciaUrl;
    private String usuarioId;
    private String usuarioNombre;
    private String fechaCreacion;
    private String fechaActualizacion;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getTitulo() { return titulo; }
    public void setTitulo(String titulo) { this.titulo = titulo; }

    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }

    public String getCategoria() { return categoria; }
    public void setCategoria(String categoria) { this.categoria = categoria; }

    public String getPrioridad() { return prioridad; }
    public void setPrioridad(String prioridad) { this.prioridad = prioridad; }

    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }

    public String getDireccion() { return direccion; }
    public void setDireccion(String direccion) { this.direccion = direccion; }

    public String getBarrio() { return barrio; }
    public void setBarrio(String barrio) { this.barrio = barrio; }

    public boolean isHayHeridos() { return hayHeridos; }
    public void setHayHeridos(boolean hayHeridos) { this.hayHeridos = hayHeridos; }

    public boolean isEsAnonima() { return esAnonima; }
    public void setEsAnonima(boolean esAnonima) { this.esAnonima = esAnonima; }

    public String getEvidenciaUrl() { return evidenciaUrl; }
    public void setEvidenciaUrl(String evidenciaUrl) { this.evidenciaUrl = evidenciaUrl; }

    public String getUsuarioId() { return usuarioId; }
    public void setUsuarioId(String usuarioId) { this.usuarioId = usuarioId; }

    public String getUsuarioNombre() { return usuarioNombre; }
    public void setUsuarioNombre(String usuarioNombre) { this.usuarioNombre = usuarioNombre; }

    public String getFechaCreacion() { return fechaCreacion; }
    public void setFechaCreacion(String fechaCreacion) { this.fechaCreacion = fechaCreacion; }

    public String getFechaActualizacion() { return fechaActualizacion; }
    public void setFechaActualizacion(String fechaActualizacion) { this.fechaActualizacion = fechaActualizacion; }
}