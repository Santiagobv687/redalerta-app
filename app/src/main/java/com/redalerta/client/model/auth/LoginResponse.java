package com.redalerta.client.model.auth;

import com.redalerta.client.model.usuario.Usuario;

import java.io.Serializable;

/**
 * Corresponde a LoginResponseDto en OpenAPI.
 */
public class LoginResponse implements Serializable {
    private static final long serialVersionUID = 1L;

    private String token;
    private String tipo = "Bearer";
    private Usuario usuario;

    public LoginResponse() {
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getTipo() {
        return tipo;
    }

    public void setTipo(String tipo) {
        this.tipo = tipo;
    }

    public Usuario getUsuario() {
        return usuario;
    }

    public void setUsuario(Usuario usuario) {
        this.usuario = usuario;
    }
}
