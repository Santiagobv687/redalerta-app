package com.redalerta.client.viewmodel;

import android.app.Application;
import android.content.ContentResolver;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.redalerta.client.model.alerta.Alerta;
import com.redalerta.client.model.alerta.CrearAlertaRequest;
import com.redalerta.client.model.alerta.CursorPageAlertaResponse;
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
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AlertaViewModel extends AndroidViewModel {

    private static final int FEED_PAGE_SIZE = 10;

    // Pool de hilos de E/S en segundo plano (equivalente a Dispatchers.IO)
    private final ExecutorService ioExecutor = Executors.newCachedThreadPool();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    private final MutableLiveData<Boolean> cargando = new MutableLiveData<>(false);
    private final MutableLiveData<String> error = new MutableLiveData<>();
    private final MutableLiveData<Alerta> alertaCreada = new MutableLiveData<>();
    private final MutableLiveData<Alerta> alertaModificada = new MutableLiveData<>();
    private final MutableLiveData<Boolean> alertaEliminadaExito = new MutableLiveData<>();
    private final MutableLiveData<List<Alerta>> misAlertas = new MutableLiveData<>();
    private final MutableLiveData<List<Alerta>> todasLasAlertas = new MutableLiveData<>();

    // Estado cerrado en memoria para el Feed Comunitario (ViewModel Cache + Keyset Pagination)
    private final MutableLiveData<FeedUiState> feedUiState = new MutableLiveData<>(FeedUiState.Loading.INSTANCE);
    private final List<Alerta> alertasAcumuladas = new ArrayList<>();
    private boolean feedHasNext = false;
    @Nullable
    private Long feedNextCursor = null;
    private boolean cargandoFeedEnCurso = false;
    private boolean cargandoSiguientePagina = false;
    private boolean feedInicializado = false;

    // Filtros de búsqueda para el endpoint /alertas/feed y /alertas
    private final MutableLiveData<String> departamentoFiltro = new MutableLiveData<>("Quindío");
    private final MutableLiveData<String> municipioFiltro = new MutableLiveData<>("TODO");
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
    public LiveData<FeedUiState> getFeedUiState() { return feedUiState; }

    public LiveData<String> getDepartamentoFiltro() { return departamentoFiltro; }
    public LiveData<String> getMunicipioFiltro() { return municipioFiltro; }
    public LiveData<String> getEstadoFiltro() { return estadoFiltro; }

    /**
     * Inicializa el filtro de municipio según el rol del usuario sin disparar recargas redundantes
     * si el filtro ya coincide con el valor en memoria.
     */
    public void inicializarMunicipioFiltroPorDefecto(String municipioPorDefecto) {
        String normalizado = (municipioPorDefecto == null || municipioPorDefecto.trim().isEmpty())
                ? "TODO"
                : municipioPorDefecto.trim();
        String actual = municipioFiltro.getValue();
        if (actual == null || !actual.equalsIgnoreCase(normalizado)) {
            municipioFiltro.setValue(normalizado);
            if (feedInicializado) {
                invalidarCacheYRecargarFeed();
            }
        }
    }

    public void setDepartamentoFiltro(String departamento) {
        String actual = departamentoFiltro.getValue();
        if (Objects.equals(actual, departamento)) return;
        departamentoFiltro.setValue(departamento);
        invalidarCacheYRecargarFeed();
    }

    public void setMunicipioFiltro(String municipio) {
        String normalizado = (municipio == null || municipio.trim().isEmpty()) ? "TODO" : municipio.trim();
        String actual = municipioFiltro.getValue();
        if (actual != null && actual.equalsIgnoreCase(normalizado) && feedInicializado) {
            return;
        }
        municipioFiltro.setValue(normalizado);
        invalidarCacheYRecargarFeed();
    }

    public void setEstadoFiltro(String estado) {
        String actual = estadoFiltro.getValue();
        if (Objects.equals(actual, estado) && feedInicializado) {
            return;
        }
        estadoFiltro.setValue(estado);
        invalidarCacheYRecargarFeed();
    }

    private void invalidarCacheYRecargarFeed() {
        synchronized (alertasAcumuladas) {
            alertasAcumuladas.clear();
            feedNextCursor = null;
            feedHasNext = false;
            feedInicializado = false;
        }
        cargarFeed(true);
    }

    public interface OnEvidenciaSubidaListener {
        void onExito(String urlSubida);
        void onError(String mensajeError);
    }

    // ══════════════════════════════════════════════════
    // SUBIR EVIDENCIA MULTIPART (POST /api/v1/alertas/evidencias/upload)
    // Ejecuta la lectura de bytes en segundo plano (ioExecutor) para no bloquear el hilo principal.
    // ══════════════════════════════════════════════════
    public void subirEvidencia(Uri uri, OnEvidenciaSubidaListener listener) {
        if (uri == null) return;
        ioExecutor.execute(() -> {
            try {
                ContentResolver resolver = getApplication().getContentResolver();
                String mimeType = resolver.getType(uri);
                if (mimeType == null) mimeType = "image/jpeg";

                byte[] bytes;
                try (InputStream inputStream = resolver.openInputStream(uri)) {
                    if (inputStream == null) {
                        mainHandler.post(() -> {
                            if (listener != null) listener.onError("No se pudo abrir la imagen seleccionada");
                        });
                        return;
                    }
                    bytes = readAllBytes(inputStream);
                }

                RequestBody requestFile = RequestBody.create(MediaType.parse(mimeType), bytes);
                MultipartBody.Part body = MultipartBody.Part.createFormData("archivo", "evidencia.jpg", requestFile);

                ApiClient.getApiService(getApplication())
                        .subirEvidencia(body)
                        .enqueue(new Callback<ApiResponse<UploadEvidenciaResponse>>() {
                            @Override
                            public void onResponse(@NonNull Call<ApiResponse<UploadEvidenciaResponse>> call,
                                                   @NonNull Response<ApiResponse<UploadEvidenciaResponse>> response) {
                                if (response.isSuccessful() && response.body() != null
                                        && response.body().isSuccess() && response.body().getData() != null) {
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
                mainHandler.post(() -> {
                    if (listener != null) listener.onError("Error al procesar el archivo de imagen");
                });
            }
        });
    }

    private byte[] readAllBytes(InputStream inputStream) throws IOException {
        ByteArrayOutputStream byteBuffer = new ByteArrayOutputStream();
        int bufferSize = 4096;
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

        ioExecutor.execute(() -> ApiClient.getApiService(getApplication())
                .crearAlerta(request)
                .enqueue(new Callback<ApiResponse<Alerta>>() {
                    @Override
                    public void onResponse(@NonNull Call<ApiResponse<Alerta>> call,
                                           @NonNull Response<ApiResponse<Alerta>> response) {
                        cargando.setValue(false);
                        if (response.isSuccessful() && response.body() != null) {
                            ApiResponse<Alerta> body = response.body();
                            if (body.isSuccess() && body.getData() != null) {
                                Alerta nueva = body.getData();
                                alertaCreada.setValue(nueva);
                                // Refrescar el feed en segundo plano para incluir la nueva alerta
                                cargarFeed(true);
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
                }));
    }

    // ══════════════════════════════════════════════════
    // MIS ALERTAS (GET /api/v1/alertas/mis-alertas)
    // ══════════════════════════════════════════════════
    public void cargarMisAlertas() {
        cargando.setValue(true);
        error.setValue(null);

        ioExecutor.execute(() -> ApiClient.getApiService(getApplication())
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
                }));
    }

    // ══════════════════════════════════════════════════
    // MODIFICAR ALERTA (PUT /api/v1/alertas/{id})
    // ══════════════════════════════════════════════════
    public void modificarAlerta(long id, ModificarAlertaRequest request) {
        cargando.setValue(true);
        error.setValue(null);

        ioExecutor.execute(() -> ApiClient.getApiService(getApplication())
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
                                cargarFeed(true);
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
                }));
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
            ioExecutor.execute(() -> ApiClient.getApiService(getApplication())
                    .eliminarAlerta(id)
                    .enqueue(new Callback<ApiResponse<Void>>() {
                        @Override
                        public void onResponse(@NonNull Call<ApiResponse<Void>> call,
                                               @NonNull Response<ApiResponse<Void>> response) {
                            cargando.setValue(false);
                            if (response.isSuccessful()) {
                                alertaEliminadaExito.setValue(true);
                                cargarMisAlertas();
                                cargarFeed(true);
                            } else {
                                error.setValue(ManejadorErrores.obtenerMensajeAmigable(response, ContextoOperacion.ALERTAS));
                            }
                        }

                        @Override
                        public void onFailure(@NonNull Call<ApiResponse<Void>> call, @NonNull Throwable t) {
                            cargando.setValue(false);
                            error.setValue(ManejadorErrores.obtenerMensajeFalloRed(t));
                        }
                    }));
        } catch (NumberFormatException e) {
            cargando.setValue(false);
            error.setValue("El identificador de la alerta no es válido");
        }
    }

    public void actualizarEstadoAlerta(Alerta alerta, String nuevoEstado) {
        if (alerta != null) {
            alerta.setEstado(nuevoEstado);
            cargarFeed(true);
        }
    }

    // ══════════════════════════════════════════════════
    // FEED COMUNITARIO CON RETENCIÓN EN MEMORIA Y KEYSET PAGINATION
    // (GET /api/v1/alertas/feed con respaldo a GET /api/v1/alertas)
    // ══════════════════════════════════════════════════
    public void cargarTodasLasAlertas() {
        cargarFeed(false);
    }

    public void cargarFeed() {
        cargarFeed(false);
    }

    /**
     * Carga el Feed Comunitario de Alertas:
     * - Si la lista en memoria ya tiene alertas y {@code forzarRecarga == false}, NO limpia la pantalla
     *   ni emite {@link FeedUiState.Loading}; mantiene el estado {@link FeedUiState.Success} inmediatamente.
     * - Si {@code forzarRecarga == true} (ej. Pull-to-Refresh), realiza la petición en segundo plano
     *   y actualiza la lista al recibir respuesta.
     */
    public void cargarFeed(boolean forzarRecarga) {
        synchronized (alertasAcumuladas) {
            if (!forzarRecarga && feedInicializado && !alertasAcumuladas.isEmpty()) {
                // Retención en memoria: mantener alertas visibles en 0ms sin emitir Loading
                if (!(feedUiState.getValue() instanceof FeedUiState.Success)) {
                    feedUiState.setValue(new FeedUiState.Success(
                            new ArrayList<>(alertasAcumuladas),
                            feedHasNext,
                            feedNextCursor
                    ));
                }
                return;
            }

            if (cargandoFeedEnCurso) {
                return;
            }
            cargandoFeedEnCurso = true;

            if (alertasAcumuladas.isEmpty()) {
                feedUiState.setValue(FeedUiState.Loading.INSTANCE);
                cargando.setValue(true);
            }
        }

        error.setValue(null);
        final String municipioQuery = obtenerMunicipioQuery();
        final String estadoQuery = estadoFiltro.getValue();

        ioExecutor.execute(() -> {
            try {
                Response<ApiResponse<CursorPageAlertaResponse>> response = ApiClient
                        .getApiService(getApplication())
                        .listarFeedAlertas(null, FEED_PAGE_SIZE, municipioQuery, estadoQuery, null, null)
                        .execute();

                if (response.isSuccessful() && response.body() != null
                        && response.body().isSuccess() && response.body().getData() != null) {
                    CursorPageAlertaResponse cursorPage = response.body().getData();
                    List<Alerta> recibidas = cursorPage.getContent() != null
                            ? cursorPage.getContent()
                            : new ArrayList<>();

                    synchronized (alertasAcumuladas) {
                        alertasAcumuladas.clear();
                        alertasAcumuladas.addAll(recibidas);
                        feedHasNext = cursorPage.isHasNext();
                        feedNextCursor = cursorPage.getNextCursor();
                        feedInicializado = true;
                        cargandoFeedEnCurso = false;

                        List<Alerta> snapshot = new ArrayList<>(alertasAcumuladas);
                        todasLasAlertas.postValue(snapshot);
                        cargando.postValue(false);

                        if (snapshot.isEmpty()) {
                            feedUiState.postValue(FeedUiState.Empty.INSTANCE);
                        } else {
                            feedUiState.postValue(new FeedUiState.Success(snapshot, feedHasNext, feedNextCursor));
                        }
                    }
                } else {
                    // Respaldo al endpoint paginado estándar GET /api/v1/alertas
                    ejecutarRespaldoListarAlertas(municipioQuery, estadoQuery);
                }
            } catch (Exception t) {
                synchronized (alertasAcumuladas) {
                    cargandoFeedEnCurso = false;
                }
                String msgError = ManejadorErrores.obtenerMensajeFalloRed(t);
                cargando.postValue(false);
                error.postValue(msgError);
                feedUiState.postValue(new FeedUiState.Error(msgError));
            }
        });
    }

    /**
     * Carga el siguiente lote del Feed Comunitario usando Keyset Pagination ({@code nextCursor})
     * y anexa los nuevos elementos a la lista existente en memoria sin parpadeos.
     */
    public void cargarSiguientePaginaFeed() {
        final Long cursorActual;
        synchronized (alertasAcumuladas) {
            if (cargandoFeedEnCurso || cargandoSiguientePagina || !feedHasNext || feedNextCursor == null) {
                return;
            }
            cargandoSiguientePagina = true;
            cursorActual = feedNextCursor;
        }

        final String municipioQuery = obtenerMunicipioQuery();
        final String estadoQuery = estadoFiltro.getValue();

        ioExecutor.execute(() -> {
            try {
                Response<ApiResponse<CursorPageAlertaResponse>> response = ApiClient
                        .getApiService(getApplication())
                        .listarFeedAlertas(cursorActual, FEED_PAGE_SIZE, municipioQuery, estadoQuery, null, null)
                        .execute();

                if (response.isSuccessful() && response.body() != null
                        && response.body().isSuccess() && response.body().getData() != null) {
                    CursorPageAlertaResponse cursorPage = response.body().getData();
                    List<Alerta> nuevosItems = cursorPage.getContent() != null
                            ? cursorPage.getContent()
                            : new ArrayList<>();

                    synchronized (alertasAcumuladas) {
                        Set<String> idsExistentes = new HashSet<>();
                        for (Alerta a : alertasAcumuladas) {
                            if (a.getId() != null) {
                                idsExistentes.add(a.getId());
                            }
                        }
                        for (Alerta nuevo : nuevosItems) {
                            if (nuevo.getId() == null || !idsExistentes.contains(nuevo.getId())) {
                                alertasAcumuladas.add(nuevo);
                            }
                        }

                        feedHasNext = cursorPage.isHasNext();
                        feedNextCursor = cursorPage.getNextCursor();
                        cargandoSiguientePagina = false;

                        List<Alerta> snapshot = new ArrayList<>(alertasAcumuladas);
                        todasLasAlertas.postValue(snapshot);
                        feedUiState.postValue(new FeedUiState.Success(snapshot, feedHasNext, feedNextCursor));
                    }
                } else {
                    synchronized (alertasAcumuladas) {
                        cargandoSiguientePagina = false;
                    }
                }
            } catch (Exception e) {
                synchronized (alertasAcumuladas) {
                    cargandoSiguientePagina = false;
                }
            }
        });
    }

    private void ejecutarRespaldoListarAlertas(@Nullable String municipioQuery, @Nullable String estadoQuery) {
        try {
            Response<ApiResponse<PageAlertaResponse>> fallbackResponse = ApiClient
                    .getApiService(getApplication())
                    .listarAlertas(municipioQuery, estadoQuery, null, null, 0, 20, "fechaCreacion,desc")
                    .execute();

            synchronized (alertasAcumuladas) {
                cargandoFeedEnCurso = false;
                cargando.postValue(false);

                if (fallbackResponse.isSuccessful() && fallbackResponse.body() != null
                        && fallbackResponse.body().isSuccess() && fallbackResponse.body().getData() != null) {
                    List<Alerta> recibidas = fallbackResponse.body().getData().getContent() != null
                            ? fallbackResponse.body().getData().getContent()
                            : new ArrayList<>();

                    alertasAcumuladas.clear();
                    alertasAcumuladas.addAll(recibidas);
                    feedHasNext = false;
                    feedNextCursor = null;
                    feedInicializado = true;

                    List<Alerta> snapshot = new ArrayList<>(alertasAcumuladas);
                    todasLasAlertas.postValue(snapshot);

                    if (snapshot.isEmpty()) {
                        feedUiState.postValue(FeedUiState.Empty.INSTANCE);
                    } else {
                        feedUiState.postValue(new FeedUiState.Success(snapshot, false, null));
                    }
                } else {
                    String msg = ManejadorErrores.obtenerMensajeAmigable(fallbackResponse, ContextoOperacion.ALERTAS);
                    error.postValue(msg);
                    feedUiState.postValue(new FeedUiState.Error(msg));
                }
            }
        } catch (Exception e) {
            synchronized (alertasAcumuladas) {
                cargandoFeedEnCurso = false;
            }
            String msg = ManejadorErrores.obtenerMensajeFalloRed(e);
            cargando.postValue(false);
            error.postValue(msg);
            feedUiState.postValue(new FeedUiState.Error(msg));
        }
    }

    @Nullable
    private String obtenerMunicipioQuery() {
        String mun = municipioFiltro.getValue();
        if (mun != null && (mun.equalsIgnoreCase("TODO") || mun.trim().isEmpty())) {
            return null;
        }
        return mun;
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        ioExecutor.shutdownNow();
    }
}
