package com.redalerta.client.model.auth;

import com.google.gson.annotations.SerializedName;

/**
 * Corresponde a SolicitarRecuperacionRequestDto en OpenAPI.
 */
public class SolicitarRecuperacionRequest {

    @SerializedName("email")
    private String email;

    public SolicitarRecuperacionRequest() {}

    public SolicitarRecuperacionRequest(String email) {
        this.email = email;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }
}
