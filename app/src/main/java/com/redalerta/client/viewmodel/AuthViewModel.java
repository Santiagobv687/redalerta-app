package com.redalerta.client.viewmodel;

import android.app.Application;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.redalerta.client.model.*;
import com.redalerta.client.network.ApiClient;
import com.redalerta.client.network.ApiResponse;
import com.redalerta.client.util.ManejadorErrores;
import com.redalerta.client.util.ManejadorErrores.ContextoOperacion;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AuthViewModel extends AndroidViewModel {

    // Se establece MODO_MOCK a false para consumir la API REST real
    private static final boolean MODO_MOCK = false;

    private final MutableLiveData<Boolean> cargando = new MutableLiveData<>(false);
    private final MutableLiveData<String> error = new MutableLiveData<>();
    private final MutableLiveData<LoginResponse> loginExitoso = new MutableLiveData<>();
    private final MutableLiveData<Boolean> registroExitoso = new MutableLiveData<>();

    public AuthViewModel(@NonNull Application application) {
        super(application);
    }

    public LiveData<Boolean> getCargando() { return cargando; }
    public LiveData<String> getError() { return error; }
    public LiveData<LoginResponse> getLoginExitoso() { return loginExitoso; }
    public LiveData<Boolean> getRegistroExitoso() { return registroExitoso; }

    // ══════════════════════════════════════════════════
    // LOGIN
    // ══════════════════════════════════════════════════
    public void login(String correo, String contrasena) {
        cargando.setValue(true);
        error.setValue(null);

        if (MODO_MOCK) {
            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                cargando.setValue(false);
                if (correo.contains("@") && contrasena.length() >= 6) {
                    Usuario u = new Usuario();
                    u.setId("1");
                    u.setNombre("Usuario Demo");
                    u.setEmail(correo);
                    u.setRol("ROLE_CIUDADANO");

                    LoginResponse resp = new LoginResponse();
                    resp.setToken("mock-jwt-token-12345");
                    resp.setUsuario(u);
                    loginExitoso.setValue(resp);
                } else {
                    error.setValue("Credenciales incorrectas");
                }
            }, 1200);
            return;
        }

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
    // REGISTRO
    // ══════════════════════════════════════════════════
    public void registrar(RegistroRequest request) {
        cargando.setValue(true);
        error.setValue(null);

        if (MODO_MOCK) {
            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                cargando.setValue(false);
                registroExitoso.setValue(true);
            }, 1500);
            return;
        }

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
}
