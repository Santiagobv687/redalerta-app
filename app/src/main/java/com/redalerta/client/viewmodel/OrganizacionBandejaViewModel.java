package com.redalerta.client.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.redalerta.client.model.alerta.Alerta;
import com.redalerta.client.model.alerta.PageAlertaResponse;
import com.redalerta.client.network.ApiClient;
import com.redalerta.client.network.ApiResponse;
import com.redalerta.client.util.ManejadorErrores;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class OrganizacionBandejaViewModel extends AndroidViewModel {

    public enum UiState {
        LOADING, SUCCESS, EMPTY, ERROR
    }

    private final MutableLiveData<UiState> uiState = new MutableLiveData<>(UiState.LOADING);
    private final MutableLiveData<String> error = new MutableLiveData<>();
    private final MutableLiveData<List<Alerta>> bandejaOrganizacion = new MutableLiveData<>(new ArrayList<>());

    public OrganizacionBandejaViewModel(@NonNull Application application) {
        super(application);
    }

    public LiveData<UiState> getUiState() { return uiState; }
    public LiveData<String> getError() { return error; }
    public LiveData<List<Alerta>> getBandejaOrganizacion() { return bandejaOrganizacion; }

    public void cargarBandejaOrganizacion() {
        uiState.setValue(UiState.LOADING);
        error.setValue(null);

        ApiClient.getApiService(getApplication())
                .listarBandejaOrganizacion(0, 20, "fechaCreacion,desc")
                .enqueue(new Callback<ApiResponse<PageAlertaResponse>>() {
                    @Override
                    public void onResponse(@NonNull Call<ApiResponse<PageAlertaResponse>> call,
                                           @NonNull Response<ApiResponse<PageAlertaResponse>> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            ApiResponse<PageAlertaResponse> body = response.body();
                            if (body.isSuccess() && body.getData() != null) {
                                List<Alerta> content = body.getData().getContent();
                                if (content != null && !content.isEmpty()) {
                                    bandejaOrganizacion.setValue(content);
                                    uiState.setValue(UiState.SUCCESS);
                                } else {
                                    bandejaOrganizacion.setValue(new ArrayList<>());
                                    uiState.setValue(UiState.EMPTY);
                                }
                            } else {
                                error.setValue(body.getMessage() != null ? body.getMessage() : "Error al obtener la bandeja de la organización");
                                uiState.setValue(UiState.ERROR);
                            }
                        } else {
                            error.setValue(ManejadorErrores.obtenerMensajeAmigable(response, ManejadorErrores.ContextoOperacion.ALERTAS));
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
