package com.redalerta.client.model.auth;

import java.io.Serializable;

/**
 * Corresponde a LoginRequestDto en OpenAPI.
 */
public class LoginRequest implements Serializable {
    private static final long serialVersionUID = 1L;

    private String email;
    private String password;

    public LoginRequest() {
    }

    public LoginRequest(String email, String password) {
        this.email = email;
        this.password = password;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
