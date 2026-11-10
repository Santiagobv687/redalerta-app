package com.redalerta.client.model.notificacion;

import com.google.gson.annotations.SerializedName;
import java.io.Serializable;

/**
 * Corresponde a NotificacionResponseDto en OpenAPI.
 * Información estructurada de una notificación en la bandeja de entrada.
 */
public class NotificacionResponse implements Serializable {
    private static final long serialVersionUID = 1L;

    @SerializedName("id")
    private Long id;

    @SerializedName("alertaId")
    private Long alertaId;

    @SerializedName("titulo")
    private String titulo;

    @SerializedName("mensaje")
    private String mensaje;

    @SerializedName("nuevoEstado")
    private String nuevoEstado;

    @SerializedName("motivoAccion")
    private String motivoAccion;

    @SerializedName("responsableNombre")
    private String responsableNombre;

    @SerializedName("responsableRol")
    private String responsableRol;

    @SerializedName("leida")
    private boolean leida;

    @SerializedName("fechaCreacion")
    private String fechaCreacion;

    public NotificacionResponse() {
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getAlertaId() { return alertaId; }
    public void setAlertaId(Long alertaId) { this.alertaId = alertaId; }

    public String getTitulo() { return titulo; }
    public void setTitulo(String titulo) { this.titulo = titulo; }

    public String getMensaje() { return mensaje; }
    public void setMensaje(String mensaje) { this.mensaje = mensaje; }

    public String getNuevoEstado() { return nuevoEstado; }
    public void setNuevoEstado(String nuevoEstado) { this.nuevoEstado = nuevoEstado; }

    public String getMotivoAccion() { return motivoAccion; }
    public void setMotivoAccion(String motivoAccion) { this.motivoAccion = motivoAccion; }

    public String getResponsableNombre() { return responsableNombre; }
    public void setResponsableNombre(String responsableNombre) { this.responsableNombre = responsableNombre; }

    public String getResponsableRol() { return responsableRol; }
    public void setResponsableRol(String responsableRol) { this.responsableRol = responsableRol; }

    public boolean isLeida() { return leida; }
    public void setLeida(boolean leida) { this.leida = leida; }

    public String getFechaCreacion() { return fechaCreacion; }
    public void setFechaCreacion(String fechaCreacion) { this.fechaCreacion = fechaCreacion; }
}
