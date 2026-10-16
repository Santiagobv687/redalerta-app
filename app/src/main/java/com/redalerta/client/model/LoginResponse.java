package com.redalerta.client.model;

public class LoginResponse {
    private String token;
    private Usuario usuario;
    // getters


    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public Usuario getUsuario() {
        return usuario;
    }

    public void setUsuario(Usuario usuario) {
        this.usuario = usuario;
    }
}