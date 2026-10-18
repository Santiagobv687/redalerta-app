package com.redalerta.client.model;

import java.io.Serializable;

/**
 * Corresponde a EvidenciaResponseDto en OpenAPI.
 */
public class EvidenciaResponse implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long id;
    private String url;
    private String tipoMime;
    private String fechaSubida;

    public EvidenciaResponse() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getTipoMime() {
        return tipoMime;
    }

    public void setTipoMime(String tipoMime) {
        this.tipoMime = tipoMime;
    }

    public String getFechaSubida() {
        return fechaSubida;
    }

    public void setFechaSubida(String fechaSubida) {
        this.fechaSubida = fechaSubida;
    }
}
