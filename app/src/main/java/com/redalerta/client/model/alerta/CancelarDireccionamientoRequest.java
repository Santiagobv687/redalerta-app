package com.redalerta.client.model.alerta;

import java.io.Serializable;

public class CancelarDireccionamientoRequest implements Serializable {
    private static final long serialVersionUID = 1L;

    private String motivo;

    public CancelarDireccionamientoRequest() {
    }

    public CancelarDireccionamientoRequest(String motivo) {
        this.motivo = motivo;
    }

    public String getMotivo() {
        return motivo;
    }

    public void setMotivo(String motivo) {
        this.motivo = motivo;
    }
}
