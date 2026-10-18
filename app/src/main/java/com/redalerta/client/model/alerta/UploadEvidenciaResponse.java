package com.redalerta.client.model.alerta;

import java.io.Serializable;

/**
 * Corresponde a UploadEvidenciaResponseDto en OpenAPI.
 */
public class UploadEvidenciaResponse implements Serializable {
    private static final long serialVersionUID = 1L;

    private String url;
    private String key;
    private String nombreOriginal;
    private long pesoOriginalBytes;
    private long pesoComprimidoBytes;
    private String tipoMime;

    public UploadEvidenciaResponse() {
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getNombreOriginal() {
        return nombreOriginal;
    }

    public void setNombreOriginal(String nombreOriginal) {
        this.nombreOriginal = nombreOriginal;
    }

    public long getPesoOriginalBytes() {
        return pesoOriginalBytes;
    }

    public void setPesoOriginalBytes(long pesoOriginalBytes) {
        this.pesoOriginalBytes = pesoOriginalBytes;
    }

    public long getPesoComprimidoBytes() {
        return pesoComprimidoBytes;
    }

    public void setPesoComprimidoBytes(long pesoComprimidoBytes) {
        this.pesoComprimidoBytes = pesoComprimidoBytes;
    }

    public String getTipoMime() {
        return tipoMime;
    }

    public void setTipoMime(String tipoMime) {
        this.tipoMime = tipoMime;
    }
}
