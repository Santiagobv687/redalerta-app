package com.redalerta.client.viewmodel;

import android.app.Application;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.redalerta.client.model.alerta.*;
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

    // Se conecta directamente al backend real de Spring Boot en /api/v1/alertas
    private static final boolean MODO_MOCK = false;

    private final MutableLiveData<Boolean> cargando = new MutableLiveData<>(false);
    private final MutableLiveData<String> error = new MutableLiveData<>();
    private final MutableLiveData<Alerta> alertaCreada = new MutableLiveData<>();
    private final MutableLiveData<List<Alerta>> misAlertas = new MutableLiveData<>();
    private final MutableLiveData<List<Alerta>> todasLasAlertas = new MutableLiveData<>();

    // Filtros de búsqueda para el endpoint /alertas
    private final MutableLiveData<String> departamentoFiltro = new MutableLiveData<>("Quindío");
    private final MutableLiveData<String> municipioFiltro = new MutableLiveData<>("Armenia");
    private final MutableLiveData<String> estadoFiltro = new MutableLiveData<>(null);

    public AlertaViewModel(@NonNull Application application) {
        super(application);
    }

    public LiveData<Boolean> getCargando() { return cargando; }
    public LiveData<String> getError() { return error; }
    public LiveData<Alerta> getAlertaCreada() { return alertaCreada; }
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
                alerta.setUsuarioNombre(request.isEsAnonima() ? null : "Usuario Demo");
                alerta.setFechaCreacion(
                        new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", new Locale("es", "CO")).format(new Date())
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
    // MIS ALERTAS
    // ══════════════════════════════════════════════════
    public void cargarMisAlertas() {
        cargando.setValue(true);
        error.setValue(null);

        if (MODO_MOCK) {
            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                cargando.setValue(false);
                List<Alerta> mock = new ArrayList<>();

                Alerta a1 = new Alerta();
                a1.setId("1");
                a1.setTitulo("Inundación");
                a1.setDescripcion("Alcantarilla desbordada; el agua está entrando a varias viviendas.");
                a1.setEstado("PENDIENTE_REVISION");
                a1.setDireccion("Calle 48 #12-30");
                a1.setBarrio("San Luis");
                a1.setUsuarioNombre("Carlos Ruiz");
                a1.setFechaActualizacion("2024-11-20T10:24:00");
                mock.add(a1);

                misAlertas.setValue(mock);
            }, 600);
            return;
        }

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

        if (MODO_MOCK) {
            final String finalMun = mun;
            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                cargando.setValue(false);

                List<Alerta> mock = new ArrayList<>();

                Alerta a1 = new Alerta();
                a1.setId("101");
                a1.setTitulo("Incendio Estructural en Edificio");
                a1.setDescripcion("Incendio de gran magnitud reportado en el tercer piso. Llamas visibles y humo denso. Bomberos en camino al lugar.");
                a1.setEstado("EN_GESTION");
                a1.setPrioridad(PrioridadAlerta.ALTA);
                a1.setCategoria(CategoriaAlerta.INCENDIO);
                a1.setBarrio("Armenia");
                a1.setDireccion("Cra 15 #82-34, Barrio Chapinero");
                a1.setUsuarioNombre("María González");
                a1.setFechaActualizacion("2024-11-18T14:32:00");
                a1.setEvidenciaUrl("https://picsum.photos/600/338");
                a1.setTotalEvidencias(3);
                mock.add(a1);

                Alerta a2 = new Alerta();
                a2.setId("102");
                a2.setTitulo("Accidente de Tránsito Múltiple");
                a2.setDescripcion("Colisión entre bus de servicio público y vehículo particular. Personas lesionadas requieren atención médica urgente.");
                a2.setEstado("DIRECCIONADA");
                a2.setPrioridad(PrioridadAlerta.ALTA);
                a2.setCategoria(CategoriaAlerta.ACCIDENTE);
                a2.setBarrio("Armenia");
                a2.setDireccion("Av. Bolivar con Calle 10 Norte");
                a2.setUsuarioNombre("Carlos Ramírez");
                a2.setFechaActualizacion("2024-11-18T15:10:00");
                mock.add(a2);

                Alerta a3 = new Alerta();
                a3.setId("103");
                a3.setTitulo("Desbordamiento de Quebrada e Inundación");
                a3.setDescripcion("Fuerte corriente por lluvias torrenciales desbordó el cauce. Varias viviendas afectadas por ingreso de lodo.");
                a3.setEstado("PENDIENTE_REVISION");
                a3.setPrioridad(PrioridadAlerta.ALTA);
                a3.setCategoria(CategoriaAlerta.INUNDACION);
                a3.setBarrio("Calarcá");
                a3.setDireccion("Sector La Bella, Calle 38 Sur");
                a3.setUsuarioNombre("Ana Torres");
                a3.setFechaActualizacion("2024-11-18T11:45:00");
                a3.setEvidenciaUrl("https://picsum.photos/600/339");
                a3.setTotalEvidencias(1);
                mock.add(a3);

                List<Alerta> filtradas = new ArrayList<>();
                for (Alerta a : mock) {
                    boolean coincideMunicipio = (finalMun == null || (a.getBarrio() != null && a.getBarrio().equalsIgnoreCase(finalMun)));
                    boolean coincideEstado = (estado == null || (a.getEstado() != null && a.getEstado().equalsIgnoreCase(estado)));
                    if (coincideMunicipio && coincideEstado) {
                        filtradas.add(a);
                    }
                }
                todasLasAlertas.setValue(filtradas);
            }, 600);
            return;
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

    public void actualizarEstadoAlerta(Alerta alerta, String nuevoEstado) {
        if (MODO_MOCK) {
            alerta.setEstado(nuevoEstado);
            alerta.setFechaActualizacion(new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault()).format(new Date()));
            cargarTodasLasAlertas();
            return;
        }
        // Endpoint futuro para actualización de estado
    }
}
