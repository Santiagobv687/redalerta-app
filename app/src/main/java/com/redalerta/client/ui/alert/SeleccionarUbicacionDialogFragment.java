package com.redalerta.client.ui.alert;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.DialogFragment;

import com.redalerta.client.R;
import com.redalerta.client.databinding.DialogSeleccionarUbicacionBinding;
import com.redalerta.client.model.alerta.UbicacionAlertaDto;
import com.redalerta.client.util.GeocodificacionHelper;

import org.osmdroid.config.Configuration;
import org.osmdroid.events.MapListener;
import org.osmdroid.events.ScrollEvent;
import org.osmdroid.events.ZoomEvent;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.CustomZoomButtonsController;

import java.util.Locale;

/**
 * Ventana superpuesta a pantalla completa para elegir el punto exacto del incidente en el mapa.
 * No navega a otra pantalla: se muestra sobre el formulario y, al confirmar o cancelar, lo deja intacto.
 * El resultado se entrega con la Fragment Result API bajo {@link #RESULT_KEY}.
 */
public class SeleccionarUbicacionDialogFragment extends DialogFragment {

    public static final String TAG = "SeleccionarUbicacionDialog";
    public static final String RESULT_KEY = "seleccionar_ubicacion_result";
    public static final String RESULT_LATITUD = "latitud";
    public static final String RESULT_LONGITUD = "longitud";

    private static final String ARG_LATITUD = "arg_latitud";
    private static final String ARG_LONGITUD = "arg_longitud";
    private static final String ARG_DEPARTAMENTO = "arg_departamento";
    private static final String ARG_MUNICIPIO = "arg_municipio";

    private static final double ZOOM_INICIAL = 18.0;
    private static final long GEOCODER_DEBOUNCE_MS = 700L;
    private static final float PUNTERO_REPOSO_DP = -22f;
    private static final float PUNTERO_ARRASTRE_DP = -30f;

    private DialogSeleccionarUbicacionBinding binding;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private Runnable geocodificacionPendiente;
    private int solicitudGeocoder = 0;

    private String departamento = "Quindío";
    private String municipio = "Armenia";

    private ActivityResultLauncher<String[]> permisoUbicacionLauncher;

    public static SeleccionarUbicacionDialogFragment newInstance(double latitud,
                                                                 double longitud,
                                                                 @Nullable String departamento,
                                                                 @Nullable String municipio) {
        SeleccionarUbicacionDialogFragment fragment = new SeleccionarUbicacionDialogFragment();
        Bundle args = new Bundle();
        args.putDouble(ARG_LATITUD, latitud);
        args.putDouble(ARG_LONGITUD, longitud);
        args.putString(ARG_DEPARTAMENTO, departamento);
        args.putString(ARG_MUNICIPIO, municipio);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setStyle(STYLE_NORMAL, R.style.Theme_RedAlerta_FullScreenDialog);

        Bundle args = requireArguments();
        if (args.getString(ARG_DEPARTAMENTO) != null) departamento = args.getString(ARG_DEPARTAMENTO);
        if (args.getString(ARG_MUNICIPIO) != null) municipio = args.getString(ARG_MUNICIPIO);

        permisoUbicacionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestMultiplePermissions(),
                result -> {
                    boolean fine = Boolean.TRUE.equals(result.get(Manifest.permission.ACCESS_FINE_LOCATION));
                    boolean coarse = Boolean.TRUE.equals(result.get(Manifest.permission.ACCESS_COARSE_LOCATION));
                    if (fine || coarse) {
                        centrarEnUbicacionDelDispositivo();
                    } else {
                        Toast.makeText(requireContext(),
                                R.string.mapa_selector_permiso_gps_denegado,
                                Toast.LENGTH_LONG).show();
                    }
                }
        );
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        Context ctx = requireContext().getApplicationContext();
        Configuration.getInstance().load(ctx, PreferenceManager.getDefaultSharedPreferences(ctx));
        Configuration.getInstance().setUserAgentValue(requireContext().getPackageName());

        binding = DialogSeleccionarUbicacionBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        Bundle args = requireArguments();
        double latInicial = args.getDouble(ARG_LATITUD);
        double lonInicial = args.getDouble(ARG_LONGITUD);

        binding.mapViewSeleccion.setTileSource(TileSourceFactory.MAPNIK);
        binding.mapViewSeleccion.setMultiTouchControls(true);
        binding.mapViewSeleccion.getZoomController().setVisibility(CustomZoomButtonsController.Visibility.NEVER);
        binding.mapViewSeleccion.getController().setZoom(ZOOM_INICIAL);
        binding.mapViewSeleccion.getController().setCenter(new GeoPoint(latInicial, lonInicial));

        actualizarCoordenadas(latInicial, lonInicial);
        binding.tvDireccionSeleccion.setText(
                getString(R.string.alerta_municipio_detectado, municipio, departamento));
        programarGeocodificacion(latInicial, lonInicial);

        // Levantar el puntero mientras el usuario arrastra para dar retroalimentación de precisión
        binding.mapViewSeleccion.setOnTouchListener((v, event) -> {
            int action = event.getActionMasked();
            if (action == MotionEvent.ACTION_DOWN) {
                animarPuntero(true);
            } else if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
                animarPuntero(false);
            }
            return false;
        });

        binding.mapViewSeleccion.addMapListener(new MapListener() {
            @Override
            public boolean onScroll(ScrollEvent event) {
                if (binding != null) {
                    GeoPoint centro = (GeoPoint) binding.mapViewSeleccion.getMapCenter();
                    actualizarCoordenadas(centro.getLatitude(), centro.getLongitude());
                    programarGeocodificacion(centro.getLatitude(), centro.getLongitude());
                }
                return false;
            }

            @Override
            public boolean onZoom(ZoomEvent event) {
                return false;
            }
        });

        binding.toolbarSeleccionUbicacion.setNavigationOnClickListener(v -> dismiss());
        binding.btnCancelarSeleccion.setOnClickListener(v -> dismiss());
        binding.btnConfirmarSeleccion.setOnClickListener(v -> confirmarUbicacion());

        binding.fabZoomMas.setOnClickListener(v -> binding.mapViewSeleccion.getController().zoomIn());
        binding.fabZoomMenos.setOnClickListener(v -> binding.mapViewSeleccion.getController().zoomOut());
        binding.fabMiGpsSeleccion.setOnClickListener(v -> solicitarUbicacionDelDispositivo());
    }

    private void confirmarUbicacion() {
        if (binding == null) return;
        GeoPoint centro = (GeoPoint) binding.mapViewSeleccion.getMapCenter();
        Bundle resultado = new Bundle();
        resultado.putDouble(RESULT_LATITUD, redondear(centro.getLatitude()));
        resultado.putDouble(RESULT_LONGITUD, redondear(centro.getLongitude()));
        getParentFragmentManager().setFragmentResult(RESULT_KEY, resultado);
        dismiss();
    }

    private void animarPuntero(boolean arrastrando) {
        if (binding == null) return;
        float density = getResources().getDisplayMetrics().density;
        binding.ivPunteroSeleccion.animate()
                .translationY((arrastrando ? PUNTERO_ARRASTRE_DP : PUNTERO_REPOSO_DP) * density)
                .setDuration(150)
                .start();
    }

    private void actualizarCoordenadas(double latitud, double longitud) {
        if (binding == null) return;
        binding.tvCoordenadasSeleccion.setText(
                String.format(Locale.US, "Lat: %.6f, Lon: %.6f", latitud, longitud));
    }

    /** Espera a que el usuario deje de mover el mapa antes de consultar el geocodificador. */
    private void programarGeocodificacion(double latitud, double longitud) {
        if (geocodificacionPendiente != null) {
            handler.removeCallbacks(geocodificacionPendiente);
        }
        geocodificacionPendiente = () -> consultarDireccion(latitud, longitud);
        handler.postDelayed(geocodificacionPendiente, GEOCODER_DEBOUNCE_MS);
    }

    private void consultarDireccion(double latitud, double longitud) {
        if (!isAdded() || binding == null) return;
        final int idSolicitud = ++solicitudGeocoder;
        binding.tvDireccionSeleccion.setText(R.string.mapa_selector_consultando_direccion);

        GeocodificacionHelper.obtenerUbicacionDesdeCoordenadas(
                requireContext(), latitud, longitud, null, departamento, municipio,
                resultado -> {
                    // Descartar respuestas obsoletas si el usuario siguió moviendo el mapa
                    if (!isAdded() || binding == null || idSolicitud != solicitudGeocoder) return;

                    UbicacionAlertaDto dto = resultado.getUbicacionDto();
                    if (dto.getDepartamento() != null && !dto.getDepartamento().isEmpty()) {
                        departamento = dto.getDepartamento();
                    }
                    if (dto.getMunicipio() != null && !dto.getMunicipio().isEmpty()) {
                        municipio = dto.getMunicipio();
                    }

                    String direccion = dto.getDireccionDetallada();
                    boolean hayDireccion = resultado.isExitoGeocoder()
                            && direccion != null && !direccion.isEmpty();
                    binding.tvDireccionSeleccion.setText(hayDireccion
                            ? getString(R.string.mapa_selector_direccion_con_municipio, direccion, municipio)
                            : getString(R.string.alerta_municipio_detectado, municipio, departamento));
                }
        );
    }

    private void solicitarUbicacionDelDispositivo() {
        boolean fine = ContextCompat.checkSelfPermission(
                requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
        boolean coarse = ContextCompat.checkSelfPermission(
                requireContext(), Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED;
        if (fine || coarse) {
            centrarEnUbicacionDelDispositivo();
        } else {
            permisoUbicacionLauncher.launch(new String[]{
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
            });
        }
    }

    @SuppressLint("MissingPermission")
    private void centrarEnUbicacionDelDispositivo() {
        if (!isAdded() || binding == null) return;
        LocationManager lm = (LocationManager) requireContext().getSystemService(Context.LOCATION_SERVICE);
        Location ubicacion = null;
        try {
            if (lm != null && lm.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                ubicacion = lm.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            }
            if (ubicacion == null && lm != null && lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                ubicacion = lm.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
            }
        } catch (Exception ignored) {
            // Se informa al usuario más abajo
        }

        if (ubicacion != null) {
            binding.mapViewSeleccion.getController()
                    .animateTo(new GeoPoint(ubicacion.getLatitude(), ubicacion.getLongitude()));
        } else {
            Toast.makeText(requireContext(),
                    R.string.mapa_selector_sin_senal_gps,
                    Toast.LENGTH_LONG).show();
        }
    }

    private double redondear(double valor) {
        return Math.round(valor * 1_000_000.0) / 1_000_000.0;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (binding != null) binding.mapViewSeleccion.onResume();
    }

    @Override
    public void onPause() {
        if (binding != null) binding.mapViewSeleccion.onPause();
        super.onPause();
    }

    @Override
    public void onDestroyView() {
        if (geocodificacionPendiente != null) {
            handler.removeCallbacks(geocodificacionPendiente);
            geocodificacionPendiente = null;
        }
        solicitudGeocoder++;
        if (binding != null) binding.mapViewSeleccion.onDetach();
        binding = null;
        super.onDestroyView();
    }
}
