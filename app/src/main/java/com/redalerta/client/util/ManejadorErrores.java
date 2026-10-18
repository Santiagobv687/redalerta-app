package com.redalerta.client.util;

import android.util.Log;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.redalerta.client.model.common.ErrorResponse;
import retrofit2.Response;

public class ManejadorErrores {

    private static final String TAG = "ManejadorErrores";

    public enum ContextoOperacion {
        LOGIN,
        REGISTRO,
        PERFIL,
        ALERTAS,
        EVIDENCIAS,
        GENERAL
    }

    public static ErrorResponse parsearErrorResponse(Response<?> response) {
        if (response == null || response.errorBody() == null) {
            return null;
        }
        try {
            String rawJson = response.errorBody().string();
            if (rawJson != null && !rawJson.trim().isEmpty()) {
                Gson gson = new Gson();
                return gson.fromJson(rawJson, ErrorResponse.class);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error al parsear ErrorResponse", e);
        }
        return null;
    }

    public static String obtenerMensajeAmigable(Response<?> response, ContextoOperacion contexto) {
        if (response == null) {
            return "No fue posible recibir respuesta del sistema RedAlerta. Por favor, intenta nuevamente.";
        }

        String rawJson = null;
        try {
            if (response.errorBody() != null) {
                rawJson = response.errorBody().string();
                Log.d(TAG, "ErrorBody JSON recibido (" + response.code() + "): " + rawJson);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error al leer errorBody", e);
        }

        // 1. Intentar extraer el mensaje desde la cadena JSON recibida
        if (rawJson != null && !rawJson.trim().isEmpty()) {
            try {
                // Intentar mapear a ErrorResponse según OpenAPI
                Gson gson = new Gson();
                ErrorResponse errorBody = gson.fromJson(rawJson, ErrorResponse.class);

                StringBuilder sb = new StringBuilder();
                if (errorBody != null) {
                    if (errorBody.getMessage() != null && !errorBody.getMessage().trim().isEmpty()) {
                        sb.append(errorBody.getMessage().trim());
                    }
                    if (errorBody.getErrors() != null && !errorBody.getErrors().isEmpty()) {
                        if (sb.length() > 0) sb.append("\n• ");
                        else sb.append("• ");
                        sb.append(String.join("\n• ", errorBody.getErrors()));
                    }
                }

                if (sb.length() > 0) {
                    return sb.toString();
                }

                // 2. Si no cuadró con ErrorResponse estricto, buscar campos genéricos
                JsonElement jsonElement = JsonParser.parseString(rawJson);
                if (jsonElement.isJsonObject()) {
                    JsonObject jsonObject = jsonElement.getAsJsonObject();

                    if (jsonObject.has("message") && !jsonObject.get("message").isJsonNull()) {
                        String msg = jsonObject.get("message").getAsString().trim();
                        if (!msg.isEmpty()) return msg;
                    }
                    if (jsonObject.has("error") && !jsonObject.get("error").isJsonNull()) {
                        String err = jsonObject.get("error").getAsString().trim();
                        if (!err.isEmpty()) return err;
                    }
                    if (jsonObject.has("errors") && jsonObject.get("errors").isJsonArray()) {
                        JsonArray arr = jsonObject.getAsJsonArray("errors");
                        StringBuilder errSb = new StringBuilder();
                        for (JsonElement el : arr) {
                            if (errSb.length() > 0) errSb.append("\n• ");
                            else errSb.append("• ");
                            errSb.append(el.getAsString());
                        }
                        if (errSb.length() > 0) return errSb.toString();
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "Error al parsear el JSON de error", e);
            }
        }

        // 3. Fallback de respuesta amigable centrado en el ciudadano
        int code = response.code();
        switch (code) {
            case 400:
                if (contexto == ContextoOperacion.LOGIN) {
                    return "Formato de correo o contraseña inválido. Por favor verifique sus datos.";
                } else if (contexto == ContextoOperacion.REGISTRO) {
                    return "Los datos ingresados contienen inconsistencias o el número de documento ya está registrado.";
                }
                return "La información ingresada no es válida. Por favor revise el formulario.";

            case 401:
                if (contexto == ContextoOperacion.LOGIN) {
                    return "Credenciales incorrectas. Por favor verifique su correo y contraseña.";
                }
                return "Su sesión ha caducado. Por favor inicie sesión nuevamente.";

            case 403:
                return "No tiene autorización para realizar esta acción.";

            case 404:
                return "La información o alerta solicitada no se encuentra disponible.";

            case 409:
                if (contexto == ContextoOperacion.REGISTRO) {
                    return "El correo electrónico o número de documento ya se encuentra registrado.";
                }
                return "Existe un conflicto con la solicitud enviada. Por favor intente nuevamente.";

            case 500:
            case 502:
            case 503:
                return "El sistema RedAlerta está experimentando un mantenimiento o problema temporal. Por favor intente en unos momentos.";

            default:
                return "No fue posible completar la solicitud. Por favor intente más tarde.";
        }
    }

    public static String obtenerMensajeFalloRed(Throwable t) {
        if (t == null) {
            return "No pudimos conectar con los servicios de RedAlerta. Por favor, verifica tu conexión a internet o intenta de nuevo en unos momentos.";
        }
        String msg = t.getLocalizedMessage() != null ? t.getLocalizedMessage().toLowerCase() : "";
        if (msg.contains("timeout") || msg.contains("timed out")) {
            return "El servicio de RedAlerta tardó demasiado en responder. Por favor, verifica la señal de tu red e intenta nuevamente.";
        }
        return "No fue posible conectar con el servicio de RedAlerta. Por favor, verifica que tu dispositivo tenga acceso a internet e intenta nuevamente.";
    }
}
