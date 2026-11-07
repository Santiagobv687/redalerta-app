package com.redalerta.client.util;

import android.content.Context;
import android.location.Address;
import android.location.Geocoder;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.redalerta.client.model.alerta.UbicacionAlertaDto;

import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Utilidad para geocodificación inversa de coordenadas GPS utilizando {@link Geocoder} nativo,
 * compatible con Android 13+ (API 33 GeocodeListener asíncrono) y versiones anteriores (hilo en segundo plano).
 */
public class GeocodificacionHelper {

    public static final String DIRECCION_POR_DEFECTO_GPS = "Ubicación capturada por GPS";
    private static final int MAX_RESULTS = 1;
    private static final int MAX_DIRECCION_LENGTH = 200;

    private static final ExecutorService IO_EXECUTOR = Executors.newSingleThreadExecutor();
    private static final Handler MAIN_HANDLER = new Handler(Looper.getMainLooper());

    public static class ResultadoGeocodificacion {
        private final UbicacionAlertaDto ubicacionDto;
        private final boolean exitoGeocoder;
        private final String mensajeInformativo;

        public ResultadoGeocodificacion(UbicacionAlertaDto ubicacionDto,
                                        boolean exitoGeocoder,
                                        String mensajeInformativo) {
            this.ubicacionDto = ubicacionDto;
            this.exitoGeocoder = exitoGeocoder;
            this.mensajeInformativo = mensajeInformativo;
        }

        public UbicacionAlertaDto getUbicacionDto() {
            return ubicacionDto;
        }

        public boolean isExitoGeocoder() {
            return exitoGeocoder;
        }

        public String getMensajeInformativo() {
            return mensajeInformativo;
        }
    }

    public interface OnGeocodificacionListener {
        void onResultado(@NonNull ResultadoGeocodificacion resultado);
    }

    /**
     * Obtiene Departamento, Municipio y Dirección detallada a partir de las coordenadas GPS.
     * Si no hay conexión o Geocoder no está disponible, retorna las coordenadas con respaldo seguro
     * y un mensaje informativo para que el usuario confirme el municipio manualmente.
     */
    public static void obtenerUbicacionDesdeCoordenadas(
            @NonNull Context context,
            double latitud,
            double longitud,
            @Nullable Double altitud,
            @Nullable String deptoRespaldo,
            @Nullable String municipioRespaldo,
            @NonNull OnGeocodificacionListener listener) {

        final Context appContext = context.getApplicationContext();
        final String fallbackDepto = (deptoRespaldo != null && !deptoRespaldo.trim().isEmpty())
                ? deptoRespaldo.trim()
                : "Quindío";
        final String fallbackMun = (municipioRespaldo != null && !municipioRespaldo.trim().isEmpty())
                ? municipioRespaldo.trim()
                : "Armenia";

        // 1. Verificar disponibilidad del servicio Geocoder en el dispositivo
        if (!Geocoder.isPresent()) {
            entregarRespaldoSinConexion(
                    latitud,
                    longitud,
                    altitud,
                    fallbackDepto,
                    fallbackMun,
                    "El servicio de geocodificación no está disponible en este dispositivo. Coordenadas GPS guardadas; por favor confirme el municipio manualmente.",
                    listener
            );
            return;
        }

        Geocoder geocoder = new Geocoder(appContext, new Locale("es", "CO"));

        // 2. Android 13+ (API 33 - TIRAMISU): usar GeocodeListener asíncrono
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            try {
                geocoder.getFromLocation(latitud, longitud, MAX_RESULTS, new Geocoder.GeocodeListener() {
                    @Override
                    public void onGeocode(@NonNull List<Address> addresses) {
                        MAIN_HANDLER.post(() -> procesarListaDirecciones(
                                addresses,
                                latitud,
                                longitud,
                                altitud,
                                fallbackDepto,
                                fallbackMun,
                                listener
                        ));
                    }

                    @Override
                    public void onError(@Nullable String errorMessage) {
                        MAIN_HANDLER.post(() -> entregarRespaldoSinConexion(
                                latitud,
                                longitud,
                                altitud,
                                fallbackDepto,
                                fallbackMun,
                                "Sin conexión al servicio de direcciones. Coordenadas GPS capturadas; por favor verifique o seleccione el municipio manualmente.",
                                listener
                        ));
                    }
                });
            } catch (Exception e) {
                entregarRespaldoSinConexion(
                        latitud,
                        longitud,
                        altitud,
                        fallbackDepto,
                        fallbackMun,
                        "No fue posible consultar la dirección automáticamente. Coordenadas GPS capturadas; por favor confirme el municipio manualmente.",
                        listener
                );
            }
        } else {
            // 3. Versiones anteriores (< API 33): ejecutar versión síncrona en hilo en segundo plano (IO)
            IO_EXECUTOR.execute(() -> {
                try {
                    @SuppressWarnings("deprecation")
                    List<Address> addresses = geocoder.getFromLocation(latitud, longitud, MAX_RESULTS);
                    MAIN_HANDLER.post(() -> procesarListaDirecciones(
                            addresses,
                            latitud,
                            longitud,
                            altitud,
                            fallbackDepto,
                            fallbackMun,
                            listener
                    ));
                } catch (Exception e) {
                    MAIN_HANDLER.post(() -> entregarRespaldoSinConexion(
                            latitud,
                            longitud,
                            altitud,
                            fallbackDepto,
                            fallbackMun,
                            "Sin conexión para obtener la dirección automática. Coordenadas GPS capturadas; por favor confirme el municipio manualmente.",
                            listener
                    ));
                }
            });
        }
    }

    private static void procesarListaDirecciones(
            @Nullable List<Address> addresses,
            double latitud,
            double longitud,
            @Nullable Double altitud,
            @NonNull String fallbackDepto,
            @NonNull String fallbackMun,
            @NonNull OnGeocodificacionListener listener) {

        if (addresses == null || addresses.isEmpty() || addresses.get(0) == null) {
            entregarRespaldoSinConexion(
                    latitud,
                    longitud,
                    altitud,
                    fallbackDepto,
                    fallbackMun,
                    "Coordenadas GPS registradas. No se encontró dirección exacta para este punto; por favor confirme el municipio manualmente.",
                    listener
            );
            return;
        }

        Address address = addresses.get(0);

        // 1. Extraer Departamento de address.adminArea
        String rawAdminArea = address.getAdminArea();
        String departamento = UbicacionCatalogoHelper.buscarDepartamentoCanonico(rawAdminArea);
        if (departamento == null || departamento.trim().isEmpty()) {
            departamento = fallbackDepto;
        }

        // 2. Extraer Municipio preferiblemente de address.locality o address.subAdminArea
        String rawMunicipio = address.getLocality();
        if (rawMunicipio == null || rawMunicipio.trim().isEmpty()) {
            rawMunicipio = address.getSubAdminArea();
        }
        String municipio = UbicacionCatalogoHelper.buscarMunicipioCanonico(departamento, rawMunicipio);
        boolean municipioDetectado = (municipio != null && !municipio.trim().isEmpty());
        if (!municipioDetectado) {
            municipio = fallbackMun;
        }

        // 3. Extraer Dirección detallada de address.getAddressLine(0) o thoroughfare + featureName
        String direccionDetallada = extraerDireccionDetallada(address);

        UbicacionAlertaDto dto = new UbicacionAlertaDto(
                departamento,
                municipio,
                direccionDetallada,
                latitud,
                longitud,
                altitud
        );

        String mensaje = municipioDetectado
                ? "Ubicación detectada: " + municipio + ", " + departamento
                : "Coordenadas capturadas. Por favor confirme el municipio en el selector.";

        listener.onResultado(new ResultadoGeocodificacion(dto, municipioDetectado, mensaje));
    }

    @NonNull
    private static String extraerDireccionDetallada(@NonNull Address address) {
        String direccion = null;

        // Opción 1: thoroughfare + featureName si están presentes para una dirección más limpia
        String via = address.getThoroughfare() != null ? address.getThoroughfare().trim() : "";
        String numero = address.getFeatureName() != null ? address.getFeatureName().trim() : "";

        // Opción 2: getAddressLine(0)
        String lineaCompleta = address.getAddressLine(0) != null ? address.getAddressLine(0).trim() : "";

        if (!lineaCompleta.isEmpty()) {
            direccion = lineaCompleta;
        } else if (!via.isEmpty()) {
            if (!numero.isEmpty() && !via.contains(numero)) {
                direccion = via + " # " + numero;
            } else {
                direccion = via;
            }
        }

        if (direccion == null || direccion.trim().isEmpty()) {
            return DIRECCION_POR_DEFECTO_GPS;
        }

        direccion = direccion.trim();
        if (direccion.length() > MAX_DIRECCION_LENGTH) {
            direccion = direccion.substring(0, MAX_DIRECCION_LENGTH).trim();
        }
        return direccion;
    }

    private static void entregarRespaldoSinConexion(
            double latitud,
            double longitud,
            @Nullable Double altitud,
            @NonNull String fallbackDepto,
            @NonNull String fallbackMun,
            @NonNull String mensajeInformativo,
            @NonNull OnGeocodificacionListener listener) {

        UbicacionAlertaDto dto = new UbicacionAlertaDto(
                fallbackDepto,
                fallbackMun,
                DIRECCION_POR_DEFECTO_GPS,
                latitud,
                longitud,
                altitud
        );
        listener.onResultado(new ResultadoGeocodificacion(dto, false, mensajeInformativo));
    }
}
