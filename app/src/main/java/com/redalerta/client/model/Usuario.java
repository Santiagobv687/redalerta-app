package com.redalerta.client.model;

import java.io.Serializable;

/**
 * Corresponde a UsuarioResponseDto / UsuarioSesionDto en OpenAPI.
 */
public class Usuario implements Serializable {
    private static final long serialVersionUID = 1L;

    private String id;
    private String nombre;
    private String email;
    private String telefono;
    private String rol; // "ROLE_CIUDADANO", "ROLE_LIDER", "ROLE_ORGANIZACION", "ROLE_ADMIN"
    private Boolean activo;
    private DocumentoRequest documento;
    private UbicacionRequest ubicacion;
    private CaracterizacionRequest caracterizacion;

    public Usuario() {
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getCorreo() {
        return email;
    }

    public void setCorreo(String correo) {
        this.email = correo;
    }

    public String getTelefono() {
        return telefono;
    }

    public void setTelefono(String telefono) {
        this.telefono = telefono;
    }

    public String getRol() {
        return rol;
    }

    public void setRol(String rol) {
        this.rol = rol;
    }

    public Boolean getActivo() {
        return activo;
    }

    public void setActivo(Boolean activo) {
        this.activo = activo;
    }

    public DocumentoRequest getDocumento() {
        return documento;
    }

    public void setDocumento(DocumentoRequest documento) {
        this.documento = documento;
    }

    public UbicacionRequest getUbicacion() {
        return ubicacion;
    }

    public void setUbicacion(UbicacionRequest ubicacion) {
        this.ubicacion = ubicacion;
    }

    public CaracterizacionRequest getCaracterizacion() {
        return caracterizacion;
    }

    public void setCaracterizacion(CaracterizacionRequest caracterizacion) {
        this.caracterizacion = caracterizacion;
    }
}
