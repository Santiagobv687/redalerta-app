package com.redalerta.client.ui.alert;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.Editable;
import android.text.TextWatcher;
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
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.android.material.snackbar.Snackbar;
import com.redalerta.client.R;
import com.redalerta.client.databinding.FragmentRegistroAlertaBinding;
import com.redalerta.client.model.alerta.CrearAlertaRequest;
import com.redalerta.client.model.alerta.UbicacionAlertaDto;
import com.redalerta.client.util.GeocodificacionHelper;
import com.redalerta.client.util.SessionManager;
import com.redalerta.client.util.UbicacionCatalogoHelper;
import com.redalerta.client.viewmodel.AlertaViewModel;

import org.osmdroid.config.Configuration;
import org.osmdroid.events.MapListener;
import org.osmdroid.events.ScrollEvent;
import org.osmdroid.events.ZoomEvent;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.CustomZoomButtonsController;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class RegistroAlertaFragment extends Fragment {

    // Coordenadas por defecto de respaldo (Armenia, Quindío - ejemplo OpenAPI)
    private static final double DEFAULT_LATITUD = 4.533889;
    private static final double DEFAULT_LONGITUD = -75.681111;
    private static final double DEFAULT_ALTITUD = 1483.0;
    private static final double DEFAULT_ZOOM = 17.0;

    private FragmentRegistroAlertaBinding binding;
    private SessionManager session;
    private AlertaViewModel viewModel;

    // Estado de coordenadas GPS confirmadas para el payload UbicacionAlertaDto
    private double latitudConfirmada = DEFAULT_LATITUD;
    private double longitudConfirmada = DEFAULT_LONGITUD;
    @Nullable
    private Double altitudConfirmada = DEFAULT_ALTITUD;
    private boolean modoSeleccionMapaActivo = false;

    private LocationManager locationManager;
    private LocationListener activeLocationListener;
    private ActivityResultLauncher<String[]> locationPermissionLauncher;

    // Gestión de evidencias fotográficas seleccionadas
    private final List<EvidenciasSeleccionadasAdapter.EvidenciaItem> listaEvidencias = new ArrayList<>();
    private EvidenciasSeleccionadasAdapter evidenciasAdapter;
    private ActivityResultLauncher<String> pickerLauncher;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        Context ctx = requireContext().getApplicationContext();
        Configuration.getInstance().load(ctx, PreferenceManager.getDefaultSharedPreferences(ctx));
        Configuration.getInstance().setUserAgentValue(requireContext().getPackageName());

        binding = FragmentRegistroAlertaBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        session = new SessionManager(requireContext());
        viewModel = new ViewModelProvider(requireActivity()).get(AlertaViewModel.class);
        locationManager = (LocationManager) requireContext().getSystemService(Context.LOCATION_SERVICE);

        setupToolbar();
        setupUbicacionSelectores();
        setupMapaInteractivo();
        setupPermisosYGps();
        setupEvidencias();
        setupTextWatchers();
        observarViewModel();

        actualizarReportadoPor();
        validarEstadoBotonPublicar();

        // Obtener ubicación GPS automáticamente al abrir el formulario
        solicitarUbicacionGpsActual();
    }

    private void setupToolbar() {
        binding.toolbarReportarAlerta.setNavigationOnClickListener(v ->
                requireActivity().getSupportFragmentManager().popBackStack()
        );
    }

    private void setupUbicacionSelectores() {
        UbicacionCatalogoHelper.configurarSelectoresEncadenados(
                requireContext(),
                binding.tilDepartamento,
                binding.etDepartamento,
                binding.tilMunicipio,
                binding.etMunicipio,
                this::validarEstadoBotonPublicar
        );

        String deptoSesion = session.getDepartamento() != null ? session.getDepartamento() : "Quindío";
        String munSesion = session.getMunicipio() != null ? session.getMunicipio() : "Armenia";

        if (binding.etDepartamento.getText().toString().isEmpty()) {
            binding.etDepartamento.setText(deptoSesion, false);
        }
        if (binding.etMunicipio.getText().toString().isEmpty()) {
            binding.etMunicipio.setText(munSesion, false);
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private void setupMapaInteractivo() {
        binding.mapViewUbicacion.setTileSource(TileSourceFactory.MAPNIK);
        binding.mapViewUbicacion.setMultiTouchControls(true);
        binding.mapViewUbicacion.getZoomController().setVisibility(CustomZoomButtonsController.Visibility.SHOW_AND_FADEOUT);
        binding.mapViewUbicacion.getController().setZoom(DEFAULT_ZOOM);

        GeoPoint puntoInicial = new GeoPoint(latitudConfirmada, longitudConfirmada);
        binding.mapViewUbicacion.getController().setCenter(puntoInicial);
        actualizarTextoCoordenadas(latitudConfirmada, longitudConfirmada, altitudConfirmada);

        // Cuando está en modo selección, evitar que el NestedScrollView robe los gestos de desplazamiento del mapa
        binding.mapViewUbicacion.setOnTouchListener((v, event) -> {
            if (modoSeleccionMapaActivo) {
                int action = event.getActionMasked();
                if (action == MotionEvent.ACTION_DOWN || action == MotionEvent.ACTION_MOVE) {
                    binding.nestedScrollRegistroAlerta.requestDisallowInterceptTouchEvent(true);
                } else if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
                    binding.nestedScrollRegistroAlerta.requestDisallowInterceptTouchEvent(false);
                }
                return false;
            }
            return true;
        });

        // Actualizar la vista previa de coordenadas en tiempo real mientras el usuario mueve el mapa
        binding.mapViewUbicacion.addMapListener(new MapListener() {
            @Override
            public boolean onScroll(ScrollEvent event) {
                if (modoSeleccionMapaActivo && binding != null) {
                    GeoPoint centro = (GeoPoint) binding.mapViewUbicacion.getMapCenter();
                    actualizarTextoCoordenadas(centro.getLatitude(), centro.getLongitude(), null);
                }
                return false;
            }

            @Override
            public boolean onZoom(ZoomEvent event) {
                return false;
            }
        });

        // Botón "Seleccionar en el mapa": activa el modo de corrección manual del puntero
        binding.btnSeleccionarEnMapa.setOnClickListener(v -> activarModoSeleccionMapa(true));

        // Botón "Cancelar": descarta el movimiento del puntero y restaura la coordenada previa
        binding.btnCancelarMapa.setOnClickListener(v -> {
            activarModoSeleccionMapa(false);
            centrarMapaEnCoordenadas(latitudConfirmada, longitudConfirmada);
            actualizarTextoCoordenadas(latitudConfirmada, longitudConfirmada, altitudConfirmada);
        });

        // Botón "Guardar": confirma la nueva posición del puntero y ejecuta geocodificación inversa
        binding.btnGuardarMapa.setOnClickListener(v -> {
            GeoPoint nuevoCentro = (GeoPoint) binding.mapViewUbicacion.getMapCenter();
            latitudConfirmada = redondearCoordenada(nuevoCentro.getLatitude());
            longitudConfirmada = redondearCoordenada(nuevoCentro.getLongitude());
            // Al mover manualmente el puntero en el mapa, la altitud del sensor ya no aplica al nuevo punto
            altitudConfirmada = null;

            activarModoSeleccionMapa(false);
            actualizarTextoCoordenadas(latitudConfirmada, longitudConfirmada, null);
            ejecutarGeocodificacionInversa(latitudConfirmada, longitudConfirmada, null);
        });

        // Botón "Mi GPS": vuelve a capturar la posición del GPS del dispositivo
        binding.btnUsarMiGps.setOnClickListener(v -> {
            if (modoSeleccionMapaActivo) {
                activarModoSeleccionMapa(false);
            }
            solicitarUbicacionGpsActual();
        });
    }

    private void activarModoSeleccionMapa(boolean activar) {
        modoSeleccionMapaActivo = activar;
        if (binding == null) return;

        // Mostrar u ocultar la capa que bloquea el mapa en vista previa
        binding.viewBloqueoMapa.setVisibility(activar ? View.GONE : View.VISIBLE);
        binding.tvBannerModoEdicionMapa.setVisibility(activar ? View.VISIBLE : View.GONE);

        // Alternar entre "Seleccionar en el mapa" y la dupla "Cancelar" / "Guardar"
        binding.btnSeleccionarEnMapa.setVisibility(activar ? View.GONE : View.VISIBLE);
        binding.layoutBotonesEdicionMapa.setVisibility(activar ? View.VISIBLE : View.GONE);

        // Elevar ligeramente el puntero para dar feedback visual de arrastre
        float density = getResources().getDisplayMetrics().density;
        binding.ivPunteroMapa.animate()
                .translationY(activar ? (-24f * density) : (-18f * density))
                .setDuration(150)
                .start();
    }

    private void setupPermisosYGps() {
        locationPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestMultiplePermissions(),
                result -> {
                    Boolean fineGranted = result.getOrDefault(Manifest.permission.ACCESS_FINE_LOCATION, false);
                    Boolean coarseGranted = result.getOrDefault(Manifest.permission.ACCESS_COARSE_LOCATION, false);
                    if (Boolean.TRUE.equals(fineGranted) || Boolean.TRUE.equals(coarseGranted)) {
                        obtenerCoordenadasDesdeLocationManager();
                    } else {
                        if (binding != null) {
                            binding.tvEstadoGeocoder.setText(
                                    "Permiso de GPS no concedido. Puede usar 'Seleccionar en el mapa' para ubicar el incidente."
                            );
                        }
                        ejecutarGeocodificacionInversa(latitudConfirmada, longitudConfirmada, altitudConfirmada);
                    }
                }
        );
    }

    private void solicitarUbicacionGpsActual() {
        if (!isAdded()) return;

        boolean fineGranted = ContextCompat.checkSelfPermission(
                requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
        boolean coarseGranted = ContextCompat.checkSelfPermission(
                requireContext(), Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED;

        if (fineGranted || coarseGranted) {
            obtenerCoordenadasDesdeLocationManager();
        } else {
            locationPermissionLauncher.launch(new String[]{
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
            });
        }
    }

    @SuppressLint("MissingPermission")
    private void obtenerCoordenadasDesdeLocationManager() {
        if (locationManager == null || binding == null) return;

        binding.tvEstadoGeocoder.setText("Obteniendo señal GPS del dispositivo…");

        try {
            boolean gpsHabilitado = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
            boolean redHabilitada = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);

            Location ultimaUbicacion = null;
            if (gpsHabilitado) {
                ultimaUbicacion = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            }
            if (ultimaUbicacion == null && redHabilitada) {
                ultimaUbicacion = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
            }

            if (ultimaUbicacion != null) {
                aplicarUbicacionGpsObtenida(ultimaUbicacion);
            } else {
                // Mientras llega un fix en vivo, posicionar en las coordenadas de respaldo y geocodificar
                centrarMapaEnCoordenadas(latitudConfirmada, longitudConfirmada);
                ejecutarGeocodificacionInversa(latitudConfirmada, longitudConfirmada, altitudConfirmada);
            }

            // Solicitar actualización fresca del sensor GPS
            String proveedorActivo = gpsHabilitado
                    ? LocationManager.GPS_PROVIDER
                    : (redHabilitada ? LocationManager.NETWORK_PROVIDER : null);

            if (proveedorActivo != null) {
                detenerActualizacionesUbicacion();
                activeLocationListener = new LocationListener() {
                    @Override
                    public void onLocationChanged(@NonNull Location location) {
                        detenerActualizacionesUbicacion();
                        if (!modoSeleccionMapaActivo && isAdded()) {
                            aplicarUbicacionGpsObtenida(location);
                        }
                    }

                    @Override public void onProviderEnabled(@NonNull String provider) {}
                    @Override public void onProviderDisabled(@NonNull String provider) {}
                    @Override public void onStatusChanged(String provider, int status, Bundle extras) {}
                };
                locationManager.requestLocationUpdates(proveedorActivo, 0L, 0f, activeLocationListener);
            }
        } catch (Exception e) {
            ejecutarGeocodificacionInversa(latitudConfirmada, longitudConfirmada, altitudConfirmada);
        }
    }

    private void aplicarUbicacionGpsObtenida(@NonNull Location location) {
        latitudConfirmada = redondearCoordenada(location.getLatitude());
        longitudConfirmada = redondearCoordenada(location.getLongitude());
        altitudConfirmada = location.hasAltitude()
                ? Math.round(location.getAltitude() * 10.0) / 10.0
                : DEFAULT_ALTITUD;

        centrarMapaEnCoordenadas(latitudConfirmada, longitudConfirmada);
        actualizarTextoCoordenadas(latitudConfirmada, longitudConfirmada, altitudConfirmada);
        ejecutarGeocodificacionInversa(latitudConfirmada, longitudConfirmada, altitudConfirmada);
    }

    private void centrarMapaEnCoordenadas(double latitud, double longitud) {
        if (binding == null) return;
        GeoPoint punto = new GeoPoint(latitud, longitud);
        binding.mapViewUbicacion.getController().animateTo(punto);
    }

    private void actualizarTextoCoordenadas(double latitud, double longitud, @Nullable Double altitud) {
        if (binding == null) return;
        String textoCoord = String.format(Locale.US, "Lat: %.6f, Lon: %.6f", latitud, longitud);
        if (altitud != null) {
            textoCoord += String.format(Locale.US, " • Alt: %.0f m", altitud);
        }
        binding.tvCoordenadasGps.setText(textoCoord);
    }

    private void ejecutarGeocodificacionInversa(double latitud, double longitud, @Nullable Double altitud) {
        if (!isAdded() || binding == null) return;

        binding.tvEstadoGeocoder.setText("Consultando departamento, municipio y dirección…");
        binding.tvEstadoGeocoder.setTextColor(ContextCompat.getColor(requireContext(), R.color.ra_light_text_secondary));

        String deptoActual = binding.etDepartamento.getText() != null
                ? binding.etDepartamento.getText().toString().trim()
                : session.getDepartamento();
        String munActual = binding.etMunicipio.getText() != null
                ? binding.etMunicipio.getText().toString().trim()
                : session.getMunicipio();

        GeocodificacionHelper.obtenerUbicacionDesdeCoordenadas(
                requireContext(),
                latitud,
                longitud,
                altitud,
                deptoActual,
                munActual,
                resultado -> {
                    if (!isAdded() || binding == null) return;

                    UbicacionAlertaDto dto = resultado.getUbicacionDto();

                    // Actualizar Departamento y Municipio si se obtuvieron
                    if (dto.getDepartamento() != null && !dto.getDepartamento().isEmpty()) {
                        binding.etDepartamento.setText(dto.getDepartamento(), false);
                    }
                    if (dto.getMunicipio() != null && !dto.getMunicipio().isEmpty()) {
                        binding.etMunicipio.setText(dto.getMunicipio(), false);
                    }

                    // Actualizar Dirección detallada con lo extraído por Geocoder o el texto de respaldo GPS
                    if (dto.getDireccionDetallada() != null && !dto.getDireccionDetallada().isEmpty()) {
                        binding.etDireccionDetallada.setText(dto.getDireccionDetallada());
                    }

                    binding.tvEstadoGeocoder.setText(resultado.getMensajeInformativo());
                    int colorTexto = resultado.isExitoGeocoder()
                            ? ContextCompat.getColor(requireContext(), R.color.ra_success_card_text)
                            : ContextCompat.getColor(requireContext(), R.color.ra_status_revision_text);
                    binding.tvEstadoGeocoder.setTextColor(colorTexto);

                    limpiarErrorCampo(binding.tilDepartamento);
                    limpiarErrorCampo(binding.tilMunicipio);
                    limpiarErrorCampo(binding.tilDireccionDetallada);
                    validarEstadoBotonPublicar();
                }
        );
    }

    private double redondearCoordenada(double valor) {
        return Math.round(valor * 1_000_000.0) / 1_000_000.0;
    }

    private void detenerActualizacionesUbicacion() {
        if (locationManager != null && activeLocationListener != null) {
            try {
                locationManager.removeUpdates(activeLocationListener);
            } catch (Exception ignored) {}
            activeLocationListener = null;
        }
    }

    private void setupEvidencias() {
        evidenciasAdapter = new EvidenciasSeleccionadasAdapter(listaEvidencias, position -> {
            if (position >= 0 && position < listaEvidencias.size()) {
                listaEvidencias.remove(position);
                evidenciasAdapter.notifyDataSetChanged();
                validarEstadoBotonPublicar();
            }
        });

        binding.rvEvidencias.setLayoutManager(new LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false));
        binding.rvEvidencias.setAdapter(evidenciasAdapter);

        // Launcher de galería de imágenes
        pickerLauncher = registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
            if (uri != null) {
                adjuntarYSubirEvidencia(uri);
            }
        });

        binding.btnAgregarFoto.setOnClickListener(v -> {
            if (listaEvidencias.size() >= 5) {
                Toast.makeText(requireContext(), "Máximo 5 evidencias fotográficas permitidas", Toast.LENGTH_SHORT).show();
                return;
            }
            pickerLauncher.launch("image/*");
        });
    }

    private void adjuntarYSubirEvidencia(Uri uri) {
        EvidenciasSeleccionadasAdapter.EvidenciaItem item = new EvidenciasSeleccionadasAdapter.EvidenciaItem(uri);
        item.setSubiendo(true);
        listaEvidencias.add(item);
        int pos = listaEvidencias.size() - 1;
        evidenciasAdapter.notifyItemInserted(pos);
        validarEstadoBotonPublicar();

        binding.tvEstadoSubidaFotos.setText("Subiendo evidencia fotográfica...");

        viewModel.subirEvidencia(uri, new AlertaViewModel.OnEvidenciaSubidaListener() {
            @Override
            public void onExito(String urlSubida) {
                item.setSubiendo(false);
                item.setUrlSubida(urlSubida);
                evidenciasAdapter.notifyItemChanged(pos);
                binding.tvEstadoSubidaFotos.setText("Foto subida exitosamente.");
                validarEstadoBotonPublicar();
            }

            @Override
            public void onError(String mensajeError) {
                item.setSubiendo(false);
                listaEvidencias.remove(item);
                evidenciasAdapter.notifyDataSetChanged();
                binding.tvEstadoSubidaFotos.setText("No fue posible subir la imagen. Intente nuevamente.");
                Toast.makeText(requireContext(), mensajeError, Toast.LENGTH_SHORT).show();
                validarEstadoBotonPublicar();
            }
        });
    }

    private void setupTextWatchers() {
        TextWatcher watcher = new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                limpiarErrorCampo(binding.tilDescripcion);
                limpiarErrorCampo(binding.tilDepartamento);
                limpiarErrorCampo(binding.tilMunicipio);
                limpiarErrorCampo(binding.tilDireccionDetallada);
                validarEstadoBotonPublicar();
            }
            @Override public void afterTextChanged(Editable s) {}
        };

        binding.etDescripcion.addTextChangedListener(watcher);
        binding.etDireccionDetallada.addTextChangedListener(watcher);
        binding.cbAnonimo.setOnCheckedChangeListener((btn, checked) -> {
            actualizarReportadoPor();
            validarEstadoBotonPublicar();
        });

        binding.btnRegistrarAlerta.setOnClickListener(v -> procesarPublicarAlerta());
    }

    private void limpiarErrorCampo(com.google.android.material.textfield.TextInputLayout til) {
        if (til != null) {
            til.setError(null);
            til.setErrorEnabled(false);
        }
    }

    private void actualizarReportadoPor() {
        boolean esAnonimo = binding.cbAnonimo.isChecked();
        String fecha = new SimpleDateFormat("dd/MM/yyyy, h:mm a", Locale.getDefault()).format(new Date());
        if (esAnonimo) {
            binding.tvReportadoPor.setText("Reportado por: Anónimo — " + fecha);
        } else {
            String nombre = session.getNombre() != null ? session.getNombre() : "Usuario Ciudadano";
            binding.tvReportadoPor.setText("Reportado por: " + nombre + " — " + fecha);
        }
    }

    private void validarEstadoBotonPublicar() {
        if (binding == null) return;

        String desc = binding.etDescripcion.getText() != null ? binding.etDescripcion.getText().toString().trim() : "";
        String depto = binding.etDepartamento.getText() != null ? binding.etDepartamento.getText().toString().trim() : "";
        String mun = binding.etMunicipio.getText() != null ? binding.etMunicipio.getText().toString().trim() : "";

        boolean descValida = desc.length() >= 10 && desc.length() <= 1000;
        boolean ubicacionValida = !depto.isEmpty() && !mun.isEmpty();

        boolean subiendoFoto = false;
        for (EvidenciasSeleccionadasAdapter.EvidenciaItem item : listaEvidencias) {
            if (item.isSubiendo()) {
                subiendoFoto = true;
                break;
            }
        }

        binding.btnRegistrarAlerta.setEnabled(descValida && ubicacionValida && !subiendoFoto && !modoSeleccionMapaActivo);
    }

    private void procesarPublicarAlerta() {
        String desc = binding.etDescripcion.getText().toString().trim();
        String depto = binding.etDepartamento.getText().toString().trim();
        String mun = binding.etMunicipio.getText().toString().trim();
        String dir = binding.etDireccionDetallada.getText().toString().trim();

        if (desc.length() < 10) {
            binding.tilDescripcion.setError("La descripción debe tener mínimo 10 caracteres");
            return;
        }
        limpiarErrorCampo(binding.tilDescripcion);

        if (depto.isEmpty()) {
            binding.tilDepartamento.setError("Seleccione departamento");
            return;
        }
        limpiarErrorCampo(binding.tilDepartamento);

        if (mun.isEmpty()) {
            binding.tilMunicipio.setError("Seleccione municipio");
            return;
        }
        limpiarErrorCampo(binding.tilMunicipio);

        if (dir.isEmpty()) {
            dir = GeocodificacionHelper.DIRECCION_POR_DEFECTO_GPS;
            binding.etDireccionDetallada.setText(dir);
        }
        limpiarErrorCampo(binding.tilDireccionDetallada);

        // Recopilar URLs de evidencias subidas
        List<String> evidenciasUrls = new ArrayList<>();
        for (EvidenciasSeleccionadasAdapter.EvidenciaItem item : listaEvidencias) {
            if (item.getUrlSubida() != null && !item.getUrlSubida().isEmpty()) {
                evidenciasUrls.add(item.getUrlSubida());
            }
        }

        // Construir DTO de ubicación conforme al esquema UbicacionAlertaDto en OpenAPI
        UbicacionAlertaDto ubicacionDto = new UbicacionAlertaDto(
                depto,
                mun,
                dir,
                latitudConfirmada,
                longitudConfirmada,
                altitudConfirmada
        );

        CrearAlertaRequest request = new CrearAlertaRequest();
        request.setDescripcion(desc);
        request.setUbicacion(ubicacionDto);
        request.setEvidenciasUrls(evidenciasUrls);
        request.setEsAnonimo(binding.cbAnonimo.isChecked());

        // Deshabilitar botón y procesar envío
        binding.btnRegistrarAlerta.setEnabled(false);
        viewModel.crearAlerta(request);
    }

    private void observarViewModel() {
        viewModel.getCargando().observe(getViewLifecycleOwner(), cargando -> {
            if (cargando != null) {
                binding.btnRegistrarAlerta.setEnabled(!cargando);
                binding.btnRegistrarAlerta.setText(cargando ? "Publicando alerta..." : "Publicar Alerta");
            }
        });

        viewModel.getError().observe(getViewLifecycleOwner(), msg -> {
            binding.btnRegistrarAlerta.setEnabled(true);
            if (msg != null && !msg.trim().isEmpty()) {
                Snackbar.make(binding.getRoot(), msg, Snackbar.LENGTH_LONG).show();
            }
        });

        viewModel.getAlertaCreada().observe(getViewLifecycleOwner(), alerta -> {
            if (alerta != null) {
                Toast.makeText(requireContext(), "Alerta publicada exitosamente", Toast.LENGTH_SHORT).show();
                requireActivity().getSupportFragmentManager().popBackStack();
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        if (binding != null) {
            binding.mapViewUbicacion.onResume();
        }
    }

    @Override
    public void onPause() {
        if (binding != null) {
            binding.mapViewUbicacion.onPause();
        }
        super.onPause();
    }

    @Override
    public void onDestroyView() {
        detenerActualizacionesUbicacion();
        if (binding != null) {
            binding.mapViewUbicacion.onDetach();
        }
        super.onDestroyView();
        binding = null;
    }
}
