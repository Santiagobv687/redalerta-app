package com.redalerta.client.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.redalerta.client.model.usuario.ActualizarPerfilRequest;
import com.redalerta.client.model.usuario.UbicacionRequest;
import com.redalerta.client.model.usuario.Usuario;
import com.redalerta.client.network.ApiClient;
import com.redalerta.client.network.ApiResponse;
import com.redalerta.client.util.ManejadorErrores;
import com.redalerta.client.util.ManejadorErrores.ContextoOperacion;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class PerfilViewModel extends AndroidViewModel {

    private final MutableLiveData<Boolean> cargando = new MutableLiveData<>(false);
    private final MutableLiveData<String> error = new MutableLiveData<>();
    private final MutableLiveData<Usuario> usuario = new MutableLiveData<>();
    private final MutableLiveData<String> perfilActualizadoExito = new MutableLiveData<>();
    private final MutableLiveData<Boolean> passwordActualizadoExito = new MutableLiveData<>();

    public PerfilViewModel(@NonNull Application application) {
        super(application);
    }

    public LiveData<Boolean> getCargando() { return cargando; }
    public LiveData<String> getError() { return error; }
    public LiveData<Usuario> getUsuario() { return usuario; }
    public LiveData<String> getPerfilActualizadoExito() { return perfilActualizadoExito; }
    public LiveData<Boolean> getPasswordActualizadoExito() { return passwordActualizadoExito; }

    public void limpiarEstadoExito() {
        perfilActualizadoExito.setValue(null);
        passwordActualizadoExito.setValue(null);
        error.setValue(null);
    }

    // ══════════════════════════════════════════════════
    // CONSULTAR PERFIL (GET /api/v1/usuarios/perfil)
    // ══════════════════════════════════════════════════
    public void cargarPerfil() {
        cargando.setValue(true);
        error.setValue(null);

        ApiClient.getApiService(getApplication())
                .obtenerPerfil()
                .enqueue(new Callback<ApiResponse<Usuario>>() {
                    @Override
                    public void onResponse(@NonNull Call<ApiResponse<Usuario>> call,
                                           @NonNull Response<ApiResponse<Usuario>> response) {
                        cargando.setValue(false);
                        if (response.isSuccessful() && response.body() != null) {
                            ApiResponse<Usuario> body = response.body();
                            if (body.isSuccess() && body.getData() != null) {
                                usuario.setValue(body.getData());
                            } else {
                                error.setValue(body.getMessage() != null ? body.getMessage() : "Error al obtener perfil");
                            }
                        } else {
                            error.setValue(ManejadorErrores.obtenerMensajeAmigable(response, ContextoOperacion.PERFIL));
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
    // EDITAR DATOS PERSONALES Y RESIDENCIA (PUT /api/v1/usuarios/perfil)
    // ══════════════════════════════════════════════════
    public void actualizarDatosPersonales(String nombre, String email, String telefono, UbicacionRequest ubicacion) {
        cargando.setValue(true);
        error.setValue(null);

        ActualizarPerfilRequest request = new ActualizarPerfilRequest();
        request.setNombre(nombre.trim());
        request.setEmail(email.trim());
        request.setTelefono(telefono != null ? telefono.trim() : null);
        request.setUbicacion(ubicacion);
        request.setPasswordActual(null);
        request.setPasswordNuevo(null);

        ApiClient.getApiService(getApplication())
                .actualizarPerfil(request)
                .enqueue(new Callback<ApiResponse<Usuario>>() {
                    @Override
                    public void onResponse(@NonNull Call<ApiResponse<Usuario>> call,
                                           @NonNull Response<ApiResponse<Usuario>> response) {
                        cargando.setValue(false);
                        if (response.isSuccessful() && response.body() != null) {
                            ApiResponse<Usuario> body = response.body();
                            if (body.isSuccess() && body.getData() != null) {
                                usuario.setValue(body.getData());
                                perfilActualizadoExito.setValue("Perfil actualizado exitosamente");
                            } else {
                                error.setValue(body.getMessage() != null ? body.getMessage() : "Error al actualizar perfil");
                            }
                        } else {
                            error.setValue(ManejadorErrores.obtenerMensajeAmigable(response, ContextoOperacion.PERFIL));
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
    // CAMBIAR CONTRASEÑA (PUT /api/v1/usuarios/perfil)
    // ══════════════════════════════════════════════════
    public void cambiarPassword(String passwordActual, String passwordNuevo) {
        cargando.setValue(true);
        error.setValue(null);

        Usuario usuarioActual = usuario.getValue();
        String nombreActual = usuarioActual != null && usuarioActual.getNombre() != null ? usuarioActual.getNombre() : "Usuario";
        String emailActual = usuarioActual != null && usuarioActual.getEmail() != null ? usuarioActual.getEmail() : "usuario@redalerta.com";
        String telefonoActual = usuarioActual != null ? usuarioActual.getTelefono() : null;
        UbicacionRequest ubActual = usuarioActual != null ? usuarioActual.getUbicacion() : null;

        ActualizarPerfilRequest request = new ActualizarPerfilRequest();
        request.setNombre(nombreActual);
        request.setEmail(emailActual);
        request.setTelefono(telefonoActual);
        request.setUbicacion(ubActual);
        request.setPasswordActual(passwordActual);
        request.setPasswordNuevo(passwordNuevo);

        ApiClient.getApiService(getApplication())
                .actualizarPerfil(request)
                .enqueue(new Callback<ApiResponse<Usuario>>() {
                    @Override
                    public void onResponse(@NonNull Call<ApiResponse<Usuario>> call,
                                           @NonNull Response<ApiResponse<Usuario>> response) {
                        cargando.setValue(false);
                        if (response.isSuccessful() && response.body() != null) {
                            ApiResponse<Usuario> body = response.body();
                            if (body.isSuccess()) {
                                if (body.getData() != null) {
                                    usuario.setValue(body.getData());
                                }
                                passwordActualizadoExito.setValue(true);
                            } else {
                                error.setValue(body.getMessage() != null ? body.getMessage() : "La contraseña actual no es correcta");
                            }
                        } else {
                            error.setValue(ManejadorErrores.obtenerMensajeAmigable(response, ContextoOperacion.PERFIL));
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
