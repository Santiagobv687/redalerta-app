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
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
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

    // ══════════════════════════════════════════════════════
// Métodos para "Mis Publicaciones"
// ══════════════════════════════════════════════════════
    private final MutableLiveData<List<Alerta>> misAlertas = new MutableLiveData<>();

    public LiveData<List<Alerta>> getMisAlertas() { return misAlertas; }

    public void cargarMisAlertas() {
        if (MODO_MOCK) {
            // Simular lista de alertas del usuario
            List<Alerta> mock = new ArrayList<>();

            Alerta a1 = new Alerta();
            a1.setId("1");
            a1.setTitulo("Inundación");
            a1.setDescripcion("Alcantarilla desbordada; el agua está entrando a varias viviendas.");
            a1.setEstado("EN_REVISION");
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

        // TODO: Llamar al backend GET /api/alertas/mis-alertas
    }

    public void eliminarAlerta(Alerta alerta) {
        List<Alerta> actuales = misAlertas.getValue();
        if (actuales != null) {
            actuales.remove(alerta);
            misAlertas.setValue(actuales); // Notifica al observer
        }
        // TODO: Llamar al backend DELETE /api/alertas/{id}
    }

    // ══════════════════════════════════════════════════════
// Métodos para "Inicio" (Feed general)
// ══════════════════════════════════════════════════════
    private final MutableLiveData<List<Alerta>> todasLasAlertas = new MutableLiveData<>();

    public LiveData<List<Alerta>> getTodasLasAlertas() { return todasLasAlertas; }

    public void cargarTodasLasAlertas() {
        if (MODO_MOCK) {
            List<Alerta> mock = new ArrayList<>();

            Alerta a1 = new Alerta();
            a1.setId("ALR-2024-001");
            a1.setTitulo("Incendio");
            a1.setDescripcion("Incendio estructural en edificio de 4 pisos. Llamas visibles en el tercer piso. Hay personas atrapadas.");
            a1.setEstado("EN_ATENCION");
            a1.setPrioridad("ALTA");           // ⬅️ AGREGAR
            a1.setCategoria("INCENDIO");       // ⬅️ AGREGAR
            a1.setBarrio("Chapinero Alto");
            a1.setDireccion("Cra 15 #82-34");
            a1.setUsuarioNombre("María González");
            a1.setFechaActualizacion("2024-11-18 - 14:32");
            mock.add(a1);

            Alerta a2 = new Alerta();
            a2.setId("ALR-2024-002");
            a2.setTitulo("Accidente");
            a2.setDescripcion("Colisión entre bus y camión. Al menos 3 heridos. Vía bloqueada totalmente.");
            a2.setEstado("VERIFICADA");
            a2.setPrioridad("ALTA");           // ⬅️ AGREGAR
            a2.setCategoria("ACCIDENTE");      // ⬅️ AGREGAR
            a2.setBarrio("Teusaquillo");
            a2.setDireccion("Av. NQS con Calle 68");
            a2.setUsuarioNombre("Carlos Ramírez");
            a2.setFechaActualizacion("2024-11-18 - 15:10");
            mock.add(a2);

            Alerta a3 = new Alerta();
            a3.setId("ALR-2024-003");
            a3.setTitulo("Inundación");
            a3.setDescripcion("Fuerte inundación en el sector, el agua alcanza el nivel de las rodillas.");
            a3.setEstado("EN_REVISION");
            a3.setPrioridad("ALTA");           // ⬅️ AGREGAR
            a3.setCategoria("INUNDACION");     // ⬅️ AGREGAR
            a3.setBarrio("Kennedy");
            a3.setDireccion("Calle 38 Sur #72-15");
            a3.setUsuarioNombre("Ana Torres");
            a3.setFechaActualizacion("2024-11-18 - 11:45");
            mock.add(a3);

            // ⬇️ Opcional: una rechazada para probar el filtro de cerradas
            Alerta a4 = new Alerta();
            a4.setId("ALR-2024-004");
            a4.setTitulo("Robo / Hurto");
            a4.setDescripcion("Atraco a mano armada en tienda de conveniencia.");
            a4.setEstado("RECHAZADA");
            a4.setPrioridad("MEDIA");
            a4.setCategoria("ROBO_HURTO");
            a4.setBarrio("La Candelaria");
            a4.setDireccion("Cra 7 #22-15");
            a4.setUsuarioNombre("Carlos Ruiz");
            a4.setFechaActualizacion("2024-11-18 - 09:20");
            mock.add(a4);

            todasLasAlertas.setValue(mock);
            return;
        }
        // TODO: Llamar al backend GET /api/alertas
    }

    public void actualizarEstadoAlerta(Alerta alerta, String nuevoEstado) {
        if (MODO_MOCK) {
            alerta.setEstado(nuevoEstado);
            // Opcional: actualizar fecha
            alerta.setFechaActualizacion(new java.text.SimpleDateFormat("yyyy-MM-dd - HH:mm", java.util.Locale.getDefault()).format(new java.util.Date()));
            cargarTodasLasAlertas(); // Recargar la lista para reflejar el cambio
            return;
        }
        // TODO: Llamar al backend PATCH /api/alertas/{id}/estado
    }
}