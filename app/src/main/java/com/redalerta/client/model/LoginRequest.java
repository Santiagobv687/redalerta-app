package com.redalerta.client.model;

public class LoginRequest {
    private String correo;
    private String contrasena;

    public LoginRequest(String correo, String contrasena) {
        this.correo = correo;
        this.contrasena = contrasena;
    }
    // getters
}