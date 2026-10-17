package com.redalerta.client.viewmodel;

import android.app.Application;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.redalerta.client.model.Alerta;
import com.redalerta.client.model.CrearAlertaRequest;
import com.redalerta.client.util.Validaciones;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.UUID;

public class AlertaViewModel extends AndroidViewModel {

    // ⚠️ MODO PRUEBA: cambia a false cuando tengas el backend
    private static final boolean MODO_MOCK = true;

    private final MutableLiveData<Boolean> cargando = new MutableLiveData<>(false);
    private final MutableLiveData<String> error = new MutableLiveData<>();
    private final MutableLiveData<Alerta> alertaCreada = new MutableLiveData<>();

    public AlertaViewModel(@NonNull Application application) {
        super(application);
    }

    public LiveData<Boolean> getCargando() { return cargando; }
    public LiveData<String> getError() { return error; }
    public LiveData<Alerta> getAlertaCreada() { return alertaCreada; }

    public void crearAlerta(CrearAlertaRequest request) {
        cargando.setValue(true);
        error.setValue(null);

        if (MODO_MOCK) {
            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                cargando.setValue(false);

                // Simula respuesta exitosa del backend
                Alerta alerta = new Alerta();
                alerta.setId(UUID.randomUUID().toString());
                alerta.setTitulo(request.getTitulo());
                alerta.setDescripcion(request.getDescripcion());
                alerta.setCategoria(request.getCategoria());
                alerta.setPrioridad(request.getPrioridad());
                alerta.setEstado("REGISTRADA");
                alerta.setDireccion(request.getDireccion());
                alerta.setBarrio(request.getBarrio());
                alerta.setHayHeridos(request.isHayHeridos());
                alerta.setEsAnonima(request.isEsAnonima());
                alerta.setEvidenciaUrl(request.getEvidenciaUrl());
                alerta.setUsuarioId(request.getUsuarioId());
                alerta.setUsuarioNombre(
                        request.isEsAnonima() ? null : "Usuario Demo"
                );
                alerta.setFechaCreacion(
                        new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss",
                                new Locale("es", "CO")).format(new Date())
                );

                alertaCreada.setValue(alerta);
            }, 1200); // 1.2s de latencia simulada
            return;
        }

        // ---- Código real (backend) ----
        // ApiClient.getApiService(getApplication())
        //     .crearAlerta(request)
        //     .enqueue(new Callback<ApiResponse<Alerta>>() {
        //         @Override
        //         public void onResponse(...) {
        //             cargando.setValue(false);
        //             if (response.isSuccessful() && response.body() != null
        //                     && response.body().isSuccess()) {
        //                 alertaCreada.setValue(response.body().getData());
        //             } else {
        //                 error.setValue("No fue posible crear la alerta");
        //             }
        //         }
        //
        //         @Override
        //         public void onFailure(...) {
        //             cargando.setValue(false);
        //             error.setValue("Error de conexión: " + t.getMessage());
        //         }
        //     });
    }
}