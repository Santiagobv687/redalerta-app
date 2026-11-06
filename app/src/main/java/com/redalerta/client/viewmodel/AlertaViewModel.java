package com.redalerta.client.viewmodel;

import android.app.Application;
import android.content.ContentResolver;
import android.net.Uri;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.redalerta.client.model.alerta.Alerta;
import com.redalerta.client.model.alerta.CrearAlertaRequest;
import com.redalerta.client.model.alerta.ModificarAlertaRequest;
import com.redalerta.client.model.alerta.PageAlertaResponse;
import com.redalerta.client.model.alerta.UploadEvidenciaResponse;
import com.redalerta.client.network.ApiClient;
import com.redalerta.client.network.ApiResponse;
import com.redalerta.client.util.ManejadorErrores;
import com.redalerta.client.util.ManejadorErrores.ContextoOperacion;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AlertaViewModel extends AndroidViewModel {

    private final MutableLiveData<Boolean> cargando = new MutableLiveData<>(false);
    private final MutableLiveData<String> error = new MutableLiveData<>();
    private final MutableLiveData<Alerta> alertaCreada = new MutableLiveData<>();
    private final MutableLiveData<Alerta> alertaModificada = new MutableLiveData<>();
    private final MutableLiveData<Boolean> alertaEliminadaExito = new MutableLiveData<>();
    private final MutableLiveData<List<Alerta>> misAlertas = new MutableLiveData<>();
    private final MutableLiveData<List<Alerta>> todasLasAlertas = new MutableLiveData<>();

    // Filtros de búsqueda para el endpoint /alertas
    private final MutableLiveData<String> departamentoFiltro = new MutableLiveData<>("Quindío");
    private final MutableLiveData<String> municipioFiltro = new MutableLiveData<>("TODO"); // Por defecto Todo el Departamento
    private final MutableLiveData<String> estadoFiltro = new MutableLiveData<>(null);

    public AlertaViewModel(@NonNull Application application) {
        super(application);
    }

    public LiveData<Boolean> getCargando() { return cargando; }
    public LiveData<String> getError() { return error; }
    public LiveData<Alerta> getAlertaCreada() { return alertaCreada; }
    public LiveData<Alerta> getAlertaModificada() { return alertaModificada; }
    public LiveData<Boolean> getAlertaEliminadaExito() { return alertaEliminadaExito; }
    public LiveData<List<Alerta>> getMisAlertas() { return misAlertas; }
    public LiveData<List<Alerta>> getTodasLasAlertas() { return todasLasAlertas; }

    public LiveData<String> getDepartamentoFiltro() { return departamentoFiltro; }
    public LiveData<String> getMunicipioFiltro() { return municipioFiltro; }
    public LiveData<String> getEstadoFiltro() { return estadoFiltro; }

    public void setDepartamentoFiltro(String departamento) {
        departamentoFiltro.setValue(departamento);
        cargarTodasLasAlertas();
    }

    public void setMunicipioFiltro(String municipio) {
        municipioFiltro.setValue(municipio);
        cargarTodasLasAlertas();
    }

    public void setEstadoFiltro(String estado) {
        estadoFiltro.setValue(estado);
        cargarTodasLasAlertas();
    }

    public interface OnEvidenciaSubidaListener {
        void onExito(String urlSubida);
        void onError(String mensajeError);
    }

    // ══════════════════════════════════════════════════
    // SUBIR EVIDENCIA MULTIPART (POST /api/v1/alertas/evidencias/upload)
    // ══════════════════════════════════════════════════
    public void subirEvidencia(Uri uri, OnEvidenciaSubidaListener listener) {
        if (uri == null) return;
        try {
            ContentResolver resolver = getApplication().getContentResolver();
            String mimeType = resolver.getType(uri);
            if (mimeType == null) mimeType = "image/jpeg";

            InputStream inputStream = resolver.openInputStream(uri);
            byte[] bytes = readAllBytes(inputStream);

            RequestBody requestFile = RequestBody.create(MediaType.parse(mimeType), bytes);
            MultipartBody.Part body = MultipartBody.Part.createFormData("archivo", "evidencia.jpg", requestFile);

            ApiClient.getApiService(getApplication())
                    .subirEvidencia(body)
                    .enqueue(new Callback<ApiResponse<UploadEvidenciaResponse>>() {
                        @Override
                        public void onResponse(@NonNull Call<ApiResponse<UploadEvidenciaResponse>> call,
                                               @NonNull Response<ApiResponse<UploadEvidenciaResponse>> response) {
                            if (response.isSuccessful() && response.body() != null && response.body().isSuccess() && response.body().getData() != null) {
                                String url = response.body().getData().getUrl();
                                if (listener != null) listener.onExito(url);
                            } else {
                                if (listener != null) listener.onError("No fue posible subir la imagen seleccionada");
                            }
                        }

                        @Override
                        public void onFailure(@NonNull Call<ApiResponse<UploadEvidenciaResponse>> call, @NonNull Throwable t) {
                            if (listener != null) listener.onError(ManejadorErrores.obtenerMensajeFalloRed(t));
                        }
                    });
        } catch (Exception e) {
            Log.e("AlertaViewModel", "Error al procesar la imagen para subir", e);
            if (listener != null) listener.onError("Error al procesar el archivo de imagen");
        }
    }

    private byte[] readAllBytes(InputStream inputStream) throws IOException {
        ByteArrayOutputStream byteBuffer = new ByteArrayOutputStream();
        int bufferSize = 1024;
        byte[] buffer = new byte[bufferSize];
        int len;
        while ((len = inputStream.read(buffer)) != -1) {
            byteBuffer.write(buffer, 0, len);
        }
        return byteBuffer.toByteArray();
    }

    // ══════════════════════════════════════════════════
    // CREAR ALERTA (POST /api/v1/alertas)
    // ══════════════════════════════════════════════════
    public void crearAlerta(CrearAlertaRequest request) {
        cargando.setValue(true);
        error.setValue(null);

        ApiClient.getApiService(getApplication())
                .crearAlerta(request)
                .enqueue(new Callback<ApiResponse<Alerta>>() {
                    @Override
                    public void onResponse(@NonNull Call<ApiResponse<Alerta>> call,
                                           @NonNull Response<ApiResponse<Alerta>> response) {
                        cargando.setValue(false);
                        if (response.isSuccessful() && response.body() != null) {
                            ApiResponse<Alerta> body = response.body();
                            if (body.isSuccess() && body.getData() != null) {
                                alertaCreada.setValue(body.getData());
                            } else {
                                error.setValue(body.getMessage() != null ? body.getMessage() : "Error al registrar la alerta");
                            }
                        } else {
                            error.setValue(ManejadorErrores.obtenerMensajeAmigable(response, ContextoOperacion.ALERTAS));
                        }
                    }

                    @Override
                    public void onFailure(@NonNull Call<ApiResponse<Alerta>> call, @NonNull Throwable t) {
                        cargando.setValue(false);
                        error.setValue(ManejadorErrores.obtenerMensajeFalloRed(t));
                    }
                });
    }

    // ══════════════════════════════════════════════════
    // MIS ALERTAS (GET /api/v1/alertas/mis-alertas)
    // ══════════════════════════════════════════════════
    public void cargarMisAlertas() {
        cargando.setValue(true);
        error.setValue(null);

        ApiClient.getApiService(getApplication())
                .listarMisAlertas(0, 20, "fechaCreacion,desc")
                .enqueue(new Callback<ApiResponse<PageAlertaResponse>>() {
                    @Override
                    public void onResponse(@NonNull Call<ApiResponse<PageAlertaResponse>> call,
                                           @NonNull Response<ApiResponse<PageAlertaResponse>> response) {
                        cargando.setValue(false);
                        if (response.isSuccessful() && response.body() != null) {
                            ApiResponse<PageAlertaResponse> body = response.body();
                            if (body.isSuccess() && body.getData() != null) {
                                misAlertas.setValue(body.getData().getContent());
                            } else {
                                error.setValue(body.getMessage() != null ? body.getMessage() : "Error al obtener tus alertas");
                            }
                        } else {
                            error.setValue(ManejadorErrores.obtenerMensajeAmigable(response, ContextoOperacion.ALERTAS));
                        }
                    }

                    @Override
                    public void onFailure(@NonNull Call<ApiResponse<PageAlertaResponse>> call, @NonNull Throwable t) {
                        cargando.setValue(false);
                        error.setValue(ManejadorErrores.obtenerMensajeFalloRed(t));
                    }
                });
    }

    // ══════════════════════════════════════════════════
    // MODIFICAR ALERTA (PUT /api/v1/alertas/{id})
    // ══════════════════════════════════════════════════
    public void modificarAlerta(long id, ModificarAlertaRequest request) {
        cargando.setValue(true);
        error.setValue(null);

        ApiClient.getApiService(getApplication())
                .modificarAlerta(id, request)
                .enqueue(new Callback<ApiResponse<Alerta>>() {
                    @Override
                    public void onResponse(@NonNull Call<ApiResponse<Alerta>> call,
                                           @NonNull Response<ApiResponse<Alerta>> response) {
                        cargando.setValue(false);
                        if (response.isSuccessful() && response.body() != null) {
                            ApiResponse<Alerta> body = response.body();
                            if (body.isSuccess() && body.getData() != null) {
                                alertaModificada.setValue(body.getData());
                                cargarMisAlertas();
                                cargarTodasLasAlertas();
                            } else {
                                error.setValue(body.getMessage() != null ? body.getMessage() : "Error al modificar la alerta");
                            }
                        } else {
                            error.setValue(ManejadorErrores.obtenerMensajeAmigable(response, ContextoOperacion.ALERTAS));
                        }
                    }

                    @Override
                    public void onFailure(@NonNull Call<ApiResponse<Alerta>> call, @NonNull Throwable t) {
                        cargando.setValue(false);
                        error.setValue(ManejadorErrores.obtenerMensajeFalloRed(t));
                    }
                });
    }

    // ══════════════════════════════════════════════════
    // ELIMINAR ALERTA (DELETE /api/v1/alertas/{id})
    // ══════════════════════════════════════════════════
    public void eliminarAlerta(Alerta alerta) {
        if (alerta == null || alerta.getId() == null) return;
        cargando.setValue(true);
        error.setValue(null);

        try {
            long id = Long.parseLong(alerta.getId());
            ApiClient.getApiService(getApplication())
                    .eliminarAlerta(id)
                    .enqueue(new Callback<ApiResponse<Void>>() {
                        @Override
                        public void onResponse(@NonNull Call<ApiResponse<Void>> call,
                                               @NonNull Response<ApiResponse<Void>> response) {
                            cargando.setValue(false);
                            if (response.isSuccessful()) {
                                alertaEliminadaExito.setValue(true);
                                cargarMisAlertas();
                                cargarTodasLasAlertas();
                            } else {
                                error.setValue(ManejadorErrores.obtenerMensajeAmigable(response, ContextoOperacion.ALERTAS));
                            }
                        }

                        @Override
                        public void onFailure(@NonNull Call<ApiResponse<Void>> call, @NonNull Throwable t) {
                            cargando.setValue(false);
                            error.setValue(ManejadorErrores.obtenerMensajeFalloRed(t));
                        }
                    });
        } catch (NumberFormatException e) {
            cargando.setValue(false);
            error.setValue("El identificador de la alerta no es válido");
        }
    }

    public void actualizarEstadoAlerta(Alerta alerta, String nuevoEstado) {
        if (alerta != null) {
            alerta.setEstado(nuevoEstado);
            cargarTodasLasAlertas();
        }
    }

    // ══════════════════════════════════════════════════
    // FEED GENERAL COMUNITARIO (GET /api/v1/alertas)
    // ══════════════════════════════════════════════════
    public void cargarTodasLasAlertas() {
        cargando.setValue(true);
        error.setValue(null);

        String mun = municipioFiltro.getValue();
        String estado = estadoFiltro.getValue();

        // Si municipio es "TODO", no se envía como filtro a la API
        if (mun != null && (mun.equalsIgnoreCase("TODO") || mun.trim().isEmpty())) {
            mun = null;
        }

        // Llamada REAL al endpoint GET /api/v1/alertas (Departamento es fijo en el backend por el token del usuario)
        ApiClient.getApiService(getApplication())
                .listarAlertas(mun, estado, null, null, 0, 20, "fechaCreacion,desc")
                .enqueue(new Callback<ApiResponse<PageAlertaResponse>>() {
                    @Override
                    public void onResponse(@NonNull Call<ApiResponse<PageAlertaResponse>> call,
                                           @NonNull Response<ApiResponse<PageAlertaResponse>> response) {
                        cargando.setValue(false);
                        if (response.isSuccessful() && response.body() != null) {
                            ApiResponse<PageAlertaResponse> apiResponse = response.body();
                            if (apiResponse.isSuccess() && apiResponse.getData() != null) {
                                todasLasAlertas.setValue(apiResponse.getData().getContent());
                            } else {
                                error.setValue(apiResponse.getMessage() != null ? apiResponse.getMessage() : "Error al obtener las alertas comunitarias");
                            }
                        } else {
                            error.setValue(ManejadorErrores.obtenerMensajeAmigable(response, ContextoOperacion.ALERTAS));
                        }
                    }

                    @Override
                    public void onFailure(@NonNull Call<ApiResponse<PageAlertaResponse>> call, @NonNull Throwable t) {
                        cargando.setValue(false);
                        error.setValue(ManejadorErrores.obtenerMensajeFalloRed(t));
                    }
                });
    }
}
