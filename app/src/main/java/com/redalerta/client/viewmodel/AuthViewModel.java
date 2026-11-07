package com.redalerta.client.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.redalerta.client.model.auth.LoginRequest;
import com.redalerta.client.model.auth.LoginResponse;
import com.redalerta.client.model.auth.RegistroRequest;
import com.redalerta.client.model.auth.RestablecerPasswordRequest;
import com.redalerta.client.model.auth.SolicitarRecuperacionRequest;
import com.redalerta.client.model.auth.VerificarCodigoRequest;
import com.redalerta.client.model.auth.VerificarCodigoResponse;
import com.redalerta.client.model.usuario.Usuario;
import com.redalerta.client.network.ApiClient;
import com.redalerta.client.network.ApiResponse;
import com.redalerta.client.util.ManejadorErrores;
import com.redalerta.client.util.ManejadorErrores.ContextoOperacion;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AuthViewModel extends AndroidViewModel {

    private final MutableLiveData<Boolean> cargando = new MutableLiveData<>(false);
    private final MutableLiveData<String> error = new MutableLiveData<>();
    private final MutableLiveData<LoginResponse> loginExitoso = new MutableLiveData<>();
    private final MutableLiveData<Boolean> registroExitoso = new MutableLiveData<>();
    private final MutableLiveData<Boolean> logoutExitoso = new MutableLiveData<>();
    private final MutableLiveData<String> recuperacionSolicitada = new MutableLiveData<>();
    private final MutableLiveData<VerificarCodigoResponse> codigoVerificado = new MutableLiveData<>();
    private final MutableLiveData<Boolean> passwordRestablecida = new MutableLiveData<>();

    public AuthViewModel(@NonNull Application application) {
        super(application);
    }

    public LiveData<Boolean> getCargando() { return cargando; }
    public LiveData<String> getError() { return error; }
    public LiveData<LoginResponse> getLoginExitoso() { return loginExitoso; }
    public LiveData<Boolean> getRegistroExitoso() { return registroExitoso; }
    public LiveData<Boolean> getLogoutExitoso() { return logoutExitoso; }
    public LiveData<String> getRecuperacionSolicitada() { return recuperacionSolicitada; }
    public LiveData<VerificarCodigoResponse> getCodigoVerificado() { return codigoVerificado; }
    public LiveData<Boolean> getPasswordRestablecida() { return passwordRestablecida; }

    public void limpiarEstadoRecuperacionSolicitada() {
        recuperacionSolicitada.setValue(null);
    }

    public void limpiarEstadoCodigoVerificado() {
        codigoVerificado.setValue(null);
    }

    // ══════════════════════════════════════════════════
    // LOGIN (POST /api/v1/auth/login)
    // ══════════════════════════════════════════════════
    public void login(String correo, String contrasena) {
        cargando.setValue(true);
        error.setValue(null);

        LoginRequest request = new LoginRequest(correo, contrasena);
        ApiClient.getApiService(getApplication())
                .login(request)
                .enqueue(new Callback<ApiResponse<LoginResponse>>() {
                    @Override
                    public void onResponse(@NonNull Call<ApiResponse<LoginResponse>> call,
                                           @NonNull Response<ApiResponse<LoginResponse>> response) {
                        cargando.setValue(false);
                        if (response.isSuccessful() && response.body() != null) {
                            ApiResponse<LoginResponse> body = response.body();
                            if (body.isSuccess() && body.getData() != null) {
                                loginExitoso.setValue(body.getData());
                            } else {
                                error.setValue(body.getMessage() != null ? body.getMessage() : "Credenciales inválidas");
                            }
                        } else {
                            error.setValue(ManejadorErrores.obtenerMensajeAmigable(response, ContextoOperacion.LOGIN));
                        }
                    }

                    @Override
                    public void onFailure(@NonNull Call<ApiResponse<LoginResponse>> call, @NonNull Throwable t) {
                        cargando.setValue(false);
                        error.setValue(ManejadorErrores.obtenerMensajeFalloRed(t));
                    }
                });
    }

    // ══════════════════════════════════════════════════
    // REGISTRO (POST /api/v1/auth/registro)
    // ══════════════════════════════════════════════════
    public void registrar(RegistroRequest request) {
        cargando.setValue(true);
        error.setValue(null);

        ApiClient.getApiService(getApplication())
                .registrar(request)
                .enqueue(new Callback<ApiResponse<Usuario>>() {
                    @Override
                    public void onResponse(@NonNull Call<ApiResponse<Usuario>> call,
                                           @NonNull Response<ApiResponse<Usuario>> response) {
                        cargando.setValue(false);
                        if (response.isSuccessful() && response.body() != null) {
                            ApiResponse<Usuario> body = response.body();
                            if (body.isSuccess()) {
                                registroExitoso.setValue(true);
                            } else {
                                error.setValue(body.getMessage() != null ? body.getMessage() : "No fue posible registrar la cuenta");
                            }
                        } else {
                            error.setValue(ManejadorErrores.obtenerMensajeAmigable(response, ContextoOperacion.REGISTRO));
                        }
                    }

                    @Override
                    public void onFailure(@NonNull Call<ApiResponse<Usuario>> call, @NonNull Throwable t) {
                        cargando.setValue(false);
                        error.setValue(ManejadorErrores.obtenerMensajeFalloRed(t));
                    }
                });
    }

    // ══════════════════════════════════════════════════
    // LOGOUT (POST /api/v1/auth/logout)
    // ══════════════════════════════════════════════════
    public void logout() {
        cargando.setValue(true);
        error.setValue(null);

        ApiClient.getApiService(getApplication())
                .logout()
                .enqueue(new Callback<ApiResponse<Void>>() {
                    @Override
                    public void onResponse(@NonNull Call<ApiResponse<Void>> call,
                                           @NonNull Response<ApiResponse<Void>> response) {
                        cargando.setValue(false);
                        logoutExitoso.setValue(true);
                    }

                    @Override
                    public void onFailure(@NonNull Call<ApiResponse<Void>> call, @NonNull Throwable t) {
                        cargando.setValue(false);
                        logoutExitoso.setValue(true);
                    }
                });
    }

    // ══════════════════════════════════════════════════
    // RECUPERAR PASSWORD - PASO 1 (POST /api/v1/auth/recuperar-password)
    // ══════════════════════════════════════════════════
    public void solicitarRecuperacion(String correo) {
        cargando.setValue(true);
        error.setValue(null);

        SolicitarRecuperacionRequest request = new SolicitarRecuperacionRequest(correo);
        ApiClient.getApiService(getApplication())
                .solicitarRecuperacion(request)
                .enqueue(new Callback<ApiResponse<Void>>() {
                    @Override
                    public void onResponse(@NonNull Call<ApiResponse<Void>> call,
                                           @NonNull Response<ApiResponse<Void>> response) {
                        cargando.setValue(false);
                        if (response.isSuccessful() && response.body() != null) {
                            ApiResponse<Void> body = response.body();
                            if (body.isSuccess()) {
                                String msg = (body.getMessage() != null && !body.getMessage().trim().isEmpty())
                                        ? body.getMessage()
                                        : "Código de verificación enviado a su correo electrónico.";
                                recuperacionSolicitada.setValue(msg);
                            } else {
                                error.setValue(body.getMessage() != null
                                        ? body.getMessage()
                                        : "No fue posible procesar la solicitud");
                            }
                        } else {
                            error.setValue(ManejadorErrores.obtenerMensajeAmigable(response, ContextoOperacion.GENERAL));
                        }
                    }

                    @Override
                    public void onFailure(@NonNull Call<ApiResponse<Void>> call, @NonNull Throwable t) {
                        cargando.setValue(false);
                        error.setValue(ManejadorErrores.obtenerMensajeFalloRed(t));
                    }
                });
    }

    // ══════════════════════════════════════════════════
    // RECUPERAR PASSWORD - PASO 2 (POST /api/v1/auth/verificar-codigo)
    // ══════════════════════════════════════════════════
    public void verificarCodigo(String correo, String codigo) {
        cargando.setValue(true);
        error.setValue(null);

        VerificarCodigoRequest request = new VerificarCodigoRequest(correo, codigo);
        ApiClient.getApiService(getApplication())
                .verificarCodigo(request)
                .enqueue(new Callback<ApiResponse<VerificarCodigoResponse>>() {
                    @Override
                    public void onResponse(@NonNull Call<ApiResponse<VerificarCodigoResponse>> call,
                                           @NonNull Response<ApiResponse<VerificarCodigoResponse>> response) {
                        cargando.setValue(false);
                        if (response.isSuccessful() && response.body() != null) {
                            ApiResponse<VerificarCodigoResponse> body = response.body();
                            if (body.isSuccess() && body.getData() != null) {
                                codigoVerificado.setValue(body.getData());
                            } else {
                                error.setValue(body.getMessage() != null
                                        ? body.getMessage()
                                        : "El código ingresado es inválido o ha expirado");
                            }
                        } else {
                            error.setValue(ManejadorErrores.obtenerMensajeAmigable(response, ContextoOperacion.GENERAL));
                        }
                    }

                    @Override
                    public void onFailure(@NonNull Call<ApiResponse<VerificarCodigoResponse>> call, @NonNull Throwable t) {
                        cargando.setValue(false);
                        error.setValue(ManejadorErrores.obtenerMensajeFalloRed(t));
                    }
                });
    }

    // ══════════════════════════════════════════════════
    // RECUPERAR PASSWORD - PASO 3 (POST /api/v1/auth/restablecer-password)
    // ══════════════════════════════════════════════════
    public void restablecerPassword(String resetToken, String nuevaPassword) {
        cargando.setValue(true);
        error.setValue(null);

        RestablecerPasswordRequest request = new RestablecerPasswordRequest(resetToken, nuevaPassword);
        ApiClient.getApiService(getApplication())
                .restablecerPassword(request)
                .enqueue(new Callback<ApiResponse<Void>>() {
                    @Override
                    public void onResponse(@NonNull Call<ApiResponse<Void>> call,
                                           @NonNull Response<ApiResponse<Void>> response) {
                        cargando.setValue(false);
                        if (response.isSuccessful() && response.body() != null) {
                            ApiResponse<Void> body = response.body();
                            if (body.isSuccess()) {
                                passwordRestablecida.setValue(true);
                            } else {
                                error.setValue(body.getMessage() != null
                                        ? body.getMessage()
                                        : "No fue posible actualizar la contraseña");
                            }
                        } else {
                            error.setValue(ManejadorErrores.obtenerMensajeAmigable(response, ContextoOperacion.GENERAL));
                        }
                    }

                    @Override
                    public void onFailure(@NonNull Call<ApiResponse<Void>> call, @NonNull Throwable t) {
                        cargando.setValue(false);
                        error.setValue(ManejadorErrores.obtenerMensajeFalloRed(t));
                    }
                });
    }
}
