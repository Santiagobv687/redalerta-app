package com.redalerta.client.viewmodel;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import com.redalerta.client.model.*;
import com.redalerta.client.network.ApiClient;
import com.redalerta.client.network.ApiResponse;
import com.redalerta.client.util.Validaciones;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AuthViewModel extends AndroidViewModel {

    private final MutableLiveData<Boolean> cargando = new MutableLiveData<>(false);
    private final MutableLiveData<String> error = new MutableLiveData<>();
    private final MutableLiveData<LoginResponse> loginExitoso = new MutableLiveData<>();

    public AuthViewModel(@NonNull Application application) {
        super(application);
    }

    public LiveData<Boolean> getCargando() { return cargando; }
    public LiveData<String> getError() { return error; }
    public LiveData<LoginResponse> getLoginExitoso() { return loginExitoso; }

    public void login(String correo, String contrasena) {
        cargando.setValue(true);
        error.setValue(null);

        ApiClient.getApiService(getApplication())
                .login(new LoginRequest(correo, contrasena))
                .enqueue(new Callback<ApiResponse<LoginResponse>>() {
                    @Override
                    public void onResponse(Call<ApiResponse<LoginResponse>> call,
                                           Response<ApiResponse<LoginResponse>> response) {
                        cargando.setValue(false);

                        if (!response.isSuccessful() || response.body() == null) {
                            error.setValue("Credenciales incorrectas");
                            return;
                        }
                        ApiResponse<LoginResponse> body = response.body();
                        if (!body.isSuccess() || body.getData() == null) {
                            error.setValue(body.getMessage() != null
                                    ? body.getMessage() : "No fue posible iniciar sesión");
                            return;
                        }

                        LoginResponse data = body.getData();
                        String rol = data.getUsuario().getRol();

                        // 🔒 Regla clave: admin NO entra por la app móvil
                        if (!Validaciones.esRolPermitidoEnMovil(rol)) {
                            error.setValue(
                                    "Los administradores deben ingresar desde el portal web de RedAlerta."
                            );
                            return;
                        }

                        loginExitoso.setValue(data);
                    }

                    @Override
                    public void onFailure(Call<ApiResponse<LoginResponse>> call, Throwable t) {
                        cargando.setValue(false);
                        error.setValue("Error de conexión: " + t.getMessage());
                    }
                });
    }

    private final MutableLiveData<Boolean> registroExitoso = new MutableLiveData<>();

    public LiveData<Boolean> getRegistroExitoso() { return registroExitoso; }

    public void registrar(RegistroRequest request) {
        cargando.setValue(true);
        error.setValue(null);

        ApiClient.getApiService(getApplication())
                .registrar(request)
                .enqueue(new Callback<ApiResponse<Usuario>>() {
                    @Override
                    public void onResponse(Call<ApiResponse<Usuario>> call,
                                           Response<ApiResponse<Usuario>> response) {
                        cargando.setValue(false);
                        if (response.isSuccessful() && response.body() != null
                                && response.body().isSuccess()) {
                            registroExitoso.setValue(true);
                        } else {
                            error.setValue(response.body() != null
                                    ? response.body().getMessage()
                                    : "No fue posible completar el registro");
                        }
                    }

                    @Override
                    public void onFailure(Call<ApiResponse<Usuario>> call, Throwable t) {
                        cargando.setValue(false);
                        error.setValue("Error de conexión: " + t.getMessage());
                    }
                });
    }
}