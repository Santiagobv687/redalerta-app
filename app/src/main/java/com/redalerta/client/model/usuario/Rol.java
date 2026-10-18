package com.redalerta.client.model.usuario;

public class Rol {
    public static final String ROLE_CIUDADANO = "ROLE_CIUDADANO";
    public static final String ROLE_LIDER = "ROLE_LIDER";
    public static final String ROLE_ORGANIZACION = "ROLE_ORGANIZACION";
    public static final String ROLE_ADMIN = "ROLE_ADMIN";

    public static boolean esRolMovil(String rol) {
        if (rol == null) return false;
        String r = rol.trim().toUpperCase();
        return r.equals(ROLE_CIUDADANO) || r.equals(ROLE_LIDER)
                || r.equals(ROLE_ORGANIZACION) || r.equals(ROLE_ADMIN)
                || r.equals("CIUDADANO") || r.equals("ADMINISTRADOR") || r.equals("USUARIO");
    }
}
