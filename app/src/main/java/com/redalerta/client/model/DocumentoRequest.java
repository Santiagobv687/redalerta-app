package com.redalerta.client.model;

import java.io.Serializable;

/**
 * Corresponde a DocumentoRequestDto / DocumentoResponseDto en OpenAPI.
 * Tipos permitidos: "CEDULA", "PASAPORTE", "TARJETA_EXTRANJERIA".
 */
public class DocumentoRequest implements Serializable {
    private static final long serialVersionUID = 1L;

    private String numero;
    private String tipo;

    public DocumentoRequest() {
    }

    public DocumentoRequest(String numero, String tipo) {
        this.numero = numero;
        this.tipo = tipo;
    }

    public String getNumero() {
        return numero;
    }

    public void setNumero(String numero) {
        this.numero = numero;
    }

    public String getTipo() {
        return tipo;
    }

    public void setTipo(String tipo) {
        this.tipo = tipo;
    }
}
