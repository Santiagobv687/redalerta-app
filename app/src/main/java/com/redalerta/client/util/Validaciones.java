package com.redalerta.client.util;

import android.util.Patterns;
import com.redalerta.client.model.Rol;

public class Validaciones {
    public static boolean esCorreoValido(String correo) {
        return correo != null && Patterns.EMAIL_ADDRESS.matcher(correo).matches();
    }

    public static boolean esContrasenaValida(String contrasena) {
        return contrasena != null && contrasena.length() >= 8;
    }

    public static boolean noEstaVacio(String texto) {
        return texto != null && !texto.trim().isEmpty();
    }

    /** Ambos roles son válidos en la app móvil. */
    public static boolean esRolPermitidoEnMovil(String rol) {
        return Rol.esRolMovil(rol);
    }
}