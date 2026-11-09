package com.redalerta.client.model.alerta;

import com.google.gson.annotations.SerializedName;
import java.io.Serializable;

/**
 * Request para iniciar atención en terreno (Pasa de DIRECCIONADA a EN_GESTION).
 */
public class IniciarGestionRequest implements Serializable {
    private static final long serialVersionUID = 1L;

    @SerializedName("observacionInicial")
    private String observacionInicial; // Obligatorio, máx 500 caracteres

    public IniciarGestionRequest() {
    }

    public IniciarGestionRequest(String observacionInicial) {
        this.observacionInicial = observacionInicial;
    }

    public String getObservacionInicial() {
        return observacionInicial;
    }

    public void setObservacionInicial(String observacionInicial) {
        this.observacionInicial = observacionInicial;
    }
}
