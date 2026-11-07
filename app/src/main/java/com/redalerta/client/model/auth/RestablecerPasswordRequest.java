package com.redalerta.client.model.auth;

import com.google.gson.annotations.SerializedName;

/**
 * Corresponde a RestablecerPasswordRequestDto en OpenAPI.
 */
public class RestablecerPasswordRequest {

    @SerializedName("resetToken")
    private String resetToken;

    @SerializedName("nuevaPassword")
    private String nuevaPassword;

    public RestablecerPasswordRequest() {}

    public RestablecerPasswordRequest(String resetToken, String nuevaPassword) {
        this.resetToken = resetToken;
        this.nuevaPassword = nuevaPassword;
    }

    public String getResetToken() {
        return resetToken;
    }

    public void setResetToken(String resetToken) {
        this.resetToken = resetToken;
    }

    public String getNuevaPassword() {
        return nuevaPassword;
    }

    public void setNuevaPassword(String nuevaPassword) {
        this.nuevaPassword = nuevaPassword;
    }
}
