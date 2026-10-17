package com.redalerta.client.model;

public class CrearAlertaRequest {
    private String titulo;
    private String descripcion;
    private String categoria;
    private String prioridad;
    private String direccion;
    private String barrio;
    private boolean hayHeridos;
    private boolean esAnonima;
    private String evidenciaUrl;
    private String usuarioId;

    // getters y setters
    public String getTitulo() { return titulo; }
    public void setTitulo(String titulo) { this.titulo = titulo; }

    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }

    public String getCategoria() { return categoria; }
    public void setCategoria(String categoria) { this.categoria = categoria; }

    public String getPrioridad() { return prioridad; }
    public void setPrioridad(String prioridad) { this.prioridad = prioridad; }

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
}