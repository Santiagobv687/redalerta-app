package com.redalerta.client.viewmodel;

import android.app.Application;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.redalerta.client.model.*;
import com.redalerta.client.network.ApiClient;
import com.redalerta.client.network.ApiResponse;
import com.redalerta.client.util.ManejadorErrores;
import com.redalerta.client.util.ManejadorErrores.ContextoOperacion;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AlertaViewModel extends AndroidViewModel {

    // Cambia MODO_MOCK a false cuando te conectes al backend para alertas
    private static final boolean MODO_MOCK = true;

    private final MutableLiveData<Boolean> cargando = new MutableLiveData<>(false);
    private final MutableLiveData<String> error = new MutableLiveData<>();
    private final MutableLiveData<Alerta> alertaCreada = new MutableLiveData<>();
    private final MutableLiveData<List<Alerta>> misAlertas = new MutableLiveData<>();
    private final MutableLiveData<List<Alerta>> todasLasAlertas = new MutableLiveData<>();

    public AlertaViewModel(@NonNull Application application) {
        super(application);
    }

    public LiveData<Boolean> getCargando() { return cargando; }
    public LiveData<String> getError() { return error; }
    public LiveData<Alerta> getAlertaCreada() { return alertaCreada; }
    public LiveData<List<Alerta>> getMisAlertas() { return misAlertas; }
    public LiveData<List<Alerta>> getTodasLasAlertas() { return todasLasAlertas; }

    // ══════════════════════════════════════════════════
    // CREAR ALERTA
    // ══════════════════════════════════════════════════
    public void crearAlerta(CrearAlertaRequest request) {
        cargando.setValue(true);
        error.setValue(null);

        if (MODO_MOCK) {
            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                cargando.setValue(false);

                Alerta alerta = new Alerta();
                alerta.setId(UUID.randomUUID().toString());
                alerta.setTitulo(request.getTitulo());
                alerta.setDescripcion(request.getDescripcion());
                alerta.setCategoria(request.getCategoria());
                alerta.setPrioridad(request.getPrioridad());
                alerta.setEstado("PENDIENTE_REVISION");
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
            }, 1200);
            return;
        }

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
    // MIS PUBLICACIONES
    // ══════════════════════════════════════════════════
    public void cargarMisAlertas() {
        if (MODO_MOCK) {
            List<Alerta> mock = new ArrayList<>();

            Alerta a1 = new Alerta();
            a1.setId("1");
            a1.setTitulo("Inundación");
            a1.setDescripcion("Alcantarilla desbordada; el agua está entrando a varias viviendas.");
            a1.setEstado("PENDIENTE_REVISION");
            a1.setDireccion("Calle 48 #12-30");
            a1.setBarrio("San Luis");
            a1.setUsuarioNombre("Carlos Ruiz");
            a1.setFechaActualizacion("2024-11-20 - 10:24");
            mock.add(a1);

            Alerta a2 = new Alerta();
            a2.setId("2");
            a2.setTitulo("Otro");
            a2.setDescripcion("Árbol caído bloqueando completamente el paso vehicular.");
            a2.setEstado("RESUELTA");
            a2.setDireccion("Carrera 21 con Calle 54");
            a2.setBarrio("Galerías");
            a2.setUsuarioNombre("Carlos Ruiz");
            a2.setFechaActualizacion("2024-11-15 - 16:40");
            mock.add(a2);

            misAlertas.setValue(mock);
            return;
        }

        ApiClient.getApiService(getApplication())
                .listarMisAlertas(0, 20)
                .enqueue(new Callback<ApiResponse<PageAlertaResponse>>() {
                    @Override
                    public void onResponse(@NonNull Call<ApiResponse<PageAlertaResponse>> call,
                                           @NonNull Response<ApiResponse<PageAlertaResponse>> response) {
                        if (response.isSuccessful() && response.body() != null && response.body().getData() != null) {
                            misAlertas.setValue(response.body().getData().getContent());
                        } else {
                            error.setValue(ManejadorErrores.obtenerMensajeAmigable(response, ContextoOperacion.ALERTAS));
                        }
                    }

                    @Override
                    public void onFailure(@NonNull Call<ApiResponse<PageAlertaResponse>> call, @NonNull Throwable t) {
                        error.setValue(ManejadorErrores.obtenerMensajeFalloRed(t));
                    }
                });
    }

    public void eliminarAlerta(Alerta alerta) {
        if (MODO_MOCK) {
            List<Alerta> actuales = misAlertas.getValue();
            if (actuales != null) {
                actuales.remove(alerta);
                misAlertas.setValue(actuales);
            }
            return;
        }

        try {
            long id = Long.parseLong(alerta.getId());
            ApiClient.getApiService(getApplication())
                    .eliminarAlerta(id)
                    .enqueue(new Callback<ApiResponse<Void>>() {
                        @Override
                        public void onResponse(@NonNull Call<ApiResponse<Void>> call,
                                               @NonNull Response<ApiResponse<Void>> response) {
                            if (response.isSuccessful()) {
                                cargarMisAlertas();
                            } else {
                                error.setValue(ManejadorErrores.obtenerMensajeAmigable(response, ContextoOperacion.ALERTAS));
                            }
                        }

                        @Override
                        public void onFailure(@NonNull Call<ApiResponse<Void>> call, @NonNull Throwable t) {
                            error.setValue(ManejadorErrores.obtenerMensajeFalloRed(t));
                        }
                    });
        } catch (NumberFormatException e) {
            error.setValue("El identificador de la alerta no es válido");
        }
    }

    // ══════════════════════════════════════════════════
    // FEED GENERAL
    // ══════════════════════════════════════════════════
    public void cargarTodasLasAlertas() {
        if (MODO_MOCK) {
            List<Alerta> mock = new ArrayList<>();

            Alerta a1 = new Alerta();
            a1.setId("101");
            a1.setTitulo("Incendio");
            a1.setDescripcion("Incendio estructural en edificio de 4 pisos. Llamas visibles en el tercer piso. Hay personas atrapadas.");
            a1.setEstado("EN_GESTION");
            a1.setPrioridad(PrioridadAlerta.ALTA);
            a1.setCategoria(CategoriaAlerta.INCENDIO);
            a1.setBarrio("Chapinero Alto");
            a1.setDireccion("Cra 15 #82-34");
            a1.setUsuarioNombre("María González");
            a1.setFechaActualizacion("2024-11-18 - 14:32");
            mock.add(a1);

            Alerta a2 = new Alerta();
            a2.setId("102");
            a2.setTitulo("Accidente");
            a2.setDescripcion("Colisión entre bus y camión. Al menos 3 heridos. Vía bloqueada totalmente.");
            a2.setEstado("DIRECCIONADA");
            a2.setPrioridad(PrioridadAlerta.ALTA);
            a2.setCategoria(CategoriaAlerta.ACCIDENTE);
            a2.setBarrio("Teusaquillo");
            a2.setDireccion("Av. NQS con Calle 68");
            a2.setUsuarioNombre("Carlos Ramírez");
            a2.setFechaActualizacion("2024-11-18 - 15:10");
            mock.add(a2);

            Alerta a3 = new Alerta();
            a3.setId("103");
            a3.setTitulo("Inundación");
            a3.setDescripcion("Fuerte inundación en el sector, el agua alcanza el nivel de las rodillas.");
            a3.setEstado("PENDIENTE_REVISION");
            a3.setPrioridad(PrioridadAlerta.ALTA);
            a3.setCategoria(CategoriaAlerta.INUNDACION);
            a3.setBarrio("Kennedy");
            a3.setDireccion("Calle 38 Sur #72-15");
            a3.setUsuarioNombre("Ana Torres");
            a3.setFechaActualizacion("2024-11-18 - 11:45");
            mock.add(a3);

            todasLasAlertas.setValue(mock);
            return;
        }

        ApiClient.getApiService(getApplication())
                .listarAlertas(null, null, null, null, null, 0, 20)
                .enqueue(new Callback<ApiResponse<PageAlertaResponse>>() {
                    @Override
                    public void onResponse(@NonNull Call<ApiResponse<PageAlertaResponse>> call,
                                           @NonNull Response<ApiResponse<PageAlertaResponse>> response) {
                        if (response.isSuccessful() && response.body() != null && response.body().getData() != null) {
                            todasLasAlertas.setValue(response.body().getData().getContent());
                        } else {
                            error.setValue(ManejadorErrores.obtenerMensajeAmigable(response, ContextoOperacion.ALERTAS));
                        }
                    }

                    @Override
                    public void onFailure(@NonNull Call<ApiResponse<PageAlertaResponse>> call, @NonNull Throwable t) {
                        error.setValue(ManejadorErrores.obtenerMensajeFalloRed(t));
                    }
                });
    }

    public void actualizarEstadoAlerta(Alerta alerta, String nuevoEstado) {
        if (MODO_MOCK) {
            alerta.setEstado(nuevoEstado);
            alerta.setFechaActualizacion(new SimpleDateFormat("yyyy-MM-dd - HH:mm", Locale.getDefault()).format(new Date()));
            cargarTodasLasAlertas();
            return;
        }
        // TODO: Modificar estado cuando la API exponga endpoint específico
    }
}
