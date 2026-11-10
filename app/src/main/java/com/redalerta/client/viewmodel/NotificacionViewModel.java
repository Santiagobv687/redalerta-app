package com.redalerta.client.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.redalerta.client.model.notificacion.NotificacionResponse;
import com.redalerta.client.model.notificacion.PageNotificacionResponse;
import com.redalerta.client.network.ApiClient;
import com.redalerta.client.network.ApiResponse;
import com.redalerta.client.util.ManejadorErrores;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class NotificacionViewModel extends AndroidViewModel {

    private static final int TAMANO_PAGINA = 15;

    public enum UiState {
        IDLE, LOADING, SUCCESS, EMPTY, ERROR
    }

    private final MutableLiveData<UiState> uiState = new MutableLiveData<>(UiState.IDLE);
    private final MutableLiveData<String> error = new MutableLiveData<>();
    private final MutableLiveData<List<NotificacionResponse>> notificaciones = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<Long> conteoNoLeidas = new MutableLiveData<>(0L);
    private final MutableLiveData<Boolean> soloNoLeidasFiltro = new MutableLiveData<>(false);

    private int paginaActual = 0;
    private boolean esUltimaPagina = false;
    private boolean cargandoMas = false;
    private final List<NotificacionResponse> acumulado = new ArrayList<>();

    public NotificacionViewModel(@NonNull Application application) {
        super(application);
    }

    public LiveData<UiState> getUiState() { return uiState; }
    public LiveData<String> getError() { return error; }
    public LiveData<List<NotificacionResponse>> getNotificaciones() { return notificaciones; }
    public LiveData<Long> getConteoNoLeidas() { return conteoNoLeidas; }
    public LiveData<Boolean> getSoloNoLeidasFiltro() { return soloNoLeidasFiltro; }

    public void cambiarFiltroSoloNoLeidas(boolean soloNoLeidas) {
        Boolean actual = soloNoLeidasFiltro.getValue();
        if (actual != null && actual == soloNoLeidas) return;
        soloNoLeidasFiltro.setValue(soloNoLeidas);
        cargarNotificaciones(true);
    }

    /**
     * Consulta de conteo para actualizar el badge discreto
     * en la barra de navegación inferior.
     */
    public void cargarConteoNoLeidas() {
        ApiClient.getApiService(getApplication())
                .contarNotificacionesNoLeidas()
                .enqueue(new Callback<ApiResponse<Long>>() {
                    @Override
                    public void onResponse(@NonNull Call<ApiResponse<Long>> call,
                                           @NonNull Response<ApiResponse<Long>> response) {
                        if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                            Long total = response.body().getData();
                            conteoNoLeidas.setValue(total != null && total > 0 ? total : 0L);
                        }
                    }

                    @Override
                    public void onFailure(@NonNull Call<ApiResponse<Long>> call, @NonNull Throwable t) {
                        // Fallo silencioso para no interrumpir la experiencia de navegación
                    }
                });
    }

    /**
     * Consulta/notificaciones paginado.
     */
    public void cargarNotificaciones(boolean reiniciar) {
        if (reiniciar) {
            paginaActual = 0;
            esUltimaPagina = false;
            acumulado.clear();
            uiState.setValue(UiState.LOADING);
            error.setValue(null);
        }

        // También sincronizar el conteo de no leídas al consultar la bandeja
        cargarConteoNoLeidas();

        Boolean soloNoLeidas = Boolean.TRUE.equals(soloNoLeidasFiltro.getValue());

        ApiClient.getApiService(getApplication())
                .listarNotificaciones(soloNoLeidas, paginaActual, TAMANO_PAGINA)
                .enqueue(new Callback<ApiResponse<PageNotificacionResponse>>() {
                    @Override
                    public void onResponse(@NonNull Call<ApiResponse<PageNotificacionResponse>> call,
                                           @NonNull Response<ApiResponse<PageNotificacionResponse>> response) {
                        cargandoMas = false;
                        if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                            PageNotificacionResponse pageData = response.body().getData();
                            if (pageData != null && pageData.getContent() != null) {
                                esUltimaPagina = pageData.isLast();
                                if (reiniciar) {
                                    acumulado.clear();
                                }
                                acumulado.addAll(pageData.getContent());
                                notificaciones.setValue(new ArrayList<>(acumulado));

                                if (acumulado.isEmpty()) {
                                    uiState.setValue(UiState.EMPTY);
                                } else {
                                    uiState.setValue(UiState.SUCCESS);
                                }
                            } else {
                                notificaciones.setValue(new ArrayList<>());
                                uiState.setValue(UiState.EMPTY);
                            }
                        } else {
                            error.setValue(ManejadorErrores.obtenerMensajeAmigable(
                                    response, ManejadorErrores.ContextoOperacion.GENERAL));
                            uiState.setValue(UiState.ERROR);
                        }
                    }

                    @Override
                    public void onFailure(@NonNull Call<ApiResponse<PageNotificacionResponse>> call,
                                          @NonNull Throwable t) {
                        cargandoMas = false;
                        error.setValue(ManejadorErrores.obtenerMensajeFalloRed(t));
                        uiState.setValue(UiState.ERROR);
                    }
                });
    }

    public void cargarSiguientePagina() {
        if (cargandoMas || esUltimaPagina || uiState.getValue() == UiState.LOADING) {
            return;
        }
        cargandoMas = true;
        paginaActual++;
        cargarNotificaciones(false);
    }

    /**
     * Marca una notificación como leída mediante PATCH notificaciones/{id}/leer
     * y actualiza el listado en memoria y el badge.
     */
    public void marcarComoLeida(long notificacionId) {
        List<NotificacionResponse> lista = notificaciones.getValue();
        if (lista != null) {
            boolean modificado = false;
            for (NotificacionResponse item : lista) {
                if (item.getId() != null && item.getId() == notificacionId) {
                    if (!item.isLeida()) {
                        item.setLeida(true);
                        modificado = true;
                    }
                    break;
                }
            }
            if (modificado) {
                notificaciones.setValue(new ArrayList<>(lista));
                Long actual = conteoNoLeidas.getValue();
                if (actual != null && actual > 0) {
                    conteoNoLeidas.setValue(Math.max(0L, actual - 1));
                }
            }
        }

        ApiClient.getApiService(getApplication())
                .marcarNotificacionComoLeida(notificacionId)
                .enqueue(new Callback<ApiResponse<Void>>() {
                    @Override
                    public void onResponse(@NonNull Call<ApiResponse<Void>> call,
                                           @NonNull Response<ApiResponse<Void>> response) {
                        if (response.isSuccessful()) {
                            cargarConteoNoLeidas();
                        }
                    }

                    @Override
                    public void onFailure(@NonNull Call<ApiResponse<Void>> call, @NonNull Throwable t) {
                        // Sincronización en segundo plano
                    }
                });
    }

    public interface OnOperacionSimpleCallback {
        void onExito();
        void onError(@NonNull String mensajeError);
    }

    /**
     * Marca todas las notificaciones como leídas en lote mediante PATCH /api/v1/notificaciones/leer-todas.
     * Actualiza la UI de forma optimista y sincroniza con el backend.
     */
    public void marcarTodasComoLeidas(@NonNull OnOperacionSimpleCallback callback) {
        List<NotificacionResponse> lista = notificaciones.getValue();
        if (lista != null && !lista.isEmpty()) {
            for (NotificacionResponse n : lista) {
                n.setLeida(true);
            }
            notificaciones.setValue(new ArrayList<>(lista));
        }
        conteoNoLeidas.setValue(0L);

        // Si el usuario tenía activo el filtro de solo no leídas, al marcar todas pasará a empty
        Boolean soloNoLeidas = soloNoLeidasFiltro.getValue();
        if (Boolean.TRUE.equals(soloNoLeidas)) {
            uiState.setValue(UiState.EMPTY);
        }

        ApiClient.getApiService(getApplication())
                .marcarTodasNotificacionesComoLeidas()
                .enqueue(new Callback<ApiResponse<Void>>() {
                    @Override
                    public void onResponse(@NonNull Call<ApiResponse<Void>> call,
                                           @NonNull Response<ApiResponse<Void>> response) {
                        if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                            cargarConteoNoLeidas();
                            callback.onExito();
                        } else {
                            callback.onError(ManejadorErrores.obtenerMensajeAmigable(
                                    response, ManejadorErrores.ContextoOperacion.GENERAL));
                        }
                    }

                    @Override
                    public void onFailure(@NonNull Call<ApiResponse<Void>> call, @NonNull Throwable t) {
                        callback.onError(ManejadorErrores.obtenerMensajeFalloRed(t));
                    }
                });
    }

    public interface OnDetalleNotificacionCallback {
        void onExito(@NonNull NotificacionResponse notificacion);
        void onError(@NonNull String mensajeError);
    }

    /**
     * Consulta notificaciones/{id} para obtener el detalle exhaustivo
     * y actualiza el estado local de lectura.
     */
    public void obtenerDetalleNotificacion(long notificacionId, @NonNull OnDetalleNotificacionCallback callback) {
        // Marcado optimista local inmediato
        marcarComoLeida(notificacionId);

        ApiClient.getApiService(getApplication())
                .obtenerDetalleNotificacion(notificacionId)
                .enqueue(new Callback<ApiResponse<NotificacionResponse>>() {
                    @Override
                    public void onResponse(@NonNull Call<ApiResponse<NotificacionResponse>> call,
                                           @NonNull Response<ApiResponse<NotificacionResponse>> response) {
                        if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                            NotificacionResponse detalle = response.body().getData();
                            if (detalle != null) {
                                callback.onExito(detalle);
                            } else {
                                callback.onError("No se encontró el detalle de la notificación");
                            }
                        } else {
                            callback.onError(ManejadorErrores.obtenerMensajeAmigable(
                                    response, ManejadorErrores.ContextoOperacion.GENERAL));
                        }
                    }

                    @Override
                    public void onFailure(@NonNull Call<ApiResponse<NotificacionResponse>> call,
                                          @NonNull Throwable t) {
                        callback.onError(ManejadorErrores.obtenerMensajeFalloRed(t));
                    }
                });
    }

    /**
     * Remueve localmente una notificación de la lista y actualiza el conteo si no estaba leída.
     * Retorna la posición previa donde se encontraba para poder deshacer si es necesario.
     */
    public int removerNotificacionLocalmente(long notificacionId) {
        int pos = -1;
        List<NotificacionResponse> lista = notificaciones.getValue();
        if (lista != null) {
            for (int i = 0; i < lista.size(); i++) {
                NotificacionResponse item = lista.get(i);
                if (item.getId() != null && item.getId() == notificacionId) {
                    pos = i;
                    if (!item.isLeida()) {
                        Long c = conteoNoLeidas.getValue();
                        if (c != null && c > 0) conteoNoLeidas.setValue(c - 1);
                    }
                    lista.remove(i);
                    acumulado.remove(item);
                    break;
                }
            }
            if (pos != -1) {
                notificaciones.setValue(new ArrayList<>(lista));
                if (lista.isEmpty()) {
                    uiState.setValue(UiState.EMPTY);
                }
            }
        }
        return pos;
    }

    /**
     * Restaura una notificación previamente eliminada localmente.
     */
    public void restaurarNotificacionLocalmente(int posicion, @NonNull NotificacionResponse item) {
        List<NotificacionResponse> lista = notificaciones.getValue();
        if (lista != null) {
            int insertPos = Math.min(Math.max(posicion, 0), lista.size());
            lista.add(insertPos, item);
            if (!acumulado.contains(item)) {
                acumulado.add(insertPos, item);
            }
            if (!item.isLeida()) {
                Long c = conteoNoLeidas.getValue();
                conteoNoLeidas.setValue(c != null ? c + 1 : 1L);
            }
            notificaciones.setValue(new ArrayList<>(lista));
            uiState.setValue(UiState.SUCCESS);
        }
    }

    /**
     * Consume DELETE /api/v1/notificaciones/{id} para eliminar permanentemente en el backend.
     */
    public void eliminarNotificacionEnBackend(long notificacionId) {
        ApiClient.getApiService(getApplication())
                .eliminarNotificacion(notificacionId)
                .enqueue(new Callback<ApiResponse<Void>>() {
                    @Override
                    public void onResponse(@NonNull Call<ApiResponse<Void>> call,
                                           @NonNull Response<ApiResponse<Void>> response) {
                        cargarConteoNoLeidas();
                    }

                    @Override
                    public void onFailure(@NonNull Call<ApiResponse<Void>> call, @NonNull Throwable t) {
                        // Sincronización en background
                    }
                });
    }
}
