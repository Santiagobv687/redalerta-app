package com.redalerta.client.model;

public enum Rol {
    CIUDADANO,
    ADMINISTRADOR;

    /** Ambos roles pueden entrar por la app móvil. */
    public static boolean esRolMovil(String rol) {
        if (rol == null) return false;
        try {
            Rol.valueOf(rol.toUpperCase());
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
}