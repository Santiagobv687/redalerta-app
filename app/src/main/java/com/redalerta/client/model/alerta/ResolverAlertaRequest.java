package com.redalerta.client.model.alerta;

import com.google.gson.annotations.SerializedName;
import java.io.Serializable;

/**
 * Request para resolver y cerrar alerta (Pasa de EN_GESTION a RESUELTA).
 */
public class ResolverAlertaRequest implements Serializable {
    private static final long serialVersionUID = 1L;

    @SerializedName("informeResolucion")
    private String informeResolucion; // Obligatorio, mín 20 y máx 1500 caracteres

    public ResolverAlertaRequest() {
    }

    public ResolverAlertaRequest(String informeResolucion) {
        this.informeResolucion = informeResolucion;
    }

    public String getInformeResolucion() {
        return informeResolucion;
    }

    public void setInformeResolucion(String informeResolucion) {
        this.informeResolucion = informeResolucion;
    }
}
