package com.redalerta.client.model.usuario;

import java.io.Serializable;

/**
 * Corresponde a ActualizarPerfilRequestDto en OpenAPI.
 */
public class ActualizarPerfilRequest implements Serializable {
    private static final long serialVersionUID = 1L;

    private String nombre;
    private String email;
    private String telefono;
    private String passwordActual;
    private String passwordNuevo;

    public ActualizarPerfilRequest() {
    }

    public ActualizarPerfilRequest(String nombre, String email, String telefono) {
        this.nombre = nombre;
        this.email = email;
        this.telefono = telefono;
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

    public String getTelefono() {
        return telefono;
    }

    public void setTelefono(String telefono) {
        this.telefono = telefono;
    }

    public String getPasswordActual() {
        return passwordActual;
    }

    public void setPasswordActual(String passwordActual) {
        this.passwordActual = passwordActual;
    }

    public String getPasswordNuevo() {
        return passwordNuevo;
    }

    public void setPasswordNuevo(String passwordNuevo) {
        this.passwordNuevo = passwordNuevo;
    }
}
