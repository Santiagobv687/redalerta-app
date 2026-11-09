package com.redalerta.client.model.alerta;

import com.google.gson.annotations.SerializedName;
import java.io.Serializable;

/**
 * Request para descartar o rechazar alerta (Pasa a RECHAZADA).
 */
public class RechazarAlertaRequest implements Serializable {
    private static final long serialVersionUID = 1L;

    @SerializedName("motivo")
    private String motivo; // Obligatorio, mín 10 y máx 500 caracteres

    public RechazarAlertaRequest() {
    }

    public RechazarAlertaRequest(String motivo) {
        this.motivo = motivo;
    }

    public String getMotivo() {
        return motivo;
    }

    public void setMotivo(String motivo) {
        this.motivo = motivo;
    }
}
