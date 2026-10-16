package com.redalerta.client.model;

public enum Rol {
    USUARIO,
    LIDER,
    ORGANIZACION;

    public static boolean esRolMovil(String rol) {
        if (rol == null) return false;
        try {
            Rol.valueOf(rol.toUpperCase());
            return true;
        } catch (IllegalArgumentException e) {
            return false; // ADMIN u otro rol → no permitido en móvil
        }
    }
}