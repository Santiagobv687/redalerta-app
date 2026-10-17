package com.redalerta.client.model;

public enum CategoriaAlerta {
    INCENDIO,
    INUNDACION,
    ACCIDENTE,
    ROBO_HURTO,
    EMERGENCIA_MEDICA,
    DESASTRE_NATURAL,
    OTRO;

    public String etiqueta() {
        switch (this) {
            case INCENDIO:          return "Incendio";
            case INUNDACION:        return "Inundación";
            case ACCIDENTE:         return "Accidente";
            case ROBO_HURTO:        return "Robo / Hurto";
            case EMERGENCIA_MEDICA: return "Emergencia Médica";
            case DESASTRE_NATURAL:  return "Desastre Natural";
            default:                return "Otro";
        }
    }
}