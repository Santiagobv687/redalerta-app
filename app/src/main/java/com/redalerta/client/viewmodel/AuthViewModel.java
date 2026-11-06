package com.redalerta.client.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.redalerta.client.model.auth.LoginRequest;
import com.redalerta.client.model.auth.LoginResponse;
import com.redalerta.client.model.auth.RegistroRequest;
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

    public AuthViewModel(@NonNull Application application) {
        super(application);
    }

    public LiveData<Boolean> getCargando() { return cargando; }
    public LiveData<String> getError() { return error; }
    public LiveData<LoginResponse> getLoginExitoso() { return loginExitoso; }
    public LiveData<Boolean> getRegistroExitoso() { return registroExitoso; }
    public LiveData<Boolean> getLogoutExitoso() { return logoutExitoso; }

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
}
