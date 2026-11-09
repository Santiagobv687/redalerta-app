package com.redalerta.client.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;
import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKeys;
import java.security.KeyStore;

public class SessionManager {

    private static final String TAG = "SessionManager";
    private static final String PREF_NAME = "redalerta_session";
    private static final String KEY_TOKEN = "jwt_token";
    private static final String KEY_USUARIO_ID = "usuario_id";
    private static final String KEY_ROL = "usuario_rol";
    private static final String KEY_NOMBRE = "usuario_nombre";
    private static final String KEY_MUNICIPIO = "usuario_municipio";
    private static final String KEY_DEPARTAMENTO = "usuario_departamento";

    private static final String MASTER_KEY_ALIAS = "_master_key_";

    private final SharedPreferences prefs;

    public SessionManager(Context context) {
        Context appContext = context.getApplicationContext();
        SharedPreferences tempPrefs;
        try {
            tempPrefs = createEncryptedSharedPreferences(appContext);
        } catch (Exception e) {
            Log.w(TAG, "Error al abrir EncryptedSharedPreferences. Limpiando datos corruptos e intentando de nuevo...", e);
            try {
                // Borrar archivo de preferencias desincronizado
                appContext.deleteSharedPreferences(PREF_NAME);

                // Intentar borrar la entrada de la clave maestra en KeyStore si existe
                try {
                    KeyStore keyStore = KeyStore.getInstance("AndroidKeyStore");
                    keyStore.load(null);
                    if (keyStore.containsAlias(MASTER_KEY_ALIAS)) {
                        keyStore.deleteEntry(MASTER_KEY_ALIAS);
                    }
                } catch (Exception ksEx) {
                    Log.w(TAG, "No se pudo eliminar la MasterKey de KeyStore", ksEx);
                }

                tempPrefs = createEncryptedSharedPreferences(appContext);
            } catch (Exception e2) {
                Log.e(TAG, "Fallo el reintento de EncryptedSharedPreferences. Usando SharedPreferences estándar como respaldo.", e2);
                try {
                    tempPrefs = appContext.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
                } catch (Exception e3) {
                    Log.e(TAG, "Error crítico al obtener SharedPreferences estándar", e3);
                    tempPrefs = null;
                }
            }
        }
        this.prefs = tempPrefs;
    }

    private SharedPreferences createEncryptedSharedPreferences(Context context) throws Exception {
        String masterKey = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC);
        return EncryptedSharedPreferences.create(
                PREF_NAME, masterKey, context,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        );
    }

    public void guardarSesion(String token, String usuarioId, String rol, String nombre) {
        if (prefs == null) return;
        prefs.edit()
                .putString(KEY_TOKEN, token)
                .putString(KEY_USUARIO_ID, usuarioId)
                .putString(KEY_ROL, rol)
                .putString(KEY_NOMBRE, nombre)
                .apply();
    }

    public void guardarUbicacion(String departamento, String municipio) {
        if (prefs == null) return;
        prefs.edit()
                .putString(KEY_DEPARTAMENTO, departamento)
                .putString(KEY_MUNICIPIO, municipio)
                .apply();
    }

    public String getToken() { return prefs != null ? prefs.getString(KEY_TOKEN, null) : null; }
    public String getUsuarioId() { return prefs != null ? prefs.getString(KEY_USUARIO_ID, null) : null; }
    public String getRol() { return prefs != null ? prefs.getString(KEY_ROL, null) : null; }
    public String getNombre() { return prefs != null ? prefs.getString(KEY_NOMBRE, null) : null; }
    public String getMunicipio() { return prefs != null ? prefs.getString(KEY_MUNICIPIO, "Armenia") : "Armenia"; }
    public String getDepartamento() { return prefs != null ? prefs.getString(KEY_DEPARTAMENTO, "Quindío") : "Quindío"; }

    public boolean estaLogueado() { return getToken() != null; }
    public void cerrarSesion() { if (prefs != null) prefs.edit().clear().apply(); }
}

