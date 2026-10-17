package com.redalerta.client.viewmodel;

import android.app.Application;
import android.os.Handler;
import android.os.Looper;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import com.redalerta.client.model.*;
import com.redalerta.client.util.Validaciones;

public class AuthViewModel extends AndroidViewModel {

    // ⚠️ MODO PRUEBA: cambia a false cuando tengas el backend
    private static final boolean MODO_MOCK = true;

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
                    u.setId("mock-admin-001");
                    u.setNombre("Admin Demo");
                    u.setCorreo(correo);
                    u.setRol("ADMINISTRADOR");   // ✅ Ahora es admin

                    LoginResponse resp = new LoginResponse();
                    resp.setToken("mock-jwt-token-admin");
                    resp.setUsuario(u);
                    loginExitoso.setValue(resp);
                } else {
                    error.setValue("Credenciales incorrectas");
                }
            }, 1200);
            return;
        }
        /*
        if (MODO_MOCK) {
            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                cargando.setValue(false);
                // Simula credenciales válidas para cualquier correo con @
                if (correo.contains("@") && contrasena.length() >= 6) {
                    Usuario u = new Usuario();
                    u.setId("mock-001");
                    u.setNombre("Usuario Demo");
                    u.setCorreo(correo);
                    u.setRol("CIUDADANO"); // Prueba también con "ADMINISTRADOR"

                    LoginResponse resp = new LoginResponse();
                    resp.setToken("mock-jwt-token-12345");
                    resp.setUsuario(u);
                    loginExitoso.setValue(resp);
                } else {
                    error.setValue("Credenciales incorrectas");
                }
            }, 1200); // Simula 1.2s de latencia de red
            return;


        }
 */
        // ---- Código real (backend) ----
        // ApiClient.getApiService(getApplication())
        //     .login(new LoginRequest(correo, contrasena))
        //     .enqueue(...); // tu implementación real
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
                // Simula registro exitoso siempre
                registroExitoso.setValue(true);
            }, 1500);
            return;
        }

        // ---- Código real (backend) ----
        // ApiClient.getApiService(getApplication())
        //     .registrar(request)
        //     .enqueue(...); // tu implementación real
    }
}
/*
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

 */