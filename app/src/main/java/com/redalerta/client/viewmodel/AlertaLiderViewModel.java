package com.redalerta.client.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.redalerta.client.model.alerta.Alerta;
import com.redalerta.client.model.alerta.CancelarDireccionamientoRequest;
import com.redalerta.client.model.alerta.DireccionarAlertaRequest;
import com.redalerta.client.model.alerta.OrganizacionResponse;
import com.redalerta.client.model.alerta.PageAlertaResponse;
import com.redalerta.client.network.ApiClient;
import com.redalerta.client.network.ApiResponse;
import com.redalerta.client.util.ManejadorErrores;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AlertaLiderViewModel extends AndroidViewModel {

    public enum UiState {
        IDLE, LOADING, SUCCESS, ERROR
    }

    private final MutableLiveData<UiState> uiState = new MutableLiveData<>(UiState.IDLE);
    private final MutableLiveData<String> error = new MutableLiveData<>();
    private final MutableLiveData<List<OrganizacionResponse>> organizacionesElegibles = new MutableLiveData<>();
    private final MutableLiveData<Alerta> alertaDireccionada = new MutableLiveData<>();
    private final MutableLiveData<List<Alerta>> bandejaDireccionadas = new MutableLiveData<>();

    public AlertaLiderViewModel(@NonNull Application application) {
        super(application);
    }

    public LiveData<UiState> getUiState() { return uiState; }
    public LiveData<String> getError() { return error; }
    public LiveData<List<OrganizacionResponse>> getOrganizacionesElegibles() { return organizacionesElegibles; }
    public LiveData<Alerta> getAlertaDireccionada() { return alertaDireccionada; }
    public LiveData<List<Alerta>> getBandejaDireccionadas() { return bandejaDireccionadas; }

    public void cargarOrganizacionesElegibles() {
        uiState.setValue(UiState.LOADING);
        ApiClient.getApiService(getApplication()).obtenerOrganizacionesElegibles().enqueue(new Callback<ApiResponse<List<OrganizacionResponse>>>() {
            @Override
            public void onResponse(@NonNull Call<ApiResponse<List<OrganizacionResponse>>> call, @NonNull Response<ApiResponse<List<OrganizacionResponse>>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    organizacionesElegibles.setValue(response.body().getData());
                    uiState.setValue(UiState.SUCCESS);
                } else {
                    error.setValue(ManejadorErrores.obtenerMensajeAmigable(response, ManejadorErrores.ContextoOperacion.GENERAL));
                    uiState.setValue(UiState.ERROR);
                }
            }

            @Override
            public void onFailure(@NonNull Call<ApiResponse<List<OrganizacionResponse>>> call, @NonNull Throwable t) {
                error.setValue(ManejadorErrores.obtenerMensajeFalloRed(t));
                uiState.setValue(UiState.ERROR);
            }
        });
    }

    public void direccionarAlerta(long alertaId, String categoria, String prioridad, Long organizacionId, String observacion) {
        uiState.setValue(UiState.LOADING);
        DireccionarAlertaRequest request = new DireccionarAlertaRequest(categoria, prioridad, organizacionId, observacion);
        ApiClient.getApiService(getApplication()).direccionarAlerta(alertaId, request).enqueue(new Callback<ApiResponse<Alerta>>() {
            @Override
            public void onResponse(@NonNull Call<ApiResponse<Alerta>> call, @NonNull Response<ApiResponse<Alerta>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    alertaDireccionada.setValue(response.body().getData());
                    uiState.setValue(UiState.SUCCESS);
                } else {
                    error.setValue(ManejadorErrores.obtenerMensajeAmigable(response, ManejadorErrores.ContextoOperacion.GENERAL));
                    uiState.setValue(UiState.ERROR);
                }
            }

            @Override
            public void onFailure(@NonNull Call<ApiResponse<Alerta>> call, @NonNull Throwable t) {
                error.setValue(ManejadorErrores.obtenerMensajeFalloRed(t));
                uiState.setValue(UiState.ERROR);
            }
        });
    }

    public void modificarDireccionamiento(long alertaId, String categoria, String prioridad, Long organizacionId, String observacion) {
        uiState.setValue(UiState.LOADING);
        DireccionarAlertaRequest request = new DireccionarAlertaRequest(categoria, prioridad, organizacionId, observacion);
        ApiClient.getApiService(getApplication()).modificarDireccionamiento(alertaId, request).enqueue(new Callback<ApiResponse<Alerta>>() {
            @Override
            public void onResponse(@NonNull Call<ApiResponse<Alerta>> call, @NonNull Response<ApiResponse<Alerta>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    alertaDireccionada.setValue(response.body().getData());
                    uiState.setValue(UiState.SUCCESS);
                } else {
                    error.setValue(ManejadorErrores.obtenerMensajeAmigable(response, ManejadorErrores.ContextoOperacion.GENERAL));
                    uiState.setValue(UiState.ERROR);
                }
            }

            @Override
            public void onFailure(@NonNull Call<ApiResponse<Alerta>> call, @NonNull Throwable t) {
                error.setValue(ManejadorErrores.obtenerMensajeFalloRed(t));
                uiState.setValue(UiState.ERROR);
            }
        });
    }

    public void cancelarDireccionamiento(long alertaId, String motivo) {
        uiState.setValue(UiState.LOADING);
        CancelarDireccionamientoRequest request = new CancelarDireccionamientoRequest(motivo);
        ApiClient.getApiService(getApplication()).cancelarDireccionamiento(alertaId, request).enqueue(new Callback<ApiResponse<Alerta>>() {
            @Override
            public void onResponse(@NonNull Call<ApiResponse<Alerta>> call, @NonNull Response<ApiResponse<Alerta>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    alertaDireccionada.setValue(response.body().getData());
                    uiState.setValue(UiState.SUCCESS);
                } else {
                    error.setValue(ManejadorErrores.obtenerMensajeAmigable(response, ManejadorErrores.ContextoOperacion.GENERAL));
                    uiState.setValue(UiState.ERROR);
                }
            }

            @Override
            public void onFailure(@NonNull Call<ApiResponse<Alerta>> call, @NonNull Throwable t) {
                error.setValue(ManejadorErrores.obtenerMensajeFalloRed(t));
                uiState.setValue(UiState.ERROR);
            }
        });
    }

    public void cargarBandejaDireccionadas(int page, int size) {
        uiState.setValue(UiState.LOADING);
        ApiClient.getApiService(getApplication()).listarAlertasDireccionadasLider(page, size).enqueue(new Callback<ApiResponse<PageAlertaResponse>>() {
            @Override
            public void onResponse(@NonNull Call<ApiResponse<PageAlertaResponse>> call, @NonNull Response<ApiResponse<PageAlertaResponse>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess() && response.body().getData() != null) {
                    bandejaDireccionadas.setValue(response.body().getData().getContent());
                    uiState.setValue(UiState.SUCCESS);
                } else {
                    error.setValue(ManejadorErrores.obtenerMensajeAmigable(response, ManejadorErrores.ContextoOperacion.GENERAL));
                    uiState.setValue(UiState.ERROR);
                }
            }

            @Override
            public void onFailure(@NonNull Call<ApiResponse<PageAlertaResponse>> call, @NonNull Throwable t) {
                error.setValue(ManejadorErrores.obtenerMensajeFalloRed(t));
                uiState.setValue(UiState.ERROR);
            }
        });
    }
}
