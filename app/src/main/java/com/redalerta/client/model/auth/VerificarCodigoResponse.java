package com.redalerta.client.model.auth;

import com.google.gson.annotations.SerializedName;

/**
 * Corresponde a VerificarCodigoResponseDto en OpenAPI.
 */
public class VerificarCodigoResponse {

    @SerializedName("resetToken")
    private String resetToken;

    @SerializedName("mensaje")
    private String mensaje;

    public String getResetToken() {
        return resetToken;
    }

    public void setResetToken(String resetToken) {
        this.resetToken = resetToken;
    }

    public String getMensaje() {
        return mensaje;
    }

    public void setMensaje(String mensaje) {
        this.mensaje = mensaje;
    }
}
