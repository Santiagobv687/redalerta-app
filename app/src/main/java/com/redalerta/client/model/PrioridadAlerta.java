package com.redalerta.client.model;

public enum PrioridadAlerta {
    ALTA,
    MEDIA,
    BAJA;

    public String etiqueta() {
        switch (this) {
            case ALTA:  return "Alta";
            case MEDIA: return "Media";
            default:    return "Baja";
        }
    }
}