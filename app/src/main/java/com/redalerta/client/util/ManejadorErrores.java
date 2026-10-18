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
            return "No se recibió respuesta del servidor. Intente nuevamente.";
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

                // 2. Si no cuadró con ErrorResponse estricto, buscar campos "message", "error" o "errors" genéricos de Spring Boot
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

        // 3. Fallback de respuesta amigable basado en el código HTTP de respuesta según OpenAPI
        int code = response.code();
        switch (code) {
            case 400:
                if (contexto == ContextoOperacion.LOGIN) {
                    return "Formato de correo o contraseña inválido. Por favor verifique sus datos.";
                } else if (contexto == ContextoOperacion.REGISTRO) {
                    return "Los datos ingresados contienen inconsistencias o el número de documento ya está registrado.";
                }
                return "Los datos enviados no son válidos. Por favor revise el formulario.";

            case 401:
                if (contexto == ContextoOperacion.LOGIN) {
                    return "Credenciales inválidas. Por favor verifique su correo y contraseña.";
                }
                return "Su sesión ha caducado o no se encuentra autenticado.";

            case 403:
                return "No tiene permisos para realizar esta acción.";

            case 404:
                return "El usuario o recurso solicitado no fue encontrado.";

            case 409:
                if (contexto == ContextoOperacion.REGISTRO) {
                    return "El correo electrónico o número de documento ya se encuentra registrado. Por favor intente iniciar sesión.";
                }
                return "Existe un conflicto con los datos ingresados en el servidor.";

            case 500:
            case 502:
            case 503:
                return "El servidor de RedAlerta está experimentando problemas técnicos. Por favor intente más tarde.";

            default:
                return "Ocurrió un error inesperado al comunicarse con el servidor (Código " + code + ").";
        }
    }

    public static String obtenerMensajeFalloRed(Throwable t) {
        if (t == null) {
            return "No fue posible conectar con el servidor de RedAlerta.";
        }
        String msg = t.getLocalizedMessage() != null ? t.getLocalizedMessage().toLowerCase() : "";
        if (msg.contains("connect") || msg.contains("failed to connect") || msg.contains("refused")) {
            return "No fue posible conectar con el servidor. Verifique que la aplicación backend esté ejecutándose y que su teléfono esté en la misma red.";
        }
        if (msg.contains("timeout")) {
            return "La conexión con el servidor tardó demasiado tiempo. Verifique su red e intente nuevamente.";
        }
        return "No fue posible establecer conexión con el servidor. Verifique su red e intente nuevamente.";
    }
}
