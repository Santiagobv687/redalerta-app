package com.redalerta.client.util;

import android.content.Context;
import android.content.SharedPreferences;
import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKeys;

public class SessionManager {

    private static final String PREF_NAME = "redalerta_session";
    private static final String KEY_TOKEN = "jwt_token";
    private static final String KEY_USUARIO_ID = "usuario_id";
    private static final String KEY_ROL = "usuario_rol";
    private static final String KEY_NOMBRE = "usuario_nombre";

    private final SharedPreferences prefs;

    public SessionManager(Context context) {
        try {
            String masterKey = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC);
            prefs = EncryptedSharedPreferences.create(
                    PREF_NAME, masterKey, context,
                    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            );
        } catch (Exception e) {
            throw new RuntimeException("Error al inicializar sesión segura", e);
        }
    }

    public void guardarSesion(String token, String usuarioId, String rol, String nombre) {
        prefs.edit()
                .putString(KEY_TOKEN, token)
                .putString(KEY_USUARIO_ID, usuarioId)
                .putString(KEY_ROL, rol)
                .putString(KEY_NOMBRE, nombre)
                .apply();
    }

    public String getToken() { return prefs.getString(KEY_TOKEN, null); }
    public String getUsuarioId() { return prefs.getString(KEY_USUARIO_ID, null); }
    public String getRol() { return prefs.getString(KEY_ROL, null); }
    public String getNombre() { return prefs.getString(KEY_NOMBRE, null); }

    public boolean estaLogueado() { return getToken() != null; }
    public void cerrarSesion() { prefs.edit().clear().apply(); }
}